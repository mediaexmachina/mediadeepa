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
package media.mexm.mediadeepa.exportformat;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record ImageArtifact(String name, Dimension size, String contentType, byte[] data) {

	public static final String IMAGE_PNG = "image/png";
	public static final String IMAGE_JPEG = "image/jpeg";

	@JsonIgnore
	public boolean isPNG() {
		return IMAGE_PNG.equalsIgnoreCase(contentType);
	}

	@JsonIgnore
	public boolean isJPEG() {
		return IMAGE_JPEG.equalsIgnoreCase(contentType);
	}

	public void writeToFile(final File file) {
		try {
			FileUtils.writeByteArrayToFile(file, data, false);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write to file " + file, e);
		}
	}

	public static ImageArtifact renderChartToJPEGImage(final String name,
													   final JFreeChart chart,
													   final Dimension imageSize,
													   final float jpegCompressRatio) {
		try {
			final var b = new ByteArrayOutputStream();
			ChartUtils.writeChartAsJPEG(b, jpegCompressRatio, chart, imageSize.width, imageSize.height);
			return new ImageArtifact(name, imageSize, IMAGE_JPEG, b.toByteArray());
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't export JPG", e);
		}
	}

	public static ImageArtifact renderChartToPNGImage(final String name,
													  final JFreeChart chart,
													  final Dimension imageSize) {
		try {
			final var b = new ByteArrayOutputStream();
			ChartUtils.writeChartAsPNG(b, chart, imageSize.width, imageSize.height);
			return new ImageArtifact(name, imageSize, IMAGE_PNG, b.toByteArray());
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't export PNG", e);
		}
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("ImageArtifact [name=");
		builder.append(name);
		builder.append(", data=");
		builder.append(data.length);
		builder.append(" bytes, imageSize=");
		builder.append(size);
		builder.append(", contentType=");
		builder.append(contentType);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final var prime = 31;
		var result = 1;
		result = prime * result + Arrays.hashCode(data);
		result = prime * result + Objects.hash(name);
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final var other = (ImageArtifact) obj;
		return Arrays.equals(data, other.data) && Objects.equals(name, other.name);
	}

}
