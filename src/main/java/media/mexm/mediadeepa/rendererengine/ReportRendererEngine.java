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

import org.slf4j.Logger;

import media.mexm.mediadeepa.cli.AppCommand;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.report.ImageReportEntry;
import media.mexm.mediadeepa.exportformat.report.ReportDocument;
import media.mexm.mediadeepa.exportformat.report.ReportSection;

public interface ReportRendererEngine {
	Logger internalLog = getLogger(ReportRendererEngine.class);

	void addToReport(DataResult result, ReportDocument document);

	default void addAllGraphicsToReport(final GraphicRendererEngine graphicRendererEngine,
										final DataResult result,
										final ReportSection section,
										final AppConfig appConfig,
										final AppCommand appCommand) {
		if (appConfig.getReportConfig().isAddImages() == false) {
			internalLog.debug("Don't compute graphic for report producing, by conf");
			return;
		}

		final var graphics = graphicRendererEngine.toGraphic(result);
		if (graphics.isEmpty()) {
			internalLog.debug("No graphics to display for report producing");
			return;
		}

		internalLog.debug("Add {} graphic(s) to report producing", graphics.size());
		graphics.stream()
				.map(f -> f.toGraphicReportEntry(appCommand, appConfig))
				.forEach(section::add);
	}

	default void addAllSignalImageToReport(final SignalImageRendererEngine signalImageRendererEngine,
										   final DataResult result,
										   final ReportSection section,
										   final AppConfig appConfig) {
		if (appConfig.getReportConfig().isAddImages() == false) {
			internalLog.debug("Don't compute signal images for report producing, by conf");
			return;
		}

		signalImageRendererEngine.makeimagePNG(result)
				.map(image -> new ImageReportEntry(image, signalImageRendererEngine))
				.ifPresent(section::add);
	}

}
