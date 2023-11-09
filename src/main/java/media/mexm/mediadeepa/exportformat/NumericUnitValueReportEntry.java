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

import j2html.tags.DomContent;
import media.mexm.mediadeepa.components.NumberUtils;

public record NumericUnitValueReportEntry(
										  String key,
										  Number value,
										  String unit) implements ReportEntry, NumericReportEntry {

	@Override
	public boolean isEmpty() {
		return value == null || ((Double) value.doubleValue()).isNaN();
	}

	@Override
	public DomContent toDomContent(final NumberUtils numberUtils) {
		if (value.longValue() == 0) {
			return div(attrs(".entry"),
					span(attrs(".key"), getKeyWithPlurial(false)),
					span(attrs(".value"), "no"),
					span(attrs(".unit"), " " + getUnitWithPlurial(false)));
		}

		final var plurial = value.longValue() > 1;
		return div(attrs(".entry"),
				span(attrs(".key"), getKeyWithPlurial(plurial)),
				span(attrs(".value"), numberUtils.valueToString(value)),
				span(attrs(".unit"), " " + getUnitWithPlurial(plurial)));
	}

}
