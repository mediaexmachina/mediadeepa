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
package media.mexm.mediadeepa.cli;

import java.io.File;
import java.util.Set;

import lombok.Data;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

@Data
public class ExportToCmd {

	@Option(names = { "-f", "--format" },
			description = "Format to export datas",
			paramLabel = "FORMAT_TYPE")
	private Set<String> format;

	@Option(names = { "-e", "--export" },
			description = "Export datas to this directory",
			paramLabel = "DIRECTORY",
			required = true)
	private File export;

	@Option(names = { "--export-base-filename" },
			description = "Base file name for exported data file(s)",
			paramLabel = "FILENAME",
			required = false)
	private String baseFileName;

	@ArgGroup(exclusive = false, heading = "Export options%n")
	private ExportOptions exportOptions;

	public File makeOutputFile(final String suffix) {
		if (baseFileName != null && baseFileName.isEmpty() == false) {
			if (baseFileName.endsWith("_")
				|| baseFileName.endsWith(" ")
				|| baseFileName.endsWith("-")
				|| baseFileName.endsWith("|")) {
				return new File(export, baseFileName + suffix);
			}
			return new File(export, baseFileName + "_" + suffix);
		}
		return new File(export, suffix);
	}

}
