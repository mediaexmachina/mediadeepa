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
package media.mexm.mediadeepa.exportformat.report;

import static j2html.TagCreator.attrs;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.figure;
import static j2html.TagCreator.img;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;

import java.util.List;

import j2html.tags.DomContent;
import j2html.tags.specialized.ImgTag;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.exportformat.ImageArtifact;

public record ImageStripReportEntry(List<ImageArtifact> images) implements ReportEntry {

	@Override
	public DomContent toDomContent(final NumberUtils numberUtils) {
		final var fullStrip = each(images.stream().map(this::makeEmbeddedHTMLImage));
		return figure(attrs(".imagestrip"), div(fullStrip));
	}

	private ImgTag makeEmbeddedHTMLImage(final ImageArtifact image) {
		final var imageWidth = image.size().width;
		final var imageHeight = image.size().height;

		return img()
				.withAlt("Image strip")
				.withWidth(String.valueOf(imageWidth))
				.withHeight(String.valueOf(imageHeight))
				.withSrc("data:" + image.contentType() + ";base64," + encodeBase64String(image.data()));
	}

	@Override
	public boolean isEmpty() {
		return images.isEmpty();
	}

}
