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
import static j2html.TagCreator.each;
import static j2html.TagCreator.span;

import java.awt.Dimension;
import java.util.List;

import j2html.tags.DomContent;
import media.mexm.mediadeepa.components.NumberUtils;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdCropdetect;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdValue;

public record CropEventTableReportEntry(List<LavfiMtdValue<LavfiMtdCropdetect>> cropEvents,
										Dimension fullFrame) implements ReportEntry {

	@Override
	public DomContent toDomContent(final NumberUtils numberUtils) {
		return div(attrs(".entry.cropevents"),
				span(attrs(".key"), "First crop values:"),
				div(attrs(".cropeventslist"),
						CropEventReportEntry.getHeader(),
						each(cropEvents.stream()
								.map(c -> new CropEventReportEntry(c, fullFrame))
								.map(f -> f.toDomContent(numberUtils)))));
	}

	@Override
	public boolean isEmpty() {
		return cropEvents.isEmpty();
	}

}
