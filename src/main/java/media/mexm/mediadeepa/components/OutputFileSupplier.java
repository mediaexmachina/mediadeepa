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
 * Copyright (C) Media ex Machina 2024
 *
 */
package media.mexm.mediadeepa.components;

import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;

import java.io.File;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.cli.AppCommand;
import media.mexm.mediadeepa.cli.ExportToCmd;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;

@Component
@Slf4j
public class OutputFileSupplier {

	private static final String MAKE_OUTPUT_FILE_NAME = "Make output file name: {}";

	@Autowired
	private AppCommand appCommand;
	@Autowired
	private AppConfig appConfig;

	public File makeOutputFile(final DataResult dataResult, final String suffix) {
		var filePrefix = "";
		if (dataResult.isInMultipleSourcesSet()) {
			final var rawSource = dataResult.getSource();
			var correctedSource = getBaseName(rawSource);
			final var sourceExt = getExtension(rawSource);
			if (appConfig.isAddSourceExtToOutputDirectories() && sourceExt.isEmpty() == false) {
				correctedSource = correctedSource + "-" + sourceExt;
			}
			filePrefix = correctedSource + "_";
		}

		final var oExportToCmd = Optional.ofNullable(appCommand.getOutputCmd())
				.flatMap(o -> Optional.ofNullable(o.getExportToCmd()));
		final var baseFileName = oExportToCmd.map(ExportToCmd::getBaseFileName).orElse(null);
		final var export = oExportToCmd.map(ExportToCmd::getExport).orElseThrow();

		final var result = assembleOutputParams(suffix, filePrefix, baseFileName, export);
		log.trace(MAKE_OUTPUT_FILE_NAME, result);
		return result;
	}

	private File assembleOutputParams(final String suffix,
									  final String filePrefix,
									  final String baseFileName,
									  final File export) {
		if (baseFileName != null && baseFileName.isEmpty() == false) {
			if (baseFileName.endsWith("_")
				|| baseFileName.endsWith(" ")
				|| baseFileName.endsWith("-")
				|| baseFileName.endsWith("|")) {
				return new File(export, filePrefix + baseFileName + suffix);
			} else {
				return new File(export, filePrefix + baseFileName + "_" + suffix);
			}
		} else {
			return new File(export, filePrefix + suffix);
		}
	}

}
