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

import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.apache.commons.io.FilenameUtils.getBaseName;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import media.mexm.mediadeepa.cli.ExportToCmd;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;

public abstract class TabularExportFormat implements ExportFormat, TabularDocumentExporter {

	protected final List<TabularRendererEngine> engines;

	protected TabularExportFormat(final List<TabularRendererEngine> engines) {
		this.engines = Objects.requireNonNull(engines, "\"engines\" can't to be null");
	}

	@Override
	public Map<String, File> exportResult(final DataResult result, final ExportToCmd exportToCmd) {
		return engines.stream()
				.map(en -> en.toTabularDocument(result, this))
				.flatMap(List::stream)
				.map(tabular -> tabular.exportToFile(exportToCmd))
				.flatMap(Optional::stream)
				.collect(toUnmodifiableMap(f -> getBaseName(f.getName()), f -> f));
	}

}
