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

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help;
import picocli.CommandLine.IFactory;
import picocli.CommandLine.Option;

@Component
public class CLIRunner implements CommandLineRunner, ExitCodeGenerator {
	private static Logger log = LogManager.getLogger();

	@Autowired
	private IFactory factory;
	@Autowired
	private ApplicationContext context;

	private CommandLine commandLine;
	private int exitCode;

	@PostConstruct
	void init() {
		commandLine = new CommandLine(new AppCommand(), factory);
	}

	public void printVersion() {
		final var version = context.getBeansWithAnnotation(SpringBootApplication.class).entrySet().stream()
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
				version,
				LocalDate.now().getYear());
	}

	public PrintWriter out() {
		return commandLine.getOut();
	}

	public PrintWriter err() {
		return commandLine.getErr();
	}

	@Command(name = "mediadeepa",
			 description = "Extract technical informations from audio and videos files and streams",
			 version = { "Media Deep Analysis %1$s",
						 "Copyright (C) 2022-%2$s Media ex Machina, under the GNU General Public License" })
	public class AppCommand implements Callable<Integer> {

		@Option(names = { "-v", "--version" }, description = "Show version") // versionHelp = true,
		private boolean version;

		@Option(names = { "-h", "--help" }, description = "Show usage help")
		private boolean help;

		@Override
		public Integer call() throws Exception {
			if (version) {
				printVersion();
			} else {
				out().println("hello");
			}

			return 0;
		}

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
