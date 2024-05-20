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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import j2html.tags.DomContent;
import media.mexm.mediadeepa.components.NumberUtils;

public final class ReportEntryStreamList implements ReportEntry, JsonContentProvider {

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

	private Stream<ReportEntryStream> getSortedStream() {
		return streamList.stream()
				.sorted((l, r) -> Integer.compare(l.getIndex(), r.getIndex()));
	}

	@Override
	public DomContent toDomContent(final NumberUtils numberUtils) {
		return div(attrs(".entry.streamentries"),
				span(attrs(".key"), "Stream list:"),
				div(attrs(".streamlist"),
						each(getSortedStream()
								.map(r -> r.toDomContent(numberUtils)))));
	}

	@Override
	public boolean isEmpty() {
		return streamList.isEmpty();
	}

	@Override
	public void toJson(final JsonGenerator gen,
					   final SerializerProvider provider) throws IOException {
		gen.writeObjectFieldStart("streams");

		final var streamsByCodecType = getSortedStream().reduce(new LinkedHashMap<String, List<ReportEntryStream>>(),
				(map, mStream) -> {
					final var codecType = mStream.getCodecType();
					if (map.containsKey(codecType) == false) {
						map.put(codecType, new ArrayList<>(List.of(mStream)));
					} else {
						map.get(codecType).add(mStream);
					}
					return map;
				}, (l, r) -> {
					l.putAll(r);
					return l;
				});

		for (final var entry : streamsByCodecType.entrySet()) {
			final var codecType = entry.getKey();
			final var streams = entry.getValue();

			gen.writeArrayFieldStart(jsonHeader(codecType));
			for (final var stream : streams) {
				gen.writeStartObject();
				gen.writeObject(stream);
				gen.writeEndObject();
			}
			gen.writeEndArray();
		}
		gen.writeEndObject();
	}

}
