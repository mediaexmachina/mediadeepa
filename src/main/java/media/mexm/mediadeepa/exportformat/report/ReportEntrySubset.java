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

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import j2html.tags.DomContent;
import media.mexm.mediadeepa.components.NumberUtils;

public record ReportEntrySubset(List<ReportEntry> entries) implements ReportEntry, JsonContentProvider {

	@Override
	public DomContent toDomContent(final NumberUtils numberUtils) {
		return div(attrs(".subset"),
				each(entries.stream()
						.filter(not(ReportEntry::isEmpty))
						.map(r -> r.toDomContent(numberUtils))));
	}

	@Override
	public boolean isEmpty() {
		return ReportEntry.isEmpty(entries);
	}

	public static <T extends ReportEntry> void toEntrySubset(final Stream<T> entries,
															 final ReportSection addToSection) {
		addToSection.add(new ReportEntrySubset(entries
				.map(en -> (ReportEntry) en)
				.toList()));
	}

	@Override
	public void toJson(final JsonGenerator gen,
					   final SerializerProvider provider) throws IOException {
		for (final var item : entries.stream().filter(not(ReportEntry::isEmpty)).toList()) {
			gen.writeObject(item);
		}
	}

}
