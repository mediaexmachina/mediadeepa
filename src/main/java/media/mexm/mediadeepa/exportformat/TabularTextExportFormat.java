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
 * Copyright (C) hdsdi3g for hd3g.tv 2023
 *
 */
package media.mexm.mediadeepa.exportformat;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ffmpeg.ffprobe.FormatType;

import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdEvent;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserResult;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.RawStdErrFilterEvent;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;

// TODO remove duplicate log:
// INFO ffprobe result: Matroska / WebM, 00:01:00, 2 MB, 415 kbps, video: vp9 480×480 Profile 0 @ 30 fps yuv420p/rng:TV/spce:BT709/tsfer:BT709/prim:BT709, audio: opus mono @ 48000 Hz
// INFO Source file: Matroska / WebM, 00:01:00, 2 MB, 415 kbps, video: vp9 480×480 Profile 0 @ 30 fps yuv420p/rng:TV/spce:BT709/tsfer:BT709/prim:BT709, audio: opus mono @ 48000 Hz

public class TabularTextExportFormat implements ExportFormat {// TODO test
	private static final String PTS_TIME = "Pts time";
	private static final String STREAM_INDEX = "Stream index";
	private static final String VALUE = "Value";
	private static final String PTS = "PTS";
	private static final String FRAME = "Frame";

	private static final Logger log = LogManager.getLogger();

	@Override
	public void exportMediaAnalyserResult(final String source,
										  final MediaAnalyserResult maResult,
										  final File exportDirectory) {
		Optional.ofNullable(maResult.ebur128Summary())
				.ifPresent(ebu -> {
					final var t = new Tabs("Type", VALUE);
					t.row("Integrated", ebu.getIntegrated());
					t.row("Integrated Threshold", ebu.getIntegratedThreshold());
					t.row("Loudness Range", ebu.getLoudnessRange());
					t.row("Loudness Range Threshold", ebu.getLoudnessRangeThreshold());
					t.row("Loudness Range Low", ebu.getLoudnessRangeLow());
					t.row("Loudness Range High", ebu.getLoudnessRangeHigh());
					t.row("Sample Peak", ebu.getSamplePeak());
					t.row("True Peak", ebu.getTruePeak());
					save("audio-ebur128-summary.txt", exportDirectory, t.getLines());
				});

		final var lavfiMetadatas = maResult.lavfiMetadatas();

		final var aPhaseMeter = new Tabs(FRAME, PTS, PTS_TIME, VALUE);
		lavfiMetadatas.getAPhaseMeterReport()
				.forEach(a -> aPhaseMeter.row(a.frame(), a.pts(), a.ptsTime(), a.value()));
		save("audio-phase-meter.txt", exportDirectory, aPhaseMeter.getLines());

		final var aStats = new Tabs(FRAME, PTS, PTS_TIME,
				"Channel",
				"DC Offset",
				"Entropy",
				"Flat factor",
				"Noise floor",
				"Noise floor count",
				"Peak level",
				"Peak count",
				"Other");
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
		save("audio-stats.txt", exportDirectory, aStats.getLines());

		final var siti = new Tabs(FRAME, PTS, PTS_TIME, "Spatial Info", "Temporal Info");
		lavfiMetadatas.getSitiReport()
				.forEach(a -> siti.row(a.frame(), a.pts(), a.ptsTime(), a.value().si(), a.value().ti()));
		save("video-siti-ITU-T_P-910.txt", exportDirectory, siti.getLines());

		if (lavfiMetadatas.getSitiReport().isEmpty() == false) {
			final var sitiStats = new Tabs("Type", "Average", "Max", "Min");
			final var stats = lavfiMetadatas.computeSitiStats();
			sitiStats.row("Spatial Info",
					stats.si().getAverage(),
					stats.si().getMax(),
					stats.si().getMin());
			sitiStats.row("Temporal Info",
					stats.ti().getAverage(),
					stats.ti().getMax(),
					stats.ti().getMin());
			save("video-siti-stats-ITU-T_P-910.txt", exportDirectory, sitiStats.getLines());
		}

		final var block = new Tabs(FRAME, PTS, PTS_TIME, VALUE);
		lavfiMetadatas.getBlockDetectReport()
				.forEach(a -> block.row(a.frame(), a.pts(), a.ptsTime(), a.value()));
		save("video-block-detect.txt", exportDirectory, block.getLines());

		final var blur = new Tabs(FRAME, PTS, PTS_TIME, VALUE);
		lavfiMetadatas.getBlurDetectReport()
				.forEach(a -> blur.row(a.frame(), a.pts(), a.ptsTime(), a.value()));
		save("video-blur-detect.txt", exportDirectory, blur.getLines());

		final var crop = new Tabs(FRAME, PTS, PTS_TIME,
				"x1", "x2", "y1", "y2", "w", "h", "x", "y");
		lavfiMetadatas.getCropDetectReport().forEach(a -> {
			final var value = a.value();
			crop.row(a.frame(), a.pts(), a.ptsTime(),
					value.x1(), value.x2(), value.y1(), value.y2(),
					value.w(), value.h(), value.x(), value.y());
		});
		save("video-crop-detect.txt", exportDirectory, crop.getLines());

		final var idet = new Tabs(FRAME, PTS, PTS_TIME,
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
		save("video-interlace-detect.txt", exportDirectory, idet.getLines());

		final var fileDuration = maResult.session().getFFprobeResult()
				.map(FFprobeJAXB::getFormat)
				.map(FormatType::getDuration)
				.map(Optional::ofNullable)
				.stream()
				.flatMap(Optional::stream)
				.map(LavfiMtdEvent::secFloatToDuration)
				.findFirst()
				.orElse(Duration.ZERO);
		final var events = new Tabs("Name", "Scope/Channel", "Start", "End", "Duration");
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
						ev.getEndOr(fileDuration),
						ev.getEndOr(fileDuration).minus(ev.start())));
		save("events.txt", exportDirectory, events.getLines());

		final var aboutMeasure = new Tabs("Type", "Name", "Setup", "Java class");
		maResult.session().getAudioFilters()
				.forEach(f -> {
					final var filter = f.toFilter();
					aboutMeasure.row(
							"audio",
							filter.getFilterName(),
							filter.toString(),
							f.getClass().getName());
				});

		maResult.session().getVideoFilters()
				.forEach(f -> {
					final var filter = f.toFilter();
					aboutMeasure.row(
							"video",
							filter.getFilterName(),
							filter.toString(),
							f.getClass().getName());
				});

		save("filters.txt", exportDirectory, aboutMeasure.getLines());
	}

	@Override
	public void exportEbur128StrErrFilterEvent(final String source,
											   final List<Ebur128StrErrFilterEvent> ebur128events,
											   final File exportDirectory) {
		final var t = new Tabs(
				"Integrated",
				"Momentary",
				"Short-term",
				"Loudness Range",
				"Sample-peak L", "Sample-peak R",
				"True-peak per frame L", "True-peak per frame R",
				"True-peak L", "True-peak R");
		ebur128events.forEach(ebu -> t.row(
				ebu.getI(),
				ebu.getM(),
				ebu.getS(),
				ebu.getLra(),
				ebu.getSpk().left(),
				ebu.getSpk().right(),
				ebu.getFtpk().left(),
				ebu.getFtpk().right(),
				ebu.getTpk().left(),
				ebu.getTpk().right()));
		save("audio-ebur128.txt", exportDirectory, t.getLines());
	}

	@Override
	public void exportRawStdErrFilterEvent(final String source,
										   final List<RawStdErrFilterEvent> rawStdErrEvents,
										   final File exportDirectory) {
		final var t = new Tabs("Filter name", "Chain pos", "Line");
		rawStdErrEvents.stream()
				.filter(r -> r.getFilterName().equals("cropdetect") == false)
				.forEach(r -> t.row(r.getFilterName(), r.getFilterChainPos(), r.getLineValue()));
		save("rawstderrfilters.txt", exportDirectory, t.getLines());
	}

	// TODO test exportContainerAnalyserResult

	@Override
	public void exportContainerAnalyserResult(final String source,
											  final ContainerAnalyserResult caResult,
											  final File exportDirectory) {
		final var packets = new Tabs(
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
		save("packets.txt", exportDirectory, packets.getLines());

		final var vFrames = new Tabs(
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
		save("video-frames.txt", exportDirectory, vFrames.getLines());

		final var aFrames = new Tabs(
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
		save("audio-frames.txt", exportDirectory, aFrames.getLines());

		final var vConsts = new Tabs(
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
		Stream.concat(
				caResult.olderVideoConsts().stream(),
				Stream.of(caResult.videoConst()))
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
		save("video-consts.txt", exportDirectory, vConsts.getLines());

		final var aConsts = new Tabs(
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
		Stream.concat(
				caResult.olderAudioConsts().stream(),
				Stream.of(caResult.audioConst()))
				.forEach(c -> {
					final var frame = c.updatedWith().frame();
					vConsts.row(
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
		save("audio-consts.txt", exportDirectory, aConsts.getLines());
	}

	@Override
	public void exportFFprobeJAXB(final String source,
								  final FFprobeJAXB ffprobeResult,
								  final File exportDirectory) {
		try {
			FileUtils.write(
					new File(exportDirectory, "ffprobe.xml"),
					ffprobeResult.getXmlContent(),
					UTF_8,
					false);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write file", e);
		}
	}

	private void save(final String fileName, final File exportDirectory, final String lines) {
		if (lines.isEmpty()) {
			log.debug("Nothing to save to {}", fileName);
			return;
		}
		log.info("Save to {}", fileName);
		try {
			FileUtils.write(
					new File(exportDirectory, fileName),
					lines,
					UTF_8,
					false);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write file", e);
		}
	}

}
