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

import static org.jfree.chart.plot.PlotOrientation.VERTICAL;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.DefaultXYDataset;

public class XYLineChartDataGraphic extends DataGraphic {

	private final double[] xValues;
	private final DefaultXYDataset dataset;
	private final List<SeriesStyle> seriesStyles;

	protected XYLineChartDataGraphic(final RangeAxis rangeAxis,
									 final int xSize) {
		super(rangeAxis);
		xValues = new double[xSize];
		dataset = new DefaultXYDataset();
		seriesStyles = new ArrayList<>();
		IntStream.range(0, xSize).parallel().forEach(i -> xValues[i] = i);
	}

	public void addSeries(final SeriesStyle style, final double[] values) {
		dataset.addSeries(style.getName(), new double[][] { xValues, values });
		seriesStyles.add(style);
	}

	@Override
	protected List<? extends SeriesStyle> getSeriesStyle() {
		return seriesStyles;
	}

	@Override
	protected JFreeChart getChart() {
		return ChartFactory.createXYLineChart("", "", "", dataset, VERTICAL, true, false, false);
	}

}
