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

import media.mexm.mediadeepa.ExportFormat;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.RawStdErrFilterEvent;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;

public class TabularTextExportFormat implements ExportFormat {
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
					save("ebur128-summary.txt", exportDirectory, t.getLines());
				});

		// XXX
		maResult.lavfiMetadatas().getAPhaseMeterReport();
		maResult.lavfiMetadatas().getAStatsReport();
		maResult.lavfiMetadatas().getBlockDetectReport();
		maResult.lavfiMetadatas().getBlurDetectReport();
		maResult.lavfiMetadatas().getCropDetectReport();
		maResult.lavfiMetadatas().getIdetReport();
		maResult.lavfiMetadatas().getSitiReport();
		maResult.lavfiMetadatas().computeSitiStats();

		if (maResult.lavfiMetadatas().getEventCount() > 0) {
			final var events = new Tabs(SOURCE, "Name", "Scope/Channel", "Start", "End");
			Stream.of(
					maResult.lavfiMetadatas().getMonoEvents(),
					maResult.lavfiMetadatas().getSilenceEvents(),
					maResult.lavfiMetadatas().getBlackEvents(),
					maResult.lavfiMetadatas().getFreezeEvents())
					.flatMap(List::stream)
					.sorted()
					.forEach(ev -> events.row(source, ev.name(), ev.scope(), ev.start(), ev.end()));

			save("events.txt", exportDirectory, events.getLines());
		}

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
		final var t = new Tabs("Filter name", "Chain pos", "Line");
		rawStdErrEvents.forEach(
				r -> t.row(r.getFilterName(), r.getFilterChainPos(), r.getLineValue()));
		save("rawstderrfilters.txt", exportDirectory, t.getLines());
	}

	class Tabs {
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

	public static String durationToString(final Duration d) {
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
