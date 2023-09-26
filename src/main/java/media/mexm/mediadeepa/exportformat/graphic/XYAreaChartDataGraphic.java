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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;

public class XYAreaChartDataGraphic extends DataGraphic {

	private final DefaultTableXYDataset dataset;
	private final List<SeriesStyle> seriesStyles;

	protected XYAreaChartDataGraphic(final RangeAxis rangeAxis) {
		super(rangeAxis);
		dataset = new DefaultTableXYDataset();
		seriesStyles = new ArrayList<>();
	}

	public <T extends Number> void addSeriesByCounter(final SeriesStyle style, final Stream<T> values) {
		final var iterator = values.iterator();
		var pos = 0;
		final var result = new LinkedHashMap<Integer, T>();
		while (iterator.hasNext()) {
			result.put(pos++, iterator.next());
		}
		addSeries(style, result);
	}

	public <T extends Number, U extends Number> void addSeries(final SeriesStyle style, final Map<T, U> values) {
		final var series = new XYSeries(style.getName(), false, false);
		values.keySet().stream().sorted().forEach(k -> series.add(k, values.get(k)));
		dataset.addSeries(series);
		seriesStyles.add(style);
	}

	@Override
	protected JFreeChart getChart() {
		return ChartFactory.createStackedXYAreaChart("", "", "", dataset, VERTICAL, true, false, false);
	}

	@Override
	protected List<? extends SeriesStyle> getSeriesStyle() {
		return seriesStyles;
	}

}
