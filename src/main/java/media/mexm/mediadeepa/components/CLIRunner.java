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

import java.io.File;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.LoggerConfiguration;
import media.mexm.mediadeepa.RunnedJavaCmdLine;
import media.mexm.mediadeepa.cli.AppCommand;
import media.mexm.mediadeepa.service.AppSessionService;
import picocli.CommandLine;
import picocli.CommandLine.UnmatchedArgumentException;

@Component
@Slf4j
public class CLIRunner implements CommandLineRunner, ExitCodeGenerator {

	public static final String PROP_EXPORTDOCUMENTATION_README = "exportdocumentation.readme";
	public static final String PROP_EXPORTDOCUMENTATION_MANPAGE = "exportdocumentation.manpage";
	public static final String PROP_EXPORTDOCUMENTATION_WEBSITE = "exportdocumentation.website";

	public static final int EXIT_CODE_GENERATE_DOC = 42;

	@Autowired
	private CommandLine commandLine;
	@Autowired
	private AppCommand appCommand;
	@Autowired
	private DocumentationExporter documentationExporter;
	@Autowired
	private AppSessionService appSessionService;
	@Autowired
	private LoggerConfiguration loggerConfiguration;
	@Autowired
	private RunnedJavaCmdLine runnedJavaCmdLine;
	private int exitCode;

	@Override
	public void run(final String... args) throws Exception {
		commandLine.setParameterExceptionHandler((ex, params) -> 0);
		loggerConfiguration.apply(args, Set.of("media.mexm"), commandLine);

		commandLine.setParameterExceptionHandler((ex, params) -> {
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

		var hasExportedDoc = false;
		final var manPageFileName = System.getProperty(PROP_EXPORTDOCUMENTATION_MANPAGE);
		if (manPageFileName != null) {
			documentationExporter.exportManPage(new File(manPageFileName));
			hasExportedDoc = true;
		}

		final var readmeFileName = System.getProperty(PROP_EXPORTDOCUMENTATION_README);
		if (readmeFileName != null) {
			documentationExporter.exportReadmeProjectMarkdown(new File(readmeFileName));
			hasExportedDoc = true;
		}

		final var websiteFileName = System.getProperty(PROP_EXPORTDOCUMENTATION_WEBSITE);
		if (websiteFileName != null) {
			documentationExporter.exportProjectPage(new File(websiteFileName));
			hasExportedDoc = true;
		}

		if (hasExportedDoc) {
			exitCode = EXIT_CODE_GENERATE_DOC;
			return;
		}

		log.debug("Start CLI application with param: {}", List.of(args));
		runnedJavaCmdLine.setup(args, NAME);
		appCommand.setDoCall(() -> appSessionService.runCli());
		exitCode = commandLine.execute(args);
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

}
