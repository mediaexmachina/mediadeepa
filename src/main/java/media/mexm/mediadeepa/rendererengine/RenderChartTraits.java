/*
 * This file is part of mediadeepa.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2024
 *
 */
package media.mexm.mediadeepa.rendererengine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;

public interface RenderChartTraits {

	default byte[] renderPNGFromChart(final JFreeChart chart, final int width, final int height) {
		try {
			final var b = new ByteArrayOutputStream();
			ChartUtils.writeChartAsPNG(b, chart, width, height);
			return b.toByteArray();
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't export PNG", e);
		}
	}

	default byte[] renderJPEGFromChart(final JFreeChart chart,
									   final int width,
									   final int height,
									   final float jpegCompressRatio) {
		try {
			final var b = new ByteArrayOutputStream();
			ChartUtils.writeChartAsJPEG(b, jpegCompressRatio, chart, width, height);
			return b.toByteArray();
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't export JPG", e);
		}
	}

}
