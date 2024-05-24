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

import static media.mexm.mediadeepa.exportformat.DataGraphic.THIN_STROKE;
import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.CONTAINER;
import static media.mexm.mediadeepa.exportformat.report.StatisticsUnitValueReportEntry.createFromFloat;
import static media.mexm.mediadeepa.exportformat.report.StatisticsUnitValueReportEntry.createFromInteger;

import java.awt.Color;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

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
import media.mexm.mediadeepa.exportformat.report.NumericUnitValueReportEntry;
import media.mexm.mediadeepa.exportformat.report.ReportDocument;
import media.mexm.mediadeepa.exportformat.report.ReportSection;
import media.mexm.mediadeepa.rendererengine.GraphicRendererEngine;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.SingleGraphicDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.SingleTabularDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeAudioFrame;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeBaseFrame;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserResult;

@Component
public class AFramesRendererEngine implements
								   ReportRendererEngine,
								   TableRendererEngine,
								   TabularRendererEngine,
								   GraphicRendererEngine,
								   ConstStrings,
								   SingleTabularDocumentExporterTraits,
								   SingleGraphicDocumentExporterTraits {

	@Autowired
	private AppConfig appConfig;
	@Autowired
	private AppCommand appCommand;
	@Autowired
	private NumberUtils numberUtils;

	public static final List<String> HEAD_AFRAMES = List.of(
			STREAM_INDEX,
			NB_SAMPLES,
			PTS,
			PTS_TIME,
			PKT_DTS,
			PKT_DTS_TIME,
			BEST_EFFORT_TIMESTAMP,
			BEST_EFFORT_TIMESTAMP_TIME,
			PKT_DURATION,
			PKT_DURATION_TIME,
			PKT_POS,
			PKT_SIZE);

	@Override
	public String getSingleUniqTabularDocumentBaseFileName() {
		return "container-audio-frames";
	}

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		return result.getContainerAnalyserResult()
				.map(caResult -> {
					final var aFrames = new TabularDocument(tabularExportFormat,
							getSingleUniqTabularDocumentBaseFileName()).head(
									HEAD_AFRAMES);

					caResult.audioFrames().forEach(r -> {
						final var frame = r.frame();
						aFrames.row(
								frame.streamIndex(),
								r.nbSamples(),
								frame.pts(),
								frame.ptsTime(),
								frame.pktDts(),
								frame.pktDtsTime(),
								frame.bestEffortTimestamp(),
								frame.bestEffortTimestampTime(),
								frame.duration(),
								frame.durationTime(),
								frame.pktPos(),
								frame.pktSize());
					});
					return aFrames;
				})
				.stream()
				.toList();
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		result.getContainerAnalyserResult()
				.ifPresent(caResult -> {
					final var aFrames = tableDocument.createTable("Container audio frames").head(HEAD_AFRAMES);
					caResult.audioFrames().forEach(r -> {
						final var frame = r.frame();
						aFrames.addRow()
								.addCell(frame.streamIndex())
								.addCell(r.nbSamples())
								.addCell(frame.pts())
								.addCell(frame.ptsTime())
								.addCell(frame.pktDts())
								.addCell(frame.pktDtsTime())
								.addCell(frame.bestEffortTimestamp())
								.addCell(frame.bestEffortTimestampTime())
								.addCell(frame.duration())
								.addCell(frame.durationTime())
								.addCell(frame.pktPos())
								.addCell(frame.pktSize());
					});
				});
	}

	@Override
	public List<GraphicArtifact> toGraphic(final DataResult result) {
		final var audioBReport = result.getContainerAnalyserResult()
				.map(ContainerAnalyserResult::audioFrames)
				.stream()
				.flatMap(List::stream)
				.toList();
		if (audioBReport.isEmpty()) {
			return List.of();
		}

		final var firstStreamIndex = audioBReport.stream()
				.map(FFprobeAudioFrame::frame)
				.map(FFprobeBaseFrame::streamIndex)
				.findFirst().orElseThrow(() -> new IllegalArgumentException("Can't found audio stream index"));

		final var dataGraphic = TimedDataGraphic.create(
				audioBReport.stream()
						.map(FFprobeAudioFrame::frame)
						.filter(f -> f.streamIndex() == firstStreamIndex)
						.map(FFprobeBaseFrame::ptsTime),
				RangeAxis.createFromRelativesValueSet(
						"Audio packet/frame size (bytes)", 0,
						audioBReport.stream()
								.map(FFprobeAudioFrame::frame)
								.map(FFprobeBaseFrame::pktSize)));

		final var allStreamIndexes = audioBReport.stream()
				.map(FFprobeAudioFrame::frame)
				.map(FFprobeBaseFrame::streamIndex)
				.distinct()
				.sorted()
				.toList();
		final var colorSpliter = 1f / allStreamIndexes.size();

		allStreamIndexes.forEach(streamIndex -> dataGraphic
				.addSeries(dataGraphic.new Series(
						"Stream #" + streamIndex + " (audio)",
						Color.getHSBColor(allStreamIndexes.indexOf(streamIndex) * colorSpliter, 1, 1),
						THIN_STROKE,
						audioBReport.stream()
								.map(FFprobeAudioFrame::frame)
								.filter(f -> f.streamIndex() == streamIndex)
								.map(FFprobeBaseFrame::pktSize))));

		return List.of(
				new GraphicArtifact(
						getSingleUniqGraphicBaseFileName(),
						dataGraphic.addMinMaxValueMarkers().makeLinearAxisGraphic(numberUtils),
						appConfig.getGraphicConfig().getImageSizeHalfSize()));
	}

	@Override
	public String getSingleUniqGraphicBaseFileName() {
		return appConfig.getGraphicConfig().getABitrateGraphicFilename();
	}

	@Override
	public void addToReport(final DataResult result, final ReportDocument document) {
		result.getContainerAnalyserResult()
				.map(ContainerAnalyserResult::audioFrames)
				.flatMap(Optional::ofNullable)
				.filter(Predicate.not(List::isEmpty))
				.ifPresent(audioFrames -> {
					final var section = new ReportSection(CONTAINER, AUDIO_FRAMES);
					section.add(new NumericUnitValueReportEntry(COUNT, audioFrames.size(), FRAMES));
					section.add(createFromInteger(
							FRAME_SIZE,
							audioFrames.stream()
									.map(FFprobeAudioFrame::frame)
									.map(FFprobeBaseFrame::pktSize), BYTES, numberUtils));

					section.add(createFromInteger(
							FRAME_LENGTH,
							audioFrames.stream()
									.map(FFprobeAudioFrame::nbSamples), SAMPLES, numberUtils));

					section.add(createFromFloat(
							FRAME_DURATION,
							audioFrames.stream()
									.map(FFprobeAudioFrame::frame)
									.map(FFprobeBaseFrame::durationTime)
									.filter(f -> f > 0f)
									.filter(f -> f.isNaN() == false)
									.map(d -> d * 1000f), MILLISECONDS, numberUtils::formatDecimalFull1En));

					addAllGraphicsToReport(this, result, section, appConfig, appCommand);
					document.add(section);
				});
	}

}
