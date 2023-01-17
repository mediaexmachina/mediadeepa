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

import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdCropdetect;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdValue;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.RawStdErrFilterEvent;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;

public class TabularTextExportFormat implements ExportFormat {// TODO test
	private static final String VALUE = "Value";
	private static final String PTS_TIME = "PTS time";
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

	class Tabs {// TODO export
		private final String header;
		private final List<String> lines;

		Tabs(final String... head) {
			lines = new ArrayList<>();
			header = Optional.ofNullable(head).stream()
					.flatMap(Stream::of)
					.collect(joining("\t"));
			if (header.isEmpty()) {
				throw new IllegalArgumentException("Empty header");
			}
		}

		void row(final Object... item) {
			final var row = Optional.ofNullable(item).stream()
					.flatMap(Stream::of)
					.map(o -> {
						if (o == null) {
							return "";
						} else if (o instanceof final String str) {
							return str;
						} else if (o instanceof final Duration d) {
							return durationToString(d);
						} else if (o instanceof Number) {
							return String.valueOf(o);
						} else {
							return o.toString();
						}
					})
					.collect(joining("\t"));
			if (row.isEmpty() == false) {
				lines.add(row);
			}
		}

		String getLines() {
			if (lines.isEmpty()) {
				return "";
			}
			return header + lineSeparator()
				   + lines.stream().collect(joining(lineSeparator()))
				   + lineSeparator();
		}
	}

	public static String durationToString(final Duration d) {// TODO export
		if (d == null || d == Duration.ZERO) {
			return "0";
		}

		final var buf = new StringBuilder();

		final var hours = d.toHoursPart();
		if (hours > 9) {
			buf.append(hours).append(':');
		} else {
			buf.append("0").append(hours).append(':');
		}

		final var minutes = d.toMinutesPart();
		if (minutes > 9) {
			buf.append(minutes).append(':');
		} else {
			buf.append("0").append(minutes).append(':');
		}

		final var secs = d.toSecondsPart();
		if (secs > 9) {
			buf.append(secs);
		} else {
			buf.append("0").append(secs);
		}

		final var msec = d.toMillisPart();
		if (msec > 99) {
			buf.append('.').append(msec);
		} else if (msec > 9) {
			buf.append(".0").append(msec);
		} else if (msec > 0) {
			buf.append(".00").append(msec);
		}

		return buf.toString();
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
