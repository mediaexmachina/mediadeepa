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
import static java.util.stream.Collectors.joining;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class Tabs {// TODO test
	private final String header;
	private final List<String> lines;

	public Tabs(final String... head) {
		lines = new ArrayList<>();
		header = Optional.ofNullable(head).stream()
				.flatMap(Stream::of)
				.collect(joining("\t"));
		if (header.isEmpty()) {
			throw new IllegalArgumentException("Empty header");
		}
	}

	public void row(final Object... item) {
		final var row = Optional.ofNullable(item).stream()
				.flatMap(Stream::of)
				.map(o -> {
					if (o == null) {
						return "";
					} else if (o instanceof final String str) {
						return str;
					} else if (o instanceof final Duration d) {
						return durationToString(d);
					} else if (o instanceof final Float oF
							   && oF == Float.NEGATIVE_INFINITY) {
						return "-144";
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
