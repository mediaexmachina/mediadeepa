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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import j2html.tags.DomContent;
import lombok.Getter;

public class ReportSection implements DomContentProvider {

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
	public DomContent toDomContent() {
		return section(
				attrs(".reportsection"),
				each(entries.stream()
						.filter(not(ReportEntry::isEmpty))
						.map(ReportEntry::toDomContent)));
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

	boolean isNotEmpty() {
		return ReportEntry.isEmpty(entries) == false;
	}

	public String getSectionAnchorName() {
		return category.toString().toLowerCase() + "_" + title.toLowerCase().replace(' ', '_');
	}

}
