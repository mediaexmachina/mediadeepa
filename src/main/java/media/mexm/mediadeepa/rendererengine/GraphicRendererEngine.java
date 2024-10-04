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
package media.mexm.mediadeepa.rendererengine;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;

import media.mexm.mediadeepa.cli.AppCommand;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.GraphicArtifact;
import media.mexm.mediadeepa.exportformat.report.ReportSection;

public interface GraphicRendererEngine {
	Logger internalLog = getLogger(GraphicRendererEngine.class);

	List<GraphicArtifact> toGraphic(DataResult result);

	Optional<GraphicArtifact> toSingleGraphic(String graphicBaseFileName, DataResult result);

	Set<String> getGraphicInternalProducedBaseFileNames();

	default void addAllGraphicsToReport(final DataResult result,
										final ReportSection section,
										final AppConfig appConfig,
										final AppCommand appCommand) {
		if (appConfig.getReportConfig().isAddImages() == false) {
			internalLog.debug("Don't compute graphic for report producing, by conf");
			return;
		}

		final var graphics = toGraphic(result);
		if (graphics.isEmpty()) {
			internalLog.debug("No graphics to display for report producing");
			return;
		}

		internalLog.debug("Add {} graphic(s) to report producing", graphics.size());
		graphics.stream()
				.map(f -> f.toGraphicReportEntry(appCommand, appConfig))
				.forEach(section::add);
	}

}
