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
 * Copyright (C) Media ex Machina 2024
 *
 */
package media.mexm.mediadeepa.exportformat.report;

import static j2html.TagCreator.attrs;
import static j2html.TagCreator.figcaption;
import static j2html.TagCreator.figure;

import java.awt.Dimension;
import java.io.IOException;
import java.util.Objects;

import org.apache.commons.codec.binary.Base64;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import j2html.TagCreator;
import j2html.tags.DomContent;
import media.mexm.mediadeepa.components.NumberUtils;

public record GraphicReportEntry(DomContentProvider caption,
								 Dimension imageSize,
								 byte[] rawContent,
								 String contentType) implements ReportEntry, JsonContentProvider {

	@Override
	public boolean isEmpty() {
		return rawContent == null || rawContent.length == 0;
	}

	@Override
	public DomContent toDomContent(final NumberUtils numberUtils) {
		final var img = TagCreator.img()
				.withAlt("Graphic image")
				.withHeight(String.valueOf(imageSize.height / 2))
				.withWidth(String.valueOf(imageSize.width / 2))
				.withSrc("data:" + contentType + ";base64," + Base64.encodeBase64String(rawContent));
		return figure(attrs(".graphic"), img, figcaption(caption.toDomContent(numberUtils)));
	}

	@Override
	public void toJson(final JsonGenerator gen, final SerializerProvider provider) throws IOException {
		/**
		 * No graphic export with json
		 */
	}

	@Override
	public int hashCode() {
		return Objects.hash(caption);
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
		final var other = (GraphicReportEntry) obj;
		return Objects.equals(caption, other.caption);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("GraphicReportEntry [caption=");
		builder.append(caption);
		builder.append(", imageSize=");
		builder.append(imageSize);
		builder.append(", contentType=");
		builder.append(contentType);
		builder.append("]");
		return builder.toString();
	}

}
