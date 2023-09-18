/*
 * This file is part of mediadeepa.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2023
 *
 */
package media.mexm.mediadeepa.exportformat.tabular;

import static java.nio.charset.StandardCharsets.UTF_8;
import static media.mexm.mediadeepa.components.CLIRunner.makeOutputFileName;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormat;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMetadataFilterParser;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserResult;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.Ebur128Summary;
import tv.hd3g.fflauncher.resultparser.RawStdErrFilterEvent;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;
import tv.hd3g.ffprobejaxb.MediaSummary;

public abstract class TabularExportFormat implements ExportFormat, TabularDocumentExporter {
	public static final String TEMPORAL_INFO = "Temporal Info";
	public static final String SPATIAL_INFO = "Spatial Info";
	public static final String LOUDNESS_RANGE = "Loudness Range";
	public static final String INTEGRATED = "Integrated";
	public static final String PTS_TIME = "Pts time";
	public static final String STREAM_INDEX = "Stream index";
	public static final String VALUE = "Value";
	public static final String PTS = "PTS";
	public static final String FRAME = "Frame";

	@Override
	public void exportResult(final DataResult result, final File exportDirectory, final String baseFileName) {
		final var exportTo = new TabularDocument.ExportTo(exportDirectory, baseFileName);

		result.getMediaAnalyserResult()
				.ifPresent(maResult -> {
					Optional.ofNullable(maResult.ebur128Summary())
							.ifPresent(ebu -> makeEbur128Summary(ebu).exportToFile(exportTo));

					final var lavfiMetadatas = maResult.lavfiMetadatas();

					makeAPhaseMeter(lavfiMetadatas).exportToFile(exportTo);
					makeAStats(lavfiMetadatas).exportToFile(exportTo);
					makeSITI(lavfiMetadatas).exportToFile(exportTo);
					makeSITIReport(lavfiMetadatas).exportToFile(exportTo);
					makeBlock(lavfiMetadatas).exportToFile(exportTo);
					makeBlur(lavfiMetadatas).exportToFile(exportTo);
					makeCrop(lavfiMetadatas).exportToFile(exportTo);
					makeIdet(lavfiMetadatas).exportToFile(exportTo);
					makeAboutMeasure(maResult).exportToFile(exportTo);
					result.getSourceDuration()
							.ifPresent(sourceDuration -> makeEvents(
									sourceDuration, lavfiMetadatas).exportToFile(exportTo));
				});

		makeEbur128(result.getEbur128events()).exportToFile(exportTo);
		makeRawstderrfilters(result.getRawStdErrEvents()).exportToFile(exportTo);

		result.getContainerAnalyserResult()
				.ifPresent(caResult -> {
					makePackets(caResult).exportToFile(exportTo);
					makeVFrames(caResult).exportToFile(exportTo);
					makeAFrames(caResult).exportToFile(exportTo);
					makeVConsts(caResult).exportToFile(exportTo);
					makeAConsts(caResult).exportToFile(exportTo);
					makeGopStats(caResult).exportToFile(exportTo);
				});

		result.getFFprobeResult()
				.ifPresent(ffprobeResult -> {
					saveFFprobeJAXB(ffprobeResult, exportDirectory, baseFileName);
					makeMediaSummary(ffprobeResult.getMediaSummary()).exportToFile(exportTo);
				});

		makeAppAbout(result).exportToFile(exportTo);
	}

	public static final List<String> HEAD_APP_ABOUT = List.of("Type", VALUE);

	private TabularDocument makeAppAbout(final DataResult result) {
		final var t = new TabularDocument(this, "about").head(HEAD_APP_ABOUT);
		result.getVersions().entrySet().forEach(entry -> t.row(entry.getKey(), entry.getValue()));
		return t;
	}

	public static final List<String> HEAD_MEDIA_SUMMARY = List.of("Type", VALUE);

	private TabularDocument makeMediaSummary(final MediaSummary mediaSummary) {
		final var t = new TabularDocument(this, "media-summary").head(HEAD_MEDIA_SUMMARY);
		mediaSummary.streams().forEach(s -> t.row("Stream", s));
		t.row("Format", mediaSummary.format());
		return t;
	}

	public static final List<String> HEAD_EBUR128_SUMMARY = List.of(
			INTEGRATED,
			"Integrated Threshold",
			LOUDNESS_RANGE,
			"Loudness Range Threshold",
			"Loudness Range Low",
			"Loudness Range High",
			"Sample Peak",
			"True Peak");

	private TabularDocument makeEbur128Summary(final Ebur128Summary ebu) {
		final var t = new TabularDocument(this, "audio-ebur128-summary").head(HEAD_EBUR128_SUMMARY);
		t.row(
				formatToString(ebu.getIntegrated(), true),
				formatToString(ebu.getIntegratedThreshold(), true),
				formatToString(ebu.getLoudnessRange(), true),
				formatToString(ebu.getLoudnessRangeThreshold(), true),
				formatToString(ebu.getLoudnessRangeLow(), true),
				formatToString(ebu.getLoudnessRangeHigh(), true),
				formatToString(ebu.getSamplePeak(), true),
				formatToString(ebu.getTruePeak(), true));
		return t;
	}

	public static final List<String> HEAD_SITI_REPORT = List.of("Type", "Average", "Max", "Min");

	private TabularDocument makeSITIReport(final LavfiMetadataFilterParser lavfiMetadatas) {
		final var sitiStats = new TabularDocument(this, "video-siti-stats-ITU-T_P-910").head(HEAD_SITI_REPORT);
		if (lavfiMetadatas.getSitiReport().isEmpty() == false) {
			final var stats = lavfiMetadatas.computeSitiStats();
			sitiStats
					.row(SPATIAL_INFO,
							stats.si().getAverage(),
							stats.si().getMax(),
							stats.si().getMin())
					.row(TEMPORAL_INFO,
							stats.ti().getAverage(),
							stats.ti().getMax(),
							stats.ti().getMin());
		}
		return sitiStats;
	}

	public static final List<String> HEAD_SITI = List.of(FRAME, PTS, PTS_TIME, SPATIAL_INFO, TEMPORAL_INFO);

	private TabularDocument makeSITI(final LavfiMetadataFilterParser lavfiMetadatas) {
		final var siti = new TabularDocument(this, "video-siti-ITU-T_P-910").head(HEAD_SITI);
		lavfiMetadatas.getSitiReport()
				.forEach(a -> siti.row(a.frame(), a.pts(), a.ptsTime(), a.value().si(), a.value().ti()));
		return siti;
	}

	public static final List<String> HEAD_ASTATS = List.of(
			FRAME, PTS, PTS_TIME,
			"Channel",
			"DC Offset",
			"Entropy",
			"Flat factor",
			"Noise floor",
			"Noise floor count",
			"Peak level",
			"Peak count",
			"Other");

	private TabularDocument makeAStats(final LavfiMetadataFilterParser lavfiMetadatas) {
		final var aStats = new TabularDocument(this, "audio-stats").head(HEAD_ASTATS);
		lavfiMetadatas.getAStatsReport()
				.forEach(a -> {
					final var channels = a.value().channels();
					for (var pos = 0; pos < channels.size(); pos++) {
						final var channel = channels.get(pos);
						var other = channel.other().toString();
						if (other.equals("{}")) {
							other = "";
						}
						aStats.row(a.frame(), a.pts(), a.ptsTime(),
								pos + 1,
								channel.dcOffset(),
								channel.entropy(),
								channel.flatFactor(),
								channel.noiseFloor(),
								channel.noiseFloorCount(),
								channel.peakLevel(),
								channel.peakCount(),
								other);
					}
				});
		return aStats;
	}

	public static final List<String> HEAD_BLOCK = List.of(FRAME, PTS, PTS_TIME, VALUE);

	private TabularDocument makeBlock(final LavfiMetadataFilterParser lavfiMetadatas) {
		final var block = new TabularDocument(this, "video-block-detect").head(HEAD_BLOCK);
		lavfiMetadatas.getBlockDetectReport()
				.forEach(a -> block.row(a.frame(), a.pts(), a.ptsTime(), a.value()));
		return block;
	}

	public static final List<String> HEAD_BLUR = List.of(FRAME, PTS, PTS_TIME, VALUE);

	private TabularDocument makeBlur(final LavfiMetadataFilterParser lavfiMetadatas) {
		final var blur = new TabularDocument(this, "video-blur-detect").head(HEAD_BLUR);
		lavfiMetadatas.getBlurDetectReport()
				.forEach(a -> blur.row(a.frame(), a.pts(), a.ptsTime(), a.value()));
		return blur;
	}

	public static final List<String> HEAD_CROP = List.of(FRAME, PTS, PTS_TIME,
			"x1", "x2", "y1", "y2", "w", "h", "x", "y");

	private TabularDocument makeCrop(final LavfiMetadataFilterParser lavfiMetadatas) {
		final var crop = new TabularDocument(this, "video-crop-detect").head(HEAD_CROP);
		lavfiMetadatas.getCropDetectReport().forEach(a -> {
			final var value = a.value();
			crop.row(a.frame(), a.pts(), a.ptsTime(),
					value.x1(), value.x2(), value.y1(), value.y2(),
					value.w(), value.h(), value.x(), value.y());
		});
		return crop;
	}

	public static final List<String> HEAD_IDET = List.of(FRAME, PTS, PTS_TIME,
			"Single top field first",
			"Single bottom field first",
			"Single current frame",
			"Single progressive",
			"Single undetermined",
			"Multiple top field first",
			"Multiple bottom field first",
			"Multiple current frame",
			"Multiple progressive",
			"Multiple undetermined",
			"Repeated current frame",
			"Repeated top",
			"Repeated bottom",
			"Repeated neither");

	private TabularDocument makeIdet(final LavfiMetadataFilterParser lavfiMetadatas) {
		final var idet = new TabularDocument(this, "video-interlace-detect").head(HEAD_IDET);
		lavfiMetadatas.getIdetReport()
				.forEach(a -> {
					final var value = a.value();
					final var single = value.single();
					final var multiple = value.multiple();
					final var repeated = value.repeated();

					idet.row(a.frame(), a.pts(), a.ptsTime(),
							single.tff(),
							single.bff(),
							single.currentFrame(),
							single.progressive(),
							single.undetermined(),

							multiple.tff(),
							multiple.bff(),
							multiple.currentFrame(),
							multiple.progressive(),
							multiple.undetermined(),

							repeated.currentFrame(),
							repeated.top(),
							repeated.bottom(),
							repeated.neither());
				});
		return idet;
	}

	public static final List<String> HEAD_EVENTS = List.of("Name", "Scope/Channel", "Start", "End", "Duration");

	private TabularDocument makeEvents(final Duration sourceDuration,
									   final LavfiMetadataFilterParser lavfiMetadatas) {
		final var events = new TabularDocument(this, "events").head(HEAD_EVENTS);
		Stream.of(
				lavfiMetadatas.getMonoEvents(),
				lavfiMetadatas.getSilenceEvents(),
				lavfiMetadatas.getBlackEvents(),
				lavfiMetadatas.getFreezeEvents())
				.flatMap(List::stream)
				.sorted()
				.forEach(ev -> events.row(
						ev.name(),
						ev.scope(),
						ev.start(),
						ev.getEndOr(sourceDuration),
						ev.getEndOr(sourceDuration).minus(ev.start())));
		return events;
	}

	public static final List<String> HEAD_ABOUT = List.of("Type", "Name", "Setup", "Java class");

	private TabularDocument makeAboutMeasure(final MediaAnalyserResult maResult) {
		final var aboutMeasure = new TabularDocument(this, "filters").head(HEAD_ABOUT);
		maResult.filters()
				.forEach(f -> aboutMeasure.row(
						f.type(),
						f.name(),
						f.setup(),
						f.className()));
		return aboutMeasure;
	}

	public static final List<String> HEAD_APHASE = List.of(FRAME, PTS, PTS_TIME, VALUE);

	private TabularDocument makeAPhaseMeter(final LavfiMetadataFilterParser lavfiMetadatas) {
		final var aPhaseMeter = new TabularDocument(this, "audio-phase-meter").head(HEAD_APHASE);
		lavfiMetadatas.getAPhaseMeterReport()
				.forEach(a -> aPhaseMeter.row(a.frame(), a.pts(), a.ptsTime(), a.value()));
		return aPhaseMeter;
	}

	public static final List<String> HEAD_EBUR128 = List.of(
			"Position",
			INTEGRATED,
			"Momentary",
			"Short-term",
			LOUDNESS_RANGE,
			"Sample-peak L", "Sample-peak R",
			"True-peak per frame L", "True-peak per frame R",
			"True-peak L", "True-peak R");

	private TabularDocument makeEbur128(final List<Ebur128StrErrFilterEvent> ebur128events) {
		final var t = new TabularDocument(this, "audio-ebur128").head(HEAD_EBUR128);
		ebur128events.forEach(ebu -> t.row(
				formatToString(ebu.getT(), true),
				formatToString(ebu.getI(), true),
				formatToString(ebu.getM(), true),
				formatToString(ebu.getS(), true),
				formatToString(ebu.getLra(), true),
				formatToString(ebu.getSpk().left(), true),
				formatToString(ebu.getSpk().right(), true),
				formatToString(ebu.getFtpk().left(), true),
				formatToString(ebu.getFtpk().right(), true),
				formatToString(ebu.getTpk().left(), true),
				formatToString(ebu.getTpk().right(), true)));
		return t;
	}

	public static final List<String> HEAD_STDERRFILTERS = List.of("Filter name", "Chain pos", "Line");

	private TabularDocument makeRawstderrfilters(final List<RawStdErrFilterEvent> rawStdErrEvents) {
		final var t = new TabularDocument(this, "rawstderrfilters").head(HEAD_STDERRFILTERS);
		rawStdErrEvents.stream()
				.filter(r -> r.getFilterName().equals("cropdetect") == false)
				.forEach(r -> t.row(r.getFilterName(), r.getFilterChainPos(), r.getLineValue()));
		return t;
	}

	public static final List<String> HEAD_GOPSTATS = List.of(
			"GOP frame count",
			"P frames count",
			"B frames count",
			"GOP data size",
			"I frame data size",
			"P frames data size",
			"B frames data size");

	private TabularDocument makeGopStats(final ContainerAnalyserResult caResult) {
		final var gopStats = new TabularDocument(this, "container-video-gop").head(HEAD_GOPSTATS);
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
	}

	public static final List<String> HEAD_ACONSTS = List.of(
			"Channel layout",
			"Channels",
			"Sample format",
			"Ref pts",
			"Ref pts time",
			"Ref pkt dts",
			"Ref pkt dts time",
			"Ref best effort timestamp",
			"Ref best effort timestamp time",
			"Ref pkt duration",
			"Ref pkt duration time",
			"Ref pkt pos");

	private TabularDocument makeAConsts(final ContainerAnalyserResult caResult) {
		final var aConsts = new TabularDocument(this, "container-audio-consts").head(HEAD_ACONSTS);
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
							frame.pktDuration(),
							frame.pktDurationTime(),
							frame.pktPos());
				});
		return aConsts;
	}

	public static final List<String> HEAD_VCONSTS = List.of(
			"Width",
			"Height",
			"Sample aspect ratio",
			"Top field first",
			"Interlaced frame",
			"Pix fmt",
			"Color range",
			"Color primaries",
			"Color transfer",
			"Color space",
			"Coded picture number",
			"Display picture number",
			"Ref pts",
			"Ref pts time",
			"Ref pkt dts",
			"Ref pkt dts time",
			"Ref best effort timestamp",
			"Ref best effort timestamp time",
			"Ref pkt duration",
			"Ref pkt duration time",
			"Ref pkt pos");

	private TabularDocument makeVConsts(final ContainerAnalyserResult caResult) {
		final var vConsts = new TabularDocument(this, "container-video-consts").head(HEAD_VCONSTS);
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
							c.codedPictureNumber(),
							c.displayPictureNumber(),
							frame.pts(),
							frame.ptsTime(),
							frame.pktDts(),
							frame.pktDtsTime(),
							frame.bestEffortTimestamp(),
							frame.bestEffortTimestampTime(),
							frame.pktDuration(),
							frame.pktDurationTime(),
							frame.pktPos());
				});
		return vConsts;
	}

	public static final List<String> HEAD_AFRAMES = List.of(
			"Media type",
			STREAM_INDEX,
			"Nb samples",
			"Pts",
			PTS_TIME,
			"Pkt dts",
			"Pkt dts time",
			"Best effort timestamp",
			"Best effort timestamp time",
			"Pkt duration",
			"Pkt duration time",
			"Pkt pos",
			"Pkt size");

	private TabularDocument makeAFrames(final ContainerAnalyserResult caResult) {
		final var aFrames = new TabularDocument(this, "container-audio-frames").head(HEAD_AFRAMES);

		caResult.audioFrames().forEach(r -> {
			final var frame = r.frame();
			aFrames.row(
					frame.mediaType(),
					frame.streamIndex(),
					r.nbSamples(),
					frame.pts(),
					frame.ptsTime(),
					frame.pktDts(),
					frame.pktDtsTime(),
					frame.bestEffortTimestamp(),
					frame.bestEffortTimestampTime(),
					frame.pktDuration(),
					frame.pktDurationTime(),
					frame.pktPos(),
					frame.pktSize());
		});
		return aFrames;
	}

	public static final List<String> HEAD_VFRAMES = List.of(
			"Media type",
			STREAM_INDEX,
			"Key frame",
			"Pict type",
			"Repeat pict",
			"Pts",
			PTS_TIME,
			"Pkt dts",
			"Pkt dts time",
			"Best effort timestamp",
			"Best effort timestamp time",
			"Pkt duration",
			"Pkt duration time",
			"Pkt pos",
			"Pkt size");

	private TabularDocument makeVFrames(final ContainerAnalyserResult caResult) {
		final var vFrames = new TabularDocument(this, "container-video-frames").head(HEAD_VFRAMES);

		caResult.videoFrames().forEach(r -> {
			final var frame = r.frame();
			vFrames.row(
					frame.mediaType(),
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
					frame.pktDuration(),
					frame.pktDurationTime(),
					frame.pktPos(),
					frame.pktSize());
		});
		return vFrames;
	}

	public static final List<String> HEAD_CONTAINER_PACKETS = List.of(
			"Codec type",
			STREAM_INDEX,
			"Pts",
			PTS_TIME,
			"Dts",
			"Dts time",
			"Duration",
			"Duration time",
			"Size",
			"Pos",
			"Flags");

	private TabularDocument makePackets(final ContainerAnalyserResult caResult) {
		final var packets = new TabularDocument(this, "container-packets").head(HEAD_CONTAINER_PACKETS);
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
	}

	public static void saveFFprobeJAXB(final FFprobeJAXB ffprobeResult,
									   final File exportDirectory,
									   final String baseFileName) {
		try {
			FileUtils.write(
					new File(exportDirectory, makeOutputFileName(baseFileName, "ffprobe.xml")),
					ffprobeResult.getXmlContent(),
					UTF_8,
					false);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write file", e);
		}
	}
}
