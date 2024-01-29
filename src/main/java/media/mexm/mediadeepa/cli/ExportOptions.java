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

import lombok.Data;
import picocli.CommandLine.Option;

@Data
public class ExportOptions {

	@Option(names = { "--graphic-jpg" }, description = "Export to JPEG instead to PNG the produced graphic images",
			required = false)
	private boolean graphicJpg;

	@Option(names = { "--single-export" },
			description = { "Export only this file",
							"Usage: \"internal-file-name:outputfilename.ext\"",
							"With \"internal-file-name\" the choosed file to export, as \"-o\" list it",
							"And \"outputfilename.ext\" the new file path name to produce",
							"Use \":\" in Linux/Posix and \";\" in Windows as separator",
							"This option invalidate \"-f\", \"-e\" and \"--export-base-filename\"" }, // TODO check multiline desc in MD+SITE+MAN
			required = false)
	private String singleExport;

}
