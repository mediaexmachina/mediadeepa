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
import static java.awt.Color.BLUE;
import static java.awt.Color.GRAY;
import static java.awt.Color.RED;
import static java.util.Locale.ENGLISH;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Stroke;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.ui.TextAnchor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class DataGraphic {

	public static final Dimension IMAGE_SIZE_FULL_HEIGHT = new Dimension(2000, 1200);
	public static final Dimension IMAGE_SIZE_HALF_HEIGHT = new Dimension(2000, IMAGE_SIZE_FULL_HEIGHT.height / 2);

	public static final Color FULL_PINK = Color.getHSBColor(0.83f, 1f, 1f);
	public static final List<Color> COLORS_CHANNEL = List.of(BLUE, RED);

	public static final Stroke THIN_STROKE = new BasicStroke(3, CAP_BUTT, JOIN_MITER);
	public static final Stroke THICK_STROKE = new BasicStroke(6, CAP_BUTT, JOIN_MITER);
	public static final List<Stroke> STROKES_CHANNEL = List.of(THIN_STROKE, THICK_STROKE);

	static final float JPG_COMPRESSION_RATIO = 0.95f;
	static final String DECIMAL_FORMAT_PATTERN = "#.#";

	private final List<Double> markers;
	private final DecimalFormat decimalFormat;
	private final RangeAxis rangeAxis;

	protected DataGraphic(final RangeAxis rangeAxis) {
		this.rangeAxis = rangeAxis;
		markers = new ArrayList<>();
		decimalFormat = new DecimalFormat(DECIMAL_FORMAT_PATTERN);
		decimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(ENGLISH));
	}

	public DataGraphic addValueMarker(final Number position) {
		final var longPos = Math.round(position.doubleValue());
		if (markers.stream()
				.map(Math::round)
				.noneMatch(l -> l == longPos)) {
			markers.add(position.doubleValue());
		}
		return this;
	}

	public void makeLogarithmicAxisGraphic(final File outputFile, final Dimension imageSize) {
		makeGraphic(outputFile, imageSize, true);
	}

	public void makeLinearAxisGraphic(final File outputFile, final Dimension imageSize) {
		makeGraphic(outputFile, imageSize, false);
	}

	protected abstract JFreeChart getChart();

	protected abstract List<? extends SeriesStyle> getSeriesStyle();// NOSONAR S1452

	protected void makeGraphic(final File outputFile, final Dimension imageSize, final boolean logarithmicAxis) {
		log.info("Save graphic to {}", outputFile);

		final var timechart = getChart();
		timechart.setAntiAlias(true);
		timechart.setTextAntiAlias(true);
		timechart.setBackgroundPaint(BLACK);
		final var legend = timechart.getLegend();
		legend.setBackgroundPaint(BLACK);

		final var plot = timechart.getXYPlot();
		plot.setBackgroundPaint(BLACK);
		plot.setOutlinePaint(GRAY);

		final var renderer = (AbstractXYItemRenderer) plot.getRenderer(0);
		final var font = renderer.getDefaultItemLabelFont().deriveFont(28f);

		markers.stream()
				.map(ref -> {
					final var marker = new ValueMarker(ref);
					final var label = decimalFormat.format(ref);
					if (label.endsWith(".0")) {
						marker.setLabel(String.valueOf(Math.round(ref)));
					} else {
						marker.setLabel(label);
					}

					marker.setLabelPaint(Color.WHITE);
					marker.setPaint(Color.WHITE);
					marker.setLabelFont(font.deriveFont(Font.BOLD));
					marker.setLabelTextAnchor(TextAnchor.CENTER_LEFT);

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
		renderer.setDefaultLegendTextPaint(GRAY);
		renderer.setDefaultLegendTextFont(font);
		renderer.setLegendTextFont(0, font);
		renderer.setSeriesItemLabelFont(0, font);

		final var series = getSeriesStyle();
		for (var pos = 0; pos < series.size(); pos++) {
			final var s = series.get(pos);
			renderer.setSeriesPaint(pos, s.getPaint());
			renderer.setSeriesStroke(pos, s.getStroke());
		}

		final var domainAxis = plot.getDomainAxis();
		domainAxis.setAxisLineVisible(false);
		domainAxis.setLabelPaint(GRAY);
		domainAxis.setTickLabelPaint(GRAY);
		domainAxis.setLowerMargin(0);
		domainAxis.setUpperMargin(0);
		domainAxis.setMinorTickMarksVisible(true);
		domainAxis.setMinorTickCount(10);
		domainAxis.setMinorTickMarkInsideLength(5);
		domainAxis.setMinorTickMarkOutsideLength(0);
		domainAxis.setLabelFont(font.deriveFont(10f));
		domainAxis.setTickLabelFont(font.deriveFont(20f));

		if (domainAxis instanceof final DateAxis timeAxis) {
			timeAxis.setTimeZone(TimeZone.getTimeZone("GMT"));
		}

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
