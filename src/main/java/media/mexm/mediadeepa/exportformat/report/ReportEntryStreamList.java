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
import static java.util.Collections.unmodifiableSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import j2html.tags.DomContent;

public class ReportEntryStreamList implements ReportEntry {

	private final List<ReportEntryStream> streamList;

	public ReportEntryStreamList() {
		streamList = new ArrayList<>();
	}

	public ReportEntryStream addStream(final String id,
									   final String codecType,
									   final int index) {
		final var s = new ReportEntryStream(id, codecType, index, this);
		streamList.add(s);
		return s;
	}

	public Set<String> getFirstItemsToDisplay() {
		if (streamList.size() < 2) {
			return Set.of();
		}
		final var list = streamList.stream().map(ReportEntryStream::getAllItemKeys).toList();
		final Set<String> reducedList = new HashSet<>(list.get(0));
		for (var pos = 1; pos < list.size(); pos++) {
			reducedList.retainAll(list.get(pos));
		}
		return unmodifiableSet(reducedList);
	}

	@Override
	public DomContent toDomContent() {
		return div(attrs(".entry.streamentries"),
				span(attrs(".key"), "Stream list:"),
				div(attrs(".streamlist"),
						each(streamList.stream()
								.sorted((l, r) -> Integer.compare(l.getIndex(), r.getIndex()))
								.map(ReportEntryStream::toDomContent))));
	}

	@Override
	public boolean isEmpty() {
		return streamList.isEmpty();
	}

}
