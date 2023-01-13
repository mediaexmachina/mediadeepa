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
import java.io.PrintWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

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
	@Autowired
	private ScheduledExecutorService scheduledExecutorService;

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
		} else if (processFile == null
				   && extractTo == null
				   && exportTo == null
				   && importFrom == null) {
			throw new ParameterException(commandLine,
					"You must setup options like --input --import --extract --export");
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
							  final File tempDir) throws IOException {
		if (processFile != null && extractTo != null) {
			log.info("Prepare extraction session");

			if (processFile.isNoMediaAnalysing() == false) {
				final var lavfiSecondaryFile = prepareTempFile(tempDir);
				final var maSession = ffmpegService.createMediaAnalyserSession(processFile, lavfiSecondaryFile);
				maSession.setMaxExecutionTime(Duration.ofSeconds(processFile.getMaxSec()), scheduledExecutorService);

				final var stderrList = new ArrayList<String>();

				if (maSession.getAudioFilters().isEmpty() == false) {
					final var aLavfiList = new ArrayList<String>();
					final var cAlavfi = makeConsumerToList(aLavfiList, extractTo.getAlavfi());
					final var cStderr = makeConsumerToList(stderrList, extractTo.getStderr());

					maSession.extract(cAlavfi, cStderr);

					writeNonEmptyLines(extractTo.getAlavfi(), aLavfiList);
					writeNonEmptyLines(extractTo.getStderr(), stderrList);

					if (maSession.getVideoFilters().isEmpty() == false
						&& lavfiSecondaryFile.exists()) {
						if (lavfiSecondaryFile.length() == 0
							|| extractTo.getVlavfi() == null) {
							FileUtils.deleteQuietly(lavfiSecondaryFile);
						} else {
							FileUtils.moveFile(lavfiSecondaryFile, extractTo.getVlavfi());
						}
					}
				} else {
					final var vLavfiList = new ArrayList<String>();
					final var cVlavfi = makeConsumerToList(vLavfiList, extractTo.getVlavfi());
					final var cStderr = makeConsumerToList(stderrList, extractTo.getStderr());

					maSession.extract(cVlavfi, cStderr);
					writeNonEmptyLines(extractTo.getVlavfi(), vLavfiList);
					writeNonEmptyLines(extractTo.getStderr(), stderrList);
				}
			}

			if (processFile.isContainerAnalysing()) {
				final var caSession = ffmpegService.createContainerAnalyserSession(processFile);
				caSession.setMaxExecutionTime(Duration.ofSeconds(processFile.getMaxSec()), scheduledExecutorService);
				Objects.requireNonNull(extractTo.getContainer(), "You must set a --extract-container FILE");

				try (var pw = new PrintWriter(extractTo.getContainer())) {
					caSession.extract(pw::println);
				}
			}
		} else if (processFile != null && exportTo != null) {
			log.info("Prepare processing session from media file");
			// XXX

			exportTo.getExport();
			exportTo.getFormat();

		} else if (importFrom != null && exportTo != null) {
			log.info("Prepare processing session from offline ffmpeg/ffprobe exports");
			// XXX
		} else {
			throw new IllegalArgumentException("Nothing to do");
		}

		if (processFile != null) {
			// validateInputFile(commandLine, processFile.getInput());
		}
		if (extractTo != null) {
			/*Optional.ofNullable(extractTo.getVlavfi())
					.ifPresent(f -> validateOutputFile(commandLine, f));
			Optional.ofNullable(extractTo.getAlavfi())
					.ifPresent(f -> validateOutputFile(commandLine, f));
			Optional.ofNullable(extractTo.getContainer())
					.ifPresent(f -> validateOutputFile(commandLine, f));
			Optional.ofNullable(extractTo.getEbur128())
					.ifPresent(f -> validateOutputFile(commandLine, f));
			Optional.ofNullable(extractTo.getStderr())
					.ifPresent(f -> validateOutputFile(commandLine, f));*/
		}
		if (exportTo != null) {
			/*validateOutputDir(commandLine, exportTo.getExport());
			if (exportTo.getFormat() == null || exportTo.getFormat().isEmpty()) {
				throw new ParameterException(commandLine, "Export format can't be empty");
			}*/
		}
		if (importFrom != null) {
			/*Optional.ofNullable(importFrom.getContainer())
					.ifPresent(f -> validateInputFile(commandLine, f));
			Optional.ofNullable(importFrom.getLavfi())
					.stream()
					.flatMap(Set::stream)
					.forEach(f -> validateInputFile(commandLine, f));
			Optional.ofNullable(importFrom.getStderr())
					.ifPresent(f -> validateInputFile(commandLine, f));*/
		}

	}

	File prepareTempFile(final File tempDir) {
		return null; // TODO
	}

	void writeNonEmptyLines(final File file, final List<String> lines) throws IOException {// TODO expprt
		if (lines.isEmpty()) {
			return;
		}
		FileUtils.writeLines(file, lines, false);
	}

	Consumer<String> makeConsumerToList(final List<String> list, final File reference) {// TODO expprt
		if (reference == null) {
			return line -> {
			};
		}
		return list::add;
	}

	// FIXME missing silence detect

}
