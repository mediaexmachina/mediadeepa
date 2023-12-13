/*
 * This file is part of mediadeepa.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2023
 *
 */
package media.mexm.mediadeepa;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static org.slf4j.LoggerFactory.getILoggerFactory;
import static picocli.CommandLine.defaultFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Set;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.cli.AppCommand;
import media.mexm.mediadeepa.config.AppConfig;
import picocli.CommandLine;

@Slf4j
public class LoggerConfiguration {

	private final AppConfig appConfig;
	private final CommandLine internalCommandLine;
	private final AppCommand appCommand;

	public LoggerConfiguration(final AppConfig appConfig) {
		this.appConfig = appConfig;
		appCommand = new AppCommand();
		internalCommandLine = new CommandLine(appCommand, defaultFactory());
		internalCommandLine.setStopAtUnmatched(false);
		internalCommandLine.setUnmatchedArgumentsAllowed(true);
		internalCommandLine.setUnmatchedOptionsAllowedAsOptionParameters(true);
		internalCommandLine.setUnmatchedOptionsArePositionalParams(true);
	}

	public void apply(final String[] args,
					  final Set<String> verboseRootLoggersNames,
					  final CommandLine externalCommandLine) {
		appCommand.setDoCall(() -> {
			if (log instanceof ch.qos.logback.classic.Logger == false) {
				log.warn("Non-managed app logger: {}", log.getClass());
				return 0;
			}

			if (appCommand.getLogToFile() != null) {
				final var logFile = appCommand.getLogToFile();
				final var rootLogger = (ch.qos.logback.classic.Logger) getILoggerFactory().getLogger(ROOT_LOGGER_NAME);
				final var context = rootLogger.getLoggerContext();
				rootLogger.detachAndStopAllAppenders();

				final var fos = new FileOutputStream(logFile, true);
				final var pwLogToFile = new PrintWriter(fos);
				externalCommandLine.setErr(pwLogToFile);
				externalCommandLine.setOut(pwLogToFile);

				final var osa = new OutputStreamAppender<ILoggingEvent>();
				osa.setContext(context);
				osa.setName("Log to file " + logFile.getPath());

				final var encoder = new PatternLayoutEncoder();
				encoder.setPattern(appConfig.getLogtofilePattern());
				encoder.setCharset(UTF_8);
				encoder.setContext(context);
				encoder.start();

				osa.setEncoder(encoder);
				osa.setOutputStream(fos);
				osa.setImmediateFlush(true);
				osa.start();

				rootLogger.addAppender(osa);
			}

			if (appCommand.isVerbose()) {
				verboseRootLoggersNames.stream()
						.map(LoggerFactory::getLogger)
						.distinct()
						.map(l -> (ch.qos.logback.classic.Logger) l)
						.forEach(l -> l.setLevel(ch.qos.logback.classic.Level.TRACE));
				log.trace("Switch to verbose logger CLI mode");

			} else if (appCommand.isQuiet()) {
				final var rootLogger = (ch.qos.logback.classic.Logger) getILoggerFactory().getLogger(ROOT_LOGGER_NAME);
				rootLogger.getLoggerContext()
						.getLoggerList()
						.forEach(l -> l.setLevel(ch.qos.logback.classic.Level.ERROR));

				externalCommandLine.setOut(new PrintWriter(new OutputStream() {
					@Override
					public void write(final int b) throws IOException {
						/**
						 * Do nothing
						 */
					}
				}));
			}

			return 0;
		});
		internalCommandLine.execute(args);
	}

}
