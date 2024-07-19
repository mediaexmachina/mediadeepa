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

import static media.mexm.mediadeepa.exportformat.ImageArtifact.renderChartToJPEGImage;
import static media.mexm.mediadeepa.exportformat.ImageArtifact.renderChartToPNGImage;

import java.awt.Dimension;
import java.io.File;
import java.util.Objects;

import lombok.Getter;
import media.mexm.mediadeepa.cli.AppCommand;
import media.mexm.mediadeepa.components.OutputFileSupplier;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.report.ImageReportEntry;

public class GraphicArtifact {

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

	public ImageArtifact getImage(final AppCommand appCommand, final AppConfig appConfig) {
		if (appCommand.isGraphicJpg()) {
			final var cRatio = appConfig.getGraphicConfig().getJpegCompressionRatio();
			return renderChartToJPEGImage(fileNameWOExt, graphic.chart(), imageSize, cRatio);
		} else {
			return renderChartToPNGImage(fileNameWOExt, graphic.chart(), imageSize);
		}
	}

	public File save(final AppCommand appCommand,
					 final AppConfig appConfig,
					 final OutputFileSupplier outputFileSupplier,
					 final DataResult result) {
		final var image = getImage(appCommand, appConfig);

		String fileName;
		if (image.isJPEG()) {
			fileName = fileNameWOExt + DOT_JPEG;
		} else {
			fileName = fileNameWOExt + DOT_PNG;
		}

		final var outputFile = outputFileSupplier.makeOutputFile(result, fileName);
		image.writeToFile(outputFile);
		return outputFile;

	}

	public ImageReportEntry toGraphicReportEntry(final AppCommand appCommand, final AppConfig appConfig) {
		return new ImageReportEntry(getImage(appCommand, appConfig), graphic);
	}

}
