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

import static j2html.TagCreator.each;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.lang.Math.ceilDiv;
import static javax.imageio.ImageWriteParam.MODE_EXPLICIT;
import static media.mexm.mediadeepa.exportformat.ImageArtifact.IMAGE_JPEG;
import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.SUMMARY;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import j2html.tags.DomContent;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ImageArtifact;
import media.mexm.mediadeepa.exportformat.report.DomContentProvider;
import media.mexm.mediadeepa.exportformat.report.ImageReportEntry;
import media.mexm.mediadeepa.exportformat.report.ImageStripReportEntry;
import media.mexm.mediadeepa.exportformat.report.ReportDocument;
import media.mexm.mediadeepa.exportformat.report.ReportSection;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;

@Component
@Slf4j
public class SignificantSnapshotImageRendererEngine implements ConstStrings, ReportRendererEngine, DomContentProvider {

	@Autowired
	private AppConfig appConfig;

	public Optional<ImageArtifact> makeimage(final DataResult result) {
		return result.getVideoImageSnapshots()
				.map(vis -> new ImageArtifact(
						"main-snapshot",
						vis.imageSize(),
						IMAGE_JPEG,
						vis.significantImageData()));
	}

	public String getDefaultInternalFileName() {
		return appConfig.getSnapshotImageConfig().getSignificantJpegFilename();
	}

	@Override
	public DomContent toDomContent(final NumberUtils numberUtils) {
		return each();
	}

	@Override
	public void addToReport(final DataResult result, final ReportDocument document) {
		if (result.getVideoImageSnapshots().isEmpty()) {
			return;
		}
		if (appConfig.getReportConfig().isAddImages() == false) {
			log.debug("Don't add significant snapshots images in report producing, by conf");
			return;
		}

		final var section = new ReportSection(SUMMARY, SNAPSHOT);
		makeimage(result)
				.map(image -> new ImageReportEntry(image, this, appConfig.getReportConfig().getDisplayImageSizeWidth()))
				.ifPresent(section::add);

		final var videoImageSnapshots = result.getVideoImageSnapshots().get();
		final var stripList = videoImageSnapshots.stripImagesData();

		if (stripList.isEmpty() == false) {
			final var imageWidth = videoImageSnapshots.imageSize().width;
			final var imageHeight = videoImageSnapshots.imageSize().height;
			final var displayWidth = appConfig.getReportConfig().getDisplayImageSizeWidth();
			final var stripOutWidth = ceilDiv(displayWidth, stripList.size());
			final var displayHeight = Math.round((float) stripOutWidth / imageWidth * imageHeight);

			final var stripImageList = stripList.parallelStream()
					.map(this::loadImage)
					.map(img -> resizeImage(img, stripOutWidth, displayHeight))
					.map(this::toJpeg)
					.map(data -> new ImageArtifact("strip", new Dimension(stripOutWidth, displayHeight), IMAGE_JPEG,
							data))
					.toList();

			section.add(new ImageStripReportEntry(stripImageList));
		}

		document.add(section);
	}

	private BufferedImage loadImage(final byte[] rawData) {
		try {
			return ImageIO.read(new ByteArrayInputStream(rawData));
		} catch (final IOException e) {
			throw new UncheckedIOException("Load and transform images", e);
		}
	}

	/**
	 * See https://www.baeldung.com/java-resize-image#1-javaawtgraphics2d
	 */
	private BufferedImage resizeImage(final BufferedImage originalImage,
									  final int targetWidth,
									  final int targetHeight) {
		final var resizedImage = new BufferedImage(targetWidth, targetHeight, TYPE_INT_RGB);
		final var graphics2D = resizedImage.createGraphics();
		graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
		graphics2D.dispose();
		return resizedImage;
	}

	private byte[] toJpeg(final BufferedImage source) {
		try {
			final var baos = new ByteArrayOutputStream();
			final var writer = ImageIO.getImageWritersByFormatName("jpeg").next();
			final var iwp = writer.getDefaultWriteParam();
			iwp.setCompressionMode(MODE_EXPLICIT);
			iwp.setCompressionQuality(appConfig.getJpegCompressionRatio());
			writer.setOutput(new MemoryCacheImageOutputStream(baos));
			writer.write(null, new IIOImage(source, null, null), iwp);
			writer.dispose();
			return baos.toByteArray();
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't export to JPEG strip image", e);
		}
	}

}
