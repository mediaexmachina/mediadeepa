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

import java.io.File;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
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

	public File makeOutputFile(final DataResult result, final String suffix) {
		var filePrefix = "";
		if (appCommand.getInput() != null && appCommand.getInput().size() > 1
			|| appCommand.getInputList() != null && appCommand.getInputList().isEmpty() == false) {
			final var rawSource = result.getSource();
			var correctedSource = FilenameUtils.getBaseName(rawSource);
			final var sourceExt = FilenameUtils.getExtension(rawSource);
			if (appConfig.isAddSourceExtToOutputDirectories() && sourceExt.isEmpty() == false) {
				correctedSource = correctedSource + "." + sourceExt;
			}
			filePrefix = correctedSource + "_";
		}

		final var oExportToCmd = Optional.ofNullable(appCommand.getOutputCmd())
				.flatMap(o -> Optional.ofNullable(o.getExportToCmd()));
		final var baseFileName = oExportToCmd.map(ExportToCmd::getBaseFileName).orElse(null);
		final var export = oExportToCmd.map(ExportToCmd::getExport).orElseThrow();

		if (baseFileName != null && baseFileName.isEmpty() == false) {
			if (baseFileName.endsWith("_")
				|| baseFileName.endsWith(" ")
				|| baseFileName.endsWith("-")
				|| baseFileName.endsWith("|")) {
				final var f = new File(export, filePrefix + baseFileName + suffix);
				log.trace(MAKE_OUTPUT_FILE_NAME, f);
				return f;
			}
			final var f = new File(export, filePrefix + baseFileName + "_" + suffix);
			log.trace(MAKE_OUTPUT_FILE_NAME, f);
			return f;
		}
		final var f = new File(export, filePrefix + suffix);
		log.trace(MAKE_OUTPUT_FILE_NAME, f);
		return f;
	}

}
