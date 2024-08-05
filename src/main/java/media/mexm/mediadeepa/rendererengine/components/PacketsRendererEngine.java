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
import static media.mexm.mediadeepa.exportformat.report.StatisticsUnitValueReportEntry.createFromInteger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.TableDocument;
import media.mexm.mediadeepa.exportformat.TabularDocument;
import media.mexm.mediadeepa.exportformat.TabularExportFormat;
import media.mexm.mediadeepa.exportformat.report.ReportDocument;
import media.mexm.mediadeepa.exportformat.report.ReportSection;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.SingleTabularDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeCodecType;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserProcessResult;

@Component
public class PacketsRendererEngine implements
								   ReportRendererEngine,
								   TableRendererEngine,
								   TabularRendererEngine,
								   ConstStrings,
								   SingleTabularDocumentExporterTraits {

	@Autowired
	private NumberUtils numberUtils;

	public static final List<String> HEAD_CONTAINER_PACKETS = List.of(
			CODEC_TYPE,
			STREAM_INDEX,
			PTS,
			PTS_TIME,
			DTS,
			DTS_TIME,
			DURATION,
			DURATION_TIME,
			SIZE,
			POS,
			FLAGS);

	@Override
	public String getSingleUniqTabularDocumentBaseFileName() {
		return "container-packets";
	}

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		return result.getContainerAnalyserProcessResult()
				.map(caResult -> {
					final var packets = new TabularDocument(tabularExportFormat,
							getSingleUniqTabularDocumentBaseFileName())
									.head(HEAD_CONTAINER_PACKETS);
					caResult.packets().forEach(
							r -> packets.row(
									r.codecType(),
									r.streamIndex(),
									r.pts(),
									r.ptsTime(),
									r.dts(),
									r.dtsTime(),
									r.duration(),
									r.durationTime(),
									r.size(),
									r.pos(),
									r.flags()));
					return packets;
				})
				.stream()
				.toList();
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		result.getContainerAnalyserProcessResult()
				.ifPresent(caResult -> {
					final var packets = tableDocument.createTable("Container packets").head(HEAD_CONTAINER_PACKETS);
					caResult.packets().forEach(
							r -> packets.addRow()
									.addOptionalToString(r.codecType())
									.addCell(r.streamIndex())
									.addCell(r.pts())
									.addCell(r.ptsTime())
									.addCell(r.dts())
									.addCell(r.dtsTime())
									.addCell(r.duration())
									.addCell(r.durationTime())
									.addCell(r.size())
									.addCell(r.pos())
									.addCell(r.flags()));
				});
	}

	@Override
	public void addToReport(final DataResult result, final ReportDocument document) {
		result.getContainerAnalyserProcessResult()
				.map(ContainerAnalyserProcessResult::packets)
				.flatMap(Optional::ofNullable)
				.filter(Predicate.not(List::isEmpty))
				.ifPresent(packets -> {
					final var section = new ReportSection(CONTAINER, STREAM_PACKETS);

					final var sumList = new ArrayList<Map<FFprobeCodecType, Integer>>();
					final var sumSizes = packets.stream().reduce(
							sumList,
							(list, packet) -> {
								final var time = packet.ptsTime() < 0f ? packet.dtsTime() : packet.ptsTime();
								final var position = (int) Math.round(Math.ceil(time));
								if (position >= list.size()) {
									final var map = new EnumMap<FFprobeCodecType, Integer>(FFprobeCodecType.class);
									map.put(packet.codecType(), packet.size());
									list.add(map);
								} else if (list.isEmpty() == false) {
									final var pos = list.size() - 1;
									list.get(pos).merge(
											packet.codecType(),
											packet.size(),
											(actual, pSize) -> actual + pSize);
								}
								return list;
							},
							(l, r) -> {
								l.addAll(r);
								return l;
							});

					section.add(createFromInteger(VIDEO_BITRATE,
							sumSizes.stream()
									.filter(f -> f.containsKey(FFprobeCodecType.VIDEO))
									.map(f -> f.get(FFprobeCodecType.VIDEO)),
							BYTES_SECONDS, numberUtils));
					section.add(createFromInteger(AUDIO_BITRATE,
							sumSizes.stream()
									.filter(f -> f.containsKey(FFprobeCodecType.AUDIO))
									.map(f -> f.get(FFprobeCodecType.AUDIO)),
							BYTES_SECONDS, numberUtils));
					section.add(createFromInteger(DATA_BITRATE,
							sumSizes.stream()
									.filter(f -> f.containsKey(FFprobeCodecType.DATA))
									.map(f -> f.get(FFprobeCodecType.DATA)),
							BYTES_SECONDS, numberUtils));
					section.add(createFromInteger(OTHER_BITRATE,
							sumSizes.stream()
									.filter(f -> f.containsKey(FFprobeCodecType.OTHER))
									.map(f -> f.get(FFprobeCodecType.OTHER)),
							BYTES_SECONDS, numberUtils));

					section.add(createFromInteger(ALL_STREAMS_BITRATES,
							Stream.of(FFprobeCodecType.values())
									.flatMap(codecType -> sumSizes.stream()
											.filter(f -> f.containsKey(codecType))
											.map(f -> f.get(codecType))),
							BYTES_SECONDS, numberUtils));

					document.add(section);
				});
	}

}
