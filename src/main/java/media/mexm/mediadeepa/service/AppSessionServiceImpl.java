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

import static java.nio.charset.Charset.defaultCharset;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ZERO;
import static java.util.Collections.unmodifiableMap;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static java.util.stream.Stream.concat;
import static media.mexm.mediadeepa.App.NAME;
import static media.mexm.mediadeepa.ExportOnlyParamConfiguration.fromOutputCmd;
import static media.mexm.mediadeepa.ImpExArchiveExtractionSession.TEN_MB;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.ExportOnlyParamConfiguration;
import media.mexm.mediadeepa.ImpExArchiveExtractionSession;
import media.mexm.mediadeepa.ImpExArchiveExtractionSession.ExtractedFileEntry;
import media.mexm.mediadeepa.KeyPressToExit;
import media.mexm.mediadeepa.RunnedJavaCmdLine;
import media.mexm.mediadeepa.cli.AppCommand;
import media.mexm.mediadeepa.cli.ProcessFileCmd;
import media.mexm.mediadeepa.cli.ScanDirCmd;
import media.mexm.mediadeepa.components.ExportFormatComparator;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormat;
import media.mexm.mediadeepa.workingsession.WorkingSession;
import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.Help;
import picocli.CommandLine.ParameterException;
import tv.hd3g.commons.version.EnvironmentVersion;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserProcessResult;
import tv.hd3g.fflauncher.recipes.MediaAnalyserProcessResult;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;
import tv.hd3g.jobkit.engine.JobKitEngine;
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
	private ExecutableFinder executableFinder;
	@Autowired
	private KeyPressToExit keyPressToExit;
	@Autowired
	private RunnedJavaCmdLine runnedJavaCmdLine;
	@Autowired
	private MediaAnalyticsTransformerService mediaAnalyticsTransformerService;
	@Autowired
	private List<ExportFormat> exportFormatList;
	@Autowired
	private ExportFormatComparator exportFormatComparator;
	@Autowired
	private JobKitEngine jobKitEngine;
	@Autowired
	private String spoolNameWatchfolder;

	@Value("${mediadeepa.disableKeyPressExit:false}")
	private boolean disableKeyPressExit;

	@Override
	public int runCli() throws IOException {
		if (appCommand.isVersion()) {
			displayVersion();
			return 0;
		}
		if (appCommand.isOptions()) {
			displayOptions();
			return 0;
		}
		if (appCommand.isAutocomplete()) {
			displayAutocomplete();
			return 0;
		}

		if (appCommand.getOutputCmd() == null) {
			throw new ParameterException(commandLine, "Nothing to do, missing an output action!");
		}

		final var inputFiles = appCommand.getInput();
		final var inputList = appCommand.getInputList();
		if ((inputFiles == null || inputFiles.isEmpty())
			&& (inputList == null || inputList.isEmpty())) {
			throw new ParameterException(commandLine, "You must set at least an input file");
		}

		verifyExtractToCmdOutput();
		verifyExportToCmdOutput();

		final var isSingleExportCmd = appCommand.getOutputCmd().getSingleExportCmd() != null;

		new WorkingSession(
				appConfig,
				Optional.ofNullable(appCommand.getScanDirCmd()).orElse(new ScanDirCmd()),
				appCommand.getInput(),
				this,
				isSingleExportCmd,
				() -> {
					throw new ParameterException(commandLine,
							"Can't process multiple input sources on single export mode (only one in, one out)!");
				}).startWork(
						jobKitEngine,
						spoolNameWatchfolder);

		if (inputList != null) {
			inputList.stream()
					.map(File::new)
					.forEach(this::validateInputFile);
		}

		List<File> inputListFile = List.of();
		if (inputList != null && inputList.isEmpty() == false) {
			inputListFile = inputList.stream()
					.map(File::new)
					.flatMap(il -> {
						log.info("Load input file list: {}", il);
						final var listFiles = readInputListFile(il);
						listFiles.forEach(f -> log.info("Prepare to work on {}", f.getAbsolutePath()));
						return listFiles.stream();
					})
					.toList();
		}

		inputListFile.forEach(f -> fileWork(f, true));

		return 0;
	}

	private void displayAutocomplete() {
		commandLine.getOut()
				.println(AutoComplete.bash(NAME, commandLine).replace(
						"# " + NAME + " Bash Completion",
						"# " + NAME + " " + environmentVersion.appVersion() + " Bash Completion"));
	}

	private void displayOptions() throws FileNotFoundException {
		final var out = commandLine.getOut();
		final var versions = ffmpegService.getVersions();
		out.println("Use:");
		out.format("%-15s%-15s\n", "ffmpeg", executableFinder.get(appConfig.getFfmpegExecName())); // NOSONAR S3457
		out.format("%-15s%-15s\n", "", versions.get("ffmpeg"));// NOSONAR S3457
		out.println("");
		out.format("%-15s%-15s\n", "ffprobe", executableFinder.get(appConfig.getFfprobeExecName())); // NOSONAR S3457
		out.format("%-15s%-15s\n", "", versions.get("ffprobe"));// NOSONAR S3457
		out.println("");
		out.println("Detected (and usable) filters:");
		ffmpegService.getMtdFiltersAvaliable()
				.forEach((k, v) -> out.format("%-15s%-15s\n", k, v)); // NOSONAR S3457
		out.println("");
		out.println("Export formats available (and produced files):");
		exportFormatList.stream()
				.sorted(exportFormatComparator)
				.forEach(eF -> {
					final var files = eF.getInternalProducedFileNames();
					final var fileList = files.stream().sorted().collect(joining(", "));
					if (files.size() > 1) {
						out.format("%-15s%-15s\n%-15s%-15s\n", // NOSONAR S3457
								eF.getFormatName(),
								eF.getFormatLongName(),
								"",
								" > " + fileList);
					} else {
						out.format("%-15s%-15s\n", // NOSONAR S3457
								eF.getFormatName(),
								eF.getFormatLongName() + " > " + fileList);
					}
				});
	}

	private void displayVersion() {
		commandLine.printVersionHelp(
				commandLine.getOut(),
				Help.Ansi.AUTO,
				environmentVersion.appVersion(),
				LocalDate.now().getYear());
	}

	@Override
	public Map<String, File> fileWork(final File inputFile, final boolean multipleSources) {
		setupTempDir();
		final var extractToCmd = appCommand.getOutputCmd().getExtractToCmd();
		Map<String, File> result;
		if (checkIfSourceIsZIP(inputFile)) {
			if (extractToCmd != null) {
				throw new ParameterException(commandLine,
						"You can't import an archive/ZIP and export to an another archive/ZIP: "
														  + inputFile.getAbsolutePath());
			}
			log.info("Prepare processing session from offline ffmpeg/ffprobe exports: {}", inputFile);
			startKeyPressExit();
			result = createOfflineProcessingSession(inputFile, multipleSources);
		} else {
			if ((appCommand.getInput() == null || appCommand.getInput().isEmpty())
				&& (appCommand.getInputList() == null || appCommand.getInputList().isEmpty())) {
				cleanTempDir(appCommand.getTempDir());
				throw new ParameterException(commandLine, "No input file/dir!");
			}
			if (extractToCmd != null) {
				log.info("Prepare extraction session from media file: {}", inputFile);
				startKeyPressExit();
				result = createExtractionSession(inputFile, multipleSources);
			} else {
				log.info("Prepare processing session from media file: {}", inputFile);
				startKeyPressExit();
				result = createProcessingSession(inputFile, multipleSources);
			}
		}
		cleanTempDir(appCommand.getTempDir());
		return result;
	}

	private void setupTempDir() {
		if (appCommand.getTempDir() == null) {
			appCommand.setTempDir(FileUtils.getTempDirectory());
			log.debug("Use {} as temp dir", appCommand.getTempDir());
		} else {
			log.debug("Create {} temp dir", appCommand.getTempDir());
			try {
				forceMkdir(appCommand.getTempDir());
			} catch (final IOException e) {
				throw new UncheckedIOException("Can't create temp directory: " + appCommand.getTempDir(), e);
			}
		}
	}

	private void startKeyPressExit() {
		if (disableKeyPressExit == false) {
			keyPressToExit.start();
		}
	}

	private void cleanTempDir(final File tempDir) {
		if (tempDir.equals(FileUtils.getTempDirectory()) == false
			&& Optional.ofNullable(tempDir.listFiles()).map(f -> f.length).orElse(0) == 0) {
			log.debug("Delete empty created temp dir {}", tempDir);
			try {
				FileUtils.forceDelete(tempDir);
			} catch (final IOException e) {
				throw new UncheckedIOException("Can't delete temp directory: " + tempDir.getAbsolutePath(), e);
			}
		}
	}

	private void verifyExtractToCmdOutput() {
		Optional.ofNullable(appCommand.getOutputCmd().getExtractToCmd())
				.ifPresent(et -> Optional.ofNullable(et.getArchiveFile())
						.ifPresent(this::validateOutputFile));
	}

	private void verifyExportToCmdOutput() {
		Optional.ofNullable(appCommand.getOutputCmd().getExportToCmd())
				.ifPresent(et -> {
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
	}

	@Override
	public void validateInputFile(final File file) throws ParameterException {
		if (file == null) {
			throw new ParameterException(commandLine, "You must set at least an input file");
		} else if (file.exists() == false) {
			throw new ParameterException(commandLine, "Can't found the provided input file: "
													  + file.getPath(), new FileNotFoundException(file.getPath()));
		} else if (file.isFile() == false) {
			throw new ParameterException(commandLine, "The provided file is not a regular file: "
													  + file.getPath(), new FileNotFoundException(file.getPath()));
		}
	}

	private List<File> readInputListFile(final File inputList) {
		try {
			final var lines = FileUtils.readLines(inputList, defaultCharset())
					.stream()
					.filter(not(String::isEmpty))
					.map(File::new)
					.toList();
			final var errors = lines.stream()
					.filter(not(File::exists)
							.or(not(File::isFile))
							.or(not(File::canRead))
							.or(f -> f.length() == 0))
					.map(File::getAbsolutePath)
					.toList();
			if (errors.isEmpty()) {
				return lines;
			}
			log.error("Invalid file entries in list file: {}", inputList);
			errors.forEach(e -> log.error("Can't found or invalid file: {}", e));
			throw new ParameterException(commandLine, "Please check input list file: " + inputList.getPath());
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't open/read: " + inputList.getPath(), e);
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

	private Map<String, File> createExtractionSession(final File inputFile, final boolean inMultipleSourcesSet) {
		final var processFileCmd = Optional.ofNullable(appCommand.getProcessFileCmd()).orElse(new ProcessFileCmd());
		final var extractToCmd = appCommand.getOutputCmd().getExtractToCmd();
		final var tempDir = appCommand.getTempDir();
		final var zippedTxtFileNames = appConfig.getZippedArchive();

		final var extractSession = new ImpExArchiveExtractionSession();

		final var probeResult = ffmpegService.getFFprobeJAXBFromFileToProcess(inputFile, processFileCmd);
		log.info("Source file: {}", probeResult);
		if (probeResult.getXmlContent().isEmpty() == false) {
			extractSession.add(zippedTxtFileNames.getFfprobeXml(), probeResult.getXmlContent());
			final var mediaS = probeResult.getMediaSummary();
			extractSession.add(
					zippedTxtFileNames.getSummaryTxt(),
					concat(Stream.of(mediaS.format()), mediaS.streams().stream()).toList());
		}

		if (processFileCmd.isNoMediaAnalysing() == false) {
			log.debug("Prepare media analysing...");

			final var lavfiSecondaryFile = prepareTempFile(tempDir);
			final var maResult = ffmpegService.extractMedia(
					inputFile,
					processFileCmd,
					lavfiSecondaryFile,
					probeResult,
					processFileCmd.getFilterCmd());
			extractSession.add(zippedTxtFileNames.getLavfiTxtBase() + "0.txt", maResult.sysOut());

			if (lavfiSecondaryFile.exists()) {
				extractSession.add(zippedTxtFileNames.getLavfiTxtBase() + "1.txt", readLines(lavfiSecondaryFile));
				FileUtils.deleteQuietly(lavfiSecondaryFile);
			}
			extractSession.addFilterContext(zippedTxtFileNames.getFiltersJson(), maResult.filters());
			extractSession.add(zippedTxtFileNames.getFfmpegCommandLineTxt(), maResult.ffmpegCommandLine());
		}

		if (processFileCmd.isContainerAnalysing()) {
			log.info("Start container analysing...");
			final var caResult = ffmpegService.extractContainer(
					inputFile, processFileCmd, probeResult.getDuration().orElse(ZERO));
			extractSession.add(zippedTxtFileNames.getContainerXml(), caResult.sysOut());
			extractSession.add(zippedTxtFileNames.getFfprobeCommandLineTxt(), caResult.ffprobeCommandLine());
		}

		extractSession.add(zippedTxtFileNames.getSourceNameTxt(), inputFile.getName());
		extractSession.addVersion(zippedTxtFileNames.getVersionJson(), getVersion());
		extractSession.addRunnedJavaCmdLine(zippedTxtFileNames.getCommandLineJson(), runnedJavaCmdLine);

		ffmpegService.measureWav(inputFile, probeResult, processFileCmd)
				.ifPresent(measuredWav -> extractSession.addMeasuredWav(zippedTxtFileNames.getMeasuredWavJson(),
						measuredWav));

		ffmpegService.extractVideoImageSnapshots(inputFile, probeResult, processFileCmd)
				.ifPresent(vis -> extractSession.addVideoImageSnapshots(
						zippedTxtFileNames.getImageSnapshotJson(),
						zippedTxtFileNames.getSignificantImageSnapshotJpg(),
						zippedTxtFileNames.getStripImageSnapshotJpg(),
						vis));

		File outputFile;
		if (inMultipleSourcesSet) {
			final var inputFileName = inputFile.getName();
			final var inputExt = getExtension(inputFileName);
			final var prefix = getBaseName(inputFileName)
							   + (inputExt.isEmpty() ? "" : "-" + inputExt);
			final var archiveFile = extractToCmd.getArchiveFile();
			final var outFileName = prefix + "_" + archiveFile.getName();
			outputFile = new File(archiveFile.getParentFile(), outFileName);
		} else {
			outputFile = extractToCmd.getArchiveFile();
		}

		extractSession.saveToZip(outputFile);
		return Map.of("ziparchive", outputFile);
	}

	private List<String> readLines(final File lavfiSecondaryFile) {
		try {
			return FileUtils.readLines(lavfiSecondaryFile, UTF_8);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't read file", e);
		}
	}

	private Map<String, String> getVersion() {
		final var version = new LinkedHashMap<String, String>();
		version.put(NAME, environmentVersion.appVersion());
		version.putAll(ffmpegService.getVersions());
		version.put(environmentVersion.jvmNameVendor(), environmentVersion.jvmVersion());
		return unmodifiableMap(version);
	}

	private Map<String, File> createProcessingSession(final File inputFile, final boolean inMultipleSourcesSet) {
		final var processFileCmd = Optional.ofNullable(appCommand.getProcessFileCmd()).orElse(new ProcessFileCmd());
		final var tempDir = appCommand.getTempDir();

		final var dataResult = new DataResult(inputFile.getName(), getVersion(), inMultipleSourcesSet);

		final var ffprobeResult = ffmpegService.getFFprobeJAXBFromFileToProcess(inputFile, processFileCmd);
		log.info("Source file: {}", ffprobeResult);
		dataResult.setFfprobeResult(ffprobeResult);

		if (processFileCmd.isNoMediaAnalysing() == false) {
			log.debug("Prepare media analysing...");

			final var lavfiSecondaryFile = prepareTempFile(tempDir);
			log.debug("Start media analysing session...");
			final var maResult = ffmpegService.processMedia(
					inputFile,
					processFileCmd,
					lavfiSecondaryFile,
					ffprobeResult,
					processFileCmd.getFilterCmd());
			dataResult.setMediaAnalyserProcessResult(maResult);
			FileUtils.deleteQuietly(lavfiSecondaryFile);
		}

		if (processFileCmd.isContainerAnalysing()) {
			log.info("Start container analysing...");
			final var caResult = ffmpegService.processContainer(
					inputFile, processFileCmd, ffprobeResult.getDuration().orElse(ZERO));
			dataResult.setContainerAnalyserProcessResult(caResult);
		}

		final var canHandleWaveForm = getSessionUsedExportFormats()
				.anyMatch(ExportFormat::canHandleMeasuredWaveForm);
		if (canHandleWaveForm) {
			ffmpegService.measureWav(inputFile, ffprobeResult, processFileCmd)
					.ifPresent(dataResult::setWavForm);
		}

		final var canHandleSnapshots = getSessionUsedExportFormats()
				.anyMatch(ExportFormat::canHandleSnapshotImage);
		if (canHandleSnapshots) {
			ffmpegService.extractVideoImageSnapshots(inputFile, ffprobeResult, processFileCmd)
					.ifPresent(dataResult::setVideoImageSnapshots);
		}

		return exportAnalytics(dataResult);
	}

	private Stream<ExportFormat> getSessionUsedExportFormats() {
		return mediaAnalyticsTransformerService.getSelectedExportFormats(
				appCommand.getOutputCmd().getExportToCmd(),
				fromOutputCmd(appCommand.getOutputCmd(), commandLine)
						.map(ExportOnlyParamConfiguration::internalFileName));
	}

	private boolean checkIfSourceIsZIP(final File zipFile) {
		log.debug("Try to load source {} as zip zip file", zipFile);
		try (var zipIn = new ZipInputStream(
				new BufferedInputStream(new FileInputStream(zipFile), TEN_MB))) {
			return zipIn.getNextEntry() != null;// NOSONAR S5042
		} catch (final FileNotFoundException e) {
			throw new UncheckedIOException("Can't found input file", e);
		} catch (final IOException e) {
			if (log.isTraceEnabled()) {
				log.trace("Can't open source file {} as ZIP", zipFile, e);
			} else {
				log.debug("Can't open source file {} as ZIP", zipFile);
			}
			return false;
		}
	}

	private Map<String, File> createOfflineProcessingSession(final File archiveFile,
															 final boolean inMultipleSourcesSet) {
		final var zippedTxtFileNames = appConfig.getZippedArchive();

		final var extractSession = new ImpExArchiveExtractionSession().readFromZip(archiveFile);
		final var extractEntries = extractSession.getEntries()
				.collect(toUnmodifiableMap(ExtractedFileEntry::internalFileName, ExtractedFileEntry::content));

		final var dataResult = new DataResult(
				extractEntries.getOrDefault(zippedTxtFileNames.getSourceNameTxt(),
						getBaseName(archiveFile.getName())),
				extractSession.getVersions(zippedTxtFileNames.getVersionJson()),
				inMultipleSourcesSet);

		final var zipAppVersion = dataResult.getVersions().getOrDefault(NAME, "Unknown");
		final var currentAppVersion = environmentVersion.appVersion();
		if (appConfig.isSilentWarnMismatchZipArchiveVersion() == false
			&& currentAppVersion.equalsIgnoreCase(zipAppVersion) == false) {
			log.warn("Mismatch Zip archive version ({}) and current app version ({}).",
					zipAppVersion, currentAppVersion);
		}

		runnedJavaCmdLine.setArchiveJavaCmdLine(
				extractSession.getRunnedJavaCmdLine(zippedTxtFileNames.getCommandLineJson()));

		log.debug("Try to load ffprobe headers");

		dataResult.setFfprobeResult(Optional.ofNullable(extractEntries.get(zippedTxtFileNames.getFfprobeXml()))
				.map(FFprobeJAXB::load)
				.orElse(null));

		log.debug("Try to load lavfi/stdOutLines sources");
		final var stdOutLines = extractEntries.keySet()
				.stream()
				.filter(f -> f.startsWith(zippedTxtFileNames.getLavfiTxtBase()))
				.map(extractEntries::get)
				.flatMap(String::lines);

		log.debug("Load MediaAnalyserSession");
		final var ffmpegCommandLine = extractSession.getFFmpegCommandLine(
				zippedTxtFileNames.getFfmpegCommandLineTxt()).orElse(null);
		final var filters = extractSession.getFilterContext(zippedTxtFileNames.getFiltersJson());
		dataResult.setMediaAnalyserProcessResult(MediaAnalyserProcessResult.importFromOffline(
				stdOutLines,
				filters,
				ffmpegCommandLine));

		log.debug("Try to load container offline");
		final var ffprobeCommandLine = extractSession.getFFprobeCommandLine(
				zippedTxtFileNames.getFfprobeCommandLineTxt()).orElse(null);
		Optional.ofNullable(extractEntries.get(zippedTxtFileNames.getContainerXml()))
				.ifPresent(f -> dataResult.setContainerAnalyserProcessResult(ContainerAnalyserProcessResult
						.importFromOffline(new ByteArrayInputStream(f.getBytes(UTF_8)), ffprobeCommandLine)));

		extractSession.getMeasuredWav(zippedTxtFileNames.getMeasuredWavJson())
				.ifPresent(dataResult::setWavForm);

		extractSession.getVideoImageSnapshots(
				zippedTxtFileNames.getImageSnapshotJson(),
				zippedTxtFileNames.getSignificantImageSnapshotJpg(),
				zippedTxtFileNames.getStripImageSnapshotJpg())
				.ifPresent(dataResult::setVideoImageSnapshots);

		return exportAnalytics(dataResult);
	}

	private Map<String, File> exportAnalytics(final DataResult dataResult) {
		final var oExportOnly = ExportOnlyParamConfiguration.fromOutputCmd(appCommand.getOutputCmd(), commandLine);
		if (oExportOnly.isPresent()) {
			final var exportOnlyParams = oExportOnly.get();
			if (exportOnlyParams.isOutToStdOut()) {
				log.debug("Single export analytics {} file to stdout...",
						exportOnlyParams.internalFileName());
				mediaAnalyticsTransformerService.singleExportAnalyticsToOutputStream(
						exportOnlyParams.internalFileName(),
						dataResult,
						System.out);// NOSONAR S106
				return Map.of();
			} else {
				final var outputFile = new File(exportOnlyParams.outputDest());
				log.debug("Single export analytics {} file to {}...",
						exportOnlyParams.internalFileName(), outputFile);
				mediaAnalyticsTransformerService.singleExportAnalytics(
						exportOnlyParams.internalFileName(), dataResult,
						outputFile);
				return Map.of(exportOnlyParams.internalFileName(), outputFile);
			}
		} else {
			log.debug("Export analytics");
			return mediaAnalyticsTransformerService
					.exportAnalytics(dataResult, appCommand.getOutputCmd().getExportToCmd());
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
