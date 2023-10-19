/*
 * This file is part of mediadeepa.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either command 3 of the License, or
 * any later command.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2023
 *
 */
package media.mexm.mediadeepa.components;

import static media.mexm.mediadeepa.App.NAME;
import static org.apache.commons.io.FileUtils.forceMkdir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.FilterOptions;
import media.mexm.mediadeepa.KeyPressToExit;
import media.mexm.mediadeepa.exportformat.ExportFormatManager;
import media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat;
import media.mexm.mediadeepa.exportformat.report.HTMLExportFormat;
import media.mexm.mediadeepa.exportformat.tables.TableJsonExportFormat;
import media.mexm.mediadeepa.exportformat.tables.TableSQLiteExportFormat;
import media.mexm.mediadeepa.exportformat.tables.TableXLSXExportFormat;
import media.mexm.mediadeepa.exportformat.tables.TableXMLExportFormat;
import media.mexm.mediadeepa.exportformat.tabular.CSVExportFormat;
import media.mexm.mediadeepa.exportformat.tabular.CSVFrExportFormat;
import media.mexm.mediadeepa.exportformat.tabular.TabularTXTExportFormat;
import media.mexm.mediadeepa.service.AppSessionService;
import media.mexm.mediadeepa.service.FFmpegService;
import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help;
import picocli.CommandLine.IFactory;
import picocli.CommandLine.Option;
import picocli.CommandLine.UnmatchedArgumentException;
import tv.hd3g.commons.version.EnvironmentVersion;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

@Component
@Slf4j
public class CLIRunner implements CommandLineRunner, ExitCodeGenerator {

	@Autowired
	private IFactory factory;
	@Autowired
	private EnvironmentVersion environmentVersion;
	@Autowired
	private FFmpegService ffmpegService;
	@Autowired
	private String ffmpegExecName;
	@Autowired
	private String ffprobeExecName;
	@Autowired
	private ExecutableFinder executableFinder;
	@Autowired
	private AppSessionService appSessionService;
	@Autowired
	private KeyPressToExit keyPressToExit;
	@Autowired
	private ExportFormatManager exportFormatManager;

	@Value("${mediadeepa.disableKeyPressExit:false}")
	private boolean disableKeyPressExit;
	@Value("classpath:html-report-style.css")
	private Resource cssHTMLReportResource;

	private CommandLine commandLine;
	private int exitCode;

	@PostConstruct
	void init() {
		exportFormatManager.register("txt", new TabularTXTExportFormat());
		exportFormatManager.register("csv", new CSVExportFormat());
		exportFormatManager.register("csvfr", new CSVFrExportFormat());
		exportFormatManager.register("xlsx", new TableXLSXExportFormat());
		exportFormatManager.register("sqlite", new TableSQLiteExportFormat());
		exportFormatManager.register("xml", new TableXMLExportFormat());
		exportFormatManager.register("json", new TableJsonExportFormat());
		exportFormatManager.register("graphic", new GraphicExportFormat());
		exportFormatManager.register("html", new HTMLExportFormat(cssHTMLReportResource));
		commandLine = new CommandLine(new AppCommand(), factory);

		commandLine.setParameterExceptionHandler((ex, args) -> {
			final var cmd = ex.getCommandLine();
			final var writer = cmd.getErr();

			writer.println(ex.getMessage());
			UnmatchedArgumentException.printSuggestions(ex, writer);
			writer.print(cmd.getHelp().fullSynopsis());

			final var spec = cmd.getCommandSpec();
			writer.printf("Try '%s -h' for more information.%n", spec.qualifiedName());

			return cmd.getExitCodeExceptionMapper() != null
															? cmd.getExitCodeExceptionMapper().getExitCode(ex)
															: spec.exitCodeOnInvalidInput();
		});
	}

	@Command(name = NAME,
			 description = "Extract/process technical informations from audio/videos files/streams",
			 version = { "Media Deep Analysis %1$s",
						 "Copyright (C) 2022-%2$s Media ex Machina, under the GNU General Public License" },
			 sortOptions = false,
			 separator = " ",
			 usageHelpAutoWidth = true,
			 synopsisHeading = "",
			 customSynopsis = {
								"Base usage: mediadeepa [-hov] [--temp DIRECTORY] [-i FILE]",
								"                       [-c] [-mn] [-an | -vn] [-f FORMAT_TYPE] [-e DIRECTORY]",
								"                       [-fo FILTER] [-fn FILTER] [--filter-X VALUE]",
								"                       [--extract-X FILE] [--import-X FILE]"
			 })
	public class AppCommand implements Callable<Integer> {

		@Option(names = { "-h", "--help" }, description = "Show the usage help", usageHelp = true)
		boolean help;

		@Option(names = { "-v", "--version" }, description = "Show the application version")
		boolean version;

		@Option(names = { "-o", "--options" }, description = "Show the avaliable options on this system")
		boolean options;

		@Option(names = { "--autocomplete" }, description = "Show the autocomplete bash script for this application")
		boolean autocomplete;

		@ArgGroup(exclusive = false)
		ProcessFile processFile;

		@Getter
		public static class ProcessFile {
			@Option(names = { "-i", "--input" },
					description = "Input (media) file to process",
					paramLabel = "FILE")
			File input;

			@Option(names = { "-c", "--container" },
					description = "Do a container analysing (ffprobe streams)")
			boolean containerAnalysing;

			@Option(names = { "-t" },
					description = { "Duration of input file to proces it",
									"See https://ffmpeg.org/ffmpeg-utils.html#time-duration-syntax" },
					paramLabel = "DURATION")
			String duration;

			@Option(names = { "-ss" },
					description = { "Seek time in input file before to proces it",
									"See https://ffmpeg.org/ffmpeg-utils.html#time-duration-syntax" },
					paramLabel = "DURATION")
			String startTime;

			@Option(names = { "-max" },
					description = { "Max time let to process a file" },
					paramLabel = "SECONDS")
			int maxSec;

			@Option(names = { "-fo", "--filter-only" },
					description = { "Allow only this filter(s) to process (-o to get list)" },
					paramLabel = "FILTER")
			Set<String> filtersOnly;

			@Option(names = { "-fn", "--filter-no" },
					description = { "Not use this filter(s) to process (-o to get list)" },
					paramLabel = "FILTER")
			Set<String> filtersIgnore;

			@ArgGroup(exclusive = true)
			TypeExclusive typeExclusive;

			@Getter
			public static class TypeExclusive {
				@Option(names = { "-an", "--audio-no" },
						description = "Ignore all video filters",
						required = false)
				boolean audioNo;

				@Option(names = { "-vn", "--video-no" },
						description = "Ignore all audio filters",
						required = false)
				boolean videoNo;
			}

			@Option(names = { "-mn", "--media-no" },
					description = "Disable media analysing (ffmpeg)")
			boolean noMediaAnalysing;

			@ArgGroup(exclusive = false)
			FilterOptions filterOptions;
		}

		@Option(names = { "--temp" },
				description = "Temp dir to use in the case of the needs to export to a temp file",
				paramLabel = "DIRECTORY")
		File tempDir;

		@ArgGroup(exclusive = false)
		ExtractTo extractTo;

		@Getter
		public static class ExtractTo {
			@Option(names = { "--extract-alavfi" },
					description = "Extract raw ffmpeg datas from LAVFI audio metadata filter",
					paramLabel = "TEXT_FILE")
			File alavfi;

			@Option(names = { "--extract-vlavfi" },
					description = "Extract raw ffmpeg datas from LAVFI video metadata filter",
					paramLabel = "TEXT_FILE")
			File vlavfi;

			@Option(names = { "--extract-stderr" },
					description = "Extract raw ffmpeg datas from stderr",
					paramLabel = "TEXT_FILE")
			File stderr;

			@Option(names = { "--extract-probeheaders" },
					description = "Extract XML ffprobe datas from container headers",
					paramLabel = "XML_FILE")
			File probeHeaders;

			@Option(names = { "--extract-probesummary" },
					description = "Extract simple ffprobe data summary from container headers",
					paramLabel = "TEXT_FILE")
			File probeSummary;

			@Option(names = { "--extract-container" },
					description = "Extract XML ffprobe datas from container analyser",
					paramLabel = "XML_FILE")
			File container;
		}

		@ArgGroup(exclusive = false)
		ImportFrom importFrom;

		@Getter
		public static class ImportFrom {
			@Option(names = { "--import-lavfi" },
					description = "Import raw ffmpeg datas from LAVFI metadata filter",
					paramLabel = "TEXT_FILE")
			Set<File> lavfi;

			@Option(names = { "--import-stderr" },
					description = "Import raw ffmpeg datas from stderr filter",
					paramLabel = "TEXT_FILE")
			File stderr;

			@Option(names = { "--import-probeheaders" },
					description = "Import XML ffprobe datas from container headers",
					paramLabel = "XML_FILE")
			File probeHeaders;

			@Option(names = { "--import-container" },
					description = "Import raw ffprobe datas from container analyser",
					paramLabel = "XML_FILE")
			File container;
		}

		@ArgGroup(exclusive = false)
		ExportTo exportTo;

		@Getter
		public static class ExportTo {

			@Option(names = { "-f", "--format" },
					description = "Format to export datas",
					paramLabel = "FORMAT_TYPE")
			Set<String> format;

			@Option(names = { "-e", "--export" },
					description = "Export datas to this directory",
					paramLabel = "DIRECTORY")
			File export;

			@Option(names = { "--export-base-filename" },
					description = "Base file name for exported data file(s)",
					paramLabel = "FILENAME",
					required = false)
			String baseFileName;
		}

		@Override
		public Integer call() throws Exception {
			if (version) {
				printVersion();
				return 0;
			} else if (options) {
				printOptions();
				return 0;
			} else if (autocomplete) {
				printAutoComplete();
				return 0;
			}

			if (tempDir == null) {
				tempDir = FileUtils.getTempDirectory();
				log.debug("Use {} as temp dir", tempDir);
			} else {
				log.debug("Create {} temp dir", tempDir);
				forceMkdir(tempDir);
			}

			appSessionService.verifyOptions(commandLine, exportTo, extractTo, importFrom, processFile, tempDir);

			if (disableKeyPressExit == false) {
				keyPressToExit.start();
			}

			if (processFile != null && extractTo != null) {
				log.info("Prepare extraction session from media file: {}", processFile.getInput());
				appSessionService.createExtractionSession(processFile, extractTo, tempDir);
			} else if (processFile != null && exportTo != null) {
				log.info("Prepare processing session from media file: {} to {}",
						processFile.getInput(), exportTo.getExport());
				appSessionService.createProcessingSession(processFile, exportTo, tempDir);
			} else if (importFrom != null && exportTo != null) {
				log.info("Prepare processing session from offline ffmpeg/ffprobe exports");
				appSessionService.createOfflineProcessingSession(importFrom, exportTo);
			} else {
				cleanTempDir();
				throw new IllegalArgumentException("Nothing to do");
			}

			cleanTempDir();
			return 0;
		}

		private void cleanTempDir() throws IOException {
			if (tempDir.equals(FileUtils.getTempDirectory()) == false
				&& tempDir.listFiles().length == 0) {
				log.debug("Delete empty created temp dir {}", tempDir);
				FileUtils.forceDelete(tempDir);
			}
		}

		private void printVersion() {
			commandLine.printVersionHelp(
					commandLine.getOut(),
					Help.Ansi.AUTO,
					environmentVersion.appVersion(),
					LocalDate.now().getYear());
		}

		private void printOptions() throws FileNotFoundException {
			final var versions = ffmpegService.getVersions();
			out().println("Use:");
			out().format("%-15s%-15s\n", "ffmpeg", executableFinder.get(ffmpegExecName)); // NOSONAR S3457
			out().format("%-15s%-15s\n", "", versions.get("ffmpeg"));// NOSONAR S3457
			out().println("");
			out().format("%-15s%-15s\n", "ffprobe", executableFinder.get(ffprobeExecName)); // NOSONAR S3457
			out().format("%-15s%-15s\n", "", versions.get("ffprobe"));// NOSONAR S3457
			out().println("");
			out().println("Detected (and usable) filters:");
			ffmpegService.getMtdFiltersAvaliable()
					.forEach((k, v) -> out().format("%-15s%-15s\n", k, v)); // NOSONAR S3457
			out().println("");
			out().println("Export formats available:");
			exportFormatManager.getRegisted()
					.forEach((k, v) -> out().format("%-15s%-15s\n", k, v)); // NOSONAR S3457
		}

		private void printAutoComplete() {
			out().println(AutoComplete.bash(NAME, commandLine).replace(
					"# " + NAME + " Bash Completion",
					"# " + NAME + " " + environmentVersion.appVersion() + " Bash Completion"));
		}

	}

	public PrintWriter out() {
		return commandLine.getOut();
	}

	public PrintWriter err() {
		return commandLine.getErr();
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

	@Override
	public void run(final String... args) throws Exception {
		exitCode = commandLine.execute(args);
	}

	public static String makeOutputFileName(final String optionalPrefix, final String baseFileName) {
		if (optionalPrefix != null && optionalPrefix.isEmpty() == false) {
			if (optionalPrefix.endsWith("_")
				|| optionalPrefix.endsWith(" ")
				|| optionalPrefix.endsWith("-")
				|| optionalPrefix.endsWith("|")) {
				return optionalPrefix + baseFileName;
			}
			return optionalPrefix + "_" + baseFileName;
		}
		return baseFileName;
	}

}
