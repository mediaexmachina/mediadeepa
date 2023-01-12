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
 * Copyright (C) Media ex Machina 2023
 *
 */
package media.mexm.mediadeepa.service;

import java.io.File;

import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ExportTo;
import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ExtractTo;
import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ImportFrom;
import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ProcessFile;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;

public interface AppSessionService {

	void verifyOptions(CommandLine commandLine,
					   ExportTo exportTo,
					   ExtractTo extractTo,
					   ImportFrom importFrom,
					   ProcessFile processFile,
					   File tempDir) throws ParameterException;

	void validateInputFile(final CommandLine commandLine, final File file) throws ParameterException;

	void validateOutputFile(final CommandLine commandLine, final File file) throws ParameterException;

	void validateOutputDir(final CommandLine commandLine, final File dir) throws ParameterException;

	void createSession(ExportTo exportTo,
					   ExtractTo extractTo,
					   ImportFrom importFrom,
					   ProcessFile processFile,
					   File tempDir);

}
