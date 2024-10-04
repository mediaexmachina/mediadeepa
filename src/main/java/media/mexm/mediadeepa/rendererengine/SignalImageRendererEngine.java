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
 * Copyright (C) hdsdi3g for hd3g.tv 2024
 *
 */
package media.mexm.mediadeepa.rendererengine;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Optional;

import org.slf4j.Logger;

import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ImageArtifact;
import media.mexm.mediadeepa.exportformat.report.DomContentProvider;
import media.mexm.mediadeepa.exportformat.report.ImageReportEntry;
import media.mexm.mediadeepa.exportformat.report.ReportSection;

public interface SignalImageRendererEngine extends DomContentProvider {
	Logger internalLog = getLogger(SignalImageRendererEngine.class);

	Optional<ImageArtifact> makeimagePNG(DataResult result);

	String getManagedReturnName();

	String getDefaultInternalFileName();

	default void addAllSignalImageToReport(final DataResult result,
										   final ReportSection section,
										   final AppConfig appConfig) {
		if (appConfig.getReportConfig().isAddImages() == false) {
			internalLog.debug("Don't compute signal images for report producing, by conf");
			return;
		}

		makeimagePNG(result)
				.map(image -> new ImageReportEntry(image, this, appConfig.getReportConfig().getDisplayImageSizeWidth()))
				.ifPresent(section::add);
	}

}
