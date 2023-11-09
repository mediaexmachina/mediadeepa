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
import static media.mexm.mediadeepa.exportformat.ReportSectionCategory.CONTAINER;
import static tv.hd3g.ffprobejaxb.MediaSummary.getLevelTag;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.ffmpeg.ffprobe.PacketSideDataListType;
import org.ffmpeg.ffprobe.StreamType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.NumericUnitValueReportEntry;
import media.mexm.mediadeepa.exportformat.ReportDocument;
import media.mexm.mediadeepa.exportformat.ReportEntry;
import media.mexm.mediadeepa.exportformat.ReportEntryStream;
import media.mexm.mediadeepa.exportformat.ReportEntryStreamList;
import media.mexm.mediadeepa.exportformat.ReportSection;
import media.mexm.mediadeepa.exportformat.SimpleKeyValueListReportEntry;
import media.mexm.mediadeepa.exportformat.SimpleKeyValueReportEntry;
import media.mexm.mediadeepa.exportformat.TableDocument;
import media.mexm.mediadeepa.exportformat.TabularDocument;
import media.mexm.mediadeepa.exportformat.TabularExportFormat;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;
import tv.hd3g.ffprobejaxb.MediaSummary;

@Component
public class MediaSummaryRendererEngine implements
										ReportRendererEngine,
										TableRendererEngine,
										TabularRendererEngine,
										ConstStrings {

	@Autowired
	private NumberUtils numberUtils;

	public static final List<String> HEAD_MEDIA_SUMMARY = List.of(TYPE, VALUE);

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		return result.getFFprobeResult()
				.map(FFprobeJAXB::getMediaSummary)
				.map(mediaSummary -> {
					final var t = new TabularDocument(tabularExportFormat, "media-summary").head(HEAD_MEDIA_SUMMARY);
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
					final var format = ffprobe.getFormat();
					section.add(new SimpleKeyValueListReportEntry(FILE_FORMAT, List.of(
							format.getFormatLongName(), format.getFormatName())));

					section.add(new NumericUnitValueReportEntry(FILE_SIZE, format.getSize(), BYTE_S));
					section.add(new NumericUnitValueReportEntry(CONTAINER_DECLATED_BITRATE, format.getBitRate(),
							BYTE_S_SEC));
					section.add(new SimpleKeyValueReportEntry(CONTAINER_START_TIME, durationToString(format
							.getStartTime())));
					section.add(new NumericUnitValueReportEntry(PROGRAM_COUNT, format.getNbPrograms(), PROGRAM_S));
					section.add(new NumericUnitValueReportEntry(ALL_STREAM_COUNT, format.getNbStreams(),
							STREAM_S));
					section.add(saveFFprobeStreamList(ffprobe.getStreams()));

					section.add(new NumericUnitValueReportEntry(
							CHAPTERS, ffprobe.getChapters().size(), CHAPTER_S));
					document.add(section);
				});
	}

	private ReportEntry saveFFprobeStreamList(final List<StreamType> streams) {
		final var sList = new ReportEntryStreamList();
		streams.forEach(s -> saveFFprobeStreamToReportEntries(
				s, sList.addStream(s.getId(), s.getCodecType(), s.getIndex())));
		return sList;
	}

	private void saveFFprobeStreamToReportEntries(final StreamType stream, final ReportEntryStream reportEntry) {
		reportEntry.add(CODEC_NAME, stream.getCodecLongName());
		reportEntry.add(CODEC_INTERNAL_NAME, stream.getCodecName());
		reportEntry.add(CODEC_TAG, stream.getCodecTag() + "/" + stream.getCodecTagString() + ")");

		Optional.ofNullable(stream.getTag())
				.map(List::stream)
				.stream()
				.flatMap(identity())
				.forEach(tag -> reportEntry.add("Tag: " + tag.getKey(), tag.getValue()));

		reportEntry.add(DISPOSITION,
				Optional.ofNullable(stream.getDisposition())
						.map(MediaSummary::resumeDispositions)
						.stream()
						.flatMap(identity()));

		reportEntry.add(BITRATE_INDICATED, stream.getBitRate(), BYTE_S_SEC);
		reportEntry.add(MAX_BITRATE_INDICATED, stream.getMaxBitRate(), BYTE_S_SEC);
		reportEntry.add(BITS_PER_RAW_SAMPLE, stream.getBitsPerRawSample(), BIT_S);
		if (stream.getBitsPerSample() != null && stream.getBitsPerSample() > 0) {
			reportEntry.add(BITS_PER_SAMPLE, stream.getBitsPerSample(), BIT_S);
		}
		reportEntry.add(REAL_FRAME_RATE, stream.getRFrameRate(), "0/0");
		reportEntry.add(AVERAGE_FRAME_RATE, stream.getAvgFrameRate(), "0/0");
		if (stream.getExtradata() != null && stream.getExtradata().isEmpty() == false) {
			reportEntry.add(EXTRADATA, stream.getExtradata() + ", " +
									   stream.getExtradataSize() + " " + BIT_S + " (" +
									   stream.getExtradataHash() + ")");
		}
		if (TRUE.equals(stream.isClosedCaptions())) {
			reportEntry.add(CLOSED_CAPTIONS, CAN_CONTAIN);
		}
		if (TRUE.equals(stream.isFilmGrain())) {
			reportEntry.add(FILM_GRAIN, CAN_CONTAIN);
		}

		reportEntry.add(START_PTS, stream.getStartPts(), "");
		reportEntry.add(START_TIME, durationToString(stream.getStartTime()));
		reportEntry.add(INITIAL_PADDING, stream.getInitialPadding(), BYTE_S);
		reportEntry.add(FRAME_COUNT, stream.getNbFrames(), FRAME_S);
		reportEntry.add(DURATION, durationToString(stream.getDuration()), FRAME_S);
		reportEntry.add(DURATION_TS, stream.getDurationTs(), "");
		reportEntry.add(TIME_BASE, stream.getTimeBase());

		reportEntry.add(WIDTH, stream.getWidth(), PIXEL_S);
		reportEntry.add(HEIGHT, stream.getHeight(), PIXEL_S);
		if (stream.getCodedWidth() != null && stream.getCodedWidth() > 0) {
			reportEntry.add(CODED_WIDTH, stream.getCodedWidth(), PIXEL_S);
		}
		if (stream.getCodedHeight() != null && stream.getCodedHeight() > 0) {
			reportEntry.add(CODED_HEIGHT, stream.getCodedHeight(), PIXEL_S);
		}

		Optional.ofNullable(stream.getHasBFrames())
				.ifPresent(hasB -> reportEntry.add(HAS_B_FRAMES, hasB > 0 ? CAN_CONTAIN : "No"));

		reportEntry.add(SAMPLE_ASPECT_RATIO2, stream.getSampleAspectRatio());
		reportEntry.add(DISPLAY_ASPECT_RATIO, stream.getDisplayAspectRatio());
		reportEntry.add(PIXEL_FORMAT, stream.getPixFmt());
		reportEntry.add(CHROMA_LOCATION, stream.getChromaLocation());
		reportEntry.add(COLOR_PRIMARIES, stream.getColorPrimaries());
		reportEntry.add(COLOR_SPACE, stream.getColorSpace());
		reportEntry.add(COLOR_TRANSFERT, stream.getColorTransfer());

		reportEntry.add(CODEC_PROFILE, stream.getProfile());

		Optional.ofNullable(stream.getLevel())
				.ifPresent(c -> reportEntry.add(CODEC_LEVEL, getLevelTag(stream.getCodecName(), c)));

		reportEntry.add(FIELD_ORDER, stream.getFieldOrder());
		reportEntry.add(REFS, stream.getRefs(), "refs");

		reportEntry.add(SAMPLE_FORMAT, stream.getSampleFmt());
		reportEntry.add(SAMPLE_RATE, stream.getSampleRate(), HZ_SEC);

		reportEntry.add(CHANNEL_COUNT, stream.getChannels(), CHANNEL_S);
		reportEntry.add(CHANNEL_LAYOUT, stream.getChannelLayout());

		reportEntry.add(NB_READ_FRAMES, stream.getNbReadFrames(), FRAME_S);
		reportEntry.add(NB_READ_PACKETS, stream.getNbReadPackets(), PACKET_S);

		reportEntry.add(SIDE_DATA,
				Optional.ofNullable(stream.getSideDataList())
						.map(PacketSideDataListType::getSideData)
						.map(List::stream)
						.stream()
						.flatMap(identity())
						.map(f -> {
							if (f.getSideDataSize() != null && f.getSideDataSize() > 0) {
								return f.getSideDataType() + ": " + f.getSideDataSize() + " bytes";
							}
							return f.getSideDataType();
						}));
	}

	private String durationToString(final Float d) {
		if (d == null) {
			return null;
		}
		return numberUtils.durationToString(Duration.ofMillis(Math.round(d * 1000d)));
	}

}
