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
import static j2html.TagCreator.div;
import static j2html.TagCreator.span;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import j2html.tags.DomContent;
import media.mexm.mediadeepa.components.NumberUtils;

public record ResolutionReportEntry(String key,
									int width,
									int height,
									String unit) implements ReportEntry, NumericReportEntry, JsonContentProvider {

	@Override
	public boolean isEmpty() {
		return width == 0 || height == 0;
	}

	@Override
	public DomContent toDomContent(final NumberUtils numberUtils) {
		return div(attrs(".entry"),
				span(attrs(".key"), key), span(attrs(".value"), width + " × " + height));
	}

	@Override
	public void toJson(final JsonGenerator gen, final SerializerProvider provider) throws IOException {
		gen.writeObjectFieldStart(jsonHeader(key));
		gen.writeNumberField("w", width);
		gen.writeNumberField("h", height);
		if (unit != null && unit.isEmpty() == false) {
			gen.writeStringField("unit", getUnitWithPlurial(false));
		}
		gen.writeEndObject();
	}

}
