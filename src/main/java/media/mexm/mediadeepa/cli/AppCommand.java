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
import java.util.List;
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
							"           [-i FILE]... [--input-list TEXT_FILE_LIST]...",
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

	@Option(names = { "-i", "--input" },
			description = "Input (source media or Mediadeepa archive) file or full directory to work with",
			paramLabel = "FILE")
	private List<String> input;

	@ArgGroup(exclusive = false,
			  heading = "Scan directory options%n")
	private ScanDirCmd scanDirCmd;

	@Option(names = { "-il", "--input-list" },
			description = "Read input files from a text list",
			paramLabel = "TEXT_FILE_LIST")
	private List<String> inputList;

	@ArgGroup(exclusive = false,
			  heading = "Process media file options%n")
	private ProcessFileCmd processFileCmd;

	@Option(names = { "--temp" },
			description = "Temp dir to use in the case of the needs to export to a temp file",
			paramLabel = "DIRECTORY")
	private File tempDir;

	@ArgGroup(exclusive = true, heading = "Output options%n", multiplicity = "0..1")
	private OutputCmd outputCmd;

	@Option(names = { "--verbose" }, description = "Verbose mode")
	private boolean verbose;

	@Option(names = { "-q", "--quiet" }, description = "Quiet mode (don't log anyting, except errors)")
	private boolean quiet;

	@Option(names = { "--log" },
			description = "Redirect all log messages to text file",
			paramLabel = "LOG_FILE")
	private File logToFile;

	@Option(names = { "--graphic-jpg" }, description = "Export to JPEG instead to PNG the produced graphic images",
			required = false)
	private boolean graphicJpg;

	@Override
	public Integer call() throws Exception {
		return doCall.call();
	}

}
