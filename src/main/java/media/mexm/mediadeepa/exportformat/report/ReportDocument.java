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
package media.mexm.mediadeepa.exportformat.report;

import static java.lang.Boolean.TRUE;
import static java.util.function.Function.identity;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.getTimeDerivative;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.secToMs;
import static media.mexm.mediadeepa.exportformat.report.ReportEntrySubset.toEntrySubset;
import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.ABOUT;
import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.AUDIO;
import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.CONTAINER;
import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.SUMMARY;
import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.VIDEO;
import static media.mexm.mediadeepa.exportformat.report.StatisticsUnitValueReportEntry.createFromDouble;
import static media.mexm.mediadeepa.exportformat.report.StatisticsUnitValueReportEntry.createFromFloat;
import static media.mexm.mediadeepa.exportformat.report.StatisticsUnitValueReportEntry.createFromInteger;
import static media.mexm.mediadeepa.exportformat.report.StatisticsUnitValueReportEntry.createFromLong;
import static tv.hd3g.ffprobejaxb.MediaSummary.getLevelTag;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.ffmpeg.ffprobe.PacketSideDataListType;
import org.ffmpeg.ffprobe.StreamType;

import lombok.Getter;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.tabular.TabularDocument;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeAudioFrame;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeAudioFrameConst;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeBaseFrame;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeCodecType;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobePacket;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobePictType;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeVideoFrame;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeVideoFrameConst;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdAstats;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdAstatsChannel;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdCropdetect;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdEvent;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdet;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdSiti;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdValue;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserResult;
import tv.hd3g.fflauncher.recipes.GOPStatItem;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.recipes.MediaAnalyserSessionFilterContext;
import tv.hd3g.fflauncher.resultparser.Ebur128Summary;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;
import tv.hd3g.ffprobejaxb.MediaSummary;

public class ReportDocument {

	private static final String CHANNEL_LAYOUT = "Channel layout";
	private static final String CAN_CONTAIN = "Can contain";
	private static final String BYTE_S_SEC = "byte(s)/sec";
	private static final String HIGH_PRECISION_FORMAT = "#,###.###";
	private static final String MILLISECOND_S = "millisecond(s)";
	private static final String EVENT_S = "event(s)";
	private static final String PIXEL_S = "pixel(s)";
	private static final String FRAMES = "frames";
	private static final String COUNT = "Count";
	private static final String BYTES_SECONDS = "bytes/seconds";
	private static final String BYTES = "bytes";
	private static final String FRAME_S = "frame(s)";
	private static final String INTERLACING_FRAME_STATUS = "Interlacing frame status";
	private static final String SAMPLE_S = "sample(s)";
	private final DataResult result;
	private final List<ReportSection> sections;
	@Getter
	private final ReportSection summarySection;
	private final Optional<Duration> sourceDuration;

	public ReportDocument(final DataResult result) {
		this.result = Objects.requireNonNull(result, "\"result\" can't to be null");
		sections = new ArrayList<>();

		sourceDuration = result.getSourceDuration();
		summarySection = new ReportSection(SUMMARY, "Media summary");
		result.getFFprobeResult().ifPresent(this::add);
		result.getContainerAnalyserResult().ifPresent(this::add);
		result.getMediaAnalyserResult().ifPresent(this::add);
	}

	private void saveEbur128Summary(final Ebur128Summary ebu) {
		final var section = new ReportSection(AUDIO, "Loudness EBU-R128");
		section.add(new NumericUnitValueReportEntry("Integrated", ebu.getIntegrated(), "dBFS"));
		section.add(new NumericUnitValueReportEntry("Range (LRA)", ebu.getLoudnessRange(), "dB"));
		section.add(new NumericUnitValueReportEntry("High range", ebu.getLoudnessRangeHigh(), "dBFS"));
		section.add(new NumericUnitValueReportEntry("Low range", ebu.getLoudnessRangeLow(), "dBFS"));
		section.add(new NumericUnitValueReportEntry("True peak", ebu.getTruePeak(), "dBTPK"));
		sections.add(section);
	}

	private void saveAPhaseMeter(final List<LavfiMtdValue<Float>> aPhaseMeterReport) {
		if (aPhaseMeterReport.isEmpty()) {
			return;
		}
		final var section = new ReportSection(AUDIO, "Phase correlation");
		section.add(
				StatisticsUnitValueReportEntry.createFromLong(
						"Phase correlation (L/R)",
						aPhaseMeterReport.stream()
								.map(LavfiMtdValue::value)
								.map(v -> Math.round(v * 100d)), "%"));
		sections.add(section);
	}

	private void saveAStats(final List<LavfiMtdValue<LavfiMtdAstats>> aStatsReport) {
		final var section = new ReportSection(AUDIO, "Signal stats");
		final var channelCount = aStatsReport.stream()
				.map(LavfiMtdValue::value)
				.map(LavfiMtdAstats::channels)
				.mapToInt(List::size)
				.max()
				.orElse(0);
		if (channelCount == 0) {
			return;
		}

		final var aStatsChannelsValuesList = aStatsReport.stream()
				.map(LavfiMtdValue::value)
				.map(LavfiMtdAstats::channels)
				.toList();

		toEntrySubset(IntStream.range(0, channelCount)
				.mapToObj(chIndex -> createFromFloat(
						"DC offset channel " + (chIndex + 1),
						aStatsChannelsValuesList.stream()
								.map(f -> f.get(chIndex))
								.map(LavfiMtdAstatsChannel::dcOffset)
								.map(v -> v * 100f),
						"%")), section);

		toEntrySubset(IntStream.range(0, channelCount)
				.mapToObj(chIndex -> createFromFloat(
						"Peak level channel " + (chIndex + 1),
						aStatsChannelsValuesList.stream()
								.map(f -> f.get(chIndex))
								.map(LavfiMtdAstatsChannel::peakLevel),
						"dBFS")), section);

		toEntrySubset(IntStream.range(0, channelCount)
				.mapToObj(chIndex -> createFromFloat(
						"Noise floor channel " + (chIndex + 1),
						aStatsChannelsValuesList.stream()
								.map(f -> f.get(chIndex))
								.map(LavfiMtdAstatsChannel::noiseFloor),
						"dBFS")), section);

		toEntrySubset(IntStream.range(0, channelCount)
				.mapToObj(chIndex -> createFromFloat(
						"Entropy (complexity) channel " + (chIndex + 1),
						aStatsChannelsValuesList.stream()
								.map(f -> f.get(chIndex))
								.map(LavfiMtdAstatsChannel::entropy)
								.map(v -> v * 100f),
						"%")), section);

		toEntrySubset(IntStream.range(0, channelCount)
				.mapToObj(chIndex -> {
					final var list = aStatsChannelsValuesList.stream()
							.map(f -> f.get(chIndex))
							.map(p -> (long) p.peakCount()) /** Workaround for maven compiler bug */
							.toList();
					return new NumericUnitValueReportEntry("Peak count channel " + (chIndex + 1),
							list.get(list.size() - 1),
							SAMPLE_S);
				}), section);

		toEntrySubset(IntStream.range(0, channelCount)
				.mapToObj(chIndex -> {
					final var list = aStatsChannelsValuesList.stream()
							.map(f -> f.get(chIndex))
							.map(p -> (long) p.flatness()) /** Workaround for maven compiler bug */
							.toList();
					return new NumericUnitValueReportEntry("Flatness count channel " + (chIndex + 1),
							list.get(list.size() - 1),
							SAMPLE_S);
				}), section);

		toEntrySubset(IntStream.range(0, channelCount)
				.mapToObj(chIndex -> {
					final var list = aStatsChannelsValuesList.stream()
							.map(f -> f.get(chIndex))
							.map(p -> (long) p.noiseFloorCount()) /** Workaround for maven compiler bug */
							.toList();
					return new NumericUnitValueReportEntry("Noise floor count channel " + (chIndex + 1),
							list.get(list.size() - 1),
							SAMPLE_S);
				}), section);

		sections.add(section);
	}

	private void saveVideoQuality(final List<LavfiMtdValue<LavfiMtdSiti>> sitiReport,
								  final List<LavfiMtdValue<Float>> blockDetectReport,
								  final List<LavfiMtdValue<Float>> blurDetectReport) {
		final var section = new ReportSection(VIDEO, "Image and motion quality");

		toEntrySubset(Stream.of(
				createFromFloat(
						"Spatial complexity",
						sitiReport.stream()
								.map(LavfiMtdValue::value)
								.map(LavfiMtdSiti::si),
						""),
				createFromFloat(
						"Temporal complexity",
						sitiReport.stream()
								.map(LavfiMtdValue::value)
								.map(LavfiMtdSiti::ti),
						"")), section);

		section.add(
				StatisticsUnitValueReportEntry.createFromFloat(
						"Blockiness detection",
						blockDetectReport.stream()
								.map(LavfiMtdValue::value),
						""));

		section.add(
				StatisticsUnitValueReportEntry.createFromFloat(
						"Blurriness detection",
						blurDetectReport.stream()
								.map(LavfiMtdValue::value),
						""));
		sections.add(section);
	}

	private void addIdetStatus(final ReportSection section, final String name, final int value, final int frameCount) {
		if (value == 0) {
			return;
		}
		section.add(new NumericUnitValueReportEntry(
				name,
				Math.round(value * 100f / frameCount),
				"%"));
	}

	private void saveIdet(final List<LavfiMtdValue<LavfiMtdIdet>> idetReport) {
		if (idetReport.isEmpty()) {
			return;
		}
		final var section = new ReportSection(VIDEO, "Interlacing detection");

		final var frameCount = idetReport.size();
		final var lastIdet = idetReport.get(frameCount - 1).value();

		addIdetStatus(section, "Detected as progressive",
				lastIdet.single().progressive(), frameCount);
		addIdetStatus(section, "Detected as progressive, using multiple-frame detection",
				lastIdet.multiple().progressive(), frameCount);

		addIdetStatus(section, "Detected as top field first",
				lastIdet.single().tff(), frameCount);
		addIdetStatus(section, "Detected as top field first, using multiple-frame detection",
				lastIdet.multiple().tff(), frameCount);
		if (lastIdet.repeated().neither() != frameCount) {
			addIdetStatus(section, "With the top field repeated from the previous frame’s top field",
					lastIdet.repeated().top(), frameCount);
		}

		addIdetStatus(section, "Detected as bottom field first",
				lastIdet.single().bff(), frameCount);
		addIdetStatus(section, "Detected as bottom field first, using multiple-frame detection",
				lastIdet.multiple().bff(), frameCount);
		if (lastIdet.repeated().neither() != frameCount) {
			addIdetStatus(section, "With the bottom field repeated from the previous frame’s bottom field",
					lastIdet.repeated().bottom(), frameCount);
		}

		addIdetStatus(section, "Could not be classified using single-frame detection",
				lastIdet.single().undetermined(), frameCount);
		addIdetStatus(section, "Could not be classified using multiple-frame detection",
				lastIdet.multiple().undetermined(), frameCount);
		if (lastIdet.repeated().neither() != frameCount) {
			addIdetStatus(section, "No repeated field",
					lastIdet.repeated().neither(), frameCount);
		}
		sections.add(section);
	}

	private void saveCrop(final List<LavfiMtdValue<LavfiMtdCropdetect>> cropDetectReport) {
		if (cropDetectReport.isEmpty()) {
			return;
		}
		final var section = new ReportSection(VIDEO, "Black borders / crop detection");
		final var cropEvents = cropDetectReport.stream()
				.map(LavfiMtdValue::value)
				.distinct()
				.toList();
		section.add(new NumericUnitValueReportEntry("Crop detection activity", cropEvents.size(), EVENT_S));
		section.add(new SimpleKeyValueListReportEntry("First crop values",
				cropEvents.stream()
						.limit(10)
						.map(l -> Stream.of(
								"x1: " + l.x1(),
								"x2: " + l.x2(),
								"y1: " + l.y1(),
								"y2: " + l.y2(),
								"X: " + l.x(),
								"Y: " + l.y(),
								"Width: " + l.w(),
								"Height: " + l.h()))
						.map(l -> l.collect(Collectors.joining(", ")))
						.toList()));
		sections.add(section);
	}

	private void saveEvents(final String name, final List<LavfiMtdEvent> events, final boolean audio) {
		if (events.isEmpty()) {
			return;
		}
		final var section = new ReportSection(audio ? AUDIO : VIDEO, name + " events");
		Stream.concat(
				Stream.of(EventReportEntry.createHeader(events, sourceDuration)),
				events.stream()
						.map(event -> new EventReportEntry(event, sourceDuration)))
				.forEach(section::add);
		sections.add(section);
	}

	private void saveAboutMeasure(final Collection<MediaAnalyserSessionFilterContext> filters) {
		if (filters.isEmpty()) {
			return;
		}
		final var section = new ReportSection(ABOUT, "FFmpeg filters used in this measure");
		filters.stream()
				.sorted((l, r) -> l.name().compareTo(r.name()))
				.map(filter -> new SimpleKeyValueReportEntry(filter.name(), filter.setup()))
				.forEach(section::add);
		sections.add(section);
	}

	private void add(final MediaAnalyserResult maResult) {
		Optional.ofNullable(maResult.ebur128Summary()).ifPresent(this::saveEbur128Summary);

		final var lavfiMetadatas = maResult.lavfiMetadatas();
		saveEvents("Audio silence", lavfiMetadatas.getSilenceEvents(), true);
		saveEvents("Audio mono", lavfiMetadatas.getMonoEvents(), true);
		saveAPhaseMeter(lavfiMetadatas.getAPhaseMeterReport());
		saveAStats(lavfiMetadatas.getAStatsReport());

		saveVideoQuality(
				lavfiMetadatas.getSitiReport(),
				lavfiMetadatas.getBlockDetectReport(),
				lavfiMetadatas.getBlurDetectReport());
		saveEvents("Black frames", lavfiMetadatas.getBlackEvents(), false);
		saveEvents("Freeze (static) frames", lavfiMetadatas.getFreezeEvents(), false);
		saveIdet(lavfiMetadatas.getIdetReport());
		saveCrop(lavfiMetadatas.getCropDetectReport());

		saveAboutMeasure(maResult.filters());
	}

	private void saveAudioConst(final FFprobeAudioFrameConst audioConst) {
		if (audioConst == null) {
			return;
		}
		final var aConstSection = new ReportSection(CONTAINER, "Audio media file information");
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
		sections.add(aConstSection);
	}

	private void saveVideoConst(final FFprobeVideoFrameConst videoConst) {
		if (videoConst == null) {
			return;
		}

		final var vConstSection = new ReportSection(CONTAINER, "Video media file information");
		vConstSection.add(new SimpleKeyValueReportEntry(
				"Image resolution", videoConst.width() + " × " + videoConst.height()));

		vConstSection.add(new NumericUnitValueReportEntry(
				"Pixel surface", videoConst.width() * videoConst.height(), PIXEL_S));

		vConstSection.add(new SimpleKeyValueReportEntry(
				"Display aspect ratio", videoConst.sampleAspectRatio()));
		vConstSection.add(SimpleKeyValueReportEntry.getFromRatio("Storage aspect ratio (w/y)",
				videoConst.width(), videoConst.height()));

		try {
			final var darW = Integer.parseInt(videoConst.sampleAspectRatio().split(":")[0]);
			final var darH = Integer.parseInt(videoConst.sampleAspectRatio().split(":")[1]);
			vConstSection.add(SimpleKeyValueReportEntry.getFromRatio("Pixel aspect ratio",
					darH * videoConst.width(), darW * videoConst.height()));
		} catch (NumberFormatException | IndexOutOfBoundsException e) {
			vConstSection.add(new SimpleKeyValueReportEntry(
					"Pixel aspect ratio", "(unknow)"));
		}

		if (videoConst.interlacedFrame() && videoConst.topFieldFirst()) {
			vConstSection.add(new SimpleKeyValueReportEntry(
					INTERLACING_FRAME_STATUS, "Interlaced, top field first"));
		} else if (videoConst.interlacedFrame() && videoConst.topFieldFirst() == false) {
			vConstSection.add(new SimpleKeyValueReportEntry(
					INTERLACING_FRAME_STATUS, "Interlaced, bottom field first"));
		} else {
			vConstSection.add(new SimpleKeyValueReportEntry(
					INTERLACING_FRAME_STATUS, "progressive"));
		}

		vConstSection.add(new SimpleKeyValueReportEntry(
				"Pixel format", videoConst.pixFmt()));
		vConstSection.add(new SimpleKeyValueReportEntry(
				"Color primaries", videoConst.colorPrimaries()));
		vConstSection.add(new SimpleKeyValueReportEntry(
				"Color range", videoConst.colorRange()));
		vConstSection.add(new SimpleKeyValueReportEntry(
				"Color space", videoConst.colorSpace()));
		vConstSection.add(new SimpleKeyValueReportEntry(
				"Color transfer", videoConst.colorTransfer()));
		sections.add(vConstSection);
	}

	public static String durationToString(final Float d) {
		if (d == null) {
			return null;
		}
		return TabularDocument.durationToString(Duration.ofMillis(Math.round(d * 1000d)));
	}

	private void add(final FFprobeJAXB ffprobe) {
		final var mediaSummary = ffprobe.getMediaSummary();
		summarySection.add(new SimpleKeyValueReportEntry(
				"File format", mediaSummary.format()));
		summarySection.add(new SimpleKeyValueListReportEntry(
				"File stream(s)", mediaSummary.streams()));

		final var section = new ReportSection(CONTAINER, "Media container");
		final var format = ffprobe.getFormat();
		section.add(new SimpleKeyValueListReportEntry("File format", List.of(
				format.getFormatLongName(), format.getFormatName())));

		section.add(new NumericUnitValueReportEntry("File size", format.getSize(), "byte(s)"));
		section.add(new NumericUnitValueReportEntry("Container declated bitrate", format.getBitRate(), BYTE_S_SEC));
		section.add(new SimpleKeyValueReportEntry("Container start time", durationToString(format.getStartTime())));
		section.add(new NumericUnitValueReportEntry("Program count", format.getNbPrograms(), "program(s)"));
		section.add(new NumericUnitValueReportEntry("All stream count", format.getNbStreams(), "stream(s)"));
		section.add(saveFFprobeStreamList(ffprobe.getStreams()));

		section.add(new NumericUnitValueReportEntry(
				"Chapters", ffprobe.getChapters().size(), "chapter(s)"));
		sections.add(section);
	}

	private ReportEntry saveFFprobeStreamList(final List<StreamType> streams) {
		final var sList = new ReportEntryStreamList();
		streams.forEach(s -> saveFFprobeStreamToReportEntries(
				s, sList.addStream(s.getId(), s.getCodecType(), s.getIndex())));
		return sList;
	}

	private static void saveFFprobeStreamToReportEntries(final StreamType stream, final ReportEntryStream reportEntry) {
		reportEntry.add("Codec name", stream.getCodecLongName());
		reportEntry.add("Codec internal name", stream.getCodecName());
		reportEntry.add("Codec tag", stream.getCodecTag() + "/" + stream.getCodecTagString() + ")");

		Optional.ofNullable(stream.getTag())
				.map(List::stream)
				.stream()
				.flatMap(identity())
				.forEach(tag -> reportEntry.add("Tag: " + tag.getKey(), tag.getValue()));

		reportEntry.add("Disposition",
				Optional.ofNullable(stream.getDisposition())
						.map(MediaSummary::resumeDispositions)
						.stream()
						.flatMap(identity()));

		reportEntry.add("Bitrate (indicated)", stream.getBitRate(), BYTE_S_SEC);
		reportEntry.add("Max bitrate (indicated)", stream.getMaxBitRate(), BYTE_S_SEC);
		reportEntry.add("Bits per raw sample", stream.getBitsPerRawSample(), "bit(s)");
		if (stream.getBitsPerSample() != null && stream.getBitsPerSample() > 0) {
			reportEntry.add("Bits per sample", stream.getBitsPerSample(), "bit(s)");
		}
		reportEntry.add("Real frame rate", stream.getRFrameRate(), "0/0");
		reportEntry.add("Average frame rate", stream.getAvgFrameRate(), "0/0");
		if (stream.getExtradata() != null && stream.getExtradata().isEmpty() == false) {
			reportEntry.add("Extradata", stream.getExtradata() + ", " +
										 stream.getExtradataSize() + " bytes(s) (" +
										 stream.getExtradataHash() + ")");
		}
		if (TRUE.equals(stream.isClosedCaptions())) {
			reportEntry.add("Closed captions", CAN_CONTAIN);
		}
		if (TRUE.equals(stream.isFilmGrain())) {
			reportEntry.add("Film grain", CAN_CONTAIN);
		}

		reportEntry.add("Start PTS", stream.getStartPts(), "");
		reportEntry.add("Start time", durationToString(stream.getStartTime()));
		reportEntry.add("Initial padding", stream.getInitialPadding(), "byte(s)");
		reportEntry.add("Frame count", stream.getNbFrames(), FRAME_S);
		reportEntry.add("Duration", durationToString(stream.getDuration()), FRAME_S);
		reportEntry.add("Duration TS", stream.getDurationTs(), "");
		reportEntry.add("Time base", stream.getTimeBase());

		reportEntry.add("Width", stream.getWidth(), PIXEL_S);
		reportEntry.add("Height", stream.getHeight(), PIXEL_S);
		if (stream.getCodedWidth() != null && stream.getCodedWidth() > 0) {
			reportEntry.add("Coded width", stream.getCodedWidth(), PIXEL_S);
		}
		if (stream.getCodedHeight() != null && stream.getCodedHeight() > 0) {
			reportEntry.add("Coded height", stream.getCodedHeight(), PIXEL_S);
		}

		Optional.ofNullable(stream.getHasBFrames())
				.ifPresent(hasB -> reportEntry.add("Has B frames", hasB > 0 ? CAN_CONTAIN : "No"));

		reportEntry.add("Sample aspect ratio", stream.getSampleAspectRatio());
		reportEntry.add("Display aspect ratio", stream.getDisplayAspectRatio());
		reportEntry.add("Pixel format", stream.getPixFmt());
		reportEntry.add("Chroma location", stream.getChromaLocation());
		reportEntry.add("Color primaries", stream.getColorPrimaries());
		reportEntry.add("Color space", stream.getColorSpace());
		reportEntry.add("Color transfert", stream.getColorTransfer());

		reportEntry.add("Codec Profile", stream.getProfile());

		Optional.ofNullable(stream.getLevel())
				.ifPresent(c -> reportEntry.add("Codec level", getLevelTag(stream.getCodecName(), c)));

		reportEntry.add("Field order", stream.getFieldOrder());
		reportEntry.add("Refs", stream.getRefs(), "refs");

		reportEntry.add("Sample format", stream.getSampleFmt());
		reportEntry.add("Sample rate", stream.getSampleRate(), "Hz/sec");

		reportEntry.add("Channel count", stream.getChannels(), "channel(s)");
		reportEntry.add(CHANNEL_LAYOUT, stream.getChannelLayout());

		reportEntry.add("Nb read frames", stream.getNbReadFrames(), FRAME_S);
		reportEntry.add("Nb read packets", stream.getNbReadPackets(), "packet(s)");

		reportEntry.add("Side data",
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

	private void saveVideoFrames(final List<FFprobeVideoFrame> videoFrames) {
		if (videoFrames.isEmpty()) {
			return;
		}
		final var section = new ReportSection(CONTAINER, "Video frames");

		final var firstStreamIndex = videoFrames.stream()
				.map(FFprobeVideoFrame::frame)
				.map(FFprobeBaseFrame::streamIndex)
				.findFirst().orElseThrow(() -> new IllegalArgumentException("Can't found video stream index"));
		final var allFrames = videoFrames.stream()
				.map(FFprobeVideoFrame::frame)
				.filter(f -> f.streamIndex() == firstStreamIndex)
				.toList();

		section.add(StatisticsUnitValueReportEntry.createFromInteger(
				"Frame size",
				allFrames.stream()
						.map(FFprobeBaseFrame::pktSize), BYTES));

		final var frameCount = allFrames.size();
		final var keyFrameCount = (int) allFrames.stream()
				.filter(FFprobeBaseFrame::keyFrame)
				.count();
		if (keyFrameCount == frameCount) {
			section.add(new NumericUnitValueReportEntry(COUNT, frameCount, "frames (all are key frames, no GOP)"));
			return;
		}
		section.add(new NumericUnitValueReportEntry(COUNT, frameCount, FRAMES));
		if (keyFrameCount > 0) {
			section.add(new NumericUnitValueReportEntry("Key count", keyFrameCount, "key frames"));
		}

		final var repeatFrameCount = videoFrames.stream()
				.filter(f -> f.frame().streamIndex() == firstStreamIndex)
				.filter(FFprobeVideoFrame::repeatPict)
				.count();
		if (repeatFrameCount > 0) {
			section.add(new NumericUnitValueReportEntry("Repeat count", repeatFrameCount, FRAMES));
		}

		section.add(createFromFloat(
				"Frame duration (declared)",
				allFrames.stream()
						.map(FFprobeBaseFrame::pktDurationTime)
						.filter(f -> f > 0f)
						.filter(f -> f.isNaN() == false)
						.map(d -> d * 1000f), MILLISECOND_S));

		section.add(createFromDouble("Frame PTS time",
				computeTimeDerivative(allFrames.stream()
						.map(FFprobeBaseFrame::ptsTime),
						allFrames.size()), MILLISECOND_S, HIGH_PRECISION_FORMAT));

		section.add(createFromDouble("Frame DTS time",
				computeTimeDerivative(allFrames.stream()
						.map(FFprobeBaseFrame::pktDtsTime),
						allFrames.size()), MILLISECOND_S, HIGH_PRECISION_FORMAT));

		section.add(createFromDouble("Frame best effort time",
				computeTimeDerivative(allFrames.stream()
						.map(FFprobeBaseFrame::bestEffortTimestampTime),
						allFrames.size()), MILLISECOND_S, HIGH_PRECISION_FORMAT));

		sections.add(section);
	}

	private Stream<Double> computeTimeDerivative(final Stream<Float> frames, final int size) {
		return Arrays.stream(getTimeDerivative(frames
				.map(ms -> ms > -1f ? ms : Float.NaN)
				.map(secToMs), size))
				.mapToObj(d -> d);
	}

	private void saveGOPStats(final List<GOPStatItem> extractGOPStats, final List<FFprobeVideoFrame> videoFrames) {
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

		final var section = new ReportSection(CONTAINER, "Video compression group-of-pictures");

		/**
		 * GOP
		 */
		section.add(new NumericUnitValueReportEntry(COUNT, extractGOPStats.size(), "GOPs"));

		section.add(createFromInteger("GOPs length",
				extractGOPStats.stream().map(GOPStatItem::gopFrameCount), FRAME_S));
		section.add(createFromLong("GOPs size",
				extractGOPStats.stream().map(GOPStatItem::gopDataSize), BYTES));

		/**
		 * I
		 */
		if (keyFrameCount > 0 && iFrameCount != keyFrameCount) {
			section.add(new NumericUnitValueReportEntry("I frames count", iFrameCount, FRAMES));

		}
		section.add(createFromLong("I frames size in GOPs",
				extractGOPStats.stream().map(GOPStatItem::iFrameDataSize), BYTES));

		final var iFrameSize = videoFrames.stream()
				.filter(f -> FFprobePictType.I.equals(f.pictType()))
				.map(FFprobeVideoFrame::frame)
				.mapToLong(FFprobeBaseFrame::pktSize)
				.sum();
		section.add(new NumericUnitValueReportEntry("All I size", iFrameSize, BYTES));

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
							extractGOPStats.stream().map(GOPStatItem::pFramesDataSize), BYTES),
					createFromInteger("P frames length in GOPs",
							extractGOPStats.stream().map(GOPStatItem::pFramesCount), FRAME_S)),
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
							extractGOPStats.stream().map(GOPStatItem::bFramesDataSize), BYTES),
					createFromInteger("B frames length in GOPs",
							extractGOPStats.stream().map(GOPStatItem::bFramesCount), FRAME_S)),
					section);
		} else {
			section.add(new SimpleKeyValueReportEntry("B frame presence", "no B frames"));
		}

		sections.add(section);
	}

	private void saveAudioFrames(final List<FFprobeAudioFrame> audioFrames) {
		if (audioFrames.isEmpty()) {
			return;
		}
		final var section = new ReportSection(CONTAINER, "Audio frames");
		section.add(new NumericUnitValueReportEntry(COUNT, audioFrames.size(), FRAMES));
		section.add(createFromInteger(
				"Frame size",
				audioFrames.stream()
						.map(FFprobeAudioFrame::frame)
						.map(FFprobeBaseFrame::pktSize), BYTES));

		section.add(createFromInteger(
				"Frame length",
				audioFrames.stream()
						.map(FFprobeAudioFrame::nbSamples), "samples"));

		section.add(createFromFloat(
				"Frame duration",
				audioFrames.stream()
						.map(FFprobeAudioFrame::frame)
						.map(FFprobeBaseFrame::pktDurationTime)
						.filter(f -> f > 0f)
						.filter(f -> f.isNaN() == false)
						.map(d -> d * 1000f), "milliseconds"));

		sections.add(section);
	}

	private void savePackets(final List<FFprobePacket> packets) {
		if (packets.isEmpty()) {
			return;
		}
		final var section = new ReportSection(CONTAINER, "Stream packets");

		final var sumList = new ArrayList<Map<FFprobeCodecType, Integer>>();
		final var sumSizes = packets.stream().reduce(
				sumList,
				(list, packet) -> {
					final var position = (int) Math.round(Math.ceil(packet.dtsTime()));
					if (position > list.size()) {
						final var map = new EnumMap<FFprobeCodecType, Integer>(FFprobeCodecType.class);
						map.put(packet.codecType(), packet.size());
						list.add(map);
					} else {
						final var pos = list.size() - 1;
						list.get(pos).merge(packet.codecType(), packet.size(), (actual, pSize) -> actual + pSize);
					}
					return list;
				},
				(l, r) -> {
					l.addAll(r);
					return l;
				});

		section.add(createFromInteger("Video bitrate",
				sumSizes.stream()
						.filter(f -> f.containsKey(FFprobeCodecType.VIDEO))
						.map(f -> f.get(FFprobeCodecType.VIDEO)),
				BYTES_SECONDS));
		section.add(createFromInteger("Audio bitrate",
				sumSizes.stream()
						.filter(f -> f.containsKey(FFprobeCodecType.AUDIO))
						.map(f -> f.get(FFprobeCodecType.AUDIO)),
				BYTES_SECONDS));
		section.add(createFromInteger("Data bitrate",
				sumSizes.stream()
						.filter(f -> f.containsKey(FFprobeCodecType.DATA))
						.map(f -> f.get(FFprobeCodecType.DATA)),
				BYTES_SECONDS));
		section.add(createFromInteger("Other bitrate",
				sumSizes.stream()
						.filter(f -> f.containsKey(FFprobeCodecType.OTHER))
						.map(f -> f.get(FFprobeCodecType.OTHER)),
				BYTES_SECONDS));

		section.add(createFromInteger("All streams bitrates",
				Stream.of(FFprobeCodecType.values())
						.flatMap(codecType -> sumSizes.stream()
								.filter(f -> f.containsKey(codecType))
								.map(f -> f.get(codecType))),
				BYTES_SECONDS));

		sections.add(section);
	}

	private void add(final ContainerAnalyserResult caResult) {
		saveAudioConst(caResult.audioConst());
		saveVideoConst(caResult.videoConst());
		saveVideoFrames(caResult.videoFrames());
		saveGOPStats(caResult.extractGOPStats(), caResult.videoFrames());
		saveAudioFrames(caResult.audioFrames());
		savePackets(caResult.packets());
	}

	public ReportSection getAboutDocument() {
		final var version = result.getVersions();
		final var items = version.entrySet().stream()
				.map(entry -> new SimpleKeyValueReportEntry(entry.getKey(), entry.getValue()))
				.toList();
		return new ReportSection(ABOUT, "About this document").add(items);
	}

	public Stream<ReportSection> getSections() {
		return sections.stream()
				.filter(ReportSection::isNotEmpty)
				.sorted((l, r) -> l.getCategory().compareTo(r.getCategory()));
	}

}
