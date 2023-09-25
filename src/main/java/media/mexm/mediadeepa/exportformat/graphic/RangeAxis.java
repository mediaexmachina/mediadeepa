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
package media.mexm.mediadeepa.exportformat.graphic;

import static java.awt.Color.GRAY;
import static java.lang.Double.isNaN;
import static java.lang.Math.abs;

import java.awt.Font;
import java.util.stream.Stream;

import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;

public record RangeAxis(String name, Number min, Number max) {

	public static <T extends Number> RangeAxis createFromValueSet(final String name,
																  final Number floor,
																  final Number subMarginMin,
																  final Number addMarginMax,
																  final Stream<T> values) {
		final var doubleFloor = floor.doubleValue();
		final var stats = values
				.mapToDouble(Number::doubleValue)
				.filter(Double::isFinite)
				.filter(d -> isNaN(d) == false)
				.filter(v -> v > doubleFloor)
				.summaryStatistics();
		return new RangeAxis(
				name,
				stats.getMin() - subMarginMin.doubleValue(),
				stats.getMax() + addMarginMax.doubleValue());
	}

	public static <T extends Number> RangeAxis createFromRelativesValueSet(final String name,
																		   final Number min,
																		   final Stream<T> values) {
		final var stats = values.mapToDouble(Number::doubleValue)
				.filter(Double::isFinite)
				.filter(v -> Double.isNaN(v) == false)
				.summaryStatistics();
		final var maxAbsValue = Math.max(abs(stats.getMax()), abs(stats.getMin()));

		double maxRangeValue;
		if (maxAbsValue < min.doubleValue()) {
			maxRangeValue = min.doubleValue();
		} else {
			final var floorValue = Math.floor(maxAbsValue);
			maxRangeValue = floorValue + floorValue / 10;
		}

		final var manageNegative = stats.getMax() < 0d || stats.getMin() < 0d;
		if (manageNegative) {
			return new RangeAxis(name, -maxRangeValue, maxRangeValue);
		} else {
			return new RangeAxis(name, 0, maxRangeValue);
		}
	}

	LogarithmicAxis toLogarithmicAxis(final Font font) {
		final var rangeAxis = new LogarithmicAxis(name);
		rangeAxis.setAllowNegativesFlag(true);
		setRangeAxisParam(font, rangeAxis);
		return rangeAxis;
	}

	public ValueAxis toLinearAxis(final Font font) {
		final var rangeAxis = new NumberAxis(name);
		setRangeAxisParam(font, rangeAxis);
		return rangeAxis;
	}

	private void setRangeAxisParam(final Font font, final NumberAxis rangeAxis) {
		rangeAxis.setRange(min.doubleValue(), max.doubleValue());
		rangeAxis.setAutoRange(false);
		rangeAxis.setLabelPaint(GRAY);
		rangeAxis.setTickLabelPaint(GRAY);
		rangeAxis.setLabelFont(font.deriveFont(10));
		rangeAxis.setTickLabelFont(font.deriveFont(10));
		rangeAxis.setTickLabelsVisible(true);
		rangeAxis.setTickMarksVisible(true);
		rangeAxis.setTickMarkPaint(GRAY);
	}
}
