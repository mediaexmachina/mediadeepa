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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.stream.Stream;

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
import tv.hd3g.fflauncher.recipes.ContainerAnalyserSession;
import tv.hd3g.fflauncher.recipes.MediaAnalyserSession;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.RawStdErrFilterEvent;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;

@Service
public class AppSessionServiceImpl implements AppSessionService {
	private static Logger log = LogManager.getLogger();

	@Autowired
	private FFmpegService ffmpegService;
	@Autowired
	private ScheduledExecutorService scheduledExecutorService;
	@Autowired
	private MediaAnalyticsTransformerService mediaAnalyticsTransformerService;

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
			Optional.ofNullable(extractTo.getProbeHeaders())
					.ifPresent(f -> validateOutputFile(commandLine, f));
			Optional.ofNullable(extractTo.getProbeSummary())
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
			Optional.ofNullable(importFrom.getProbeHeaders())
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

	private void ffprobeToFiles(final ExtractTo extractTo, final FFprobeJAXB probeResult) throws IOException {
		if (extractTo.getProbeHeaders() != null && probeResult.getXmlContent().isEmpty()) {
			try {
				FileUtils.write(extractTo.getProbeHeaders(), probeResult.getXmlContent(), UTF_8);
			} catch (final IOException e) {
				throw new UncheckedIOException("Can't write to ffprobe file", e);
			}
		}

		final var mediaS = probeResult.getMediaSummary();
		writeNonEmptyLines(extractTo.getProbeSummary(),
				Stream.concat(Stream.of(mediaS.format()), mediaS.streams().stream()).toList());
	}

	@Override
	public void createExtractionSession(final ProcessFile processFile,
										final ExtractTo extractTo,
										final File tempDir) throws IOException {
		// TODO add logs
		if (processFile.isNoMediaAnalysing() == false) {
			log.debug("Prepare media analysing...");

			final var lavfiSecondaryFile = prepareTempFile(tempDir);
			final var maSession = ffmpegService.createMediaAnalyserSession(processFile, lavfiSecondaryFile);
			final var probeResult = ffmpegService.getFFprobeJAXBFromFileToProcess(processFile);
			maSession.setFFprobeResult(probeResult);
			maSession.setMaxExecutionTime(Duration.ofSeconds(processFile.getMaxSec()), scheduledExecutorService);
			ffprobeToFiles(extractTo, probeResult);

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
			log.debug("Prepare continer analysing...");

			final var caSession = ffmpegService.createContainerAnalyserSession(processFile);
			caSession.setMaxExecutionTime(Duration.ofSeconds(processFile.getMaxSec()), scheduledExecutorService);
			Objects.requireNonNull(extractTo.getContainer(), "You must set a --extract-container FILE");

			try (var pw = new PrintWriter(extractTo.getContainer())) {
				caSession.extract(pw::println);
			}
		}
	}

	@Override
	public void createProcessingSession(final ProcessFile processFile,
										final ExportTo exportTo,
										final File tempDir) throws IOException {
		// TODO add logs
		if (processFile.isNoMediaAnalysing() == false) {
			final var lavfiSecondaryFile = prepareTempFile(tempDir);
			final var maSession = ffmpegService.createMediaAnalyserSession(processFile, lavfiSecondaryFile);
			final var ffprobeResult = ffmpegService.getFFprobeJAXBFromFileToProcess(processFile);
			maSession.setFFprobeResult(ffprobeResult);
			maSession.setMaxExecutionTime(Duration.ofSeconds(processFile.getMaxSec()), scheduledExecutorService);

			final var ebur128events = new ArrayList<Ebur128StrErrFilterEvent>();
			maSession.setEbur128EventConsumer((s, event) -> ebur128events.add(event));

			final var rawStdErrEvents = new ArrayList<RawStdErrFilterEvent>();
			maSession.setRawStdErrEventConsumer((s, event) -> rawStdErrEvents.add(event));

			final var maResult = maSession.process(
					Optional.ofNullable(() -> openFileToLineStream(lavfiSecondaryFile)));
			FileUtils.deleteQuietly(lavfiSecondaryFile);

			mediaAnalyticsTransformerService.exportMediaAnalytics(
					processFile.getInput().getName(),
					maResult,
					ebur128events,
					rawStdErrEvents,
					Optional.ofNullable(ffprobeResult),
					exportTo);
		}
		if (processFile.isContainerAnalysing()) {
			final var caSession = ffmpegService.createContainerAnalyserSession(processFile);
			caSession.setMaxExecutionTime(Duration.ofSeconds(processFile.getMaxSec()), scheduledExecutorService);
			final var caResult = caSession.process();

			mediaAnalyticsTransformerService.exportContainerAnalytics(
					processFile.getInput().getName(),
					caResult,
					exportTo);
		}
	}

	@Override
	public void createOfflineProcessingSession(final ImportFrom importFrom,
											   final ExportTo exportTo) throws IOException {
		log.debug("Try to load XML ffprobe headers: {}", importFrom.getProbeHeaders());
		final var ffprobeJAXB = Optional.ofNullable(importFrom.getProbeHeaders())
				.map(this::openFileToLineStream)
				.map(lines -> lines.collect(joining(System.lineSeparator())))
				.map(probeXMLHeaders -> new FFprobeJAXB(probeXMLHeaders, w -> log.warn("XML warning: {}", w)));

		log.debug("Try to load stdOutLines (lavfi): {}", importFrom.getLavfi());
		final var stdOutLines = Optional.ofNullable(importFrom.getLavfi())
				.stream()
				.flatMap(Set::stream)
				.flatMap(this::openFileToLineStream);

		log.debug("Try to load stdErrLines: {}", importFrom.getStderr());
		final var stdErrLines = Optional.ofNullable(importFrom.getStderr())
				.stream()
				.flatMap(this::openFileToLineStream);

		final var ebur128events = new ArrayList<Ebur128StrErrFilterEvent>();
		final var rawStdErrEvents = new ArrayList<RawStdErrFilterEvent>();

		log.debug("Load MediaAnalyserSession...");
		final var maResult = MediaAnalyserSession.importFromOffline(
				stdOutLines,
				stdErrLines,
				ebur128events::add,
				rawStdErrEvents::add);

		final var sourceName = Stream.concat(
				Optional.ofNullable(importFrom.getLavfi())
						.stream()
						.flatMap(Set::stream),
				Optional.ofNullable(importFrom.getStderr())
						.stream())
				.findFirst()
				.or(() -> Optional.ofNullable(importFrom.getContainer()))
				.or(() -> Optional.ofNullable(importFrom.getProbeHeaders()))
				.map(File::getName)
				.orElse("(no source)");

		log.debug("Export MediaAnalytics...");
		mediaAnalyticsTransformerService.exportMediaAnalytics(
				sourceName,
				maResult,
				ebur128events,
				rawStdErrEvents,
				ffprobeJAXB,
				exportTo);

		log.debug("Try to load container: {}", importFrom.getContainer());
		Optional.ofNullable(importFrom.getContainer())
				.map(c -> {
					log.trace("Open container offline file: {}", c);
					try (var ffprobeStdOut = new BufferedInputStream(new FileInputStream(c))) {
						log.trace("Import container offline file...");
						return ContainerAnalyserSession.importFromOffline(ffprobeStdOut);
					} catch (final IOException e) {
						throw new UncheckedIOException("Can't read file", e);
					}
				})
				.ifPresent(caResult -> mediaAnalyticsTransformerService
						.exportContainerAnalytics(sourceName, caResult, exportTo));
	}

	@Override
	public Stream<String> openFileToLineStream(final File file) {
		if (file.exists() == false || file.isFile() == false) {
			return Stream.empty();
		}
		try {
			return Files.lines(file.toPath());
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't open file", e);
		}
	}

	@Override
	public File prepareTempFile(final File tempDir) {
		try {
			final var f = File.createTempFile("mediadeepa", "-tmp.txt", tempDir);
			FileUtils.forceDelete(f);
			return f;
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't prepare temp file", e);
		}
	}

	@Override
	public void writeNonEmptyLines(final File file, final List<String> lines) throws IOException {
		if (file == null || lines.isEmpty()) {
			return;
		}
		FileUtils.writeLines(file, lines, false);
	}

	@Override
	public Consumer<String> makeConsumerToList(final List<String> list, final File reference) {
		if (reference == null) {
			return line -> {
			};
		}
		return list::add;
	}

}
