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

import static media.mexm.mediadeepa.exportformat.ReportSectionCategory.CONTAINER;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ReportDocument;
import media.mexm.mediadeepa.exportformat.ReportSection;
import media.mexm.mediadeepa.exportformat.SimpleKeyValueReportEntry;
import media.mexm.mediadeepa.exportformat.TableDocument;
import media.mexm.mediadeepa.exportformat.TabularDocument;
import media.mexm.mediadeepa.exportformat.TabularExportFormat;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.SingleTabularDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserResult;

@Component
public class AConstsRendererEngine implements
								   ReportRendererEngine,
								   TableRendererEngine,
								   TabularRendererEngine,
								   ConstStrings,
								   SingleTabularDocumentExporterTraits {

	public static final List<String> HEAD_ACONSTS = List.of(
			CHANNEL_LAYOUT,
			CHANNELS,
			SAMPLE_FORMAT,
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
		return "container-audio-consts";
	}

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		return result.getContainerAnalyserResult()
				.map(caResult -> {
					final var aConsts = new TabularDocument(tabularExportFormat,
							getSingleUniqTabularDocumentBaseFileName())
									.head(HEAD_ACONSTS);
					Stream.concat(
							caResult.olderAudioConsts().stream(),
							Stream.of(caResult.audioConst()))
							.forEach(c -> {
								final var frame = c.updatedWith().frame();
								aConsts.row(
										c.channelLayout(),
										c.channels(),
										c.sampleFmt(),
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
					return aConsts;
				})
				.stream()
				.toList();
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		result.getContainerAnalyserResult()
				.ifPresent(caResult -> {
					final var aConsts = tableDocument.createTable("Container audio consts").head(HEAD_ACONSTS);
					Stream.concat(
							caResult.olderAudioConsts().stream(),
							Stream.of(caResult.audioConst()))
							.forEach(c -> {
								final var frame = c.updatedWith().frame();
								aConsts.addRow()
										.addOptionalToString(c.channelLayout())
										.addCell(c.channels())
										.addCell(c.sampleFmt())
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
				.map(ContainerAnalyserResult::audioConst)
				.flatMap(Optional::ofNullable)
				.ifPresent(audioConst -> {
					final var aConstSection = new ReportSection(CONTAINER, AUDIO_MEDIA_FILE_INFORMATION);
					aConstSection.add(new SimpleKeyValueReportEntry(
							"Channel count", String.valueOf(audioConst.channels())));
					if (audioConst.channelLayout() != null) {
						aConstSection.add(new SimpleKeyValueReportEntry(
								CHANNEL_LAYOUT, audioConst.channelLayout().toString()));
					} else {
						aConstSection.add(new SimpleKeyValueReportEntry(
								CHANNEL_LAYOUT, "(unknown)"));
					}
					aConstSection.add(new SimpleKeyValueReportEntry(
							"Audio sample format", audioConst.sampleFmt()));
					document.add(aConstSection);
				});
	}

}
