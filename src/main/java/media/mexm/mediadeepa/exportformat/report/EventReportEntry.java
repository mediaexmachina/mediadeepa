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
import static j2html.TagCreator.span;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import j2html.TagCreator;
import j2html.tags.DomContent;
import media.mexm.mediadeepa.components.NumberUtils;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdEvent;

public record EventReportEntry(LavfiMtdEvent event, Optional<Duration> sourceDuration) implements ReportEntry,
							  JsonContentProvider {

	public static final class EventReportEntryHeader implements ReportEntry, JsonContentProvider {

		final boolean hasScope;
		final boolean hasEnd;

		public EventReportEntryHeader(final List<LavfiMtdEvent> events, final Optional<Duration> sourceDuration) {
			hasScope = events.stream()
					.anyMatch(event -> event.scope() != null
									   && event.scope().isEmpty() == false);
			hasEnd = events.stream()
					.map(LavfiMtdEvent::end)
					.anyMatch(end -> end != null
									 && end.toMillis() > 0
									 || sourceDuration.isPresent());
		}

		@Override
		public DomContent toDomContent(final NumberUtils numberUtils) {
			return div(attrs(".entry.event"),
					TagCreator.iff(hasScope, span(attrs(".key.scope.value"), "Scope/channel")),
					span(attrs(".key.start.value"), "Start time"),
					TagCreator.iff(hasEnd, span(attrs(".key.end.value"), "End time")),
					TagCreator.iff(hasEnd, span(attrs(".key.duration.value"), "Event duration")));
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public void toJson(final JsonGenerator gen,
						   final SerializerProvider provider) throws IOException {// NOSONAR S1186
		}

	}

	public static EventReportEntryHeader createHeader(final List<LavfiMtdEvent> events,
													  final Optional<Duration> sourceDuration) {
		return new EventReportEntryHeader(events, sourceDuration);
	}

	private Optional<Duration> getEnd() {
		if (event.end() != null && event.end().toMillis() > 0 || sourceDuration.isPresent()) {
			return Optional.ofNullable(event.getEndOr(sourceDuration.get()));
		} else {
			return Optional.empty();
		}
	}

	private Optional<String> getScope() {
		if (event.scope() != null && event.scope().isEmpty() == false) {
			return Optional.ofNullable(event.scope());
		}
		return Optional.empty();
	}

	@Override
	public DomContent toDomContent(final NumberUtils numberUtils) {
		final var spanScope = getScope()
				.map(s -> span(attrs(".scope.value"), s))
				.orElse(span());

		final var spanStart = span(attrs(".start.value"), numberUtils.durationToString(event.start()));
		final var oEnd = getEnd();

		final var spanEnd = oEnd
				.map(end -> span(attrs(".end.value"),
						numberUtils.durationToString(end)))
				.orElse(span(attrs(".end.value"), "EOF / end of media"));

		final var spanDuration = oEnd
				.map(end -> span(attrs(".duration.value"),
						numberUtils.durationToString(end.minus(event.start()))))
				.orElse(span());

		return div(attrs(".entry.event"), spanScope, spanStart, spanEnd, spanDuration);
	}

	@Override
	public boolean isEmpty() {
		return event == null;
	}

	@Override
	public void toJson(final JsonGenerator gen,
					   final SerializerProvider provider) throws IOException {
		gen.writeObjectFieldStart(jsonHeader("event"));

		final var scope = getScope();
		if (scope.isPresent()) {
			gen.writeStringField("scope", scope.get());
		}

		gen.writeNumberField("start", event.start().toMillis());

		final var end = getEnd();
		if (end.isPresent()) {
			gen.writeNumberField("end", end.get().toMillis());
			gen.writeNumberField("duration", end.get().minus(event.start()).toMillis());
		}

		gen.writeEndObject();
	}

}
