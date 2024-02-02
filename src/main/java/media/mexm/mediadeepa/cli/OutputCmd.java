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
package media.mexm.mediadeepa.cli;

import lombok.Data;
import picocli.CommandLine.ArgGroup;

@Data
public class OutputCmd {

	@ArgGroup(exclusive = false,
			  heading = "Extract to archive%n")
	private ExtractToCmd extractToCmd;

	@ArgGroup(exclusive = false,
			  heading = "Export to generated files%n")
	private ExportToCmd exportToCmd;

	@ArgGroup(exclusive = false,
			  heading = "Single export option%n")
	private SingleExportCmd singleExportCmd;

}
