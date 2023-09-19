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
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.TimeZone;
import java.util.stream.Stream;

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
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.Ebur128Summary;

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
		// TODO polyvalent time/db (LogarithmicAxis or other ?) values graph
		// TODO split TPK / LU

		final var outputFile = new File(exportDirectory, makeOutputFileName(baseFileName, SUFFIX_FILE_NAME));
		final var jpg_compression_ratio = 0.95f;
		final var image_width = 1000 * 2;
		final var image_height = 600 * 2;
		final var truepeakRef = -3;

		final var seriesMomentary = new TimeSeries("Momentary");
		final var seriesShortTerm = new TimeSeries("Short term");
		final var seriesIntegrated = new TimeSeries("Integrated");
		final var seriesTruePeakPerFrame = new TimeSeries("True peak");

		r128events.forEach(event -> {
			final var position = new FixedMillisecond(Math.round(Math.ceil(event.getT() * 1000f)));
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
		final var legend = timechart.getLegend();
		legend.setBackgroundPaint(BLACK);

		final var plot = timechart.getXYPlot();
		plot.setBackgroundPaint(BLACK);

		final var renderer = (XYLineAndShapeRenderer) plot.getRenderer(0);
		renderer.setBaseLegendTextPaint(GRAY);

		final var font = renderer.getBaseItemLabelFont().deriveFont(24f);
		renderer.setBaseItemLabelFont(font);
		renderer.setBaseLegendTextFont(font);
		renderer.setLegendTextFont(0, font);
		renderer.setSeriesItemLabelFont(0, font);

		final var thinStroke = new BasicStroke(3, CAP_BUTT, JOIN_MITER);
		/**
		 * series_short_term
		 */
		renderer.setSeriesPaint(0, ORANGE);
		renderer.setSeriesStroke(0, thinStroke);
		/**
		 * series_momentary
		 */
		renderer.setSeriesPaint(1, RED);
		renderer.setSeriesStroke(1, thinStroke);
		/**
		 * series_true_peak_per_frame
		 */
		renderer.setSeriesPaint(2, GRAY);
		renderer.setSeriesStroke(2, thinStroke);
		/**
		 * series_integrated
		 */
		renderer.setSeriesPaint(3, BLUE);
		renderer.setSeriesStroke(3, new BasicStroke(6, CAP_BUTT, JOIN_MITER));

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

		final var floorValuesStats = -40d;
		final var valueStats = r128events.stream().flatMap(event -> Stream.of(
				event.getFtpk().left(),
				event.getFtpk().right(),
				event.getI(),
				event.getM(),
				event.getS(),
				event.getSpk().left(),
				event.getSpk().right(),
				event.getTarget(),
				event.getTpk().left(),
				event.getTpk().right()))
				.mapToDouble(v -> (double) v)
				.filter(v -> v > floorValuesStats)
				.summaryStatistics();
		final var maxValue = (int) Math.round(Math.floor(valueStats.getMax()));
		final var minValue = (int) Math.round(Math.ceil(valueStats.getMin()));

		final var oEbur128Sum = result.getMediaAnalyserResult().map(MediaAnalyserResult::ebur128Summary);
		final var integrated = Math.round(oEbur128Sum.map(Ebur128Summary::getIntegrated)
				.orElseGet(() -> r128events.get(r128events.size() - 1).getI()));
		final var lraH = oEbur128Sum.map(Ebur128Summary::getLoudnessRangeHigh)
				.map(Math::floor)
				.map(Math::round)
				.orElse(-23l);
		final var lraL = oEbur128Sum.map(Ebur128Summary::getLoudnessRangeLow)
				.map(Math::ceil)
				.map(Math::round)
				.orElse(-23l);

		final var rangeAxis = new LogarithmicAxis("dB LU");
		rangeAxis.centerRange(integrated);
		rangeAxis.setAllowNegativesFlag(true);
		rangeAxis.setRange(minValue - 10d, maxValue + 1d);
		rangeAxis.setLabelPaint(GRAY);
		rangeAxis.setAxisLineVisible(false);
		rangeAxis.setTickLabelPaint(GRAY);
		rangeAxis.setLabelFont(font);

		rangeAxis.setTickLabelFont(font);
		rangeAxis.setTickLabelsVisible(true);
		rangeAxis.setTickMarksVisible(true);
		rangeAxis.setTickMarkInsideLength(10);
		rangeAxis.setTickMarkOutsideLength(0);
		rangeAxis.setTickMarkPaint(GRAY);

		plot.setRangeAxis(rangeAxis);
		plot.setOutlinePaint(GRAY);

		Stream.of(
				r128events.stream()
						.map(Ebur128StrErrFilterEvent::getTarget)
						.map(Math::round)
						.findFirst()
						.orElse(-23),
				-14, -23, maxValue, minValue, integrated, lraH.intValue(), lraL.intValue())
				.distinct()
				.map(ref -> createValueMarker(ref, font))
				.forEach(plot::addRangeMarker);
		plot.addRangeMarker(createValueMarker(truepeakRef, font));

		timechart.setTextAntiAlias(true);

		try {
			log.info("Save LUFS graphic to {}", outputFile);
			ChartUtilities.saveChartAsJPEG(outputFile, jpg_compression_ratio, timechart, image_width, image_height);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't save chart", e);
		}
	}

	private ValueMarker createValueMarker(final int ref, final Font font) {
		final var maker = new ValueMarker(ref);
		maker.setLabel("   " + ref);
		maker.setLabelPaint(CYAN);
		maker.setPaint(CYAN);
		maker.setOutlinePaint(CYAN);
		maker.setLabelFont(font);
		final float[] dash = { 5.0f };
		maker.setStroke(new BasicStroke(1, CAP_BUTT, JOIN_MITER, 10.0f, dash, 0.0f));
		return maker;
	}

}
