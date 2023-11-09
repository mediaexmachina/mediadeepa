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
package media.mexm.mediadeepa.exportformat;

import static j2html.TagCreator.attrs;
import static j2html.TagCreator.div;
import static j2html.TagCreator.span;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import j2html.TagCreator;
import j2html.tags.DomContent;
import media.mexm.mediadeepa.components.NumberUtils;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdEvent;

public record EventReportEntry(LavfiMtdEvent event, Optional<Duration> sourceDuration) implements ReportEntry {

	public static ReportEntry createHeader(final List<LavfiMtdEvent> events, final Optional<Duration> sourceDuration) {
		final var hasScope = events.stream()
				.anyMatch(event -> event.scope() != null
								   && event.scope().isEmpty() == false);
		final var hasEnd = events.stream()
				.map(LavfiMtdEvent::end)
				.anyMatch(end -> end != null
								 && end.toMillis() > 0
								 || sourceDuration.isPresent());

		return new ReportEntry() {

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
		};
	}

	@Override
	public DomContent toDomContent(final NumberUtils numberUtils) {
		var spanScope = span();
		if (event.scope() != null && event.scope().isEmpty() == false) {
			spanScope = span(attrs(".scope.value"), event.scope());
		}

		final var spanStart = span(attrs(".start.value"), numberUtils.durationToString(event.start()));

		var spanEnd = span(attrs(".end.value"), "EOF / end of media");
		var end = event.end();
		if (end != null && end.toMillis() > 0 || sourceDuration.isPresent()) {
			end = event.getEndOr(sourceDuration.get());
			spanEnd = span(attrs(".end.value"), numberUtils.durationToString(end));
		} else {
			end = null;
		}

		var spanDuration = span();
		if (end != null) {
			spanDuration = span(attrs(".duration.value"), numberUtils.durationToString(end.minus(event.start())));
		}

		return div(attrs(".entry.event"), spanScope, spanStart, spanEnd, spanDuration);
	}

	@Override
	public boolean isEmpty() {
		return event == null;
	}

}
