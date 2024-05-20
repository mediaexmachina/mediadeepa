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
 * Copyright (C) Media ex Machina 2023
 *
 */
package media.mexm.mediadeepa.exportformat.report;

import static j2html.TagCreator.attrs;
import static j2html.TagCreator.each;
import static j2html.TagCreator.section;
import static java.util.function.Predicate.not;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import j2html.tags.DomContent;
import lombok.Getter;
import media.mexm.mediadeepa.components.NumberUtils;

public final class ReportSection implements DomContentProvider, JsonContentProvider {

	@Getter
	private final ReportSectionCategory category;
	@Getter
	private final String title;
	private final List<ReportEntry> entries;

	public ReportSection(final ReportSectionCategory category, final String title) {
		this.title = Objects.requireNonNull(title, "\"title\" can't to be null");
		this.category = Objects.requireNonNull(category, "\"category\" can't to be null");
		entries = new ArrayList<>();
	}

	@Override
	public String toString() {
		return "ReportSection[" + title + " (" + entries.size() + ")]";
	}

	@Override
	public DomContent toDomContent(final NumberUtils numberUtils) {
		return section(
				attrs(".reportsection"),
				each(entries.stream()
						.filter(not(ReportEntry::isEmpty))
						.map(r -> r.toDomContent(numberUtils))));
	}

	public ReportSection add(final ReportEntry... entry) {
		Arrays.stream(entry)
				.filter(Objects::nonNull)
				.forEach(entries::add);
		return this;
	}

	public <T extends ReportEntry> ReportSection add(final Collection<T> entryCollection) {
		Objects.requireNonNull(entryCollection);
		entryCollection.stream()
				.filter(Objects::nonNull)
				.forEach(entries::add);
		return this;
	}

	public boolean isNotEmpty() {
		return ReportEntry.isEmpty(entries) == false;
	}

	public String getSectionAnchorName() {
		return jsonHeader(category.toString().toLowerCase() + "_" + title);
	}

	@Override
	public void toJson(final JsonGenerator gen,
					   final SerializerProvider provider) throws IOException {
		if (ReportEntry.isEmpty(entries)) {
			return;
		}

		gen.writeObjectFieldStart(jsonHeader(title));
		gen.writeStringField("_section_title", title);

		entries.stream()
				.filter(not(ReportEntry::isEmpty))
				.forEach(entry -> writeObject(entry, gen));

		gen.writeEndObject();
	}

}
