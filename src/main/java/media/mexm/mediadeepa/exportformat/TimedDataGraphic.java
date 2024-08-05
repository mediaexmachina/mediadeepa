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

import java.awt.Paint;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jfree.chart.ChartFactory;
import org.jfree.data.general.SeriesException;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TimedDataGraphic extends DataGraphic {

	private static final String RANGE_AXIS_CAN_T_TO_BE_NULL = "\"rangeAxis\" can't to be null";
	private final List<Series> series;
	private final List<FixedMillisecond> positions;

	public TimedDataGraphic(final Stream<Long> positionsMs, final RangeAxis rangeAxis) {
		this(
				positionsMs.map(FixedMillisecond::new).toList(),
				Objects.requireNonNull(rangeAxis, RANGE_AXIS_CAN_T_TO_BE_NULL));
	}

	public static TimedDataGraphic create(final Stream<Float> positionsS, final RangeAxis rangeAxis) {
		return new TimedDataGraphic(
				positionsS
						.map(s -> s * 1000)
						.map(Math::round)
						.map(FixedMillisecond::new).toList(),
				Objects.requireNonNull(rangeAxis, RANGE_AXIS_CAN_T_TO_BE_NULL));
	}

	public TimedDataGraphic(final List<FixedMillisecond> positions, final RangeAxis rangeAxis) {
		super(Objects.requireNonNull(rangeAxis, RANGE_AXIS_CAN_T_TO_BE_NULL));
		series = new ArrayList<>();
		this.positions = positions;
	}

	public TimedDataGraphic addMinMaxValueMarkers() {
		final var stats = series.stream()
				.map(Series::getDatas)
				.flatMap(List::stream)
				.filter(Float::isFinite)
				.filter(f -> Float.isNaN(f) == false)
				.mapToDouble(Float::doubleValue)
				.summaryStatistics();
		if (stats.getMax() != 0d) {
			addValueMarker(stats.getMax());
		}
		if (stats.getMin() != 0d) {
			addValueMarker(stats.getMin());
		}
		if (stats.getAverage() != 0d) {
			addValueMarker(stats.getAverage());
		}
		return this;
	}

	public TimedDataGraphic addValueMarker(final Optional<Number> position) {
		position.ifPresent(this::addValueMarker);
		return this;
	}

	public TimedDataGraphic addSeries(final Series s) {
		Objects.requireNonNull(s);
		series.add(s);
		return this;
	}

	public TimedDataGraphic cloneWithSamePositions(final RangeAxis rangeAxis) {
		return new TimedDataGraphic(positions, rangeAxis);
	}

	@Data
	@EqualsAndHashCode(callSuper = true)
	public class Series extends SeriesStyle {
		private final List<Float> datas;

		public Series(final String name,
					  final Paint paint,
					  final Stroke stroke,
					  final Stream<? extends Number> datas) {
			super(
					Objects.requireNonNull(name, "\"name\" can't to be null"),
					Objects.requireNonNull(paint, "\"paint\" can't to be null"),
					Objects.requireNonNull(stroke, "\"stroke\" can't to be null"));

			this.datas = datas.map(Number::floatValue)
					.map(f -> {
						if (f.isInfinite() || f.isNaN()) {
							return 0f;
						}
						return f;
					})
					.toList();
			if (positions.size() != this.datas.size()) {
				throw new IllegalArgumentException(
						"Invalid dataset size (" + this.datas.size() + "), expect " + positions.size());
			}
		}

		private TimeSeries getTimeSeries() {
			final var ts = new TimeSeries(getName());
			IntStream.range(0, datas.size())
					.forEach(pos -> {
						final var valuePos = positions.get(pos);
						final var data = datas.get(pos);
						try {
							ts.add(valuePos, data);
						} catch (final SeriesException se) {
							log.warn("Duplicate values added on \"{}\": {} was previouly set. New value ignored: {}.",
									getName(), valuePos.getSerialIndex(), data);
						}
					});
			return ts;
		}

	}

	@Override
	protected ChartGraphicWrapper getChart() {
		final var tsc = new TimeSeriesCollection();
		series.stream()
				.map(Series::getTimeSeries)
				.forEach(tsc::addSeries);
		return new ChartGraphicWrapper(
				rangeAxis.name(),
				getSeriesStyle(),
				ChartFactory.createTimeSeriesChart("", "", "", tsc, true, false, false));
	}

	@Override
	protected List<? extends SeriesStyle> getSeriesStyle() {
		return series;
	}

}
