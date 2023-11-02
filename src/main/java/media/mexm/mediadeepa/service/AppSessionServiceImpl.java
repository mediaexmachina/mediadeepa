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
import static java.util.Collections.unmodifiableMap;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static java.util.stream.Stream.concat;
import static media.mexm.mediadeepa.App.NAME;
import static org.apache.commons.io.FilenameUtils.getBaseName;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.ffmpeg.ffprobe.FormatType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.ImpExArchiveExtractionSession;
import media.mexm.mediadeepa.ImpExArchiveExtractionSession.ExtractedFileEntry;
import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ExportTo;
import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ExtractTo;
import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ImportFrom;
import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ProcessFile;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormatManager;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;
import tv.hd3g.commons.version.EnvironmentVersion;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdEvent;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserSession;
import tv.hd3g.fflauncher.recipes.MediaAnalyserSession;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.RawStdErrFilterEvent;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;

@Service
@Slf4j
public class AppSessionServiceImpl implements AppSessionService {

	public static final String SUMMARY_TXT = "summary.txt";
	public static final String SOURCENAME_TXT = "sourcename.txt";
	public static final String VERSION_XML = "version.xml";
	public static final String CONTAINER_XML = "container.xml";
	public static final String STDERR_TXT = "stderr.txt";
	public static final String LAVFI_BASE_FILENAME = "lavfi";
	public static final String FFPROBE_XML = "ffprobe.xml";

	@Autowired
	private FFmpegService ffmpegService;
	@Autowired
	private EnvironmentVersion environmentVersion;
	@Autowired
	private ScheduledExecutorService scheduledExecutorService;
	@Autowired
	private MediaAnalyticsTransformerService mediaAnalyticsTransformerService;
	@Autowired
	private ExportFormatManager exportFormatManager;
	@Autowired
	private ImpExArchiveService impExArchiveService;

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
		Optional.ofNullable(extractTo)
				.ifPresent(et -> Optional.ofNullable(et.getArchiveFile())
						.ifPresent(f -> validateOutputFile(commandLine, f)));

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

		Optional.ofNullable(importFrom)
				.ifPresent(i -> Optional.ofNullable(i.getArchiveFile())
						.ifPresent(f -> validateInputFile(commandLine, f)));
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

	@Override
	public void createExtractionSession(final ProcessFile processFile,
										final ExtractTo extractTo,
										final File tempDir) throws IOException {
		final var extractSession = new ImpExArchiveExtractionSession();

		if (processFile.isNoMediaAnalysing() == false) {
			log.debug("Prepare media analysing...");

			final var lavfiSecondaryFile = prepareTempFile(tempDir);
			final var probeResult = ffmpegService.getFFprobeJAXBFromFileToProcess(processFile);
			log.info("Source file: {}", probeResult);

			final var maSession = ffmpegService.createMediaAnalyserSession(
					processFile,
					lavfiSecondaryFile,
					probeResult,
					processFile.getFilterOptions());
			maSession.setFFprobeResult(probeResult);
			maSession.setMaxExecutionTime(Duration.ofSeconds(processFile.getMaxSec()), scheduledExecutorService);

			if (probeResult.getXmlContent().isEmpty() == false) {
				extractSession.add(FFPROBE_XML, probeResult.getXmlContent());
			}
			final var mediaS = probeResult.getMediaSummary();
			extractSession.add(SUMMARY_TXT, concat(Stream.of(mediaS.format()), mediaS.streams().stream()).toList());

			final var stderrList = new ArrayList<String>();
			final var lavfiList = new ArrayList<String>();
			maSession.extract(lavfiList::add, stderrList::add);
			extractSession.add(LAVFI_BASE_FILENAME + "0.txt", lavfiList);
			extractSession.add(STDERR_TXT, stderrList);

			if (lavfiSecondaryFile.exists()) {
				extractSession.add(LAVFI_BASE_FILENAME + "1.txt", FileUtils.readLines(lavfiSecondaryFile, UTF_8));
				FileUtils.deleteQuietly(lavfiSecondaryFile);
			}
		}

		if (processFile.isContainerAnalysing()) {
			log.info("Start container analysing...");
			final var caSession = ffmpegService.createContainerAnalyserSession(processFile);
			caSession.setMaxExecutionTime(Duration.ofSeconds(processFile.getMaxSec()), scheduledExecutorService);

			final var containerListLines = new ArrayList<String>();
			caSession.extract(containerListLines::add);
			extractSession.add(CONTAINER_XML, containerListLines);
		}

		extractSession.add(SOURCENAME_TXT, processFile.getInput().getName());
		extractSession.addVersion(VERSION_XML, getVersion());

		impExArchiveService.saveTo(extractTo, extractSession);
	}

	private Map<String, String> getVersion() {
		final var version = new LinkedHashMap<String, String>();
		version.put(NAME, environmentVersion.appVersion());
		version.putAll(ffmpegService.getVersions());
		version.put(environmentVersion.jvmNameVendor(), environmentVersion.jvmVersion());
		return unmodifiableMap(version);
	}

	@Override
	public void createProcessingSession(final ProcessFile processFile,
										final ExportTo exportTo,
										final File tempDir) throws IOException {
		final var dataResult = new DataResult(processFile.getInput().getName(), getVersion());

		final var ffprobeResult = ffmpegService.getFFprobeJAXBFromFileToProcess(processFile);
		log.info("Source file: {}", ffprobeResult);
		dataResult.setFfprobeResult(ffprobeResult);

		if (processFile.isNoMediaAnalysing() == false) {
			log.debug("Prepare media analysing...");

			final var lavfiSecondaryFile = prepareTempFile(tempDir);
			final var maSession = ffmpegService.createMediaAnalyserSession(
					processFile,
					lavfiSecondaryFile,
					ffprobeResult,
					processFile.getFilterOptions());
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
		final var extractSession = impExArchiveService.loadFrom(importFrom);
		final var extractEntries = extractSession.getEntries()
				.collect(toUnmodifiableMap(ExtractedFileEntry::internalFileName, ExtractedFileEntry::content));

		final var dataResult = new DataResult(
				extractEntries.getOrDefault(SOURCENAME_TXT,
						getBaseName(importFrom.getArchiveFile().getName())),
				extractSession.getVersions(VERSION_XML));

		log.debug("Try to load ffprobe headers");

		dataResult.setFfprobeResult(Optional.ofNullable(extractEntries.get(FFPROBE_XML))
				.map(probeXMLHeaders -> new FFprobeJAXB(probeXMLHeaders, w -> log.warn("XML warning: {}", w)))
				.orElse(null));

		log.debug("Try to load lavfi/stdOutLines sources");
		final var stdOutLines = extractEntries.keySet()
				.stream()
				.filter(f -> f.startsWith(LAVFI_BASE_FILENAME))
				.map(extractEntries::get)
				.flatMap(String::lines);

		log.debug("Try to load stdErrLines");
		final var stdErrLines = Optional.ofNullable(extractEntries.get(STDERR_TXT))
				.stream()
				.flatMap(String::lines);

		final var ebur128events = new ArrayList<Ebur128StrErrFilterEvent>();
		final var rawStdErrEvents = new ArrayList<RawStdErrFilterEvent>();

		log.debug("Load MediaAnalyserSession");

		dataResult.setMediaAnalyserResult(MediaAnalyserSession.importFromOffline(
				stdOutLines,
				stdErrLines,
				ebur128events::add,
				rawStdErrEvents::add));
		dataResult.setEbur128events(ebur128events);
		dataResult.setRawStdErrEvents(rawStdErrEvents);

		log.debug("Try to load container offline");
		Optional.ofNullable(extractEntries.get(CONTAINER_XML))
				.ifPresent(f -> dataResult.setContainerAnalyserResult(ContainerAnalyserSession
						.importFromOffline(new ByteArrayInputStream(f.getBytes(UTF_8)))));

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

}
