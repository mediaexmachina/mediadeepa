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
import java.util.Scanner;
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
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help;
import picocli.CommandLine.IFactory;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParentCommand;
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
		commandLine.addSubcommand(new ExtractCommand());
		commandLine.addSubcommand(new ImportCommand());
		commandLine.addSubcommand(new ProcessCommand());
	}

	private class BaseCommand {
		@Option(names = { "-h", "--help" }, description = "Show the usage help", usageHelp = true)
		boolean help;
	}

	@Command(name = "mediadeepa",
			 description = "Extract/process technical informations from audio/videos files/streams",
			 version = { "Media Deep Analysis %1$s",
						 "Copyright (C) 2022-%2$s Media ex Machina, under the GNU General Public License" },
			 sortOptions = false)
	public class AppCommand extends BaseCommand implements Callable<Integer> {

		@Option(names = { "-v", "--version" }, description = "Show the application version")
		boolean version;

		@Option(names = { "-o", "--options" }, description = "Show the avaliable options on this system")
		boolean options;

		@Override
		public Integer call() throws Exception {
			if (version) {
				printVersion();
			} else if (options) {
				printOptions();
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
			final var versions = ffmpegService.getVersions();
			out().println("Use:");
			out().format("%-15s%-15s\n", "ffmpeg", executableFinder.get(ffmpegExecName)); // NOSONAR S3457
			out().format("%-15s%-15s\n", "", versions.get("ffmpeg"));// NOSONAR S3457
			out().println("");
			out().format("%-15s%-15s\n", "ffprobe", executableFinder.get(ffprobeExecName)); // NOSONAR S3457
			out().format("%-15s%-15s\n", "", versions.get("ffprobe"));// NOSONAR S3457
			out().println("");
			out().println("Detected (and usable) filters:");
			ffmpegService.getMtdFiltersAvaliable().forEach((k, v) -> {
				out().format("%-15s%-15s\n", k, v); // NOSONAR S3457
			});
		}

	}

	private abstract class BaseCommandExecFF extends BaseCommand {
		// TODO add ffprobe...

		@ParentCommand
		AppCommand parent;

		@Option(names = { "-i", "--input" },
				description = "Input (media) file to process",
				paramLabel = "FILE",
				required = true)
		File input;

		@ArgGroup(exclusive = true)
		TypeExclusive typeExclusive;

		static class TypeExclusive {
			@Option(names = { "-an", "--audio-no" },
					description = "Don't process audio stream metadatas",
					required = false)
			boolean audioNo;

			@Option(names = { "-vn", "--video-no" },
					description = "Don't process video stream metadatas",
					required = false)
			boolean videoNo;
		}

		void validate() throws ParameterException {
			if (input == null) {
				throw new ParameterException(commandLine, "You must set an input file");
			} else if (input.exists() == false) {
				throw new ParameterException(commandLine, "Can't found the provided input file",
						new FileNotFoundException(input.getPath()));
			} else if (input.isFile() == false) {
				throw new ParameterException(commandLine, "The provided input file is not a regular file",
						new FileNotFoundException(input.getPath()));
			}
		}

	}

	@Command(name = "extract",
			 description = "Run ffmpeg/ffprobe and extract raw datas to text files from it",
			 sortOptions = false)
	public class ExtractCommand extends BaseCommandExecFF implements Callable<Integer> {

		@Override
		public Integer call() throws Exception {
			validate();
			final var consoleT = new Thread(() -> {// TODO move it in this class
				out().println("Press [ENTER] to quit...");
				final var keyboard = new Scanner(System.in);
				keyboard.nextLine();
				keyboard.close();
				System.exit(0);
			});
			consoleT.setDaemon(true);
			consoleT.start();

			final var mtd = ffmpegService.doExtractMtd(input, new ProgressCLI(out()),
					typeExclusive.audioNo,
					typeExclusive.videoNo);

			final var lavfi = mtd.lavfiMetadatas();
			/*final var filters = lavfi.stream()
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

			return 0;
		}
	}

	@Command(name = "process",
			 description = "Run ffmpeg/ffprobe and process extracted datas",
			 sortOptions = false)
	public class ProcessCommand extends BaseCommandExecFF implements Callable<Integer> {

		@Override
		public Integer call() throws Exception {
			validate();
			// TODO Auto-generated method stub
			return 0;
		}
	}

	@Command(name = "import",
			 description = "Import extracted raw datas from ffmpeg/ffprobe and process it",
			 sortOptions = false)
	public class ImportCommand extends BaseCommand implements Callable<Integer> {
		@ParentCommand
		private AppCommand parent;

		@Override
		public Integer call() throws Exception {
			// TODO Auto-generated method stub
			return 0;
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
