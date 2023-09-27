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
import static java.util.function.Predicate.not;

import java.util.List;
import java.util.stream.Stream;

import j2html.tags.DomContent;

public record ReportEntrySubset(List<ReportEntry> entries) implements ReportEntry {

	@Override
	public DomContent toDomContent() {
		return div(attrs(".subset"),
				each(entries.stream()
						.filter(not(ReportEntry::isEmpty))
						.map(ReportEntry::toDomContent)));
	}

	@Override
	public boolean isEmpty() {
		return ReportEntry.isEmpty(entries);
	}

	static <T extends ReportEntry> void toEntrySubset(final Stream<T> entries, final ReportSection addToSection) {
		addToSection.add(new ReportEntrySubset(entries
				.map(en -> (ReportEntry) en)
				.toList()));
	}

}
