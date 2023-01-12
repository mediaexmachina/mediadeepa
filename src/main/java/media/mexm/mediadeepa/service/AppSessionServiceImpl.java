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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ExportTo;
import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ExtractTo;
import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ImportFrom;
import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ProcessFile;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

@Service
public class AppSessionServiceImpl implements AppSessionService {
	private static Logger log = LogManager.getLogger();

	@Autowired
	private FFmpegService ffmpegService;
	@Autowired
	private String ffmpegExecName;
	@Autowired
	private String ffprobeExecName;
	@Autowired
	private ExecutableFinder executableFinder;

	@Override
	public void verifyOptions(final CommandLine commandLine,
							  final ExportTo exportTo,
							  final ExtractTo extractTo,
							  final ImportFrom importFrom,
							  final ProcessFile processFile,
							  final File tempDir) throws ParameterException {
		if ((processFile != null && extractTo != null)
			^ (processFile != null && exportTo != null)
			^ (importFrom != null && exportTo != null)) {
			throw new ParameterException(commandLine,
					"You can't cumulate options like --input/--import, --extract/--export");
		}

		if (processFile != null) {
			validateInputFile(commandLine, processFile.getInput());
		}
		if (extractTo != null) {
			Optional.ofNullable(extractTo.getVlavfi())
					.ifPresent(f -> validateOutputFile(commandLine, f));
			Optional.ofNullable(extractTo.getAlavfi())
					.ifPresent(f -> validateOutputFile(commandLine, f));
			Optional.ofNullable(extractTo.getContainer())
					.ifPresent(f -> validateOutputFile(commandLine, f));
			Optional.ofNullable(extractTo.getEbur128())
					.ifPresent(f -> validateOutputFile(commandLine, f));
			Optional.ofNullable(extractTo.getStderr())
					.ifPresent(f -> validateOutputFile(commandLine, f));
		}
		if (exportTo != null) {
			validateOutputDir(commandLine, exportTo.getExport());
			if (exportTo.getFormat() == null || exportTo.getFormat().isEmpty()) {
				throw new ParameterException(commandLine, "Export format can't be empty");
			}
		}
		if (importFrom != null) {
			Optional.ofNullable(importFrom.getContainer())
					.ifPresent(f -> validateInputFile(commandLine, f));
			Optional.ofNullable(importFrom.getLavfi())
					.stream()
					.flatMap(Set::stream)
					.forEach(f -> validateInputFile(commandLine, f));
			Optional.ofNullable(importFrom.getStderr())
					.ifPresent(f -> validateInputFile(commandLine, f));
		}
	}

	@Override
	public void validateInputFile(final CommandLine commandLine, final File file) throws ParameterException {
		if (file == null) {
			throw new ParameterException(commandLine, "You must set an file");
		} else if (file.exists() == false) {
			throw new ParameterException(commandLine, "Can't found the provided file",
					new FileNotFoundException(file.getPath()));
		} else if (file.isFile() == false) {
			throw new ParameterException(commandLine, "The provided file is not a regular file",
					new FileNotFoundException(file.getPath()));
		}
	}

	@Override
	public void validateOutputFile(final CommandLine commandLine, final File file) throws ParameterException {
		if (file == null) {
			throw new ParameterException(commandLine, "You must set an file");
		} else if (file.exists() && file.isFile() == false) {
			throw new ParameterException(commandLine, "Can't overwrite the provided file: it's a directory",
					new IOException(file.getPath()));
		} else if (file.exists() && file.canWrite() == false) {
			throw new ParameterException(commandLine, "Can't overwrite the provided file",
					new IOException(file.getPath()));
		}
	}

	@Override
	public void validateOutputDir(final CommandLine commandLine, final File dir) throws ParameterException {
		if (dir == null) {
			throw new ParameterException(commandLine, "You must set a directory");
		} else if (dir.exists() == false) {
			try {
				FileUtils.forceMkdir(dir);
			} catch (final IOException e) {
				throw new ParameterException(commandLine, "Can't create the provided directory", e);
			}
		} else if (dir.isDirectory() == false) {
			throw new ParameterException(commandLine, "The provided directory is not a regular directory",
					new FileNotFoundException(dir.getPath()));
		}
	}

	@Override
	public void createSession(final ExportTo exportTo,
							  final ExtractTo extractTo,
							  final ImportFrom importFrom,
							  final ProcessFile processFile,
							  final File tempDir) {
	}

	/*final var mtd = ffmpegService.doExtractMtd(input, new ProgressCLI(out()),
	typeExclusive.audioNo,
	typeExclusive.videoNo);

	final var lavfi = mtd.lavfiMetadatas();
	final var filters = lavfi.stream()
	.flatMap(f -> f.getValuesByFilterKeysByFilterName().keySet().stream())
	.distinct()
	.toList();*/

	// XXX log.info("Mtd: {}, {}, I={} LU", lavfi.size(), filters, mtd.ebur128Summary().getIntegrated());

	/*lavfi.stream()
		.forEach(f -> System.out.println(f.getLavfiMtdPosition().frame()
										 + "\t"
										 + f.getValuesByFilterKeysByFilterName().size()));*/

	// FIXME missing silence detect

	// TODO test video
	/*
	final var r128s = maResult.ebur128Summary();
	Optional.ofNullable(r128s).ifPresent(r -> log.info("LUFS: {}", r));

	final var m = maResult.lavfiMetadatas();

	afAPhasemeter.getEvents(m).;
	afAPhasemeter.getMetadatas(m);
	*/

}
