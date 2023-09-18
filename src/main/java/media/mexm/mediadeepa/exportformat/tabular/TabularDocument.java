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

import static java.lang.Float.NEGATIVE_INFINITY;
import static java.lang.Float.POSITIVE_INFINITY;
import static media.mexm.mediadeepa.components.CLIRunner.makeOutputFileName;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Manage structured header/cols/row document
 */
@Slf4j
public class TabularDocument {
	private final List<List<String>> lines;
	private List<String> header;
	private final TabularDocumentExporter exporter;
	private final String baseFileName;

	public TabularDocument(final TabularDocumentExporter exporter, final String baseFileName) {
		this.exporter = Objects.requireNonNull(exporter);
		this.baseFileName = Objects.requireNonNull(baseFileName, "\"baseFileName\" can't to be null");
		lines = new ArrayList<>();
		header = List.of();
	}

	public TabularDocument head(final List<String> header) {
		this.header = Objects.requireNonNull(header, "\"header\" can't to be null");
		if (header.isEmpty()) {
			throw new IllegalArgumentException("Empty header");
		}
		return this;
	}

	public void row(final List<String> item) {
		if (item == null || item.isEmpty()) {
			return;
		} else if (header.size() != item.size()) {
			throw new IllegalArgumentException(
					"Invalid item count (" + item.size() + "), header count is " + header.size() + ". Idem=" + item);
		}
		lines.add(item);
	}

	public TabularDocument row(final Object... item) {
		row(Optional.ofNullable(item).stream()
				.flatMap(Stream::of)
				.map(o -> {
					if (o == null) {
						return "";
					} else if (o instanceof final String str) {
						return str;
					} else if (o instanceof final Duration d) {
						return durationToString(d);
					} else if (o instanceof final Float oF) {
						if (oF.isNaN()) {
							return "";
						} else if (oF == NEGATIVE_INFINITY) {
							return "-144";
						} else if (oF == POSITIVE_INFINITY) {
							return "144";
						}
						return exporter.formatToString(oF, false);
					} else if (o instanceof Number) {
						return String.valueOf(o);
					} else {
						return o.toString();
					}
				})
				.toList());
		return this;
	}

	public static List<String> toList(final String... values) {
		return Optional.ofNullable(values).stream()
				.flatMap(Stream::of)
				.filter(Objects::nonNull)
				.toList();
	}

	public static record ExportTo(File exportDirectory, String prefixFileName) {
	}

	public File exportToFile(final ExportTo exportTo) {
		final var outfile = new File(exportTo.exportDirectory,
				makeOutputFileName(exportTo.prefixFileName, baseFileName) + "." + exporter.getDocumentFileExtension());
		if (lines.isEmpty()) {
			log.trace("Nothing to export for {}", outfile.getName());
			return null;
		}

		log.info("Save to {}", outfile);
		try {
			FileUtils.writeByteArrayToFile(outfile, exporter.getDocument(header, lines));
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write file", e);
		}
		return outfile;
	}

	public static String durationToString(final Duration d) {
		if (d == null) {
			return "";
		} else if (d == Duration.ZERO) {
			return "00:00:00";
		}

		final var buf = new StringBuilder();

		final var hours = d.toHoursPart() + d.toDaysPart() * 24l;
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

}
