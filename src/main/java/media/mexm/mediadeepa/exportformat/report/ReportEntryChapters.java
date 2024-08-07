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
import static j2html.TagCreator.each;
import static j2html.TagCreator.span;
import static java.util.stream.Collectors.toUnmodifiableMap;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import j2html.tags.DomContent;
import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.components.NumberUtils;
import tv.hd3g.ffprobejaxb.data.FFProbeChapter;
import tv.hd3g.ffprobejaxb.data.FFProbeKeyValue;

public final class ReportEntryChapters implements ConstStrings, ReportEntry, JsonContentProvider {

	private final List<ReportEntryChapter> reportChapters;

	public static record ReportEntryChapter(String title,
											Duration position,
											Map<String, String> tags) implements JsonContentProvider {

		private static final String TITLE_ = "title";

		private static ReportEntryChapter getFromFFprobe(final FFProbeChapter ffprobeChapter) {
			final Map<String, String> tags = ffprobeChapter.tags().stream()
					.filter(c -> c.key().equalsIgnoreCase(TITLE_) == false)
					.collect(toUnmodifiableMap(f -> f.key().toLowerCase(), FFProbeKeyValue::value));
			final var title = ffprobeChapter.tags().stream()
					.filter(c -> c.key().equalsIgnoreCase(TITLE_))
					.findFirst().map(FFProbeKeyValue::value)
					.orElse("");

			return new ReportEntryChapter(
					title,
					Duration.ofMillis(Math.round(ffprobeChapter.startTime() * 1000f)),
					tags);
		}

		DomContent toDomContent(final NumberUtils numberUtils) {
			return div(attrs(".entry"),
					span(attrs(".key"), title),
					span(attrs(".value"), numberUtils.durationToString(position)));
		}

		@Override
		public void toJson(final JsonGenerator gen, final SerializerProvider provider) throws IOException {
			gen.writeStartObject();
			gen.writeStringField(TITLE_, title);
			gen.writeNumberField("position", position.toMillis());
			gen.writeEndObject();
		}
	}

	public ReportEntryChapters(final List<FFProbeChapter> ffprobeChapters) {
		reportChapters = ffprobeChapters.stream()
				.map(ReportEntryChapter::getFromFFprobe)
				.toList();
	}

	@Override
	public DomContent toDomContent(final NumberUtils numberUtils) {
		final var countDom = new NumericUnitValueReportEntry(CHAPTERS, reportChapters.size(), CHAPTER_S)
				.toDomContent(numberUtils);
		if (reportChapters.isEmpty()) {
			return countDom;
		}

		return each(
				countDom,
				div(attrs(".entry.chapters"),
						each(reportChapters.stream().map(f -> f.toDomContent(numberUtils)))));
	}

	@Override
	public void toJson(final JsonGenerator gen,
					   final SerializerProvider provider) throws IOException {
		gen.writeArrayFieldStart("chapters");
		for (final var chapter : reportChapters) {
			gen.writeObject(chapter);
		}
		gen.writeEndArray();
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

}
