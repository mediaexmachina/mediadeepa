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
import static j2html.TagCreator.li;
import static j2html.TagCreator.span;
import static j2html.TagCreator.ul;
import static java.util.Locale.ENGLISH;
import static java.util.function.Predicate.not;
import static media.mexm.mediadeepa.exportformat.report.NumericUnitValueReportEntry.valueToString;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.stream.Stream;

import j2html.tags.DomContent;

public class StatisticsUnitValueReportEntry implements ReportEntry, NumericReportEntry {

	static final String LABEL_MAXIMUM = "Maximum";
	static final String LABEL_MEDIAN = "Median";
	static final String LABEL_AVERAGE = "Average";
	static final String LABEL_MINIMUM = "Minimum";

	private static final ItemValue EMPTY_ITEM_VALUE = new ItemValue("", false);
	private static final String KEY_CSS = ".key";
	private static final String VALUE = ".value";
	private static final String UNIT_CSS = ".unit";
	private final String key;
	private final ItemValue min;
	private final ItemValue max;
	private final ItemValue median;
	private final ItemValue average;
	private final String unit;
	private final boolean empty;

	private StatisticsUnitValueReportEntry(final String key,
										   final ItemValue min,
										   final ItemValue max,
										   final ItemValue median,
										   final ItemValue average,
										   final String unit,
										   final boolean empty) {
		this.key = key;
		this.min = min;
		this.max = max;
		this.median = median;
		this.average = average;
		this.unit = unit;
		this.empty = empty;
	}

	private record ItemValue(String value, boolean isPlurial) {
	}

	public static StatisticsUnitValueReportEntry createFromFloat(final String key,
																 final Stream<Float> source,
																 final String unit) {
		return createFromDouble(key, source.map(d -> (double) d), unit, "#,###.#");
	}

	public static StatisticsUnitValueReportEntry createFromDouble(final String key,
																  final Stream<Double> source,
																  final String unit,
																  final String format) {
		final var decimalFormat = new DecimalFormat(format);
		decimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(ENGLISH));

		final var sourceList = source.filter(not(f -> f.isNaN())).toList();
		if (sourceList.isEmpty()) {
			return new StatisticsUnitValueReportEntry(key,
					EMPTY_ITEM_VALUE,
					EMPTY_ITEM_VALUE,
					EMPTY_ITEM_VALUE,
					EMPTY_ITEM_VALUE,
					unit, true);
		}

		final var stats = sourceList.stream().mapToDouble(d -> d).summaryStatistics();
		final var average = decimalFormat.format(stats.getAverage());
		final var max = decimalFormat.format(stats.getMax());
		final var min = decimalFormat.format(stats.getMin());
		final var valMedian = sourceList.stream()
				.mapToDouble(d -> d)
				.sorted()
				.skip(sourceList.size() / 2)
				.findFirst()
				.orElseThrow();
		final var median = decimalFormat.format(valMedian);

		return new StatisticsUnitValueReportEntry(
				key,
				new ItemValue(min, stats.getMin() > 1d),
				new ItemValue(max, stats.getMax() > 1d),
				new ItemValue(median, valMedian > 1d),
				new ItemValue(average, stats.getAverage() > 1d),
				unit, false);
	}

	public static StatisticsUnitValueReportEntry createFromInteger(final String key,
																   final Stream<Integer> source,
																   final String unit) {
		return createFromLong(key, source.map(f -> (long) f), unit);
	}

	public static StatisticsUnitValueReportEntry createFromLong(final String key,
																final Stream<Long> source,
																final String unit) {
		final var sourceList = source.toList();
		if (sourceList.isEmpty()) {
			return new StatisticsUnitValueReportEntry(key,
					EMPTY_ITEM_VALUE,
					EMPTY_ITEM_VALUE,
					EMPTY_ITEM_VALUE,
					EMPTY_ITEM_VALUE,
					unit, true);
		}
		final var stats = sourceList.stream().mapToLong(d -> d).summaryStatistics();
		final var average = valueToString(stats.getAverage());
		final var max = valueToString(stats.getMax());
		final var min = valueToString(stats.getMin());
		final var valMedian = sourceList.stream()
				.mapToLong(d -> d)
				.sorted()
				.skip(sourceList.size() / 2)
				.findFirst()
				.orElseThrow();
		final var median = valueToString(valMedian);
		return new StatisticsUnitValueReportEntry(
				key,
				new ItemValue(min, stats.getMin() > 1l),
				new ItemValue(max, stats.getMax() > 1l),
				new ItemValue(median, valMedian > 1l),
				new ItemValue(average, stats.getAverage() > 1l),
				unit, false);
	}

	@Override
	public boolean isEmpty() {
		return empty;
	}

	@Override
	public DomContent toDomContent() {
		if (min.value.equals(max.value)) {
			return div(attrs(".entry.stats"),
					span(attrs(KEY_CSS), key),
					span(attrs(VALUE), min.value),
					span(attrs(UNIT_CSS), " " + getUnitWithPlurial(min.isPlurial)));
		}

		return div(attrs(".entry.stats"),
				span(attrs(KEY_CSS), key),
				ul(attrs(VALUE),
						li(span(attrs(KEY_CSS), LABEL_MINIMUM),
								span(attrs(VALUE), min.value),
								span(attrs(UNIT_CSS), " " + getUnitWithPlurial(min.isPlurial))),
						li(span(attrs(KEY_CSS), LABEL_AVERAGE),
								span(attrs(VALUE), average.value),
								span(attrs(UNIT_CSS), " " + getUnitWithPlurial(average.isPlurial))),
						li(span(attrs(KEY_CSS), LABEL_MEDIAN),
								span(attrs(VALUE), median.value),
								span(attrs(UNIT_CSS), " " + getUnitWithPlurial(median.isPlurial))),
						li(span(attrs(KEY_CSS), LABEL_MAXIMUM),
								span(attrs(VALUE), max.value),
								span(attrs(UNIT_CSS), " " + getUnitWithPlurial(max.isPlurial)))));
	}

	@Override
	public String key() {
		return key;
	}

	@Override
	public String unit() {
		return unit;
	}
}
