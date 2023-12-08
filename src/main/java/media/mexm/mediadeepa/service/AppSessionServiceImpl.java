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
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FilenameUtils.getBaseName;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.ffmpeg.ffprobe.FormatType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.ImpExArchiveExtractionSession;
import media.mexm.mediadeepa.ImpExArchiveExtractionSession.ExtractedFileEntry;
import media.mexm.mediadeepa.KeyPressToExit;
import media.mexm.mediadeepa.cli.AppCommand;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.Help;
import picocli.CommandLine.ParameterException;
import tv.hd3g.commons.version.EnvironmentVersion;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdEvent;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserSession;
import tv.hd3g.fflauncher.recipes.MediaAnalyserSession;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.RawStdErrFilterEvent;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

@Service
@Slf4j
public class AppSessionServiceImpl implements AppSessionService {

	@Autowired
	private AppConfig appConfig;
	@Autowired
	private CommandLine commandLine;
	@Autowired
	private AppCommand appCommand;
	@Autowired
	private FFmpegService ffmpegService;
	@Autowired
	private EnvironmentVersion environmentVersion;
	@Autowired
	private ScheduledExecutorService scheduledExecutorService;
	@Autowired
	private String ffmpegExecName;
	@Autowired
	private String ffprobeExecName;
	@Autowired
	private ExecutableFinder executableFinder;
	@Autowired
	private KeyPressToExit keyPressToExit;
	@Autowired
	private MediaAnalyticsTransformerService mediaAnalyticsTransformerService;

	@Value("${mediadeepa.disableKeyPressExit:false}")
	private boolean disableKeyPressExit;

	@Override
	public int runCli() throws IOException {
		final var out = commandLine.getOut();

		if (appCommand.isVersion()) {
			commandLine.printVersionHelp(
					out,
					Help.Ansi.AUTO,
					environmentVersion.appVersion(),
					LocalDate.now().getYear());
			return 0;
		}

		if (appCommand.isOptions()) {
			final var versions = ffmpegService.getVersions();
			out.println("Use:");
			out.format("%-15s%-15s\n", "ffmpeg", executableFinder.get(ffmpegExecName)); // NOSONAR S3457
			out.format("%-15s%-15s\n", "", versions.get("ffmpeg"));// NOSONAR S3457
			out.println("");
			out.format("%-15s%-15s\n", "ffprobe", executableFinder.get(ffprobeExecName)); // NOSONAR S3457
			out.format("%-15s%-15s\n", "", versions.get("ffprobe"));// NOSONAR S3457
			out.println("");
			out.println("Detected (and usable) filters:");
			ffmpegService.getMtdFiltersAvaliable()
					.forEach((k, v) -> out.format("%-15s%-15s\n", k, v)); // NOSONAR S3457
			out.println("");
			out.println("Export formats available:");
			mediaAnalyticsTransformerService.getExportFormatInformation()
					.forEach((k, v) -> out.format("%-15s%-15s\n", k, v)); // NOSONAR S3457
			return 0;
		}

		if (appCommand.isAutocomplete()) {
			out.println(AutoComplete.bash(NAME, commandLine).replace(
					"# " + NAME + " Bash Completion",
					"# " + NAME + " " + environmentVersion.appVersion() + " Bash Completion"));
			return 0;
		}

		if (appCommand.getTempDir() == null) {
			appCommand.setTempDir(FileUtils.getTempDirectory());
			log.debug("Use {} as temp dir", appCommand.getTempDir());
		} else {
			log.debug("Create {} temp dir", appCommand.getTempDir());
			forceMkdir(appCommand.getTempDir());
		}

		verifyOptions();

		final var processFileCmd = appCommand.getProcessFileCmd();
		final var exportToCmd = appCommand.getExportToCmd();
		final var extractToCmd = appCommand.getExtractToCmd();
		final var importFromCmd = appCommand.getImportFromCmd();

		if (processFileCmd != null && extractToCmd != null) {
			log.info("Prepare extraction session from media file: {}", processFileCmd.getInput());
			startKeyPressExit();
			createExtractionSession();
		} else if (processFileCmd != null && exportToCmd != null) {
			log.info("Prepare processing session from media file: {} to {}",
					processFileCmd.getInput(), exportToCmd.getExport());
			startKeyPressExit();
			createProcessingSession();
		} else if (importFromCmd != null && exportToCmd != null) {
			log.info("Prepare processing session from offline ffmpeg/ffprobe exports");
			startKeyPressExit();
			createOfflineProcessingSession();
		} else {
			cleanTempDir(appCommand.getTempDir());
			throw new ParameterException(commandLine, "Nothing to do!");
		}

		cleanTempDir(appCommand.getTempDir());
		return 0;
	}

	private void startKeyPressExit() {
		if (disableKeyPressExit == false) {
			keyPressToExit.start();
		}
	}

	private void cleanTempDir(final File tempDir) throws IOException {
		if (tempDir.equals(FileUtils.getTempDirectory()) == false
			&& tempDir.listFiles().length == 0) {
			log.debug("Delete empty created temp dir {}", tempDir);
			FileUtils.forceDelete(tempDir);
		}
	}

	private void verifyOptions() throws ParameterException {
		final var exportToCmd = appCommand.getExportToCmd();
		final var extractToCmd = appCommand.getExtractToCmd();
		final var importFromCmd = appCommand.getImportFromCmd();
		final var processFileCmd = appCommand.getProcessFileCmd();

		if ((exportToCmd != null ? 1 : 0)
			+ (extractToCmd != null ? 1 : 0)
			+ (importFromCmd != null ? 1 : 0)
			+ (processFileCmd != null ? 1 : 0) > 2) {
			throw new ParameterException(commandLine,
					"You can't cumulate more than two options with --extract/--export/--input/--import");
		}

		if (processFileCmd == null
			&& extractToCmd == null
			&& exportToCmd == null
			&& importFromCmd == null) {
			throw new ParameterException(commandLine,
					"You must setup options like --input --import --extract --export");
		}

		Optional.ofNullable(processFileCmd)
				.ifPresent(p -> validateInputFile(p.getInput()));
		Optional.ofNullable(extractToCmd)
				.ifPresent(et -> Optional.ofNullable(et.getArchiveFile())
						.ifPresent(this::validateOutputFile));

		Optional.ofNullable(exportToCmd).ifPresent(et -> {
			validateOutputDir(et.getExport());
			if (et.getFormat() == null || et.getFormat().isEmpty()) {
				throw new ParameterException(commandLine, "Export format can't be empty");
			} else {
				final var notExists = et.getFormat().stream()
						.filter(not(mediaAnalyticsTransformerService::isExportFormatExists))
						.toList();
				if (notExists.isEmpty() == false) {
					throw new ParameterException(commandLine, "Can't found this export format: "
															  + notExists.stream().collect(joining(", ")));
				}
			}
		});

		Optional.ofNullable(importFromCmd)
				.ifPresent(i -> Optional.ofNullable(i.getArchiveFile())
						.ifPresent(this::validateInputFile));
	}

	@Override
	public void validateInputFile(final File file) throws ParameterException {
		if (file == null) {
			throw new ParameterException(commandLine, "You must set a file");
		} else if (file.exists() == false) {
			throw new ParameterException(commandLine, "Can't found the provided file: " + file.getPath(),
					new FileNotFoundException(file.getPath()));
		} else if (file.isFile() == false) {
			throw new ParameterException(commandLine, "The provided file is not a regular file: " + file.getPath(),
					new FileNotFoundException(file.getPath()));
		}
	}

	@Override
	public void validateOutputFile(final File file) throws ParameterException {
		if (file == null) {
			throw new ParameterException(commandLine, "You must set a file");
		} else if (file.exists() && file.isFile() == false) {
			throw new ParameterException(commandLine, "Can't overwrite the provided file: it's a directory: "
													  + file.getPath());
		} else if (file.exists() && file.canWrite() == false) {
			throw new ParameterException(commandLine, "Can't overwrite the provided file: "
													  + file.getPath());
		}
	}

	@Override
	public void validateOutputDir(final File dir) throws ParameterException {
		if (dir == null) {
			throw new ParameterException(commandLine, "You must set a directory");
		} else if (dir.exists() == false) {
			try {
				FileUtils.forceMkdir(dir);
			} catch (final IOException e) {
				throw new UncheckedIOException("Can't create the provided directory", e);
			}
		} else if (dir.isDirectory() == false) {
			throw new ParameterException(commandLine, "The provided directory is not a regular directory: "
													  + dir.getPath());
		}
	}

	public void createExtractionSession() throws IOException {
		final var processFileCmd = appCommand.getProcessFileCmd();
		final var extractToCmd = appCommand.getExtractToCmd();
		final var tempDir = appCommand.getTempDir();

		final var extractSession = new ImpExArchiveExtractionSession();

		if (processFileCmd.isNoMediaAnalysing() == false) {
			log.debug("Prepare media analysing...");

			final var lavfiSecondaryFile = prepareTempFile(tempDir);
			final var probeResult = ffmpegService.getFFprobeJAXBFromFileToProcess(processFileCmd);
			log.info("Source file: {}", probeResult);

			final var maSession = ffmpegService.createMediaAnalyserSession(
					processFileCmd,
					lavfiSecondaryFile,
					probeResult,
					processFileCmd.getFilterCmd());
			maSession.setFFprobeResult(probeResult);
			maSession.setMaxExecutionTime(Duration.ofSeconds(processFileCmd.getMaxSec()), scheduledExecutorService);

			if (probeResult.getXmlContent().isEmpty() == false) {
				extractSession.add(appConfig.getFfprobeZippedTxtFilename(), probeResult.getXmlContent());
			}
			final var mediaS = probeResult.getMediaSummary();
			extractSession.add(appConfig.getSummaryZippedTxtFilename(), concat(Stream.of(mediaS.format()), mediaS
					.streams().stream()).toList());

			final var stderrList = new ArrayList<String>();
			final var lavfiList = new ArrayList<String>();
			maSession.extract(lavfiList::add, stderrList::add);
			extractSession.add(appConfig.getLavfiZippedTxtBaseFilename() + "0.txt", lavfiList);
			extractSession.add(appConfig.getStdErrZippedTxtFilename(), stderrList);

			if (lavfiSecondaryFile.exists()) {
				extractSession.add(appConfig.getLavfiZippedTxtBaseFilename() + "1.txt",
						FileUtils.readLines(lavfiSecondaryFile, UTF_8));
				FileUtils.deleteQuietly(lavfiSecondaryFile);
			}
			extractSession.addFilterContext(appConfig.getFiltersZippedJsonFilename(), maSession.getFilterContextList());
		}

		if (processFileCmd.isContainerAnalysing()) {
			log.info("Start container analysing...");
			final var caSession = ffmpegService.createContainerAnalyserSession(processFileCmd);
			caSession.setMaxExecutionTime(Duration.ofSeconds(processFileCmd.getMaxSec()), scheduledExecutorService);

			final var containerListLines = new ArrayList<String>();
			caSession.extract(containerListLines::add);
			extractSession.add(appConfig.getContainerZippedXmlFilename(), containerListLines);
		}

		extractSession.add(appConfig.getSourceNameZippedTxtFilename(), processFileCmd.getInput().getName());
		extractSession.addVersion(appConfig.getVersionZippedJsonFilename(), getVersion());
		extractSession.saveToZip(extractToCmd.getArchiveFile());
	}

	private Map<String, String> getVersion() {
		final var version = new LinkedHashMap<String, String>();
		version.put(NAME, environmentVersion.appVersion());
		version.putAll(ffmpegService.getVersions());
		version.put(environmentVersion.jvmNameVendor(), environmentVersion.jvmVersion());
		return unmodifiableMap(version);
	}

	public void createProcessingSession() throws IOException {
		final var processFileCmd = appCommand.getProcessFileCmd();
		final var exportToCmd = appCommand.getExportToCmd();
		final var tempDir = appCommand.getTempDir();

		final var dataResult = new DataResult(processFileCmd.getInput().getName(), getVersion());

		final var ffprobeResult = ffmpegService.getFFprobeJAXBFromFileToProcess(processFileCmd);
		log.info("Source file: {}", ffprobeResult);
		dataResult.setFfprobeResult(ffprobeResult);

		if (processFileCmd.isNoMediaAnalysing() == false) {
			log.debug("Prepare media analysing...");

			final var lavfiSecondaryFile = prepareTempFile(tempDir);
			final var maSession = ffmpegService.createMediaAnalyserSession(
					processFileCmd,
					lavfiSecondaryFile,
					ffprobeResult,
					processFileCmd.getFilterCmd());
			maSession.setFFprobeResult(ffprobeResult);
			maSession.setMaxExecutionTime(Duration.ofSeconds(processFileCmd.getMaxSec()), scheduledExecutorService);

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

		if (processFileCmd.isContainerAnalysing()) {
			log.info("Start container analysing...");
			final var caSession = ffmpegService.createContainerAnalyserSession(processFileCmd);
			caSession.setMaxExecutionTime(Duration.ofSeconds(processFileCmd.getMaxSec()), scheduledExecutorService);
			dataResult.setContainerAnalyserResult(caSession.process());
		}

		log.debug("Export analytics");
		mediaAnalyticsTransformerService.exportAnalytics(dataResult, exportToCmd);
	}

	public void createOfflineProcessingSession() throws IOException {
		final var importFromCmd = appCommand.getImportFromCmd();
		final var exportToCmd = appCommand.getExportToCmd();

		final var extractSession = new ImpExArchiveExtractionSession().readFromZip(importFromCmd.getArchiveFile());
		final var extractEntries = extractSession.getEntries()
				.collect(toUnmodifiableMap(ExtractedFileEntry::internalFileName, ExtractedFileEntry::content));

		final var dataResult = new DataResult(
				extractEntries.getOrDefault(appConfig.getSourceNameZippedTxtFilename(),
						getBaseName(importFromCmd.getArchiveFile().getName())),
				extractSession.getVersions(appConfig.getVersionZippedJsonFilename()));

		log.debug("Try to load ffprobe headers");

		dataResult.setFfprobeResult(Optional.ofNullable(extractEntries.get(appConfig.getFfprobeZippedTxtFilename()))
				.map(probeXMLHeaders -> new FFprobeJAXB(probeXMLHeaders, w -> log.warn("XML warning: {}", w)))
				.orElse(null));

		log.debug("Try to load lavfi/stdOutLines sources");
		final var stdOutLines = extractEntries.keySet()
				.stream()
				.filter(f -> f.startsWith(appConfig.getLavfiZippedTxtBaseFilename()))
				.map(extractEntries::get)
				.flatMap(String::lines);

		log.debug("Try to load stdErrLines");
		final var stdErrLines = Optional.ofNullable(extractEntries.get(appConfig.getStdErrZippedTxtFilename()))
				.stream()
				.flatMap(String::lines);

		final var ebur128events = new ArrayList<Ebur128StrErrFilterEvent>();
		final var rawStdErrEvents = new ArrayList<RawStdErrFilterEvent>();

		log.debug("Load MediaAnalyserSession");

		final var filters = extractSession.getFilterContext(appConfig.getFiltersZippedJsonFilename());
		dataResult.setMediaAnalyserResult(MediaAnalyserSession.importFromOffline(
				stdOutLines,
				stdErrLines,
				ebur128events::add,
				rawStdErrEvents::add,
				filters));
		dataResult.setEbur128events(ebur128events);
		dataResult.setRawStdErrEvents(rawStdErrEvents);

		log.debug("Try to load container offline");
		Optional.ofNullable(extractEntries.get(appConfig.getContainerZippedXmlFilename()))
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
		mediaAnalyticsTransformerService.exportAnalytics(dataResult, exportToCmd);
	}

	private static Stream<String> openFileToLineStream(final File file) {
		if (file.exists() == false || file.isFile() == false) {
			return Stream.empty();
		}
		try {
			return Files.lines(file.toPath());
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't open file", e);
		}
	}

	private static File prepareTempFile(final File tempDir) {
		try {
			final var f = File.createTempFile("mediadeepa", "-tmp.txt", tempDir);
			FileUtils.forceDelete(f);
			return f;
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't prepare temp file", e);
		}
	}

}
