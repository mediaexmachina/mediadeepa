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
package media.mexm.mediadeepa.exportformat.report;

import static j2html.TagCreator.attrs;
import static j2html.TagCreator.div;
import static j2html.TagCreator.span;

import java.io.IOException;

import org.apache.commons.math3.exception.MathArithmeticException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import j2html.tags.DomContent;
import media.mexm.mediadeepa.NumberFormator;
import media.mexm.mediadeepa.components.NumberUtils;

public record RatioReportEntry(String key,
							   int x,
							   int y,
							   NumberFormator formator) implements ReportEntry, JsonContentProvider {

	private static int gcd(final int l, final int r) {
		if (r == 0) {
			return l;
		} else {
			return gcd(r, l % r);
		}
	}

	@Override
	public DomContent toDomContent(final NumberUtils numberUtils) {
		return div(attrs(".entry"),
				span(attrs(".key"), key), span(attrs(".value"), value()));
	}

	private String getRatio() {
		return formator.format((double) x / (double) y);// NOSONAR S3518;
	}

	public String value() {
		final var gcd = gcd(x, y);
		return x / gcd + ":" + y / gcd + " (" + getRatio() + ")";
	}

	@Override
	public boolean isEmpty() {
		return x < 1 || y < 1;
	}

	@Override
	public void toJson(final JsonGenerator gen, final SerializerProvider provider) throws IOException {
		if (y == 0) {
			throw new MathArithmeticException();
		}

		gen.writeObjectFieldStart(jsonHeader(key));

		final var gcd = gcd(x, y);
		gen.writeObjectField("w", x / gcd);
		gen.writeObjectField("h", y / gcd);
		gen.writeObjectField("ratio", (double) x / (double) y);

		gen.writeEndObject();
	}

}
