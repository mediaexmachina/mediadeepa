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

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.commons.io.FileUtils;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;

import lombok.AllArgsConstructor;
import lombok.Getter;
import media.mexm.mediadeepa.cli.ExportToCmd;

@Getter
@AllArgsConstructor
public class GraphicArtifact {

	private final String fileName;
	private final byte[] rawImage;
	private static final float JPG_COMPRESSION_RATIO = 0.95f;

	public GraphicArtifact(final String fileName,
						   final JFreeChart graphic,
						   final Dimension imageSize) {
		this(fileName, getJPEG(graphic, imageSize));
	}

	private static byte[] getJPEG(final JFreeChart graphic,
								  final Dimension imageSize) {
		try {
			final var b = new ByteArrayOutputStream();
			ChartUtils.writeChartAsJPEG(
					b,
					JPG_COMPRESSION_RATIO,
					graphic,
					imageSize.width,
					imageSize.height);
			return b.toByteArray();
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't export JPG", e);
		}
	}

	public File save(final ExportToCmd exportToCmd) {
		final var outputFile = exportToCmd.makeOutputFile(fileName);
		try {
			FileUtils.writeByteArrayToFile(outputFile, rawImage, false);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write file", e);
		}
		return outputFile;

	}

}
