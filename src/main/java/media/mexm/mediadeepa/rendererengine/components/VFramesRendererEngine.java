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
import static media.mexm.mediadeepa.exportformat.DataGraphic.THIN_STROKE;
import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.CONTAINER;
import static media.mexm.mediadeepa.exportformat.report.StatisticsUnitValueReportEntry.createFromDouble;
import static media.mexm.mediadeepa.exportformat.report.StatisticsUnitValueReportEntry.createFromFloat;
import static tv.hd3g.fflauncher.ffprobecontainer.FFprobeCodecType.VIDEO;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
import media.mexm.mediadeepa.exportformat.report.StatisticsUnitValueReportEntry;
import media.mexm.mediadeepa.rendererengine.GraphicRendererEngine;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.SingleGraphicDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.SingleTabularDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeBaseFrame;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeVideoFrame;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserResult;

@Component
public class VFramesRendererEngine implements
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

	public static final List<String> HEAD_VFRAMES = List.of(
			STREAM_INDEX,
			KEY_FRAME,
			PICT_TYPE,
			REPEAT_PICT,
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
		return "container-video-frames";
	}

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		return result.getContainerAnalyserResult()
				.map(caResult -> {
					final var vFrames = new TabularDocument(tabularExportFormat,
							getSingleUniqTabularDocumentBaseFileName())
									.head(HEAD_VFRAMES);
					caResult.videoFrames().forEach(r -> {
						final var frame = r.frame();
						vFrames.row(
								frame.streamIndex(),
								frame.keyFrame() ? "1" : "0",
								r.pictType(),
								r.repeatPict() ? "1" : "0",
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
					return vFrames;
				})
				.stream()
				.toList();
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		result.getContainerAnalyserResult()
				.ifPresent(caResult -> {
					final var vFrames = tableDocument.createTable("Container video frames").head(HEAD_VFRAMES);
					caResult.videoFrames().forEach(r -> {
						final var frame = r.frame();
						vFrames.addRow()
								.addCell(frame.streamIndex())
								.addCell(frame.keyFrame() ? 1 : 0)
								.addOptionalToString(r.pictType())
								.addCell(r.repeatPict() ? 1 : 0)
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
		final var videoFramesReport = result.getContainerAnalyserResult()
				.map(ContainerAnalyserResult::videoFrames)
				.stream()
				.flatMap(List::stream)
				.filter(d -> d.repeatPict() == false)
				.map(FFprobeVideoFrame::frame)
				.filter(f -> f.mediaType() == VIDEO)
				.toList();
		if (videoFramesReport.isEmpty()) {
			return List.of();
		}

		final var firstStreamIndex = videoFramesReport.stream()
				.map(FFprobeBaseFrame::streamIndex)
				.skip(1)
				.findFirst().orElseThrow(() -> new IllegalArgumentException("Can't found video stream index"));

		final var values = videoFramesReport.stream()
				.filter(f -> f.streamIndex() == firstStreamIndex)
				.map(FFprobeBaseFrame::pktSize)
				.map(f -> (float) f / 1024f)
				.toList();

		final var dataGraphic = TimedDataGraphic.create(
				videoFramesReport.stream()
						.filter(f -> f.streamIndex() == firstStreamIndex)
						.map(FFprobeBaseFrame::pktDtsTime),
				RangeAxis.createFromRelativesValueSet(
						"Frame size (kbytes)", 0,
						values.stream()));

		dataGraphic.addSeries(dataGraphic.new Series(
				"Video packet/frame size",
				BLUE.brighter(),
				THIN_STROKE,
				values.stream()));

		return List.of(
				new GraphicArtifact(
						getSingleUniqGraphicBaseFileName(),
						dataGraphic.addMinMaxValueMarkers().makeLinearAxisGraphic(numberUtils),
						appConfig.getGraphicConfig().getImageSizeFullSize()));
	}

	@Override
	public String getSingleUniqGraphicBaseFileName() {
		return appConfig.getGraphicConfig().getVBitrateGraphicFilename();
	}

	@Override
	public void addToReport(final DataResult result, final ReportDocument document) {
		result.getContainerAnalyserResult()
				.map(ContainerAnalyserResult::videoFrames)
				.flatMap(Optional::ofNullable)
				.filter(Predicate.not(List::isEmpty))
				.ifPresent(videoFrames -> {
					final var section = new ReportSection(CONTAINER, VIDEO_FRAMES);

					final var firstStreamIndex = videoFrames.stream()
							.map(FFprobeVideoFrame::frame)
							.map(FFprobeBaseFrame::streamIndex)
							.findFirst().orElseThrow(() -> new IllegalArgumentException(
									"Can't found video stream index"));
					final var allFrames = videoFrames.stream()
							.map(FFprobeVideoFrame::frame)
							.filter(f -> f.streamIndex() == firstStreamIndex)
							.toList();

					section.add(StatisticsUnitValueReportEntry.createFromInteger(
							FRAME_SIZE,
							allFrames.stream()
									.map(FFprobeBaseFrame::pktSize), BYTES, numberUtils));

					final var frameCount = allFrames.size();
					final var keyFrameCount = (int) allFrames.stream()
							.filter(FFprobeBaseFrame::keyFrame)
							.count();
					if (keyFrameCount == frameCount) {
						section.add(new NumericUnitValueReportEntry(COUNT, frameCount,
								FRAMES_ALL_ARE_KEY_FRAMES_NO_GOP));
						return;
					}
					section.add(new NumericUnitValueReportEntry(COUNT, frameCount, FRAMES));
					if (keyFrameCount > 0) {
						section.add(new NumericUnitValueReportEntry(KEY_COUNT, keyFrameCount, "key frames"));
					}

					final var repeatFrameCount = videoFrames.stream()
							.filter(f -> f.frame().streamIndex() == firstStreamIndex)
							.filter(FFprobeVideoFrame::repeatPict)
							.count();
					if (repeatFrameCount > 0) {
						section.add(new NumericUnitValueReportEntry(REPEAT_COUNT, repeatFrameCount, FRAMES));
					}

					section.add(createFromFloat(
							FRAME_DURATION_DECLARED,
							allFrames.stream()
									.map(FFprobeBaseFrame::durationTime)
									.filter(f -> f > 0f)
									.filter(f -> f.isNaN() == false)
									.map(d -> d * 1000f), MILLISECOND_S, numberUtils::formatDecimalFull1En));

					section.add(createFromDouble(FRAME_PTS_TIME,
							computeTimeDerivative(allFrames.stream()
									.map(FFprobeBaseFrame::ptsTime),
									allFrames.size()), MILLISECOND_S, numberUtils::formatDecimalFull3En));

					section.add(createFromDouble(FRAME_DTS_TIME,
							computeTimeDerivative(allFrames.stream()
									.map(FFprobeBaseFrame::pktDtsTime),
									allFrames.size()), MILLISECOND_S, numberUtils::formatDecimalFull3En));

					section.add(createFromDouble(FRAME_BEST_EFFORT_TIME,
							computeTimeDerivative(allFrames.stream()
									.map(FFprobeBaseFrame::bestEffortTimestampTime),
									allFrames.size()), MILLISECOND_S, numberUtils::formatDecimalFull3En));

					addAllGraphicsToReport(this, result, section, appConfig, appCommand);
					document.add(section);
				});
	}

	private Stream<Double> computeTimeDerivative(final Stream<Float> frames, final int size) {
		return Arrays.stream(numberUtils.getTimeDerivative(frames
				.map(ms -> ms > -1f ? ms : Float.NaN)
				.map(numberUtils::secToMs), size))
				.mapToObj(d -> d);
	}

}
