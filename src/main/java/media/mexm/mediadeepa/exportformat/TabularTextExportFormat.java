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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdCropdetect;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdValue;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserResult;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.RawStdErrFilterEvent;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;

// TODO remove source
// TODO "-Infinity" > -144
// TODO audio-ebur128-summary > en ligne
// TODO end events null/0 > valeur de EOF
// TODO event add duration (displayed)
// FIXME astats > error
// FIXME no video-siti-stats-ITU-T_P-910.txt
// TODO filter out cropdetect from rawstderr
// TODO no contraction for cropdetect

public class TabularTextExportFormat implements ExportFormat {// TODO test
	private static final String PTS_TIME = "Pts time";
	private static final String STREAM_INDEX = "Stream index";
	private static final String VALUE = "Value";
	private static final String PTS = "PTS";
	private static final String FRAME = "Frame";
	private static final String SOURCE = "Source";

	private static final Logger log = LogManager.getLogger();

	@Override
	public void exportMediaAnalyserResult(final String source,
										  final MediaAnalyserResult maResult,
										  final File exportDirectory) {

		Optional.ofNullable(maResult.ebur128Summary())
				.ifPresent(ebu -> {
					final var t = new Tabs(
							SOURCE,
							"Integrated",
							"Integrated Threshold",
							"Loudness Range",
							"Loudness Range Threshold",
							"Loudness Range Low",
							"Loudness Range High",
							"Sample Peak",
							"True Peak");
					t.row(
							source,
							ebu.getIntegrated(),
							ebu.getIntegratedThreshold(),
							ebu.getLoudnessRange(),
							ebu.getLoudnessRangeThreshold(),
							ebu.getLoudnessRangeLow(),
							ebu.getLoudnessRangeHigh(),
							ebu.getSamplePeak(),
							ebu.getTruePeak());
					save("audio-ebur128-summary.txt", exportDirectory, t.getLines());
				});

		final var lavfiMetadatas = maResult.lavfiMetadatas();

		final var aPhaseMeter = new Tabs(SOURCE, FRAME, PTS, PTS_TIME, VALUE);
		lavfiMetadatas.getAPhaseMeterReport()
				.forEach(a -> aPhaseMeter.row(source, a.frame(), a.pts(), a.ptsTime(), a.value()));
		save("audio-phase-meter.txt", exportDirectory, aPhaseMeter.getLines());

		final var aStats = new Tabs(SOURCE, FRAME, PTS, PTS_TIME,
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
						aPhaseMeter.row(source, a.frame(), a.pts(), a.ptsTime(),
								pos + 1,
								channel.dcOffset(),
								channel.entropy(),
								channel.flatFactor(),
								channel.noiseFloor(),
								channel.noiseFloorCount(),
								channel.peakLevel(),
								channel.peakCount(),
								channel.other().toString());
					}
				});
		save("audio-stats.txt", exportDirectory, aStats.getLines());

		final var siti = new Tabs(SOURCE, FRAME, PTS, PTS_TIME, "Spatial Info", "Temporal Info");
		lavfiMetadatas.getSitiReport()
				.forEach(a -> siti.row(source, a.frame(), a.pts(), a.ptsTime(), a.value().si(), a.value().ti()));
		save("video-siti-ITU-T_P-910.txt", exportDirectory, siti.getLines());

		if (lavfiMetadatas.getSitiReport().isEmpty() == false) {
			final var sitiStats = new Tabs(SOURCE, "Type", "Average", "Count", "Max", "Min");
			final var stats = lavfiMetadatas.computeSitiStats();
			siti.row(source, "Spatial Info",
					stats.si().getAverage(),
					stats.si().getCount(),
					stats.si().getMax(),
					stats.si().getMin());
			siti.row(source, "Temporal Info",
					stats.ti().getAverage(),
					stats.ti().getCount(),
					stats.ti().getMax(),
					stats.ti().getMin());
			save("video-siti-stats-ITU-T_P-910.txt", exportDirectory, sitiStats.getLines());
		}

		final var block = new Tabs(SOURCE, FRAME, PTS, PTS_TIME, VALUE);
		lavfiMetadatas.getBlockDetectReport()
				.forEach(a -> block.row(source, a.frame(), a.pts(), a.ptsTime(), a.value()));
		save("video-block-detect.txt", exportDirectory, block.getLines());

		final var blur = new Tabs(SOURCE, FRAME, PTS, PTS_TIME, VALUE);
		lavfiMetadatas.getBlurDetectReport()
				.forEach(a -> blur.row(source, a.frame(), a.pts(), a.ptsTime(), a.value()));
		save("video-blur-detect.txt", exportDirectory, blur.getLines());

		final var crop = new Tabs(SOURCE, FRAME, PTS, PTS_TIME,
				"x1", "x2", "y1", "y2", "w", "h", "x", "y");
		lavfiMetadatas.getCropDetectReport().stream()
				.reduce(new ArrayList<LavfiMtdValue<LavfiMtdCropdetect>>(),
						(list, frame) -> {
							if (list.isEmpty() == false
								&& list.get(list.size() - 1).value().equals(frame.value())) {
								return list;
							}
							list.add(frame);
							return list;
						},
						(l, r) -> {
							l.addAll(r);
							return l;
						})
				.forEach(a -> {
					final var value = a.value();
					crop.row(source, a.frame(), a.pts(), a.ptsTime(),
							value.x1(), value.x2(), value.y1(), value.y2(),
							value.w(), value.h(), value.x(), value.y());
				});
		save("video-crop-detect.txt", exportDirectory, crop.getLines());

		final var idet = new Tabs(SOURCE, FRAME, PTS, PTS_TIME,
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

					idet.row(source, a.frame(), a.pts(), a.ptsTime(),
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

		final var events = new Tabs(SOURCE, "Name", "Scope/Channel", "Start", "End");
		Stream.of(
				lavfiMetadatas.getMonoEvents(),
				lavfiMetadatas.getSilenceEvents(),
				lavfiMetadatas.getBlackEvents(),
				lavfiMetadatas.getFreezeEvents())
				.flatMap(List::stream)
				.sorted()
				.forEach(ev -> events.row(source, ev.name(), ev.scope(), ev.start(), ev.end()));
		save("events.txt", exportDirectory, events.getLines());

		final var aboutMeasure = new Tabs(SOURCE, "Type", "Name", "Setup", "Java class");
		maResult.session().getAudioFilters()
				.forEach(f -> {
					final var filter = f.toFilter();
					aboutMeasure.row(
							source,
							"audio",
							filter.getFilterName(),
							filter.toString(),
							f.getClass().getName());
				});

		maResult.session().getVideoFilters()
				.forEach(f -> {
					final var filter = f.toFilter();
					aboutMeasure.row(
							source,
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
				SOURCE,
				"Integrated",
				"Momentary",
				"Short-term",
				"Loudness Range",
				"Sample-peak L", "Sample-peak R",
				"True-peak per frame L", "True-peak per frame R",
				"True-peak L", "True-peak R");
		ebur128events.forEach(ebu -> t.row(
				source,
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
		save("ebur128.txt", exportDirectory, t.getLines());
	}

	@Override
	public void exportRawStdErrFilterEvent(final String source,
										   final List<RawStdErrFilterEvent> rawStdErrEvents,
										   final File exportDirectory) {
		final var t = new Tabs(SOURCE, "Filter name", "Chain pos", "Line");
		rawStdErrEvents.forEach(
				r -> t.row(source, r.getFilterName(), r.getFilterChainPos(), r.getLineValue()));
		save("rawstderrfilters.txt", exportDirectory, t.getLines());
	}

	@Override
	public void exportContainerAnalyserResult(final String source,
											  final ContainerAnalyserResult caResult,
											  final File exportDirectory) {
		final var packets = new Tabs(SOURCE,
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
						source,
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

		final var vFrames = new Tabs(SOURCE,
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
					source,
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

		final var aFrames = new Tabs(SOURCE,
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
					source,
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

		final var vConsts = new Tabs(SOURCE,
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
					vConsts.row(source,
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

		final var aConsts = new Tabs(SOURCE,
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
					vConsts.row(source,
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
			log.info("Nothing to save in {}", fileName);
			return;
		}
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
