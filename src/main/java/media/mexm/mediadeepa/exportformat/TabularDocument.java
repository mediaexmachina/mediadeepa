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

import static java.util.Locale.ENGLISH;

import java.io.File;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Manage structured header/cols/row document
 */
public class TabularDocument<T> {
	private final List<List<String>> lines;
	private List<String> header;
	private final DocumentJoiner<T> documentJoiner;

	public TabularDocument(final DocumentJoiner<T> documentJoiner) {
		this.documentJoiner = Objects.requireNonNull(documentJoiner, "\"documentJoiner\" can't to be null");
		lines = new ArrayList<>();
	}

	public TabularDocument<T> head(final String... head) {
		header = toList(head);
		if (header.isEmpty()) {
			throw new IllegalArgumentException("Empty header");
		}
		return this;
	}

	public void row(final List<String> item) {
		if (item == null || item.isEmpty()) {
			return;
		}
		lines.add(item);
	}

	// FIXME audiostat display NaN

	public TabularDocument<T> row(final Object... item) {
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
						if (oF == Float.NEGATIVE_INFINITY
							|| oF == Float.NaN) {
							return "-144";
						}
						final var dfMs = new DecimalFormat("#.#####");
						dfMs.setRoundingMode(RoundingMode.CEILING);
						dfMs.setDecimalFormatSymbols(new DecimalFormatSymbols(ENGLISH));
						return dfMs.format(oF);
					} else if (o instanceof Number) {
						return String.valueOf(o);
					} else {
						return o.toString();
					}
				})
				.toList());
		return this;
	}

	public T getDocument() {
		if (lines.isEmpty()) {
			return documentJoiner.emptyDocument();
		}
		return documentJoiner.assembleDocument(header, lines);
	}

	public static List<String> toList(final String... values) {
		return Optional.ofNullable(values).stream()
				.flatMap(Stream::of)
				.filter(Objects::nonNull)
				.toList();
	}

	public void save(final String fileName, final File exportDirectory) {
		if (lines.isEmpty() == false) {
			documentJoiner.save(fileName, exportDirectory, getDocument());
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

}
