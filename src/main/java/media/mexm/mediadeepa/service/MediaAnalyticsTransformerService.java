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
package media.mexm.mediadeepa.service;

import java.io.File;
import java.io.OutputStream;
import java.util.Optional;
import java.util.stream.Stream;

import media.mexm.mediadeepa.cli.ExportToCmd;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormat;

public interface MediaAnalyticsTransformerService {

	void exportAnalytics(DataResult result,
						 ExportToCmd exportToCmd);

	void singleExportAnalytics(String internalFileName,
							   DataResult result,
							   File outputFile);

	void singleExportAnalyticsToOutputStream(String internalFileName, DataResult dataResult, OutputStream out);

	boolean isExportFormatExists(String name);

	Stream<ExportFormat> getSelectedExportFormats(ExportToCmd exportToCmd, Optional<String> internalFileName);

}
