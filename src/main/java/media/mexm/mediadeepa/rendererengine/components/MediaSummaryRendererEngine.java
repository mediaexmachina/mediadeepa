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

import static java.lang.Boolean.TRUE;
import static java.util.function.Function.identity;
import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.CONTAINER;
import static tv.hd3g.ffprobejaxb.MediaSummary.getLevelTag;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.TableDocument;
import media.mexm.mediadeepa.exportformat.TabularDocument;
import media.mexm.mediadeepa.exportformat.TabularExportFormat;
import media.mexm.mediadeepa.exportformat.report.NumericUnitValueReportEntry;
import media.mexm.mediadeepa.exportformat.report.ReportDocument;
import media.mexm.mediadeepa.exportformat.report.ReportEntry;
import media.mexm.mediadeepa.exportformat.report.ReportEntryStream;
import media.mexm.mediadeepa.exportformat.report.ReportEntryStreamList;
import media.mexm.mediadeepa.exportformat.report.ReportSection;
import media.mexm.mediadeepa.exportformat.report.SimpleKeyValueListReportEntry;
import media.mexm.mediadeepa.exportformat.report.SimpleKeyValueReportEntry;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.SingleTabularDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;
import tv.hd3g.ffprobejaxb.data.FFProbeStream;
import tv.hd3g.ffprobejaxb.data.FFProbeStreamDisposition;

@Component
public class MediaSummaryRendererEngine implements
										ReportRendererEngine,
										TableRendererEngine,
										TabularRendererEngine,
										ConstStrings,
										SingleTabularDocumentExporterTraits {

	@Autowired
	private NumberUtils numberUtils;

	public static final List<String> HEAD_MEDIA_SUMMARY = List.of(TYPE, VALUE);

	@Override
	public String getSingleUniqTabularDocumentBaseFileName() {
		return "media-summary";
	}

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		return result.getFFprobeResult()
				.map(FFprobeJAXB::getMediaSummary)
				.map(mediaSummary -> {
					final var t = new TabularDocument(tabularExportFormat, getSingleUniqTabularDocumentBaseFileName())
							.head(HEAD_MEDIA_SUMMARY);
					mediaSummary.streams().forEach(s -> t.row(STREAM, s));
					t.row(FORMAT, mediaSummary.format());
					return t;
				})
				.stream()
				.toList();
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		result.getFFprobeResult()
				.map(FFprobeJAXB::getMediaSummary)
				.ifPresent(mediaSummary -> {
					final var t = tableDocument.createTable("Media summary").head(HEAD_MEDIA_SUMMARY);
					mediaSummary.streams().forEach(s -> t.addRow().addCell(STREAM).addCell(s));
					t.addRow().addCell(FORMAT).addCell(mediaSummary.format());
				});
	}

	@Override
	public void addToReport(final DataResult result, final ReportDocument document) {
		result.getFFprobeResult()
				.ifPresent(ffprobe -> {
					final var mediaSummary = ffprobe.getMediaSummary();
					document.addSummarySection(new SimpleKeyValueReportEntry(
							FILE_FORMAT, mediaSummary.format()));
					document.addSummarySection(new SimpleKeyValueListReportEntry(
							"File stream(s)", mediaSummary.streams()));

					final var section = new ReportSection(CONTAINER, MEDIA_CONTAINER);

					ffprobe.getFormat()
							.ifPresent(format -> {
								section.add(new SimpleKeyValueListReportEntry(FILE_FORMAT, List.of(
										format.formatLongName(), format.formatName())));

								section.add(new NumericUnitValueReportEntry(FILE_SIZE, format.size(), BYTE_S));
								section.add(new NumericUnitValueReportEntry(CONTAINER_DECLATED_BITRATE,
										format.bitRate(), BYTE_S_SEC));
								section.add(new SimpleKeyValueReportEntry(CONTAINER_START_TIME,
										durationToString(format.startTime())));
								section.add(new NumericUnitValueReportEntry(PROGRAM_COUNT,
										format.nbPrograms(), PROGRAM_S));
								section.add(new NumericUnitValueReportEntry(ALL_STREAM_COUNT,
										format.nbStreams(), STREAM_S));
							});

					section.add(saveFFprobeStreamList(ffprobe.getStreams()));

					section.add(new NumericUnitValueReportEntry(
							CHAPTERS, ffprobe.getChapters().size(), CHAPTER_S));
					document.add(section);
				});
	}

	private ReportEntry saveFFprobeStreamList(final List<FFProbeStream> streams) {
		final var sList = new ReportEntryStreamList();
		streams.forEach(s -> saveFFprobeStreamToReportEntries(
				s, sList.addStream(s.id(), s.codecType(), s.index())));
		return sList;
	}

	private void saveFFprobeStreamToReportEntries(final FFProbeStream stream, final ReportEntryStream reportEntry) {
		reportEntry.add(CODEC_NAME, stream.codecLongName());
		reportEntry.add(CODEC_INTERNAL_NAME, stream.codecName());
		reportEntry.add(CODEC_TAG, stream.codecTagString() + " (" + stream.codecTag() + ")");
		reportEntry.add(CODEC_PROFILE, stream.profile());

		Optional.ofNullable(stream.level())
				.ifPresent(c -> reportEntry.add(CODEC_LEVEL, getLevelTag(stream.codecName(), c)));

		reportEntry.add(DISPOSITION,
				Optional.ofNullable(stream.disposition())
						.map(FFProbeStreamDisposition::resumeDispositions)
						.stream()
						.flatMap(identity()));

		reportEntry.add(BITRATE_INDICATED, stream.bitRate(), BYTE_S_SEC);
		reportEntry.add(MAX_BITRATE_INDICATED, stream.maxBitRate(), BYTE_S_SEC);
		reportEntry.add(BITS_PER_RAW_SAMPLE, stream.bitsPerRawSample(), BIT_S);
		reportEntry.add(BITS_PER_SAMPLE, stream.bitsPerSample(), BIT_S, stream.bitsPerSample() > 0);
		reportEntry.add(REAL_FRAME_RATE, stream.rFrameRate(), "0/0");
		reportEntry.add(AVERAGE_FRAME_RATE, stream.avgFrameRate(), "0/0");
			reportEntry.add(EXTRADATA, stream.extradata() + ", " +
									   stream.extradataSize() + " " + BIT_S + " (" +
								   stream.extradataHash() + ")",
				stream.extradata() != null && stream.extradata().isEmpty() == false);

		reportEntry.add(START_PTS, stream.startPts(), "");
		reportEntry.add(START_TIME, durationToString(stream.startTime()));
		reportEntry.add(INITIAL_PADDING, stream.initialPadding(), BYTE_S);
		reportEntry.add(FRAME_COUNT, stream.nbFrames(), FRAME_S);
		reportEntry.add(DURATION, durationToString(stream.duration()), FRAME_S);
		reportEntry.add(DURATION_TS, stream.durationTs(), "");
		reportEntry.add(TIME_BASE, stream.timeBase());

		reportEntry.add(CLOSED_CAPTIONS, CAN_CONTAIN,
				TRUE.equals(stream.closedCaptions()));

		if ("video".equalsIgnoreCase(stream.codecType())) {
			reportEntry.add(WIDTH, stream.width(), PIXEL_S);
			reportEntry.add(HEIGHT, stream.height(), PIXEL_S);
			reportEntry.add(CODED_WIDTH, stream.codedWidth(), PIXEL_S, stream.codedWidth() > 0);
			reportEntry.add(CODED_HEIGHT, stream.codedHeight(), PIXEL_S, stream.codedHeight() > 0);
			reportEntry.add(HAS_B_FRAMES, stream.hasBFrames() ? CAN_CONTAIN : "No");
			reportEntry.add(SAMPLE_ASPECT_RATIO, stream.sampleAspectRatio());
			reportEntry.add(DISPLAY_ASPECT_RATIO, stream.displayAspectRatio());
			reportEntry.add(PIXEL_FORMAT, stream.pixFmt());
			reportEntry.add(CHROMA_LOCATION, stream.chromaLocation());
			reportEntry.add(COLOR_PRIMARIES, stream.colorPrimaries());
			reportEntry.add(COLOR_SPACE, stream.colorSpace());
			reportEntry.add(COLOR_TRANSFERT, stream.colorTransfer());
			reportEntry.add(FILM_GRAIN, CAN_CONTAIN, TRUE.equals(stream.filmGrain()));
			reportEntry.add(FIELD_ORDER, stream.fieldOrder());
			reportEntry.add(REFS, stream.refs(), REFS_MIN);
		}

		if ("audio".equalsIgnoreCase(stream.codecType())) {
			reportEntry.add(SAMPLE_FORMAT, stream.sampleFmt());
			reportEntry.add(SAMPLE_RATE, stream.sampleRate(), HZ_SEC);
			reportEntry.add(CHANNEL_COUNT, stream.channels(), CHANNEL_S);
			reportEntry.add(CHANNEL_LAYOUT, stream.channelLayout());
		}

		reportEntry.add(NB_READ_FRAMES, stream.nbReadFrames(), FRAME_S, stream.nbReadFrames() > 0);
		reportEntry.add(NB_READ_PACKETS, stream.nbReadPackets(), PACKET_S, stream.nbReadPackets() > 0);

		Optional.ofNullable(stream.tags())
				.map(List::stream)
				.stream()
				.flatMap(identity())
				.forEach(tag -> reportEntry.add("Tag: " + tag.key(), tag.value()));

		reportEntry.add(SIDE_DATA,
				stream.sideDataList().stream().map(f -> {
					if (f.size() > 0) {
						return f.type() + ": " + f.size() + " bytes";
					}
					return f.type();
				}));

	}

	private String durationToString(final Float d) {
		if (d == null) {
			return null;
		}
		return numberUtils.durationToString(Duration.ofMillis(Math.round(d * 1000d)));
	}

}
