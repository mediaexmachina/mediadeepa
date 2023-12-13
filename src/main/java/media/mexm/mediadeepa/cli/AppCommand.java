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

import static media.mexm.mediadeepa.App.NAME;

import java.io.File;
import java.util.concurrent.Callable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

@Command(name = NAME,
		 description = "Extract/process technical informations from audio/videos files/streams",
		 version = { "Mediadeepa %1$s",
					 "Copyright (C) 2022-%2$s Media ex Machina, under the GNU General Public License." },
		 sortOptions = false,
		 separator = " ",
		 usageHelpAutoWidth = true,
		 synopsisHeading = "Base usage: ",
		 exitCodeListHeading = "Exit codes:%n",
		 exitCodeList = { ExitCode.OK + ":Ok/done",
						  ExitCode.USAGE + ":Error" },
		 customSynopsis = {
							"mediadeepa [-hov] [--temp DIRECTORY]",
							"           [-i FILE] [--import FILE]",
							"           [-c] [-mn] [-an | -vn] [-fo FILTER] [-fn FILTER] [--filter-X VALUE]",
							"           [-f FORMAT_TYPE] [-e DIRECTORY] [--extract FILE]"
		 })
@Data
@EqualsAndHashCode(callSuper = false)
public class AppCommand implements Callable<Integer> {

	@Setter
	private Callable<Integer> doCall;

	@Option(names = { "-h", "--help" }, description = "Show the usage help", usageHelp = true)
	private boolean help;

	@Option(names = { "-v", "--version" }, description = "Show the application version")
	private boolean version;

	@Option(names = { "-o", "--options" }, description = "Show the avaliable options on this system")
	private boolean options;

	@Option(names = { "--autocomplete" },
			description = "Show the autocomplete bash script for this application",
			hidden = true)
	private boolean autocomplete;

	@ArgGroup(exclusive = false, heading = "Process file%n")
	private ProcessFileCmd processFileCmd;

	@Option(names = { "--temp" },
			description = "Temp dir to use in the case of the needs to export to a temp file",
			paramLabel = "DIRECTORY")
	private File tempDir;

	@ArgGroup(exclusive = false, heading = "Extract to archive%n")
	private ExtractToCmd extractToCmd;

	@ArgGroup(exclusive = false, heading = "Import from archive%n")
	private ImportFromCmd importFromCmd;

	@ArgGroup(exclusive = false, heading = "Export to generated files%n")
	private ExportToCmd exportToCmd;

	@Option(names = { "--verbose" }, description = "Verbose mode")
	private boolean verbose;

	@Option(names = { "-q", "--quiet" }, description = "Quiet mode (don't log anyting, except errors)")
	private boolean quiet;

	@Option(names = { "--log" },
			description = "Redirect all log messages to text file",
			paramLabel = "LOG_FILE")
	private File logToFile;

	@Override
	public Integer call() throws Exception {
		return doCall.call();
	}

}
