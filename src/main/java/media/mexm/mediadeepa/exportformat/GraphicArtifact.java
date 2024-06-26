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
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

import org.apache.commons.io.FileUtils;

import lombok.Getter;
import media.mexm.mediadeepa.cli.AppCommand;
import media.mexm.mediadeepa.components.OutputFileSupplier;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.report.GraphicReportEntry;
import media.mexm.mediadeepa.rendererengine.RenderChartTraits;

public class GraphicArtifact implements RenderChartTraits {

	public static final String DOT_PNG = ".png";
	public static final String DOT_JPEG = ".jpeg";

	private final String fileNameWOExt;
	@Getter
	private final ChartGraphicWrapper graphic;
	@Getter
	private final Dimension imageSize;

	public GraphicArtifact(final String fileNameWOExt,
						   final ChartGraphicWrapper graphicWrapper,
						   final Dimension imageSize) {
		this.fileNameWOExt = Objects.requireNonNull(fileNameWOExt, "\"fileNameWOExt\" can't to be null");
		graphic = Objects.requireNonNull(graphicWrapper, "\"graphicWrapper\" can't to be null");
		this.imageSize = Objects.requireNonNull(imageSize, "\"imageSize\" can't to be null");
	}

	public byte[] getRawData(final AppCommand appCommand, final AppConfig appConfig) {
		if (appCommand.isGraphicJpg()) {
			return renderJPEGFromChart(
					graphic.chart(),
					imageSize.width,
					imageSize.height,
					appConfig.getGraphicConfig().getJpegCompressionRatio());
		} else {
			return renderPNGFromChart(
					graphic.chart(),
					imageSize.width,
					imageSize.height);
		}
	}

	public File save(final AppCommand appCommand,
					 final AppConfig appConfig,
					 final OutputFileSupplier outputFileSupplier,
					 final DataResult result) {
		final var rawImage = getRawData(appCommand, appConfig);

		String fileName;
		if (appCommand.isGraphicJpg()) {
			fileName = fileNameWOExt + DOT_JPEG;
		} else {
			fileName = fileNameWOExt + DOT_PNG;
		}

		final var outputFile = outputFileSupplier.makeOutputFile(result, fileName);
		try {
			FileUtils.writeByteArrayToFile(outputFile, rawImage, false);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write file", e);
		}
		return outputFile;

	}

	public GraphicReportEntry toGraphicReportEntry(final AppCommand appCommand, final AppConfig appConfig) {
		return new GraphicReportEntry(
				graphic,
				imageSize,
				getRawData(appCommand, appConfig),
				appCommand.isGraphicJpg() ? "image/jpeg" : "image/png");
	}

}
