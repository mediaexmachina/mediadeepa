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
 * Copyright (C) hdsdi3g for hd3g.tv 2024
 *
 */
package media.mexm.mediadeepa.cli;

import java.util.List;

import lombok.Data;
import picocli.CommandLine.Option;

@Data
public class ScanDirCmd {

	@Option(names = { "-r", "--recursive" }, description = "Scan a directory and all its sub directory to work with")
	private boolean recursive;

	@Option(names = { "--scan" },
			description = "Time, in seconds, between two regular scan of input directories, if applicable",
			paramLabel = "SECONDS")
	private int timeBetweenScans;

	@Option(names = { "--include-ext" },
			description = "Only search files with this extention, during directory scan",
			paramLabel = "EXTENTION")
	private List<String> allowedExtentions;

	@Option(names = { "--exclude-ext" },
			description = "Ignore files with this extention, during directory scan",
			paramLabel = "EXTENTION")
	private List<String> blockedExtentions;

	@Option(names = { "--exclude-path" },
			description = "Ignore files founded under this directory, during directory scan",
			paramLabel = "PATH")
	private List<String> ignoreRelativePaths;

	@Option(names = { "--include-file" },
			description = "Only search files with this name (with willcards), during directory scan",
			paramLabel = "FILE_NAME")
	private List<String> allowedFileNames;

	@Option(names = { "--include-dir" },
			description = "Only search sub-directories with this name (with willcards), during directory scan",
			paramLabel = "DIRECTORY_NAME")
	private List<String> allowedDirNames;

	@Option(names = { "--exclude-file" },
			description = "Ignore files with this name (with willcards), during directory scan",
			paramLabel = "DIRECTORY_NAME")
	private List<String> blockedFileNames;

	@Option(names = { "--exclude-dir" },
			description = "Ignore sub-directories with this name (with willcards), during directory scan",
			paramLabel = "DIRECTORY_NAME")
	private List<String> blockedDirNames;

	@Option(names = { "--include-hidden" },
			description = "Allow hidded files (and dot files), during directory scan")
	private boolean allowedHidden;

	@Option(names = { "--include-link" },
			description = "Allow symbolic links, during directory scan")
	private boolean allowedLinks;

}
