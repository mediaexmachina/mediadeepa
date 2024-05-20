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
import static java.lang.Math.round;
import static java.time.Duration.ofMillis;

import java.awt.Dimension;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import j2html.tags.DomContent;
import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.components.NumberUtils;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdCropdetect;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdValue;

public record CropEventReportEntry(LavfiMtdValue<LavfiMtdCropdetect> cropEvent,
								   Dimension fullFrame)
								  implements ReportEntry, JsonContentProvider, ConstStrings {

	private static final String STATUS = "status";
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

		if (hasAFullFrame()) {
			x2 = span(attrs(".pos"), String.valueOf(getX2FullFrame()));
			y2 = span(attrs(".pos"), String.valueOf(getY2FullFrame()));
		}

		var status = span(attrs(FRMSTATUS));
		if (isBlackFrame()) {
			status = span(attrs(FRMSTATUS), BLACK_FRAME_FULL_CROP);
			x1 = span(attrs(".pos"));
			y1 = x1;
			x2 = x1;
			y2 = x1;
		} else if (isFullFrame()) {
			status = span(attrs(FRMSTATUS), BACK_TO_FULL_FRAME_NO_CROP);
			x2 = span(attrs(".pos"), "0");
			y2 = span(attrs(".pos"), "0");
		}

		return div(attrs(".cropevent"), frame, pos, x1, y1, x2, y2, status);
	}

	private int getY2FullFrame() {
		return fullFrame.height - (cropEvent.value().y2() + 1);
	}

	private int getX2FullFrame() {
		return fullFrame.width - (cropEvent.value().x2() + 1);
	}

	private boolean isFullFrame() {
		return cropEvent.value().w() == fullFrame.width
			   && cropEvent.value().h() == fullFrame.height
			   && fullFrame.width > 0
			   && fullFrame.height > 0;
	}

	private boolean isBlackFrame() {
		return cropEvent.value().w() < 0 && cropEvent.value().h() < 0;
	}

	private boolean hasAFullFrame() {
		return fullFrame.width > 0 && fullFrame.height > 0;
	}

	public static DomContent getHeader() {
		return div(attrs(".cropevent.header"),
				span(attrs(".key.frame"), FRAME),
				span(attrs(".key.ptstime"), POSITION),
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

	@Override
	public void toJson(final JsonGenerator gen,
					   final SerializerProvider provider) throws IOException {
		gen.writeNumberField("frame", cropEvent.frame());
		gen.writeNumberField("position", Math.round(cropEvent.ptsTime() * 1000f));

		if (isBlackFrame()) {
			gen.writeStringField(STATUS, "black-frame_full-crop");
		} else if (isFullFrame()) {
			gen.writeStringField(STATUS, "full-frame_no-crop");
		} else {
			var x2 = cropEvent.value().x2();
			var y2 = cropEvent.value().y2();

			if (hasAFullFrame()) {
				x2 = getX2FullFrame();
				y2 = getY2FullFrame();
			}

			gen.writeNumberField("left", cropEvent.value().x1());
			gen.writeNumberField("top", cropEvent.value().y1());
			gen.writeNumberField("right", x2);
			gen.writeNumberField("bottom", y2);
			gen.writeStringField(STATUS, "crop-frame");
		}
	}

}
