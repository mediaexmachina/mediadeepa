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
import media.mexm.mediadeepa.cli.OutputCmd;
import media.mexm.mediadeepa.components.ExportFormatComparator;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormat;
import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.Help;
import picocli.CommandLine.ParameterException;
import tv.hd3g.commons.version.EnvironmentVersion;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserProcessResult;
import tv.hd3g.fflauncher.recipes.MediaAnalyserProcessResult;
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

			return 0;
		}

		if (appCommand.isAutocomplete()) {
			out.println(AutoComplete.bash(NAME, commandLine).replace(
					"# " + NAME + " Bash Completion",
					"# " + NAME + " " + environmentVersion.appVersion() + " Bash Completion"));
			return 0;
		}

		final var outputCmd = Optional.ofNullable(appCommand.getOutputCmd())
				.orElseThrow(() -> new ParameterException(commandLine,
						"Nothing to do, missing an output action!"));
		verifyInputs(outputCmd);
		verifyExtractToCmd(outputCmd);
		verifyExportToCmd(outputCmd);

		final var inputFiles = appCommand.getInput();
		if (inputFiles != null && inputFiles.size() > 1) {
			inputFiles.forEach(f -> log.info("Prepare to work on {}", f.getAbsolutePath()));
		}

		final var inputList = appCommand.getInputList();
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

		Optional.ofNullable(inputFiles)
				.stream()
				.flatMap(List::stream)
				.forEach(this::inputFileWorkChooser);
		inputListFile.forEach(this::inputFileWorkChooser);

		return 0;
	}

	private void inputFileWorkChooser(final File inputFile) {
		setupTempDir();
		final var processFileCmd = appCommand.getProcessFileCmd();
		final var extractToCmd = appCommand.getOutputCmd().getExtractToCmd();
		if (checkIfSourceIsZIP(inputFile)) {
			if (extractToCmd != null) {
				throw new ParameterException(commandLine,
						"You can't import an archive/ZIP and export to an another archive/ZIP: "
														  + inputFile.getAbsolutePath());
			}
			log.info("Prepare processing session from offline ffmpeg/ffprobe exports: {}", inputFile);
			startKeyPressExit();
			createOfflineProcessingSession(inputFile);
		} else if (processFileCmd != null) {
			if (extractToCmd != null) {
				log.info("Prepare extraction session from media file: {}", inputFile);
				startKeyPressExit();
				createExtractionSession(inputFile);
			} else {
				log.info("Prepare processing session from media file: {}", inputFile);
				startKeyPressExit();
				createProcessingSession(inputFile);
			}
		} else {
			cleanTempDir(appCommand.getTempDir());
			throw new ParameterException(commandLine, "Nothing to do with " + inputFile.getAbsolutePath() + "!");
		}
		cleanTempDir(appCommand.getTempDir());
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

	private void verifyInputs(final OutputCmd outputCmd) {
		final var inputFiles = appCommand.getInput();
		if (inputFiles != null && inputFiles.isEmpty() == false) {
			if (inputFiles.size() > 1 && outputCmd.getSingleExportCmd() != null) {
				throw new ParameterException(commandLine,
						"Can't process multiple input sources on single export mode (only one in, one out)!");
			}

			inputFiles.forEach(this::validateInputFile);
		}

		final var inputList = appCommand.getInputList();
		if (inputList != null) {
			inputList.stream()
					.map(File::new)
					.forEach(this::validateInputFile);
		}

		if ((inputFiles == null || inputFiles.isEmpty())
			&& (inputList == null || inputList.isEmpty())) {
			throw new ParameterException(commandLine, "You must set at least an input file");
		}
	}

	private void verifyExtractToCmd(final OutputCmd outputCmd) {
		Optional.ofNullable(outputCmd.getExtractToCmd())
				.ifPresent(et -> Optional.ofNullable(et.getArchiveFile())
						.ifPresent(this::validateOutputFile));
	}

	private void verifyExportToCmd(final OutputCmd outputCmd) {
		Optional.ofNullable(outputCmd.getExportToCmd())
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

	private void createExtractionSession(final File inputFile) {
		final var processFileCmd = appCommand.getProcessFileCmd();
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

		extractSession.saveToZip(extractToCmd.getArchiveFile());
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

	private void createProcessingSession(final File inputFile) {
		final var processFileCmd = appCommand.getProcessFileCmd();
		final var tempDir = appCommand.getTempDir();

		final var dataResult = new DataResult(inputFile.getName(), getVersion());

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

		final var canHandleWaveForm = mediaAnalyticsTransformerService.getSelectedExportFormats(
				appCommand.getOutputCmd().getExportToCmd(),
				fromOutputCmd(appCommand.getOutputCmd(), commandLine)
						.map(ExportOnlyParamConfiguration::internalFileName))
				.anyMatch(ExportFormat::canHandleMeasuredWaveForm);
		if (canHandleWaveForm) {
			ffmpegService.measureWav(inputFile, ffprobeResult, processFileCmd)
					.ifPresent(dataResult::setWavForm);
		}

		exportAnalytics(dataResult);
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

	private void createOfflineProcessingSession(final File archiveFile) {
		final var zippedTxtFileNames = appConfig.getZippedArchive();

		final var extractSession = new ImpExArchiveExtractionSession().readFromZip(archiveFile);
		final var extractEntries = extractSession.getEntries()
				.collect(toUnmodifiableMap(ExtractedFileEntry::internalFileName, ExtractedFileEntry::content));

		final var dataResult = new DataResult(
				extractEntries.getOrDefault(zippedTxtFileNames.getSourceNameTxt(),
						getBaseName(archiveFile.getName())),
				extractSession.getVersions(zippedTxtFileNames.getVersionJson()));

		final var zipAppVersion = dataResult.getVersions().getOrDefault(NAME, "Unknown");
		final var currentAppVersion = environmentVersion.appVersion();
		if (currentAppVersion.equalsIgnoreCase(zipAppVersion) == false) {
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
				zippedTxtFileNames.getFfmpegCommandLineTxt()).orElse("TATA");
		final var filters = extractSession.getFilterContext(zippedTxtFileNames.getFiltersJson());
		dataResult.setMediaAnalyserProcessResult(MediaAnalyserProcessResult.importFromOffline(
				stdOutLines,
				filters,
				ffmpegCommandLine));

		log.debug("Try to load container offline");
		final var ffprobeCommandLine = extractSession.getFFprobeCommandLine(
				zippedTxtFileNames.getFfprobeCommandLineTxt()).orElse("TOTO");
		Optional.ofNullable(extractEntries.get(zippedTxtFileNames.getContainerXml()))
				.ifPresent(f -> dataResult.setContainerAnalyserProcessResult(ContainerAnalyserProcessResult
						.importFromOffline(new ByteArrayInputStream(f.getBytes(UTF_8)), ffprobeCommandLine)));

		extractSession.getMeasuredWav(zippedTxtFileNames.getMeasuredWavJson())
				.ifPresent(dataResult::setWavForm);

		exportAnalytics(dataResult);
	}

	private void exportAnalytics(final DataResult dataResult) {
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
			} else {
				final var outputFile = new File(exportOnlyParams.outputDest());
				log.debug("Single export analytics {} file to {}...",
						exportOnlyParams.internalFileName(), outputFile);
				mediaAnalyticsTransformerService.singleExportAnalytics(
						exportOnlyParams.internalFileName(), dataResult,
						outputFile);
			}
		} else {
			log.debug("Export analytics");
			mediaAnalyticsTransformerService.exportAnalytics(dataResult, appCommand.getOutputCmd().getExportToCmd());
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
