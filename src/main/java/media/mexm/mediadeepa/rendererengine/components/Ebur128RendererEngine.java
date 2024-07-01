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
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static java.util.function.Predicate.not;
import static media.mexm.mediadeepa.exportformat.DataGraphic.THICK_STROKE;
import static media.mexm.mediadeepa.exportformat.DataGraphic.THIN_STROKE;
import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.AUDIO;
import static tv.hd3g.fflauncher.recipes.MediaAnalyserProcessResult.R128_DEFAULT_LUFS_TARGET;

import java.awt.Color;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jfree.data.time.FixedMillisecond;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.cli.AppCommand;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.GraphicArtifact;
import media.mexm.mediadeepa.exportformat.RangeAxis;
import media.mexm.mediadeepa.exportformat.TableDocument;
import media.mexm.mediadeepa.exportformat.TabularDocument;
import media.mexm.mediadeepa.exportformat.TabularExportFormat;
import media.mexm.mediadeepa.exportformat.TimedDataGraphic;
import media.mexm.mediadeepa.exportformat.report.ReportDocument;
import media.mexm.mediadeepa.exportformat.report.ReportSection;
import media.mexm.mediadeepa.rendererengine.GraphicRendererEngine;
import media.mexm.mediadeepa.rendererengine.MultipleGraphicDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.SingleGraphicMaker;
import media.mexm.mediadeepa.rendererengine.SingleTabularDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.Ebur128RendererEngine.EBUR128ReportItem;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMetadataFilterParser;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdR128;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdValue;
import tv.hd3g.fflauncher.filtering.lavfimtd.Stereo;
import tv.hd3g.fflauncher.recipes.MediaAnalyserProcessResult;

@Component
public class Ebur128RendererEngine implements
								   TableRendererEngine,
								   TabularRendererEngine,
								   GraphicRendererEngine,
								   ReportRendererEngine,
								   ConstStrings,
								   SingleTabularDocumentExporterTraits,
								   MultipleGraphicDocumentExporterTraits<EBUR128ReportItem> {

	@Autowired
	private AppConfig appConfig;
	@Autowired
	private AppCommand appCommand;
	@Autowired
	private NumberUtils numberUtils;

	private List<SingleGraphicMaker<EBUR128ReportItem>> graphicMakerList;

	public static final List<String> HEAD_EBUR128 = List.of(
			POSITION,
			INTEGRATED,
			MOMENTARY,
			SHORT_TERM,
			LOUDNESS_RANGE,
			SAMPLE_PEAK_L, SAMPLE_PEAK_R,
			TRUE_PEAK_L, TRUE_PEAK_R);

	@Override
	public String getSingleUniqTabularDocumentBaseFileName() {
		return "audio-ebur128";
	}

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		final var t = new TabularDocument(tabularExportFormat, getSingleUniqTabularDocumentBaseFileName()).head(
				HEAD_EBUR128);
		result.getMediaAnalyserProcessResult()
				.map(MediaAnalyserProcessResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getR128Report)
				.stream()
				.flatMap(List::stream)
				.forEach(ebu -> t.row(
						tabularExportFormat.formatNumberLowPrecision(ebu.ptsTime()),
						tabularExportFormat.formatNumberLowPrecision(ebu.value().integrated()),
						tabularExportFormat.formatNumberLowPrecision(ebu.value().momentary()),
						tabularExportFormat.formatNumberLowPrecision(ebu.value().shortTerm()),
						tabularExportFormat.formatNumberLowPrecision(ebu.value().loudnessRange()),
						tabularExportFormat.formatNumberLowPrecision(ebu.value().samplePeaks().left()),
						tabularExportFormat.formatNumberLowPrecision(ebu.value().samplePeaks().right()),
						tabularExportFormat.formatNumberLowPrecision(ebu.value().truePeaks().left()),
						tabularExportFormat.formatNumberLowPrecision(ebu.value().truePeaks().right())));
		return List.of(t);
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		final var t = tableDocument.createTable("EBU R 128").head(HEAD_EBUR128);
		result.getMediaAnalyserProcessResult()
				.map(MediaAnalyserProcessResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getR128Report)
				.stream()
				.flatMap(List::stream)
				.forEach(ebu -> t.addRow()
						.addCell(ebu.ptsTime())
						.addCell(ebu.value().integrated())
						.addCell(ebu.value().momentary())
						.addCell(ebu.value().shortTerm())
						.addCell(ebu.value().loudnessRange())
						.addCell(ebu.value().samplePeaks().left())
						.addCell(ebu.value().samplePeaks().right())
						.addCell(ebu.value().truePeaks().left())
						.addCell(ebu.value().truePeaks().right()));
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		graphicMakerList = List.of(new LUFSGraphicMaker(), new TPKGraphicMaker());
	}

	static record EBUR128ReportItem(List<LavfiMtdR128> r128events,
									LavfiMtdR128 summary,
									List<FixedMillisecond> positions,
									float target) {
	}

	@Override
	public List<SingleGraphicMaker<EBUR128ReportItem>> getGraphicMakerList() {
		return graphicMakerList;
	}

	@Override
	public Optional<EBUR128ReportItem> makeGraphicReportItem(final DataResult result) {
		return result.getMediaAnalyserProcessResult()
				.map(MediaAnalyserProcessResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getR128Report)
				.filter(not(List::isEmpty))
				.map(r128Report -> new EBUR128ReportItem(
						r128Report.stream()
								.map(LavfiMtdValue::value)
								.toList(),
						r128Report.get(r128Report.size() - 1).value(),
						r128Report.stream()
								.map(LavfiMtdValue::ptsTime)
								.map(t -> t * 1000f)
								.map(Math::ceil)
								.map(Math::round)
								.map(FixedMillisecond::new)
								.toList(),
						result.getMediaAnalyserProcessResult()
								.flatMap(MediaAnalyserProcessResult::r128Target)
								.orElse(R128_DEFAULT_LUFS_TARGET)));
	}

	class LUFSGraphicMaker implements SingleGraphicMaker<EBUR128ReportItem> {

		@Override
		public String getBaseFileName() {
			return appConfig.getGraphicConfig().getLufsGraphicFilename();
		}

		@Override
		public GraphicArtifact makeGraphic(final EBUR128ReportItem item) {
			final var dataGraphicLUFS = new TimedDataGraphic(
					item.positions,
					RangeAxis.createFromValueSet("dB LU", -40, 10, 1,
							item.r128events.stream()
									.flatMap(event -> Stream.of(
											event.integrated(),
											event.momentary(),
											event.shortTerm(),
											item.target()))));

			dataGraphicLUFS.addSeries(dataGraphicLUFS.new Series(
					"Integrated",
					BLUE,
					THICK_STROKE,
					item.r128events.stream().map(LavfiMtdR128::integrated)));
			dataGraphicLUFS.addSeries(dataGraphicLUFS.new Series(
					"Short term",
					GREEN.darker(),
					THIN_STROKE,
					item.r128events.stream().map(LavfiMtdR128::shortTerm)));
			dataGraphicLUFS.addSeries(dataGraphicLUFS.new Series(
					"Momentary",
					Color.getHSBColor(0.5f, 1f, 0.3f),
					THIN_STROKE,
					item.r128events.stream().map(LavfiMtdR128::momentary)));
			dataGraphicLUFS
					.addValueMarker(item.summary().integrated())
					.addValueMarker(item.summary().loudnessRangeHigh())
					.addValueMarker(item.summary().loudnessRangeLow());
			return new GraphicArtifact(
					getBaseFileName(),
					dataGraphicLUFS.makeLogarithmicAxisGraphic(numberUtils),
					appConfig.getGraphicConfig().getImageSizeFullSize());
		}

	}

	class TPKGraphicMaker implements SingleGraphicMaker<EBUR128ReportItem> {

		@Override
		public String getBaseFileName() {
			return appConfig.getGraphicConfig().getLufsTPKGraphicFilename();
		}

		@Override
		public GraphicArtifact makeGraphic(final EBUR128ReportItem item) {
			final var dataGraphicTPK = new TimedDataGraphic(
					item.positions,
					RangeAxis.createFromValueSet("dB LU",
							item.summary.integrated(), 10, 1,
							item.r128events.stream()
									.flatMap(event -> Stream.of(
											event.truePeaks().left(),
											event.truePeaks().right(),
											event.samplePeaks().left(),
											event.samplePeaks().right()))));

			dataGraphicTPK.addSeries(dataGraphicTPK.new Series(
					"True peak left (per frame)", BLUE, THIN_STROKE,
					item.r128events.stream().map(LavfiMtdR128::truePeaks).map(Stereo::left)));
			dataGraphicTPK.addSeries(dataGraphicTPK.new Series(
					"True peak right (per frame)", RED, THICK_STROKE,
					item.r128events.stream().map(LavfiMtdR128::truePeaks).map(Stereo::right)));
			dataGraphicTPK
					.addValueMarker(item.summary.truePeak())
					.addValueMarker(item.summary().samplePeak())
					.addValueMarker(-3)
					.addValueMarker(item.r128events.stream()
							.flatMap(event -> Stream.of(
									event.truePeaks().left(),
									event.truePeaks().right(),
									event.samplePeaks().left(),
									event.samplePeaks().right()))
							.mapToDouble(Float::doubleValue)
							.summaryStatistics().getAverage());
			return new GraphicArtifact(
					appConfig.getGraphicConfig().getLufsTPKGraphicFilename(),
					dataGraphicTPK.makeLogarithmicAxisGraphic(numberUtils),
					appConfig.getGraphicConfig().getImageSizeFullSize());
		}

	}

	@Override
	public void addToReport(final DataResult result, final ReportDocument document) {
		final var section = new ReportSection(AUDIO, LOUDNESS_EBU_R128);
		addAllGraphicsToReport(this, result, section, appConfig, appCommand);
		document.add(section);

	}

}
