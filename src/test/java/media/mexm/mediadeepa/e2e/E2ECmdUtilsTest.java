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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import media.mexm.mediadeepa.App;

@ExtendWith(OutputCaptureExtension.class)
class E2ECmdUtilsTest extends E2EUtils {

	@ParameterizedTest
	@ValueSource(strings = { "-h", "--help" })
	void testShowHelp(final String param, final CapturedOutput output) {
		runApp(param);
		assertThat(output.getOut()).startsWith("Base usage: mediadeepa");
		assertThat(output.getErr()).isEmpty();
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
		assertThat(output.getErr()).isEmpty();
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
		assertThat(output.getErr()).isEmpty();
	}

	@Test
	void testShowAutocomplete(final CapturedOutput output) {
		runApp("--autocomplete");
		assertThat(output.getOut()).startsWith("#!/usr/bin/env bash");
		assertThat(output.getOut()).contains("mediadeepa", "picocli");
		assertThat(output.getErr()).isEmpty();
	}

}
