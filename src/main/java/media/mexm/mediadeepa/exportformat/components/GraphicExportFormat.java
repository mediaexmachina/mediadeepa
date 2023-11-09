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
package media.mexm.mediadeepa.exportformat.components;

import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.apache.commons.io.FilenameUtils.getBaseName;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.cli.ExportToCmd;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormat;
import media.mexm.mediadeepa.rendererengine.GraphicRendererEngine;

@Component
public class GraphicExportFormat implements ExportFormat {

	private final List<GraphicRendererEngine> engines;

	public GraphicExportFormat(@Autowired final List<GraphicRendererEngine> engines) {
		this.engines = Objects.requireNonNull(engines, "\"engines\" can't to be null");
	}

	@Override
	public Map<String, File> exportResult(final DataResult result, final ExportToCmd exportToCmd) {
		return engines.stream()
				.map(engine -> engine.toGraphic(result))
				.flatMap(List::stream)
				.collect(toUnmodifiableMap(
						f -> getBaseName(f.getFileName()),
						f -> f.save(exportToCmd)));
	}

	@Override
	public String getFormatName() {
		return "graphic";
	}

	@Override
	public String getFormatLongName() {
		return "Graphical representation of data";
	}

}
