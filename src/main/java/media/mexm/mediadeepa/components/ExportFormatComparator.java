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
package media.mexm.mediadeepa.components;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.exportformat.ExportFormat;
import media.mexm.mediadeepa.exportformat.components.FFProbeXMLExportFormat;
import media.mexm.mediadeepa.exportformat.components.GraphicExportFormat;
import media.mexm.mediadeepa.exportformat.components.ReportExportFormat;
import media.mexm.mediadeepa.exportformat.components.TableJsonExportFormat;
import media.mexm.mediadeepa.exportformat.components.TableSQLiteExportFormat;
import media.mexm.mediadeepa.exportformat.components.TableXLSXExportFormat;
import media.mexm.mediadeepa.exportformat.components.TableXMLExportFormat;
import media.mexm.mediadeepa.exportformat.components.TabularCSVExportFormat;
import media.mexm.mediadeepa.exportformat.components.TabularCSVFrExportFormat;
import media.mexm.mediadeepa.exportformat.components.TabularTXTExportFormat;

@Component
@Slf4j
public class ExportFormatComparator implements Comparator<ExportFormat> {

	private static final List<Class<?>> EXPORT_FORMAT_DISPLAY_ORDER = List.of(
			TabularTXTExportFormat.class,
			TabularCSVExportFormat.class,
			TabularCSVFrExportFormat.class,
			TableXMLExportFormat.class,
			TableJsonExportFormat.class,
			TableXLSXExportFormat.class,
			TableSQLiteExportFormat.class,
			GraphicExportFormat.class,
			ReportExportFormat.class,
			FFProbeXMLExportFormat.class);

	@Override
	public int compare(final ExportFormat l, final ExportFormat r) {
		var lPos = EXPORT_FORMAT_DISPLAY_ORDER.indexOf(l.getClass());
		if (lPos == -1) {
			lPos = EXPORT_FORMAT_DISPLAY_ORDER.size();
			log.warn("You should add new export format to {}: {}", getClass().getSimpleName(), l.getClass());
		}
		var rPos = EXPORT_FORMAT_DISPLAY_ORDER.indexOf(r.getClass());
		if (rPos == -1) {
			rPos = EXPORT_FORMAT_DISPLAY_ORDER.size();
			log.warn("You should add new export format to {}: {}", getClass().getSimpleName(), r.getClass());
		}
		return Integer.compare(lPos, rPos);
	}

}
