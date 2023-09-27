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

public record NumericUnitValueReportEntry(
										  String key,
										  Number value,
										  String unit) implements ReportEntry, NumericReportEntry {

	private static final DecimalFormat decimalFormat;
	private static final DecimalFormat fixedFormat;

	@Override
	public boolean isEmpty() {
		return value == null || ((Double) value.doubleValue()).isNaN();
	}

	static {
		decimalFormat = new DecimalFormat("#,###.#");
		decimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(ENGLISH));
		fixedFormat = new DecimalFormat("#,###");
		fixedFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(ENGLISH));
	}

	static String valueToString(final Number value) {
		if (value == null) {
			return "?";
		} else if (value instanceof final Long l) {
			return fixedFormat.format(l);
		} else if (value instanceof final Integer i) {
			return fixedFormat.format(i);
		} else if (value instanceof final Float f) {
			if (f.isNaN()) {
				return "?";
			} else if (f == Float.NEGATIVE_INFINITY) {
				return fixedFormat.format(-144);
			} else if (f == Float.POSITIVE_INFINITY) {
				return fixedFormat.format(144);
			} else {
				return decimalFormat.format(f);
			}
		} else if (value instanceof final Double d) {
			if (d.isNaN()) {
				return "?";
			} else if (d == Double.NEGATIVE_INFINITY) {
				return fixedFormat.format(-144);
			} else if (d == Double.POSITIVE_INFINITY) {
				return fixedFormat.format(144);
			} else {
				return decimalFormat.format(d);
			}
		}
		return String.valueOf(value);
	}

	@Override
	public DomContent toDomContent() {
		if (value.longValue() == 0) {
			return div(attrs(".entry"),
					span(attrs(".key"), getKeyWithPlurial(false)),
					span(attrs(".value"), "no"),
					span(attrs(".unit"), " " + getUnitWithPlurial(false)));
		}

		final var plurial = value.longValue() > 1;
		return div(attrs(".entry"),
				span(attrs(".key"), getKeyWithPlurial(plurial)),
				span(attrs(".value"), valueToString(value)),
				span(attrs(".unit"), " " + getUnitWithPlurial(plurial)));
	}

}
