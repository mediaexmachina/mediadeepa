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
package media.mexm.mediadeepa.e2e;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static org.slf4j.LoggerFactory.getILoggerFactory;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.App;

@ExtendWith(OutputCaptureExtension.class)
@Slf4j
class E2ECmdUtilsTest extends E2EUtils {

	@ParameterizedTest
	@ValueSource(strings = { "-h", "--help" })
	void testShowHelp(final String param, final CapturedOutput output) {
		runApp(param);
		assertThat(output.getOut()).startsWith("Base usage: mediadeepa");

	}

	@Test
	void testShowDefaultHelp(final CapturedOutput output) {
		assertEquals(
				2,
				SpringApplication.exit(SpringApplication.run(App.class)),
				"App exit code must return 2");
		assertThat(output.getOut()).isEmpty();
		assertThat(output.getErr()).contains("Base usage: mediadeepa");
	}

	@ParameterizedTest
	@ValueSource(strings = { "-v", "--version" })
	void testShowVersion(final String param, final CapturedOutput output) {
		runApp(param);
		assertThat(output.getOut()).startsWith("Mediadeepa");
		assertThat(output.getOut()).contains("Media ex Machina", "Copyright", "GNU");

	}

	@ParameterizedTest
	@ValueSource(strings = { "-o", "--options" })
	void testShowOptions(final String param, final CapturedOutput output) {
		runApp(param);
		assertThat(output.getOut()).contains(
				"ffmpeg",
				"ffprobe",
				"ebur128",
				"cropdetect",
				"metadata");

	}

	@Test
	void testShowAutocomplete(final CapturedOutput output) {
		runApp("--autocomplete");
		assertThat(output.getOut()).startsWith("#!/usr/bin/env bash");
		assertThat(output.getOut()).contains("mediadeepa", "picocli");

	}

	@Nested
	class LogConf {

		@BeforeEach
		@AfterEach
		void resetLogStatus() throws JoranException {
			final var loggerContext = ((ch.qos.logback.classic.Logger) getILoggerFactory().getLogger(ROOT_LOGGER_NAME))
					.getLoggerContext();

			final var ci = new ContextInitializer(loggerContext);
			loggerContext.reset();
			ci.autoConfig();
		}

		@Test
		void testVerbose(final CapturedOutput output) {
			log.debug("NOPE");
			runApp("--verbose", "-v");
			log.debug("YEP");
			assertThat(output.getOut()).contains("[--verbose, -v]", "YEP");
			assertThat(output.getOut()).doesNotContain("NOPE");

		}

		@ParameterizedTest
		@ValueSource(strings = { "-q", "--quiet" })
		void testQuiet(final String param, final CapturedOutput output) {
			runApp(param, "-v");
			assertThat(output.getOut()).isEmpty();

		}

		@Test
		void testQuietError(final CapturedOutput output) {
			SpringApplication.exit(SpringApplication.run(App.class, "-q", "-nope-nope"));
			assertThat(output.getOut()).isEmpty();
			assertThat(output.getErr()).contains("-nope-nope");
		}

		@Test
		void testLogToFile(final CapturedOutput output) throws IOException {
			final var logFile = File.createTempFile("mediadeepa-test", ".log");
			runApp("--log", logFile.getPath(), "-v");
			log.error("YEP");
			assertThat(output.getOut()).isEmpty();

			final var logContent = readFileToString(logFile, UTF_8);
			assertThat(logContent).contains("ERROR YEP");
		}

		@Test
		void testLogToFile_verbose(final CapturedOutput output) throws IOException {
			final var logFile = File.createTempFile("mediadeepa-test", ".log");
			runApp("--log", logFile.getPath(), "--verbose", "-v");
			log.trace("YEP");
			assertThat(output.getOut()).isEmpty();

			final var logContent = readFileToString(logFile, UTF_8);
			assertThat(logContent).contains("TRACE YEP");
		}

		@Test
		void testLogToFile_quiet(final CapturedOutput output) throws IOException {
			final var logFile = File.createTempFile("mediadeepa-test", ".log");
			runApp("--log", logFile.getPath(), "-q", "-v");
			log.info("NOPE");
			log.error("YEP");
			assertThat(output.getOut()).isEmpty();

			final var logContent = readFileToString(logFile, UTF_8);
			assertThat(logContent.trim()).endsWith("ERROR YEP").hasLineCount(1);
		}

	}

}
