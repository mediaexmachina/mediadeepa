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
package media.mexm.mediadeepa.components;

import static java.lang.Float.NaN;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.FRENCH;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

@Component
public class NumberUtils {

	private final DecimalFormat decimalFormatSimple1En;
	private final DecimalFormat decimalFormatSimple5En;
	private final DecimalFormat decimalFormatSimple1Fr;
	private final DecimalFormat decimalFormatSimple5Fr;
	private final DecimalFormat decimalFormatFull1En;
	private final DecimalFormat decimalFormatFull3En;
	private final DecimalFormat fixedFormatFullEn;

	public NumberUtils() {
		decimalFormatSimple1En = new DecimalFormat("#.#");
		decimalFormatSimple1En.setDecimalFormatSymbols(new DecimalFormatSymbols(ENGLISH));

		decimalFormatSimple5En = new DecimalFormat("#.#####");
		decimalFormatSimple5En.setDecimalFormatSymbols(new DecimalFormatSymbols(ENGLISH));

		decimalFormatSimple1Fr = new DecimalFormat("#.#");
		decimalFormatSimple1Fr.setDecimalFormatSymbols(new DecimalFormatSymbols(FRENCH));

		decimalFormatSimple5Fr = new DecimalFormat("#.#####");
		decimalFormatSimple5Fr.setDecimalFormatSymbols(new DecimalFormatSymbols(FRENCH));

		decimalFormatFull1En = new DecimalFormat("#,###.#");
		decimalFormatFull1En.setDecimalFormatSymbols(new DecimalFormatSymbols(ENGLISH));

		decimalFormatFull3En = new DecimalFormat("#,###.###");
		decimalFormatFull3En.setDecimalFormatSymbols(new DecimalFormatSymbols(ENGLISH));

		fixedFormatFullEn = new DecimalFormat("#,###");
		fixedFormatFullEn.setDecimalFormatSymbols(new DecimalFormatSymbols(ENGLISH));
	}

	/**
	 * "#.#"
	 */
	public String formatDecimalSimple1En(final Object o) {
		return decimalFormatSimple1En.format(o);
	}

	/**
	 * "#.#####"
	 */
	public String formatDecimalSimple5En(final Object o) {
		return decimalFormatSimple5En.format(o);
	}

	/**
	 * "#.#"
	 */
	public String formatDecimalSimple1Fr(final Object o) {
		return decimalFormatSimple1Fr.format(o);
	}

	/**
	 * "#.#####"
	 */
	public String formatDecimalSimple5Fr(final Object o) {
		return decimalFormatSimple5Fr.format(o);
	}

	/**
	 * "#,###.#"
	 */
	public String formatDecimalFull1En(final Object o) {
		return decimalFormatFull1En.format(o);
	}

	/**
	 * "#,###"
	 */
	public String formatFixedFullEn(final Object o) {
		return fixedFormatFullEn.format(o);
	}

	/**
	 * "#,###.###"
	 */
	public String formatDecimalFull3En(final Object o) {
		return decimalFormatFull3En.format(o);
	}

	public String valueToString(final Number value) {
		if (value == null) {
			return "?";
		} else if (value instanceof final Long l) {
			return fixedFormatFullEn.format(l);
		} else if (value instanceof final Integer i) {
			return fixedFormatFullEn.format(i);
		} else if (value instanceof final Float f) {
			if (f.isNaN()) {
				return "?";
			} else if (f == Float.NEGATIVE_INFINITY) {
				return fixedFormatFullEn.format(-144);
			} else if (f == Float.POSITIVE_INFINITY) {
				return fixedFormatFullEn.format(144);
			} else {
				return decimalFormatFull1En.format(f);
			}
		} else if (value instanceof final Double d) {
			if (d.isNaN()) {
				return "?";
			} else if (d == Double.NEGATIVE_INFINITY) {
				return fixedFormatFullEn.format(-144);
			} else if (d == Double.POSITIVE_INFINITY) {
				return fixedFormatFullEn.format(144);
			} else {
				return decimalFormatFull1En.format(d);
			}
		}
		return String.valueOf(value);
	}

	public String durationToString(final Duration d) {
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

	public float secToMs(final float s) {
		return s * 1000f;
	}

	public double[] getTimeDerivative(final Stream<Float> timeValues, final int itemCount) {
		final var result = new double[itemCount];

		final var interator = timeValues.iterator();
		Float previous;
		var actual = NaN;
		var pos = 0;
		while (interator.hasNext()) {
			previous = actual;
			actual = interator.next();
			if (Float.isNaN(previous) == false) {
				result[pos++] = actual - previous;
			}
		}
		return result;
	}

}
