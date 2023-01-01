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
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

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

	void validateInputFile(CommandLine commandLine, File file) throws ParameterException;

	void validateOutputFile(CommandLine commandLine, File file) throws ParameterException;

	void validateOutputDir(CommandLine commandLine, File dir) throws ParameterException;

	void createExtractionSession(ProcessFile processFile, ExtractTo extractTo, File tempDir) throws IOException;

	void createProcessingSession(ProcessFile processFile, ExportTo exportTo, File tempDir) throws IOException;

	void createOfflineProcessingSession(ImportFrom importFrom, ExportTo exportTo) throws IOException;

	File prepareTempFile(File tempDir);

	void writeNonEmptyLines(File file, List<String> lines) throws IOException;

	Consumer<String> makeConsumerToList(List<String> list, File reference);

	Stream<String> openFileToLineStream(File file);
}
