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
 * Copyright (C) Media ex Machina 2024
 *
 */
package media.mexm.mediadeepa.exportformat;

import java.util.List;

import org.jfree.chart.JFreeChart;

import j2html.TagCreator;
import j2html.tags.DomContent;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.exportformat.report.DomContentProvider;

public record ChartGraphicWrapper(String rangeName, List<? extends SeriesStyle> series, JFreeChart chart)
								 implements DomContentProvider {

	@Override
	public DomContent toDomContent(final NumberUtils numberUtils) {
		final var seriesDom = TagCreator.each(series.stream()
				.map(s -> TagCreator.span(TagCreator.attrs(".serie"),
						TagCreator.span(TagCreator.attrs(".color"), " ")
								.withStyle("background-color: " + s.getCSSColor() + ";"),
						TagCreator.span(TagCreator.attrs(".name"), s.getName()))));
		return TagCreator.each(
				TagCreator.span(TagCreator.attrs(".rangename"), rangeName),
				TagCreator.text(" for "),
				seriesDom);
	}

}
