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
import static java.awt.Color.CYAN;
import static java.awt.Color.GRAY;
import static java.awt.Color.ORANGE;
import static java.awt.Color.RED;
import static java.lang.Math.max;
import static media.mexm.mediadeepa.components.CLIRunner.makeOutputFileName;

import java.awt.BasicStroke;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.TimeZone;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormat;

@Slf4j
public class LoudnessGraphicExportFormat implements ExportFormat {

	public static final String SUFFIX_FILE_NAME = "lufs-events.jpg";

	@Override
	public String getFormatLongName() {
		return "Loudness graphic";
	}

	@Override
	public void exportResult(final DataResult result, final File exportDirectory, final String baseFileName) {
		final var r128events = result.getEbur128events();
		if (r128events.isEmpty()) {
			return;
		}

		final var outputFile = new File(exportDirectory, makeOutputFileName(baseFileName, SUFFIX_FILE_NAME));
		final var jpg_compression_ratio = 0.95f;
		final var image_width = 1000 * 2;
		final var image_height = 600 * 2;
		final var lufsRef = -23;
		final var truepeakRef = -3;
		final var lufsRange = -80;

		final var seriesMomentary = new TimeSeries("Momentary");
		final var seriesShortTerm = new TimeSeries("Short term");
		final var seriesIntegrated = new TimeSeries("Integrated");
		final var seriesTruePeakPerFrame = new TimeSeries("True peak");

		r128events.forEach(event -> {
			final var position = new FixedMillisecond(Math.round(Math.ceil(event.getT() * 1000f)));
			// TODO check if momentary/short_term == 0 >> -144
			seriesMomentary.add(position, event.getM());
			seriesShortTerm.add(position, event.getS());
			seriesIntegrated.add(position, event.getI());
			seriesTruePeakPerFrame.add(position, max(event.getFtpk().left(), event.getFtpk().right()));
		});

		final var tsc = new TimeSeriesCollection();
		tsc.addSeries(seriesShortTerm);
		tsc.addSeries(seriesMomentary);
		tsc.addSeries(seriesTruePeakPerFrame);
		tsc.addSeries(seriesIntegrated);

		final var timechart = ChartFactory.createTimeSeriesChart("", "", "", tsc, true, false, false);
		timechart.setAntiAlias(true);
		timechart.setBackgroundPaint(BLACK);
		timechart.getLegend().setBackgroundPaint(BLACK);
		// timechart.getLegend().setItemFont(font);

		final var plot = timechart.getXYPlot();
		plot.setBackgroundPaint(BLACK);

		final var renderer = (XYLineAndShapeRenderer) plot.getRenderer(0);
		renderer.setBaseLegendTextPaint(GRAY);

		final var font = renderer.getBaseItemLabelFont().deriveFont(24f);
		renderer.setBaseItemLabelFont(font);
		renderer.setBaseLegendTextFont(font);
		renderer.setLegendTextFont(0, font);
		renderer.setSeriesItemLabelFont(0, font);

		/**
		 * series_short_term
		 */
		renderer.setSeriesPaint(0, ORANGE);
		/**
		 * series_momentary
		 */
		renderer.setSeriesPaint(1, RED);
		/**
		 * series_true_peak_per_frame
		 */
		renderer.setSeriesPaint(2, GRAY);
		/**
		 * series_integrated
		 */
		renderer.setSeriesPaint(3, BLUE);
		final var thickStroke = new BasicStroke(6, CAP_BUTT, JOIN_MITER);
		renderer.setSeriesStroke(3, thickStroke);

		/**
		 * Time units
		 */
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

		/**
		 * Display the -23 line
		 */
		final var zeroPos = new ValueMarker(lufsRef);
		zeroPos.setLabel("   " + lufsRef);
		zeroPos.setLabelPaint(CYAN);
		// zeroPos.setAlpha(0.5f);
		// zeroPos.setLabelBackgroundColor(Color.CYAN);
		zeroPos.setPaint(CYAN);
		zeroPos.setOutlinePaint(CYAN);
		zeroPos.setLabelFont(font);

		/**
		 * Display the -3 line
		 */
		final var zeroPos2 = new ValueMarker(truepeakRef);
		zeroPos2.setLabel("   " + truepeakRef);
		zeroPos2.setLabelPaint(CYAN);
		// zeroPos2.setAlpha(0.5f);
		// zeroPos2.setLabelBackgroundColor(Color.CYAN);
		zeroPos2.setPaint(CYAN);
		zeroPos2.setOutlinePaint(CYAN);
		zeroPos2.setLabelFont(font);

		final float[] dash = { 5.0f };
		final var dashStroke = new BasicStroke(1, CAP_BUTT, JOIN_MITER, 10.0f, dash, 0.0f);
		zeroPos.setStroke(dashStroke);
		zeroPos2.setStroke(dashStroke);
		plot.addRangeMarker(zeroPos);
		plot.addRangeMarker(zeroPos2);

		final var rangeAxis = new LogarithmicAxis("dB LU");
		rangeAxis.centerRange(0d);
		rangeAxis.setAllowNegativesFlag(true);
		rangeAxis.setRange(lufsRange, 0);
		rangeAxis.setLabelPaint(GRAY);
		rangeAxis.setAxisLineVisible(false);
		rangeAxis.setTickLabelPaint(GRAY);
		rangeAxis.setLabelFont(font);
		rangeAxis.setTickLabelFont(font);

		plot.setRangeAxis(rangeAxis);
		plot.setOutlinePaint(GRAY);

		timechart.setTextAntiAlias(true);

		try {
			log.info("Save LUFS graphic to {}", outputFile);
			ChartUtilities.saveChartAsJPEG(outputFile, jpg_compression_ratio, timechart, image_width, image_height);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't save chart", e);
		}
	}

}
