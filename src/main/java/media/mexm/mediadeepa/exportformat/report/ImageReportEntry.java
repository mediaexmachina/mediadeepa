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

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import j2html.tags.DomContent;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.exportformat.ImageArtifact;

public record ImageReportEntry(ImageArtifact image,
							   DomContentProvider caption) implements ReportEntry, JsonContentProvider {

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public DomContent toDomContent(final NumberUtils numberUtils) {
		final var img = image.makeEmbeddedHTMLImage("Image " + image.name());
		return figure(attrs(".image"),
				img,
				figcaption(caption.toDomContent(numberUtils)));
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
		final var other = (ImageReportEntry) obj;
		return Objects.equals(caption, other.caption);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("ImageReportEntry [caption=");
		builder.append(caption);
		builder.append(", image=");
		builder.append(image);
		builder.append("]");
		return builder.toString();
	}

}
