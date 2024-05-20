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
package media.mexm.mediadeepa.exportformat.report;

import static j2html.TagCreator.attrs;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.span;

import java.awt.Dimension;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import j2html.tags.DomContent;
import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.components.NumberUtils;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdCropdetect;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdValue;

public record CropEventTableReportEntry(List<LavfiMtdValue<LavfiMtdCropdetect>> cropEvents,
										Dimension fullFrame,
										int maxCropEventsDisplay)
									   implements ReportEntry, JsonContentProvider, ConstStrings {

	private Stream<CropEventReportEntry> getCropEventReportEntryStreams() {
		return cropEvents.stream()
				.map(c -> new CropEventReportEntry(c, fullFrame));
	}

	@Override
	public DomContent toDomContent(final NumberUtils numberUtils) {
		return div(attrs(".entry.cropevents"),
				span(attrs(".key"), FIRST_CROP_VALUES),
				div(attrs(".cropeventslist"),
						CropEventReportEntry.getHeader(),
						each(getCropEventReportEntryStreams()
								.map(f -> f.toDomContent(numberUtils)))));
	}

	@Override
	public boolean isEmpty() {
		return cropEvents.isEmpty();
	}

	@Override
	public void toJson(final JsonGenerator gen,
					   final SerializerProvider provider) throws IOException {
		gen.writeArrayFieldStart(jsonHeader("crop_events"));
		for (final var event : getCropEventReportEntryStreams().toList()) {
			gen.writeStartObject();
			gen.writeObject(event);
			gen.writeEndObject();
		}
		gen.writeEndArray();
		gen.writeNumberField("max_crop_events", maxCropEventsDisplay);
	}

}
