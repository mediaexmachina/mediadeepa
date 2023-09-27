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
package media.mexm.mediadeepa.exportformat.report;

import static j2html.TagCreator.attrs;
import static j2html.TagCreator.div;
import static j2html.TagCreator.span;
import static java.util.Locale.ENGLISH;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import j2html.tags.DomContent;

public record SimpleKeyValueReportEntry(String key, String value) implements ReportEntry {

	private static final DecimalFormat decimalFormat;

	static {
		decimalFormat = new DecimalFormat("#,###.###");
		decimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(ENGLISH));
	}

	@Override
	public DomContent toDomContent() {
		return div(attrs(".entry"),
				span(attrs(".key"), key), span(attrs(".value"), value));
	}

	@Override
	public boolean isEmpty() {
		return value == null || value.isEmpty();
	}

	private static int gcd(final int l, final int r) {
		if (r == 0) {
			return l;
		} else {
			return gcd(r, l % r);
		}
	}

	public static SimpleKeyValueReportEntry getFromRatio(final String key, final int x, final int y) {
		if (x < 1) {
			throw new IllegalArgumentException("Invalid x: " + x);
		}
		if (y < 1) {
			throw new IllegalArgumentException("Invalid y: " + y);
		}
		final var ratio = decimalFormat.format((double) x / (double) y);// NOSONAR S3518
		final var gcd = gcd(x, y);
		return new SimpleKeyValueReportEntry(key,
				x / gcd + ":" + y / gcd + " (" + ratio + ")");
	}

}
