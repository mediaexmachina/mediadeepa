/*
 * This file is part of mediadeepa.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2023
 *
 */
package media.mexm.mediadeepa.rendererengine.components;

import java.util.List;

import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.TableDocument;
import media.mexm.mediadeepa.exportformat.TabularDocument;
import media.mexm.mediadeepa.exportformat.TabularExportFormat;
import media.mexm.mediadeepa.rendererengine.SingleTabularDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;

@Component
public class RawstderrfiltersRendererEngine implements
											TableRendererEngine,
											TabularRendererEngine,
											ConstStrings,
											SingleTabularDocumentExporterTraits {

	public static final List<String> HEAD_STDERRFILTERS = List.of(FILTER_NAME, CHAIN_POS, LINE);

	@Override
	public String getSingleUniqTabularDocumentBaseFileName() {
		return "rawstderrfilters";
	}

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		final var t = new TabularDocument(tabularExportFormat, getSingleUniqTabularDocumentBaseFileName()).head(
				HEAD_STDERRFILTERS);
		result.getRawStdErrEvents().stream()
				.filter(r -> r.getFilterName().equals("cropdetect") == false)
				.forEach(r -> t.row(r.getFilterName(), r.getFilterChainPos(), r.getLineValue()));
		return List.of(t);
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		final var t = tableDocument.createTable("rawstderrfilters").head(HEAD_STDERRFILTERS);
		result.getRawStdErrEvents().stream()
				.filter(r -> r.getFilterName().equals("cropdetect") == false)
				.forEach(r -> t.addRow()
						.addCell(r.getFilterName())
						.addCell(r.getFilterChainPos())
						.addCell(r.getLineValue()));
	}

}
