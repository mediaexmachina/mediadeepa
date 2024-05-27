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

import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.CONTAINER;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.TableDocument;
import media.mexm.mediadeepa.exportformat.TabularDocument;
import media.mexm.mediadeepa.exportformat.TabularExportFormat;
import media.mexm.mediadeepa.exportformat.report.NumericUnitValueReportEntry;
import media.mexm.mediadeepa.exportformat.report.RatioReportEntry;
import media.mexm.mediadeepa.exportformat.report.ReportDocument;
import media.mexm.mediadeepa.exportformat.report.ReportSection;
import media.mexm.mediadeepa.exportformat.report.ResolutionReportEntry;
import media.mexm.mediadeepa.exportformat.report.SimpleKeyValueReportEntry;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.SingleTabularDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserResult;

@Component
public class VConstsRendererEngine implements
								   ReportRendererEngine,
								   TableRendererEngine,
								   TabularRendererEngine,
								   ConstStrings,
								   SingleTabularDocumentExporterTraits {

	@Autowired
	private NumberUtils numberUtils;

	public static final List<String> HEAD_VCONSTS = List.of(
			WIDTH,
			HEIGHT,
			SAMPLE_ASPECT_RATIO,
			TOP_FIELD_FIRST,
			INTERLACED_FRAME,
			PIX_FMT,
			COLOR_RANGE,
			COLOR_PRIMARIES,
			COLOR_TRANSFER,
			COLOR_SPACE,
			REF_PTS,
			REF_PTS_TIME,
			REF_PKT_DTS,
			REF_PKT_DTS_TIME,
			REF_BEST_EFFORT_TIMESTAMP,
			REF_BEST_EFFORT_TIMESTAMP_TIME,
			REF_DURATION,
			REF_DURATION_TIME,
			REF_PKT_POS);

	@Override
	public String getSingleUniqTabularDocumentBaseFileName() {
		return "container-video-consts";
	}

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		return result.getContainerAnalyserResult()
				.map(caResult -> {
					final var vConsts = new TabularDocument(tabularExportFormat,
							getSingleUniqTabularDocumentBaseFileName())
									.head(HEAD_VCONSTS);
					Stream.concat(
							caResult.olderVideoConsts().stream(),
							Stream.of(caResult.videoConst()))
							.filter(Objects::nonNull)
							.forEach(c -> {
								final var frame = c.updatedWith().frame();
								vConsts.row(
										c.width(),
										c.height(),
										c.sampleAspectRatio(),
										c.topFieldFirst() ? "1" : "0",
										c.interlacedFrame() ? "1" : "0",
										c.pixFmt(),
										c.colorRange(),
										c.colorPrimaries(),
										c.colorTransfer(),
										c.colorSpace(),
										frame.pts(),
										frame.ptsTime(),
										frame.pktDts(),
										frame.pktDtsTime(),
										frame.bestEffortTimestamp(),
										frame.bestEffortTimestampTime(),
										frame.duration(),
										frame.durationTime(),
										frame.pktPos());
							});
					return vConsts;
				})
				.stream()
				.toList();
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		result.getContainerAnalyserResult()
				.ifPresent(caResult -> {
					final var vConsts = tableDocument.createTable("Container video consts").head(HEAD_VCONSTS);
					Stream.concat(
							caResult.olderVideoConsts().stream(),
							Stream.of(caResult.videoConst()))
							.filter(Objects::nonNull)
							.forEach(c -> {
								final var frame = c.updatedWith().frame();
								vConsts.addRow()
										.addCell(c.width())
										.addCell(c.height())
										.addCell(c.sampleAspectRatio())
										.addCell(c.topFieldFirst() ? 1 : 0)
										.addCell(c.interlacedFrame() ? 1 : 0)
										.addCell(c.pixFmt())
										.addCell(c.colorRange())
										.addCell(c.colorPrimaries())
										.addCell(c.colorTransfer())
										.addCell(c.colorSpace())
										.addCell(frame.pts())
										.addCell(frame.ptsTime())
										.addCell(frame.pktDts())
										.addCell(frame.pktDtsTime())
										.addCell(frame.bestEffortTimestamp())
										.addCell(frame.bestEffortTimestampTime())
										.addCell(frame.duration())
										.addCell(frame.durationTime())
										.addCell(frame.pktPos());
							});
				});
	}

	@Override
	public void addToReport(final DataResult result, final ReportDocument document) {
		result.getContainerAnalyserResult()
				.map(ContainerAnalyserResult::videoConst)
				.flatMap(Optional::ofNullable)
				.ifPresent(videoConst -> {
					final var vConstSection = new ReportSection(CONTAINER, VIDEO_MEDIA_FILE_INFORMATION);
					vConstSection.add(new ResolutionReportEntry(
							IMAGE_RESOLUTION, videoConst.width(), videoConst.height(), PIXEL_S));

					vConstSection.add(new NumericUnitValueReportEntry(
							PIXEL_SURFACE, videoConst.width() * videoConst.height(), PIXEL_S));

					try {
						final var sarW = Integer.parseInt(videoConst.sampleAspectRatio().split(":")[0]);
						final var sarH = Integer.parseInt(videoConst.sampleAspectRatio().split(":")[1]);

						vConstSection.add(new RatioReportEntry(
								DISPLAY_ASPECT_RATIO, sarW, sarH,
								numberUtils::formatDecimalSimple5En));

						vConstSection.add(new RatioReportEntry(
								STORAGE_ASPECT_RATIO, videoConst.width(), videoConst.height(),
								numberUtils::formatDecimalSimple5En));

						vConstSection.add(new RatioReportEntry(PIXEL_ASPECT_RATIO,
								sarH * videoConst.width(), sarW * videoConst.height(),
								numberUtils::formatDecimalSimple5En));
					} catch (NumberFormatException | IndexOutOfBoundsException e) {
						vConstSection.add(new SimpleKeyValueReportEntry(
								PIXEL_ASPECT_RATIO, UNKNOW));
					}

					if (videoConst.interlacedFrame() && videoConst.topFieldFirst()) {
						vConstSection.add(new SimpleKeyValueReportEntry(
								INTERLACING_FRAME_STATUS, INTERLACED_TOP_FIELD_FIRST));
					} else if (videoConst.interlacedFrame() && videoConst.topFieldFirst() == false) {
						vConstSection.add(new SimpleKeyValueReportEntry(
								INTERLACING_FRAME_STATUS, INTERLACED_BOTTOM_FIELD_FIRST));
					} else {
						vConstSection.add(new SimpleKeyValueReportEntry(
								INTERLACING_FRAME_STATUS, PROGRESSIVE));
					}

					vConstSection.add(new SimpleKeyValueReportEntry(
							PIXEL_FORMAT, videoConst.pixFmt()));
					vConstSection.add(new SimpleKeyValueReportEntry(
							COLOR_PRIMARIES, videoConst.colorPrimaries()));
					vConstSection.add(new SimpleKeyValueReportEntry(
							COLOR_RANGE, videoConst.colorRange()));
					vConstSection.add(new SimpleKeyValueReportEntry(
							COLOR_SPACE, videoConst.colorSpace()));
					vConstSection.add(new SimpleKeyValueReportEntry(
							COLOR_TRANSFER, videoConst.colorTransfer()));
					document.add(vConstSection);
				});
	}

}
