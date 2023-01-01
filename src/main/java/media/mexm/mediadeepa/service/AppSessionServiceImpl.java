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
import static java.util.function.Predicate.not;
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
import org.ffmpeg.ffprobe.FormatType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ExportTo;
import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ExtractTo;
import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ImportFrom;
import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ProcessFile;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormatManager;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdEvent;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserSession;
import tv.hd3g.fflauncher.recipes.MediaAnalyserSession;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.RawStdErrFilterEvent;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;

@Service
@Slf4j
public class AppSessionServiceImpl implements AppSessionService {

	@Autowired
	private FFmpegService ffmpegService;
	@Autowired
	private ScheduledExecutorService scheduledExecutorService;
	@Autowired
	private MediaAnalyticsTransformerService mediaAnalyticsTransformerService;
	@Autowired
	private ExportFormatManager exportFormatManager;

	static void checkNotNullParams(final CommandLine commandLine,
								   final ExportTo exportTo,
								   final ExtractTo extractTo,
								   final ImportFrom importFrom,
								   final ProcessFile processFile) {
		if (processFile == null
			&& extractTo == null
			&& exportTo == null
			&& importFrom == null) {
			throw new ParameterException(commandLine,
					"You must setup options like --input --import --extract --export");
		}
	}

	static void checkNotCumulateParams(final CommandLine commandLine,
									   final boolean opt0,
									   final boolean opt1,
									   final boolean opt2,
									   final boolean opt3) {
		if (opt0 && opt1) {
			throw new ParameterException(commandLine,
					"You can't cumulate options like --extract and --export");
		} else if (opt1 && opt2) {
			throw new ParameterException(commandLine,
					"You can't cumulate options like --input and --import");
		} else if (opt3) {
			throw new ParameterException(commandLine,
					"You can't cumulate options like --import and --extract");
		}
	}

	@Override
	public void verifyOptions(final CommandLine commandLine,
							  final ExportTo exportTo,
							  final ExtractTo extractTo,
							  final ImportFrom importFrom,
							  final ProcessFile processFile,
							  final File tempDir) throws ParameterException {

		final var opt0 = processFile != null && extractTo != null;
		final var opt1 = processFile != null && exportTo != null;
		final var opt2 = importFrom != null && exportTo != null;
		final var opt3 = importFrom != null && extractTo != null;

		checkNotCumulateParams(commandLine, opt0, opt1, opt2, opt3);
		checkNotNullParams(commandLine, exportTo, extractTo, importFrom, processFile);

		Optional.ofNullable(processFile)
				.ifPresent(p -> validateInputFile(commandLine, p.getInput()));

		Optional.ofNullable(extractTo).ifPresent(et -> {
			Optional.ofNullable(et.getVlavfi())
					.ifPresent(f -> validateOutputFile(commandLine, f));
			Optional.ofNullable(et.getAlavfi())
					.ifPresent(f -> validateOutputFile(commandLine, f));
			Optional.ofNullable(et.getContainer())
					.ifPresent(f -> validateOutputFile(commandLine, f));
			Optional.ofNullable(et.getStderr())
					.ifPresent(f -> validateOutputFile(commandLine, f));
			Optional.ofNullable(et.getProbeHeaders())
					.ifPresent(f -> validateOutputFile(commandLine, f));
			Optional.ofNullable(et.getProbeSummary())
					.ifPresent(f -> validateOutputFile(commandLine, f));
		});

		Optional.ofNullable(exportTo).ifPresent(et -> {
			validateOutputDir(commandLine, et.getExport());
			if (et.getFormat() == null || et.getFormat().isEmpty()) {
				throw new ParameterException(commandLine, "Export format can't be empty");
			} else {
				final var notExists = et.getFormat().stream()
						.filter(not(exportFormatManager::isFormatExists))
						.toList();
				if (notExists.isEmpty() == false) {
					throw new ParameterException(commandLine, "Can't found this export format: "
															  + notExists.stream().collect(joining(", ")));
				}
			}
		});

		Optional.ofNullable(importFrom).ifPresent(i -> {
			Optional.ofNullable(i.getContainer())
					.ifPresent(f -> validateInputFile(commandLine, f));
			Optional.ofNullable(i.getLavfi())
					.stream()
					.flatMap(Set::stream)
					.forEach(f -> validateInputFile(commandLine, f));
			Optional.ofNullable(i.getStderr())
					.ifPresent(f -> validateInputFile(commandLine, f));
			Optional.ofNullable(i.getProbeHeaders())
					.ifPresent(f -> validateInputFile(commandLine, f));
		});
	}

	@Override
	public void validateInputFile(final CommandLine commandLine, final File file) throws ParameterException {
		if (file == null) {
			throw new ParameterException(commandLine, "You must set an file");
		} else if (file.exists() == false) {
			throw new ParameterException(commandLine, "Can't found the provided file: " + file.getPath(),
					new FileNotFoundException(file.getPath()));
		} else if (file.isFile() == false) {
			throw new ParameterException(commandLine, "The provided file is not a regular file: " + file.getPath(),
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
		if (extractTo.getProbeHeaders() != null
			&& probeResult.getXmlContent().isEmpty() == false) {
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
		if (processFile.isNoMediaAnalysing() == false) {
			log.debug("Prepare media analysing...");

			final var lavfiSecondaryFile = prepareTempFile(tempDir);
			final var probeResult = ffmpegService.getFFprobeJAXBFromFileToProcess(processFile);
			log.info("Source file: {}", probeResult);

			final var maSession = ffmpegService.createMediaAnalyserSession(
					processFile,
					lavfiSecondaryFile,
					probeResult);
			maSession.setFFprobeResult(probeResult);
			maSession.setMaxExecutionTime(Duration.ofSeconds(processFile.getMaxSec()), scheduledExecutorService);
			ffprobeToFiles(extractTo, probeResult);

			final var stderrList = new ArrayList<String>();

			if (maSession.getAudioFilters().isEmpty() == false) {
				final var aLavfiList = new ArrayList<String>();
				final var cAlavfi = makeConsumerToList(aLavfiList, extractTo.getAlavfi());
				final var cStderr = makeConsumerToList(stderrList, extractTo.getStderr());

				log.debug("Start media analysing session...");
				maSession.extract(cAlavfi, cStderr);

				log.debug("Save media analysing to file(s)...");
				writeNonEmptyLines(extractTo.getAlavfi(), aLavfiList);
				writeNonEmptyLines(extractTo.getStderr(), stderrList);

				if (maSession.getVideoFilters().isEmpty() == false
					&& lavfiSecondaryFile.exists()) {
					if (lavfiSecondaryFile.length() == 0
						|| extractTo.getVlavfi() == null) {
						FileUtils.deleteQuietly(lavfiSecondaryFile);
					} else {
						forceDeleteIfExists(extractTo);
						FileUtils.moveFile(lavfiSecondaryFile, extractTo.getVlavfi());
					}
				}
			} else {
				final var vLavfiList = new ArrayList<String>();
				final var cVlavfi = makeConsumerToList(vLavfiList, extractTo.getVlavfi());
				final var cStderr = makeConsumerToList(stderrList, extractTo.getStderr());

				log.debug("Start container analysing session...");
				maSession.extract(cVlavfi, cStderr);

				log.debug("Save container analysing to file(s)...");
				writeNonEmptyLines(extractTo.getVlavfi(), vLavfiList);
				writeNonEmptyLines(extractTo.getStderr(), stderrList);
			}
		}

		if (processFile.isContainerAnalysing()) {
			log.info("Start container analysing...");

			final var caSession = ffmpegService.createContainerAnalyserSession(processFile);
			caSession.setMaxExecutionTime(Duration.ofSeconds(processFile.getMaxSec()), scheduledExecutorService);
			Objects.requireNonNull(extractTo.getContainer(), "You must set a --extract-container FILE");

			log.debug("Save container analysing to file...");
			try (var pw = new PrintWriter(extractTo.getContainer())) {
				caSession.extract(pw::println);
			}
		}
	}

	private static void forceDeleteIfExists(final ExtractTo extractTo) throws IOException {
		final var vlavfi = extractTo.getVlavfi();
		if (vlavfi.exists()) {
			FileUtils.forceDelete(vlavfi);
		}
	}

	@Override
	public void createProcessingSession(final ProcessFile processFile,
										final ExportTo exportTo,
										final File tempDir) throws IOException {
		final var dataResult = new DataResult(processFile.getInput().getName());

		if (processFile.isNoMediaAnalysing() == false) {
			log.debug("Prepare media analysing...");

			final var lavfiSecondaryFile = prepareTempFile(tempDir);
			final var ffprobeResult = ffmpegService.getFFprobeJAXBFromFileToProcess(processFile);
			log.info("Source file: {}", ffprobeResult);
			dataResult.setFfprobeResult(ffprobeResult);

			final var maSession = ffmpegService.createMediaAnalyserSession(
					processFile,
					lavfiSecondaryFile,
					ffprobeResult);
			maSession.setFFprobeResult(ffprobeResult);
			maSession.setMaxExecutionTime(Duration.ofSeconds(processFile.getMaxSec()), scheduledExecutorService);

			final var ebur128events = new ArrayList<Ebur128StrErrFilterEvent>();
			maSession.setEbur128EventConsumer((s, event) -> ebur128events.add(event));
			dataResult.setEbur128events(ebur128events);

			final var rawStdErrEvents = new ArrayList<RawStdErrFilterEvent>();
			maSession.setRawStdErrEventConsumer((s, event) -> rawStdErrEvents.add(event));
			dataResult.setRawStdErrEvents(rawStdErrEvents);

			log.debug("Start media analysing session...");
			dataResult.setMediaAnalyserResult(maSession.process(
					Optional.ofNullable(() -> openFileToLineStream(lavfiSecondaryFile))));
			FileUtils.deleteQuietly(lavfiSecondaryFile);

			dataResult.setSourceDuration(
					Optional.ofNullable(ffprobeResult)
							.map(FFprobeJAXB::getFormat)
							.map(FormatType::getDuration)
							.map(Optional::ofNullable)
							.stream()
							.flatMap(Optional::stream)
							.map(LavfiMtdEvent::secFloatToDuration)
							.findFirst()
							.orElse(null));
		}

		if (processFile.isContainerAnalysing()) {
			log.info("Start container analysing...");
			final var caSession = ffmpegService.createContainerAnalyserSession(processFile);
			caSession.setMaxExecutionTime(Duration.ofSeconds(processFile.getMaxSec()), scheduledExecutorService);
			dataResult.setContainerAnalyserResult(caSession.process());
		}

		log.debug("Export analytics");
		mediaAnalyticsTransformerService.exportAnalytics(dataResult, exportTo);
	}

	@Override
	public void createOfflineProcessingSession(final ImportFrom importFrom,
											   final ExportTo exportTo) throws IOException {
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

		final var dataResult = new DataResult(sourceName);

		log.debug("Try to load XML ffprobe headers: {}", importFrom.getProbeHeaders());
		dataResult.setFfprobeResult(Optional.ofNullable(importFrom.getProbeHeaders())
				.map(this::openFileToLineStream)
				.map(lines -> lines.collect(joining(System.lineSeparator())))
				.map(probeXMLHeaders -> new FFprobeJAXB(probeXMLHeaders, w -> log.warn("XML warning: {}", w)))
				.orElse(null));

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
		dataResult.setMediaAnalyserResult(MediaAnalyserSession.importFromOffline(
				stdOutLines,
				stdErrLines,
				ebur128events::add,
				rawStdErrEvents::add));
		dataResult.setEbur128events(ebur128events);
		dataResult.setRawStdErrEvents(rawStdErrEvents);

		log.debug("Try to load container: {}", importFrom.getContainer());
		if (importFrom.getContainer() != null) {
			final var c = importFrom.getContainer();
			log.trace("Open container offline file: {}", c);

			try (var ffprobeStdOut = new BufferedInputStream(new FileInputStream(c))) {
				log.trace("Import container offline file...");
				dataResult.setContainerAnalyserResult(ContainerAnalyserSession.importFromOffline(ffprobeStdOut));
			} catch (final IOException e) {
				throw new UncheckedIOException("Can't read file", e);
			}
		}

		dataResult.setSourceDuration(dataResult.getFFprobeResult()
				.map(FFprobeJAXB::getFormat)
				.map(FormatType::getDuration)
				.map(Optional::ofNullable)
				.stream()
				.flatMap(Optional::stream)
				.map(LavfiMtdEvent::secFloatToDuration)
				.findFirst()
				.orElse(null));

		log.debug("Export analytics");
		mediaAnalyticsTransformerService.exportAnalytics(dataResult, exportTo);
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
