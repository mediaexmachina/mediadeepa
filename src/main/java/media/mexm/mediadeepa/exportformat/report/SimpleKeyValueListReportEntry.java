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
import static j2html.TagCreator.ul;

import java.util.Collection;

import j2html.TagCreator;
import j2html.tags.DomContent;

public record SimpleKeyValueListReportEntry(
											String key,
											Collection<String> value) implements ReportEntry, NumericReportEntry {
	@Override
	public DomContent toDomContent() {
		return div(attrs(".entry"),
				span(attrs(".key"), getKeyWithPlurial(value.size() > 1)),
				ul(attrs(".value"), each(value.stream().map(TagCreator::li))));
	}

	@Override
	public boolean isEmpty() {
		return value == null || value.isEmpty();
	}

}
