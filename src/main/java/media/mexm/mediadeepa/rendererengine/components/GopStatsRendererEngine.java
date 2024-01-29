/*
 * This file is part of mediadeepa.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2023
 *
 */
package media.mexm.mediadeepa.rendererengine.components;

import static java.awt.Color.BLUE;
import static java.awt.Color.GRAY;
import static java.awt.Color.RED;
import static java.util.function.Predicate.not;
import static media.mexm.mediadeepa.exportformat.DataGraphic.THIN_STROKE;
import static media.mexm.mediadeepa.exportformat.RangeAxis.createAutomaticRangeAxis;
import static media.mexm.mediadeepa.exportformat.ReportEntrySubset.toEntrySubset;
import static media.mexm.mediadeepa.exportformat.ReportSectionCategory.CONTAINER;
import static media.mexm.mediadeepa.exportformat.StatisticsUnitValueReportEntry.createFromInteger;
import static media.mexm.mediadeepa.exportformat.StatisticsUnitValueReportEntry.createFromLong;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.GraphicArtifact;
import media.mexm.mediadeepa.exportformat.NumericUnitValueReportEntry;
import media.mexm.mediadeepa.exportformat.ReportDocument;
import media.mexm.mediadeepa.exportformat.ReportSection;
import media.mexm.mediadeepa.exportformat.SeriesStyle;
import media.mexm.mediadeepa.exportformat.SimpleKeyValueReportEntry;
import media.mexm.mediadeepa.exportformat.StackedXYAreaChartDataGraphic;
import media.mexm.mediadeepa.exportformat.TableDocument;
import media.mexm.mediadeepa.exportformat.TabularDocument;
import media.mexm.mediadeepa.exportformat.TabularExportFormat;
import media.mexm.mediadeepa.rendererengine.GraphicRendererEngine;
import media.mexm.mediadeepa.rendererengine.MultipleGraphicDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.SingleGraphicMaker;
import media.mexm.mediadeepa.rendererengine.SingleTabularDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.GopStatsRendererEngine.GOPReportItem;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeBaseFrame;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobePictType;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeVideoFrame;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserResult;
import tv.hd3g.fflauncher.recipes.GOPStatItem;

@Component
public class GopStatsRendererEngine implements
									ReportRendererEngine,
									TableRendererEngine,
									TabularRendererEngine,
									GraphicRendererEngine,
									ConstStrings,
									SingleTabularDocumentExporterTraits,
									MultipleGraphicDocumentExporterTraits<GOPReportItem> {

	@Autowired
	private AppConfig appConfig;
	@Autowired
	private NumberUtils numberUtils;

	private List<SingleGraphicMaker<GOPReportItem>> graphicMakerList;

	public static final List<String> HEAD_GOPSTATS = List.of(
			GOP_FRAME_COUNT,
			P_FRAMES_COUNT,
			B_FRAMES_COUNT,
			GOP_DATA_SIZE,
			I_FRAME_DATA_SIZE,
			P_FRAMES_DATA_SIZE,
			B_FRAMES_DATA_SIZE);

	@Override
	public String getSingleUniqTabularDocumentBaseFileName() {
		return "container-video-gop";
	}

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		return result.getContainerAnalyserResult()
				.map(caResult -> {
					final var gopStats = new TabularDocument(tabularExportFormat,
							getSingleUniqTabularDocumentBaseFileName())
									.head(HEAD_GOPSTATS);
					caResult.extractGOPStats()
							.forEach(f -> gopStats.row(
									f.gopFrameCount(),
									f.pFramesCount(),
									f.bFramesCount(),
									f.gopDataSize(),
									f.iFrameDataSize(),
									f.pFramesDataSize(),
									f.bFramesDataSize()));
					return gopStats;
				})
				.stream()
				.toList();
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		result.getContainerAnalyserResult()
				.ifPresent(caResult -> {
					final var gopStats = tableDocument.createTable("Container GOP").head(HEAD_GOPSTATS);
					caResult.extractGOPStats()
							.forEach(f -> gopStats.addRow()
									.addCell(f.gopFrameCount())
									.addCell(f.pFramesCount())
									.addCell(f.bFramesCount())
									.addCell(f.gopDataSize())
									.addCell(f.iFrameDataSize())
									.addCell(f.pFramesDataSize())
									.addCell(f.bFramesDataSize()));
				});
	}

	static record GOPReportItem(List<GOPStatItem> gopStatsReport) {
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		graphicMakerList = List.of(
				new GopWidthGraphicMaker(),
				new GopSizeGraphicMaker());
	}

	@Override
	public List<SingleGraphicMaker<GOPReportItem>> getGraphicMakerList() {
		return graphicMakerList;
	}

	@Override
	public Optional<GOPReportItem> makeGraphicReportItem(final DataResult result) {
		return result.getContainerAnalyserResult()
				.map(ContainerAnalyserResult::extractGOPStats)
				.filter(not(List::isEmpty))
				.map(GOPReportItem::new);
	}

	class GopWidthGraphicMaker implements SingleGraphicMaker<GOPReportItem> {

		@Override
		public String getBaseFileName() {
			return appConfig.getGraphicConfig().getGopCountGraphicFilename();
		}

		@Override
		public GraphicArtifact makeGraphic(final GOPReportItem item) {
			final var gopWidthDataGraphic = new StackedXYAreaChartDataGraphic(createAutomaticRangeAxis(
					NUMBER_OF_FRAMES));

			gopWidthDataGraphic.addValueMarker(item.gopStatsReport.stream()
					.mapToInt(GOPStatItem::gopFrameCount)
					.max()
					.orElse(0));

			gopWidthDataGraphic.addSeriesByCounter(new SeriesStyle(P_FRAME_COUNT_BY_GOP, BLUE, THIN_STROKE),
					item.gopStatsReport.stream().map(GOPStatItem::pFramesCount));
			gopWidthDataGraphic.addSeriesByCounter(new SeriesStyle(B_FRAME_COUNT_BY_GOP, RED.darker(), THIN_STROKE),
					item.gopStatsReport.stream().map(GOPStatItem::bFramesCount));
			gopWidthDataGraphic.addSeriesByCounter(new SeriesStyle(VIDEO_FRAME_COUNT_BY_GOP, GRAY, THIN_STROKE),
					item.gopStatsReport.stream().map(g -> g.gopFrameCount() - (g.bFramesCount() + g.pFramesCount())));

			return new GraphicArtifact(
					getBaseFileName(),
					gopWidthDataGraphic.makeLinearAxisGraphic(numberUtils),
					appConfig.getGraphicConfig().getImageSizeHalfSize());
		}

	}

	class GopSizeGraphicMaker implements SingleGraphicMaker<GOPReportItem> {
		@Override
		public String getBaseFileName() {
			return appConfig.getGraphicConfig().getGopSizeGraphicFilename();
		}

		@Override
		public GraphicArtifact makeGraphic(final GOPReportItem item) {
			final var gopSizeDataGraphic = new StackedXYAreaChartDataGraphic(createAutomaticRangeAxis(
					GOP_FRAME_SIZE_KBYTES));

			gopSizeDataGraphic.addValueMarker(item.gopStatsReport.stream()
					.mapToDouble(GOPStatItem::gopDataSize)
					.max()
					.stream()
					.map(d -> d / 1024d)
					.findFirst().orElse(0d));

			gopSizeDataGraphic.addSeriesByCounter(new SeriesStyle(P_FRAMES_SIZE_IN_GOP, BLUE, THIN_STROKE),
					item.gopStatsReport.stream().flatMap(g -> g.videoFrames().stream()
							.map(f -> g.pFramesDataSize() / 1024d)));
			gopSizeDataGraphic.addSeriesByCounter(new SeriesStyle(B_FRAMES_SIZE_IN_GOP, RED.darker(), THIN_STROKE),
					item.gopStatsReport.stream().flatMap(g -> g.videoFrames().stream()
							.map(f -> g.bFramesDataSize() / 1024d)));
			gopSizeDataGraphic.addSeriesByCounter(new SeriesStyle(I_FRAMES_SIZE_IN_GOP, GRAY, THIN_STROKE),
					item.gopStatsReport.stream().flatMap(g -> g.videoFrames().stream()
							.map(f -> (g.gopDataSize() - (g.bFramesDataSize() + g.pFramesDataSize())) / 1024d)));

			return new GraphicArtifact(
					getBaseFileName(),
					gopSizeDataGraphic.makeLinearAxisGraphic(numberUtils),
					appConfig.getGraphicConfig().getImageSizeFullSize());
		}
	}

	@Override
	public void addToReport(final DataResult result, final ReportDocument document) {
		result.getContainerAnalyserResult()
				.ifPresent(caResult -> saveGOPStats(caResult.extractGOPStats(), caResult.videoFrames(), document));
	}

	private void saveGOPStats(final List<GOPStatItem> extractGOPStats,
							  final List<FFprobeVideoFrame> videoFrames,
							  final ReportDocument document) {
		if (extractGOPStats.isEmpty()) {
			return;
		}
		final var frameCount = videoFrames.size();
		final var keyFrameCount = (int) videoFrames.stream()
				.map(FFprobeVideoFrame::frame)
				.filter(FFprobeBaseFrame::keyFrame)
				.count();
		final var allFramesSize = videoFrames.stream()
				.map(FFprobeVideoFrame::frame)
				.mapToLong(FFprobeBaseFrame::pktSize)
				.sum();
		final var iFrameCount = videoFrames.stream()
				.filter(f -> FFprobePictType.I.equals(f.pictType()))
				.count();

		final var section = new ReportSection(CONTAINER, VIDEO_COMPRESSION_GROUP_OF_PICTURES);

		/**
		 * GOP
		 */
		section.add(new NumericUnitValueReportEntry(COUNT, extractGOPStats.size(), "GOPs"));

		section.add(createFromInteger(GOPS_LENGTH,
				extractGOPStats.stream().map(GOPStatItem::gopFrameCount), FRAME_S, numberUtils));
		section.add(createFromLong(GOPS_SIZE,
				extractGOPStats.stream().map(GOPStatItem::gopDataSize), BYTES, numberUtils));

		/**
		 * I
		 */
		if (keyFrameCount > 0 && iFrameCount != keyFrameCount) {
			section.add(new NumericUnitValueReportEntry(I_FRAMES_COUNT, iFrameCount, FRAMES));

		}
		section.add(createFromLong(I_FRAMES_SIZE_IN_GOPS,
				extractGOPStats.stream().map(GOPStatItem::iFrameDataSize), BYTES, numberUtils));

		final var iFrameSize = videoFrames.stream()
				.filter(f -> FFprobePictType.I.equals(f.pictType()))
				.map(FFprobeVideoFrame::frame)
				.mapToLong(FFprobeBaseFrame::pktSize)
				.sum();
		section.add(new NumericUnitValueReportEntry(ALL_I_SIZE, iFrameSize, BYTES));

		/**
		 * P
		 */
		final var pFrameCount = videoFrames.stream()
				.filter(f -> FFprobePictType.P.equals(f.pictType()))
				.count();
		if (pFrameCount > 0) {
			section.add(new NumericUnitValueReportEntry("P frames count on media", pFrameCount, FRAMES));
			section.add(new NumericUnitValueReportEntry("P frames reparition count",
					Math.round(pFrameCount * 100f / frameCount), "%"));

			final var pFrameSize = videoFrames.stream()
					.filter(f -> FFprobePictType.P.equals(f.pictType()))
					.map(FFprobeVideoFrame::frame)
					.mapToLong(FFprobeBaseFrame::pktSize)
					.sum();
			section.add(new NumericUnitValueReportEntry("Size sum for all P frames", pFrameSize, BYTES));
			section.add(new NumericUnitValueReportEntry("All P frames reparition by size",
					Math.round(pFrameSize * 100f / allFramesSize), "%"));

			toEntrySubset(Stream.of(
					createFromLong("P frames size in GOPs",
							extractGOPStats.stream().map(GOPStatItem::pFramesDataSize), BYTES, numberUtils),
					createFromInteger("P frames length in GOPs",
							extractGOPStats.stream().map(GOPStatItem::pFramesCount), FRAME_S, numberUtils)),
					section);
		} else {
			section.add(new SimpleKeyValueReportEntry("P frame presence", "no P frames"));
		}

		/**
		 * B
		 */
		final var bFrameCount = videoFrames.stream()
				.filter(f -> FFprobePictType.B.equals(f.pictType()))
				.count();
		if (bFrameCount > 0) {
			section.add(new NumericUnitValueReportEntry("B frames count on media", bFrameCount, FRAMES));
			section.add(new NumericUnitValueReportEntry("B frames reparition count",
					Math.round(bFrameCount * 100f / frameCount), "%"));

			final var bFrameSize = videoFrames.stream()
					.filter(f -> FFprobePictType.B.equals(f.pictType()))
					.map(FFprobeVideoFrame::frame)
					.mapToLong(FFprobeBaseFrame::pktSize)
					.sum();
			section.add(new NumericUnitValueReportEntry("Size sum for all P frames", bFrameSize, BYTES));
			section.add(new NumericUnitValueReportEntry("All B frames reparition by size",
					Math.round(bFrameSize * 100f / allFramesSize), "%"));

			toEntrySubset(Stream.of(
					createFromLong("B frames size in GOPs",
							extractGOPStats.stream().map(GOPStatItem::bFramesDataSize), BYTES, numberUtils),
					createFromInteger("B frames length in GOPs",
							extractGOPStats.stream().map(GOPStatItem::bFramesCount), FRAME_S, numberUtils)),
					section);
		} else {
			section.add(new SimpleKeyValueReportEntry("B frame presence", "no B frames"));
		}

		document.add(section);
	}

}
