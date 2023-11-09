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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import media.mexm.mediadeepa.cli.ExportToCmd;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.exportformat.TableDocument.Table;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;

public abstract class TableExportFormat implements ExportFormat {

	protected final List<TableRendererEngine> engines;
	protected final NumberUtils numberUtils;

	protected TableExportFormat(final List<TableRendererEngine> engines,
								final NumberUtils numberUtils) {
		this.engines = Objects.requireNonNull(engines, "\"engines\" can't to be null");
		this.numberUtils = Objects.requireNonNull(numberUtils, "\"numberUtils\" can't to be null");
	}

	@Override
	public Map<String, File> exportResult(final DataResult result, final ExportToCmd exportToCmd) {
		final var tableDocument = new TableDocument(numberUtils);
		engines.forEach(en -> en.addToTable(result, tableDocument));
		return Map.of("tables", save(result, tableDocument.getTables(), exportToCmd));
	}

	public abstract File save(DataResult result, List<Table> tables, ExportToCmd exportToCmd);

}
