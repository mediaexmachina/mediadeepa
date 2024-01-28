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
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;

import lombok.Getter;
import media.mexm.mediadeepa.cli.AppCommand;
import media.mexm.mediadeepa.config.AppConfig;

public class GraphicArtifact {

	public static final String DOT_PNG = ".png";
	public static final String DOT_JPEG = ".jpeg";

	private final String fileNameWOExt;
	@Getter
	private final JFreeChart graphic;
	@Getter
	private final Dimension imageSize;

	public GraphicArtifact(final String fileNameWOExt,
						   final JFreeChart graphic,
						   final Dimension imageSize) {
		this.fileNameWOExt = Objects.requireNonNull(fileNameWOExt, "\"fileNameWOExt\" can't to be null");
		this.graphic = Objects.requireNonNull(graphic, "\"graphic\" can't to be null");
		this.imageSize = Objects.requireNonNull(imageSize, "\"imageSize\" can't to be null");
	}

	private static byte[] getJPEG(final JFreeChart graphic,
								  final Dimension imageSize,
								  final float jpegCompressRatio) {
		try {
			final var b = new ByteArrayOutputStream();
			ChartUtils.writeChartAsJPEG(
					b,
					jpegCompressRatio,
					graphic,
					imageSize.width,
					imageSize.height);
			return b.toByteArray();
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't export JPG", e);
		}
	}

	private static byte[] getPNG(final JFreeChart graphic,
								 final Dimension imageSize) {
		try {
			final var b = new ByteArrayOutputStream();
			ChartUtils.writeChartAsPNG(
					b,
					graphic,
					imageSize.width,
					imageSize.height);
			return b.toByteArray();
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't export PNG", e);
		}
	}

	public byte[] getRawData(final AppCommand appCommand, final AppConfig appConfig) {
		if (appCommand.isGraphicJpg()) {
			return getJPEG(graphic, imageSize, appConfig.getGraphicConfig().getJpegCompressionRatio());
		} else {
			return getPNG(graphic, imageSize);
		}
	}

	public File save(final AppCommand appCommand, final AppConfig appConfig) {
		final var rawImage = getRawData(appCommand, appConfig);

		String fileName;
		if (appCommand.isGraphicJpg()) {
			fileName = fileNameWOExt + DOT_JPEG;
		} else {
			fileName = fileNameWOExt + DOT_PNG;
		}

		final var outputFile = appCommand.getOutputCmd().getExportToCmd().makeOutputFile(fileName);
		try {
			FileUtils.writeByteArrayToFile(outputFile, rawImage, false);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write file", e);
		}
		return outputFile;

	}

}
