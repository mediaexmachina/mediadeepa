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

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;
import static java.awt.Color.BLACK;
import static java.awt.Color.GRAY;
import static java.awt.Color.WHITE;
import static java.awt.Font.BOLD;
import static java.lang.Math.round;
import static java.util.Locale.ENGLISH;
import static org.jfree.chart.ui.TextAnchor.CENTER_LEFT;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Stroke;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TimedDataGraphic {

	public static final Dimension IMAGE_SIZE_FULL_HEIGHT = new Dimension(2000, 1200);
	public static final Dimension IMAGE_SIZE_HALF_HEIGHT = new Dimension(2000, IMAGE_SIZE_FULL_HEIGHT.height / 2);
	public static final Color FULL_PINK = Color.getHSBColor(0.83f, 1f, 1f);

	public static final BasicStroke THIN_STROKE = new BasicStroke(3, CAP_BUTT, JOIN_MITER);
	public static final BasicStroke THICK_STROKE = new BasicStroke(6, CAP_BUTT, JOIN_MITER);

	private static final float JPG_COMPRESSION_RATIO = 0.95f;
	private static final String DECIMAL_FORMAT_PATTERN = "#.#";

	private final List<Series> series;
	private final List<Double> markers;
	private final List<FixedMillisecond> positions;
	private final RangeAxis rangeAxis;
	private final DecimalFormat decimalFormat;

	public TimedDataGraphic(final Stream<Long> positionsMs, final RangeAxis rangeAxis) {
		this(
				positionsMs.map(FixedMillisecond::new).toList(),
				Objects.requireNonNull(rangeAxis, "\"rangeAxis\" can't to be null"));
	}

	public static TimedDataGraphic create(final Stream<Float> positionsS, final RangeAxis rangeAxis) {
		return new TimedDataGraphic(
				positionsS
						.map(s -> s * 1000)
						.map(Math::round)
						.map(FixedMillisecond::new).toList(),
				Objects.requireNonNull(rangeAxis, "\"rangeAxis\" can't to be null"));
	}

	private TimedDataGraphic(final List<FixedMillisecond> positions, final RangeAxis rangeAxis) {
		series = new ArrayList<>();
		markers = new ArrayList<>();
		this.positions = positions;
		this.rangeAxis = rangeAxis;

		decimalFormat = new DecimalFormat(DECIMAL_FORMAT_PATTERN);
		decimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(ENGLISH));
	}

	public TimedDataGraphic addValueMarker(final Number position) {
		if (position.intValue() == -10) {
			return this;
		}
		final var longPos = Math.round(position.doubleValue());
		if (markers.stream()
				.map(Math::round)
				.noneMatch(l -> l == longPos)) {
			markers.add(position.doubleValue());
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

	public static record RangeAxis(String name, Number min, Number max) {

		public static <T extends Number> RangeAxis createFromValueSet(final String name,
																	  final Number floor,
																	  final Number subMarginMin,
																	  final Number addMarginMax,
																	  final Stream<T> values) {
			final var doubleFloor = floor.doubleValue();
			final var stats = values.mapToDouble(Number::doubleValue)
					.filter(v -> v > doubleFloor)
					.summaryStatistics();
			return new RangeAxis(
					name,
					stats.getMin() - subMarginMin.doubleValue(),
					stats.getMax() + addMarginMax.doubleValue());
		}

		private LogarithmicAxis toLogarithmicAxis(final Font font) {
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
			rangeAxis.setLabelFont(font);
			rangeAxis.setTickLabelFont(font);
			rangeAxis.setTickLabelsVisible(true);
			rangeAxis.setTickMarksVisible(true);
			rangeAxis.setTickMarkPaint(GRAY);
		}

	}

	public class Series {
		private final String name;
		private final Paint paint;
		private final Stroke stroke;
		private final List<Float> datas;

		public Series(final String name,
					  final Paint paint,
					  final Stroke stroke,
					  final Stream<? extends Number> datas) {
			this.name = Objects.requireNonNull(name, "\"name\" can't to be null");
			this.paint = Objects.requireNonNull(paint, "\"paint\" can't to be null");
			this.stroke = Objects.requireNonNull(stroke, "\"stroke\" can't to be null");

			this.datas = datas.map(Number::floatValue).toList();
			if (positions.size() != this.datas.size()) {
				throw new IllegalArgumentException(
						"Invalid dataset size (" + this.datas.size() + "), expect " + positions.size());
			}
		}

		private TimeSeries getTimeSeries() {
			final var ts = new TimeSeries(name);
			IntStream.range(0, datas.size())
					.forEach(pos -> ts.add(positions.get(pos), datas.get(pos)));
			return ts;
		}

	}

	public void makeLogarithmicAxisGraphic(final File outputFile, final Dimension imageSize) {
		makeGraphic(outputFile, imageSize, true);
	}

	public void makeLinearAxisGraphic(final File outputFile, final Dimension imageSize) {
		makeGraphic(outputFile, imageSize, false);
	}

	private void makeGraphic(final File outputFile, final Dimension imageSize, final boolean logarithmicAxis) {
		log.info("Save graphic to {}", outputFile);

		final var tsc = new TimeSeriesCollection();
		series.stream()
				.map(Series::getTimeSeries)
				.forEach(tsc::addSeries);

		final var timechart = ChartFactory.createTimeSeriesChart("", "", "", tsc, true, false, false);
		timechart.setAntiAlias(true);
		timechart.setTextAntiAlias(true);
		timechart.setBackgroundPaint(BLACK);
		final var legend = timechart.getLegend();
		legend.setBackgroundPaint(BLACK);

		final var plot = timechart.getXYPlot();
		plot.setBackgroundPaint(BLACK);
		plot.setOutlinePaint(GRAY);

		final var renderer = (XYLineAndShapeRenderer) plot.getRenderer(0);
		renderer.setDefaultLegendTextPaint(GRAY);

		final var font = renderer.getDefaultItemLabelFont().deriveFont(28f);

		markers.stream()
				.map(ref -> {
					final var marker = new ValueMarker(ref);
					final var label = decimalFormat.format(ref);
					if (label.endsWith(".0")) {
						marker.setLabel(String.valueOf(round(ref)));
					} else {
						marker.setLabel(label);
					}

					marker.setLabelPaint(WHITE);
					marker.setPaint(WHITE);
					marker.setLabelFont(font.deriveFont(BOLD));
					marker.setLabelTextAnchor(CENTER_LEFT);

					final float[] dash = { 5.0f };
					marker.setStroke(new BasicStroke(1, CAP_BUTT, JOIN_MITER, 10.0f, dash, 0.0f));
					return marker;
				})
				.forEach(plot::addRangeMarker);

		if (logarithmicAxis) {
			plot.setRangeAxis(rangeAxis.toLogarithmicAxis(font));
		} else {
			plot.setRangeAxis(rangeAxis.toLinearAxis(font));
		}

		renderer.setDefaultItemLabelFont(font);
		renderer.setDefaultLegendTextFont(font);
		renderer.setLegendTextFont(0, font);
		renderer.setSeriesItemLabelFont(0, font);

		for (var pos = 0; pos < series.size(); pos++) {
			final var s = series.get(pos);
			renderer.setSeriesPaint(pos, s.paint);
			renderer.setSeriesStroke(pos, s.stroke);
		}

		final var timeAxis = (DateAxis) plot.getDomainAxis();
		timeAxis.setAxisLineVisible(false);
		timeAxis.setLabelPaint(GRAY);
		timeAxis.setTickLabelPaint(GRAY);
		timeAxis.setLowerMargin(0);
		timeAxis.setUpperMargin(0);
		timeAxis.setTimeZone(TimeZone.getTimeZone("GMT"));
		timeAxis.setMinorTickMarksVisible(true);
		timeAxis.setMinorTickCount(10);
		timeAxis.setMinorTickMarkInsideLength(5);
		timeAxis.setMinorTickMarkOutsideLength(0);
		timeAxis.setLabelFont(font);
		timeAxis.setTickLabelFont(font);

		try {
			ChartUtils.saveChartAsJPEG(
					outputFile,
					JPG_COMPRESSION_RATIO,
					timechart,
					imageSize.width,
					imageSize.height);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't save chart", e);
		}

	}

}
