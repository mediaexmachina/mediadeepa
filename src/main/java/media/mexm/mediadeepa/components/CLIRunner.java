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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import media.mexm.mediadeepa.ProgressCLI;
import media.mexm.mediadeepa.service.FFmpegService;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help;
import picocli.CommandLine.IFactory;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

@Component
public class CLIRunner implements CommandLineRunner, ExitCodeGenerator {
	private static Logger log = LogManager.getLogger();

	@Autowired
	private IFactory factory;
	@Autowired
	private ApplicationContext context;
	@Autowired
	private FFmpegService ffmpegService;
	@Autowired
	private String ffmpegExecName;
	@Autowired
	private String ffprobeExecName;
	@Autowired
	private ExecutableFinder executableFinder;

	private CommandLine commandLine;
	private int exitCode;

	@PostConstruct
	void init() {
		commandLine = new CommandLine(new AppCommand(), factory);
	}

	@Command(name = "mediadeepa",
			 description = "Extract technical informations from audio and videos files and streams",
			 version = { "Media Deep Analysis %1$s",
						 "Copyright (C) 2022-%2$s Media ex Machina, under the GNU General Public License" })
	public class AppCommand implements Callable<Integer> {

		@Option(names = { "-v", "--version" }, description = "Show the application version")
		private boolean version;

		@Option(names = { "-h", "--help" }, description = "Show the usage help", usageHelp = true)
		private boolean help;

		@Option(names = { "-o", "--options" }, description = "Show the avaliable options on this system")
		private boolean options;

		// XXX https://picocli.info/#_mutually_dependent_options
		@Option(names = { "-i", "--input" }, description = "Input (media) file to process", paramLabel = "FILE")
		private File input;

		@Option(names = { "-an", "--audio-no" }, description = "Don't process audio stream metadatas")
		private boolean audioNo;

		@Option(names = { "-vn", "--video-no" }, description = "Don't process video stream metadatas")
		private boolean videoNo;

		@Override
		public Integer call() throws Exception {
			if (version) {
				printVersion();
			} else if (options) {
				printOptions();
			} else {
				if (input == null) {
					throw new ParameterException(commandLine, "You must set an input file");
				} else if (input.exists() == false) {
					throw new ParameterException(commandLine, "Can't found the provided input file",
							new FileNotFoundException(input.getPath()));
				} else if (input.isFile() == false) {
					throw new ParameterException(commandLine, "The provided input file is not a regular file",
							new FileNotFoundException(input.getPath()));
				}
				final var mtd = ffmpegService.doExtractMtd(input, new ProgressCLI(out()), audioNo, videoNo);

				final var lavfi = mtd.lavfiMetadatas();
				final var filters = lavfi.stream()
						.flatMap(f -> f.getValuesByFilterKeysByFilterName().keySet().stream())
						.distinct()
						.toList();
				log.info("Mtd: {}, {}, {}", lavfi.size(), mtd.ebur128Summary(), filters);

				// TODO test video
				/*
				final var r128s = maResult.ebur128Summary();
				Optional.ofNullable(r128s).ifPresent(r -> log.info("LUFS: {}", r));
				
				final var m = maResult.lavfiMetadatas();
				
				afAPhasemeter.getEvents(m).;
				afAPhasemeter.getMetadatas(m);
				 *
				 *
				final var afAPhasemeter = new AudioFilterAPhasemeter();
				final var afAstats = new AudioFilterAstats();
				final var afSilencedetect = new AudioFilterSilencedetect();
				final var afEbur128 = new AudioFilterEbur128();
				
				final var vfBlackdetect = new VideoFilterBlackdetect();
				final var vfBlockdetect = new VideoFilterBlockdetect();
				final var vfBlurdetect = new VideoFilterBlurdetect();
				final var vfCropdetect = new VideoFilterCropdetect(VideoFilterCropdetect.Mode.BLACK);
				final var vfFreezedetect = new VideoFilterFreezedetect();
				final var vfIdet = new VideoFilterIdet();
				final var vfMEstimate = new VideoFilterMEstimate();
				final var vfSiti = new VideoFilterSiti();
				*/

			}

			return 0;
		}

		private void printVersion() {
			final var strVersion = context.getBeansWithAnnotation(SpringBootApplication.class).entrySet().stream()
					.findFirst()
					.flatMap(es -> {
						final var iv = es.getValue().getClass().getPackage().getImplementationVersion();
						return Optional.ofNullable(iv);
					})
					.or(() -> {
						final var pom = new File("pom.xml");
						if (pom.exists()) {
							try {
								final var fileIS = new FileInputStream(pom);
								final var builderFactory = DocumentBuilderFactory.newInstance();
								final var builder = builderFactory.newDocumentBuilder();
								final var xmlDocument = builder.parse(fileIS);
								final var xPath = XPathFactory.newInstance().newXPath();
								return Optional.ofNullable((String) xPath.compile("/project/version")
										.evaluate(xmlDocument, XPathConstants.STRING));
							} catch (final IOException
										   | ParserConfigurationException
										   | XPathExpressionException
										   | SAXException e) {
								log.warn("Can't load pom.xml file", e);
							}
						}
						return Optional.empty();
					})
					.orElse("SNAPSHOT");

			commandLine.printVersionHelp(
					commandLine.getOut(),
					Help.Ansi.AUTO,
					strVersion,
					LocalDate.now().getYear());
		}

		private void printOptions() throws FileNotFoundException {
			out().println("Use:");
			out().format("%-15s%-15s\n", "ffmpeg", executableFinder.get(ffmpegExecName)); // NOSONAR S3457
			out().format("%-15s%-15s\n", "ffprobe", executableFinder.get(ffprobeExecName)); // NOSONAR S3457

			out().println("");
			out().println("Versions:");
			ffmpegService.getVersions().forEach((k, v) -> {
				out().format("%-15s%-15s\n", k, v); // NOSONAR S3457
			});
			out().println("");
			out().println("Detected (and usable) filters:");
			ffmpegService.getMtdFiltersAvaliable().forEach((k, v) -> {
				out().format("%-15s%-15s\n", k, v); // NOSONAR S3457
			});
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

}
