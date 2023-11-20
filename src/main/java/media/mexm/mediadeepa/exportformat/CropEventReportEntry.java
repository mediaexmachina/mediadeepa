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
import static java.lang.Math.round;
import static java.time.Duration.ofMillis;

import java.awt.Dimension;

import j2html.tags.DomContent;
import media.mexm.mediadeepa.components.NumberUtils;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdCropdetect;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdValue;

public record CropEventReportEntry(LavfiMtdValue<LavfiMtdCropdetect> cropEvent,
								   Dimension fullFrame) implements ReportEntry {

	private static final String FRMSTATUS = ".framestatus";
	private static final String KEY_POS = ".key.pos";

	@Override
	public DomContent toDomContent(final NumberUtils numberUtils) {
		final var frame = span(attrs(".frame"),
				String.valueOf(cropEvent.frame()));
		final var pos = span(attrs(".ptstime"),
				numberUtils.durationToString(ofMillis(round(numberUtils.secToMs(cropEvent.ptsTime())))));
		var x1 = span(attrs(".pos"), String.valueOf(cropEvent.value().x1()));
		var y1 = span(attrs(".pos"), String.valueOf(cropEvent.value().y1()));
		var x2 = span(attrs(".pos"), String.valueOf(cropEvent.value().x2()));
		var y2 = span(attrs(".pos"), String.valueOf(cropEvent.value().y2()));

		if (fullFrame.width > 0 && fullFrame.height > 0) {
			x2 = span(attrs(".pos"), String.valueOf(fullFrame.width - (cropEvent.value().x2() + 1)));
			y2 = span(attrs(".pos"), String.valueOf(fullFrame.height - (cropEvent.value().y2() + 1)));
		}

		var status = span(attrs(FRMSTATUS));
		if (cropEvent.value().w() < 0 && cropEvent.value().h() < 0) {
			status = span(attrs(FRMSTATUS), "Black frame (full crop)");
			x1 = span(attrs(".pos"));
			y1 = x1;
			x2 = x1;
			y2 = x1;
		} else if (cropEvent.value().w() == fullFrame.width
				   && cropEvent.value().h() == fullFrame.height
				   && fullFrame.width > 0
				   && fullFrame.height > 0) {
			status = span(attrs(FRMSTATUS), "Back to full frame (no crop)");
			x2 = span(attrs(".pos"), "0");
			y2 = span(attrs(".pos"), "0");
		}

		return div(attrs(".cropevent"), frame, pos, x1, y1, x2, y2, status);
	}

	public static DomContent getHeader() {
		return div(attrs(".cropevent.header"),
				span(attrs(".key.frame"), "Frame"),
				span(attrs(".key.ptstime"), "Position"),
				span(attrs(KEY_POS), "Left"),
				span(attrs(KEY_POS), "Top"),
				span(attrs(KEY_POS), "Right"),
				span(attrs(KEY_POS), "Bottom"),
				span(attrs(".key.status"), "Status"));
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

}
