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
 * Copyright (C) hdsdi3g for hd3g.tv 2024
 *
 */
package media.mexm.mediadeepa.rendererengine.components;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;
import static java.awt.Color.BLACK;
import static media.mexm.mediadeepa.exportformat.ImageArtifact.renderChartToPNGImage;
import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.AUDIO;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.IntStream;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import j2html.TagCreator;
import j2html.tags.DomContent;
import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ImageArtifact;
import media.mexm.mediadeepa.exportformat.report.ReportDocument;
import media.mexm.mediadeepa.exportformat.report.ReportSection;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.SignalImageRendererEngine;
import tv.hd3g.fflauncher.recipes.wavmeasure.MeasuredWav;
import tv.hd3g.fflauncher.recipes.wavmeasure.MeasuredWavEntry;

@Component
public class ImageWaveformRendererEngine implements SignalImageRendererEngine, ReportRendererEngine, ConstStrings {

	@Autowired
	private AppConfig appConfig;

	@Override
	public Optional<ImageArtifact> makeimagePNG(final DataResult result) {
		final var imageSize = new Dimension(
				(int) appConfig.getWavFormConfig().getImageSize().getWidth(),
				(int) appConfig.getWavFormConfig().getImageSize().getHeight());

		return extractAndMakeChart(result)
				.map(chart -> renderChartToPNGImage(getManagedReturnName(), chart, imageSize));
	}

	private Optional<JFreeChart> extractAndMakeChart(final DataResult result) {
		final var oEntries = result.getWavForm().map(MeasuredWav::entries);
		if (oEntries.isEmpty()) {
			return Optional.empty();
		}
		final var entries = oEntries.get();
		return Optional.ofNullable(makeChart(entries));
	}

	private JFreeChart makeChart(final List<MeasuredWavEntry> entries) {
		final var positions = IntStream.range(0, entries.size())
				.mapToDouble(pos -> entries.get(pos).position())
				.map(f -> f * 1000)
				.mapToLong(Math::round)
				.mapToObj(FixedMillisecond::new)
				.toList();

		final var tsRMSPos = new TimeSeries("RMSPos");
		final var tsRMSNeg = new TimeSeries("RMSNeg");
		final var tsPeakPos = new TimeSeries("peakPos");
		final var tsPeakNeg = new TimeSeries("peakNeg");

		IntStream.range(0, entries.size())
				.forEach(pos -> {
					tsRMSPos.add(positions.get(pos), entries.get(pos).rmsPositive());
					tsRMSNeg.add(positions.get(pos), -entries.get(pos).rmsNegative());
					tsPeakPos.add(positions.get(pos), entries.get(pos).peakPositive());
					tsPeakNeg.add(positions.get(pos), -entries.get(pos).peakNegative());
				});

		final var tsc = new TimeSeriesCollection();
		tsc.addSeries(tsRMSPos);
		tsc.addSeries(tsRMSNeg);
		tsc.addSeries(tsPeakPos);
		tsc.addSeries(tsPeakNeg);

		final var timechart = ChartFactory.createTimeSeriesChart("", "", "", tsc, false, false, false);
		timechart.setAntiAlias(true);
		timechart.setTextAntiAlias(true);
		timechart.setBackgroundPaint(BLACK);

		final var fontMarkColor = new Color(64, 64, 64);

		final var plot = timechart.getXYPlot();
		plot.setBackgroundPaint(BLACK);
		plot.setOutlinePaint(fontMarkColor);

		plot.setRenderer(new XYAreaRenderer());

		final var renderer = (XYAreaRenderer) plot.getRenderer(0);
		final var font = renderer.getDefaultItemLabelFont().deriveFont(28f);
		final var rangeAxis = new LogarithmicAxis(null);
		rangeAxis.setAllowNegativesFlag(true);
		rangeAxis.setAutoRange(false);
		rangeAxis.setRange(-1d, 1d);
		rangeAxis.setLabelPaint(fontMarkColor);
		rangeAxis.setTickLabelPaint(fontMarkColor);
		rangeAxis.setLabelFont(font.deriveFont(10));
		rangeAxis.setTickLabelFont(font.deriveFont(10));
		rangeAxis.setTickLabelsVisible(false);
		rangeAxis.setTickMarksVisible(false);
		rangeAxis.setTickMarkPaint(fontMarkColor);
		plot.setRangeAxis(rangeAxis);

		renderer.setDefaultItemLabelFont(font);
		renderer.setDefaultLegendTextPaint(fontMarkColor);
		renderer.setDefaultLegendTextFont(font);
		renderer.setLegendTextFont(0, font);
		renderer.setSeriesItemLabelFont(0, font);

		final var stroke = new BasicStroke(1, CAP_BUTT, JOIN_MITER);
		renderer.setSeriesStroke(0, stroke);
		renderer.setSeriesStroke(1, stroke);
		renderer.setSeriesStroke(2, stroke);
		renderer.setSeriesStroke(3, stroke);

		final var rmsColor = new Color(128, 180, 128);
		final var peakColor = new Color(128, 160, 128);
		renderer.setSeriesPaint(0, rmsColor);
		renderer.setSeriesPaint(1, rmsColor);
		renderer.setSeriesPaint(2, peakColor);
		renderer.setSeriesPaint(3, peakColor);

		final var domainAxis = plot.getDomainAxis();
		domainAxis.setAxisLineVisible(false);
		domainAxis.setLabelPaint(fontMarkColor);
		domainAxis.setTickLabelPaint(fontMarkColor);
		domainAxis.setLowerMargin(0);
		domainAxis.setUpperMargin(0);
		domainAxis.setMinorTickMarksVisible(false);
		domainAxis.setMinorTickCount(10);
		domainAxis.setMinorTickMarkInsideLength(5);
		domainAxis.setMinorTickMarkOutsideLength(0);
		domainAxis.setLabelFont(font.deriveFont(10f));
		domainAxis.setTickLabelFont(font.deriveFont(20f));

		if (domainAxis instanceof final DateAxis timeAxis) {
			timeAxis.setTimeZone(TimeZone.getTimeZone("GMT"));
		}
		return timechart;
	}

	@Override
	public String getManagedReturnName() {
		return "waveform";
	}

	@Override
	public String getDefaultInternalFileName() {
		return appConfig.getWavFormConfig().getPngFilename();
	}

	@Override
	public DomContent toDomContent(final NumberUtils numberUtils) {
		return TagCreator.span("Mono view (peak and RMS)");
	}

	@Override
	public void addToReport(final DataResult result, final ReportDocument document) {
		if (result.getWavForm().isEmpty()) {
			return;
		}
		final var section = new ReportSection(AUDIO, AUDIO_WAVEFORM);
		addAllSignalImageToReport(result, section, appConfig);
		document.add(section);
	}
}
