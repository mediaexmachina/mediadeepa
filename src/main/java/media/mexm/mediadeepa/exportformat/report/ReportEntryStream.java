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
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import j2html.tags.DomContent;
import lombok.Getter;
import media.mexm.mediadeepa.components.NumberUtils;

public final class ReportEntryStream implements ReportEntry, JsonContentProvider {

	private final Map<String, ReportEntry> entries;

	private final String id;
	@Getter
	private final String codecType;
	@Getter
	private final int index;
	private final ReportEntryStreamList list;

	ReportEntryStream(final String id,
					  final String codecType,
					  final int index,
					  final ReportEntryStreamList list) {
		entries = new LinkedHashMap<>();
		this.id = id;
		this.codecType = codecType;
		this.index = index;
		this.list = list;
	}

	public ReportEntryStream add(final String key, final String value) {
		if (value == null) {
			return this;
		}
		checkNull(entries.put(key, new SimpleKeyValueReportEntry(key, value)));
		return this;
	}

	private void checkNull(final ReportEntry previous) {
		if (previous == null) {
			return;
		}
		throw new IllegalArgumentException(previous.toString() + " was previously added!");
	}

	public void add(final String key, final String value, final String byDefault) {
		if (value == null || value.equalsIgnoreCase(byDefault)) {
			return;
		}
		checkNull(entries.put(key, new SimpleKeyValueReportEntry(key, value)));
	}

	public void add(final String key, final Stream<String> values) {
		checkNull(entries.put(key, new SimpleKeyValueListReportEntry(key, values.toList())));
	}

	public void add(final String key, final Number value, final String unit) {
		if (value == null) {
			return;
		}
		checkNull(entries.put(key, new NumericUnitValueReportEntry(key, value, unit)));
	}

	private static final Predicate<Entry<String, ReportEntry>> notEmpty = entry -> entry.getValue().isEmpty() == false;

	Set<String> getAllItemKeys() {
		return entries.entrySet().stream()
				.filter(notEmpty)
				.map(Entry::getKey)
				.collect(toUnmodifiableSet());
	}

	private Stream<ReportEntry> getImportantItems(final Set<String> itemsFirst) {
		return entries.entrySet().stream()
				.filter(notEmpty)
				.filter(entry -> itemsFirst.contains(entry.getKey()))
				.map(Entry::getValue);
	}

	private Stream<ReportEntry> getLessImportantItems(final Set<String> itemsFirst) {
		return entries.entrySet().stream()
				.filter(notEmpty)
				.filter(entry -> itemsFirst.contains(entry.getKey()) == false)
				.map(Entry::getValue);
	}

	@Override
	public DomContent toDomContent(final NumberUtils numberUtils) {
		final var itemsFirst = list.getFirstItemsToDisplay();
		final var domFirst = getImportantItems(itemsFirst)
				.map(r -> r.toDomContent(numberUtils));
		final var domLast = getLessImportantItems(itemsFirst)
				.map(r -> r.toDomContent(numberUtils));

		final var displayedId = id != null ? ", " + id : "";
		return div(attrs(".streamentry"),
				div(attrs(".streamindex." + codecType), codecType + " #" + index + displayedId),
				each(each(domFirst), each(domLast)));
	}

	@Override
	public boolean isEmpty() {
		return entries.isEmpty();
	}

	@Override
	public void toJson(final JsonGenerator gen,
					   final SerializerProvider provider) throws IOException {
		gen.writeNumberField("index", index);
		if (id != null) {
			gen.writeStringField("id", id);
		}
		final var itemsFirst = list.getFirstItemsToDisplay();

		getImportantItems(itemsFirst).forEach(entry -> writeObject(entry, gen));
		getLessImportantItems(itemsFirst).forEach(entry -> writeObject(entry, gen));
	}

}
