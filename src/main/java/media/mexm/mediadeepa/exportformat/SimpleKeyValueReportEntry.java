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
package media.mexm.mediadeepa.exportformat;

import static j2html.TagCreator.attrs;
import static j2html.TagCreator.div;
import static j2html.TagCreator.span;

import j2html.tags.DomContent;
import media.mexm.mediadeepa.NumberFormator;
import media.mexm.mediadeepa.components.NumberUtils;

public record SimpleKeyValueReportEntry(String key, String value) implements ReportEntry {

	@Override
	public DomContent toDomContent(final NumberUtils numberUtils) {
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

	public static SimpleKeyValueReportEntry getFromRatio(final String key,
														 final int x,
														 final int y,
														 final NumberFormator formator) {
		if (x < 1) {
			throw new IllegalArgumentException("Invalid x: " + x);
		}
		if (y < 1) {
			throw new IllegalArgumentException("Invalid y: " + y);
		}
		final var ratio = formator.format((double) x / (double) y);// NOSONAR S3518
		final var gcd = gcd(x, y);
		return new SimpleKeyValueReportEntry(key,
				x / gcd + ":" + y / gcd + " (" + ratio + ")");
	}

}
