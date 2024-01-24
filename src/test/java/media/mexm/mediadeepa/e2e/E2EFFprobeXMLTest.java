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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import media.mexm.mediadeepa.ConstStrings;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;

class E2EFFprobeXMLTest extends E2EUtils implements ConstStrings {

	E2ERawOutDataFiles rawData;

	@Mock
	Consumer<String> onWarnLog;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
	}

	@AfterEach
	void end() {
		verifyNoMoreInteractions(onWarnLog);
	}

	@Test
	void testHTML() throws IOException {
		rawData = prepareTsForSimpleE2ETests();
		if (rawData == null) {
			return;
		}
		final var outputFile = makeOutputFile("mpg_ffprobe.xml");
		assertThat(outputFile).exists().size().isGreaterThan(1);

		final var xmlContent = FileUtils.readFileToString(outputFile, UTF_8);

		final var ffprobeJAXB = FFprobeJAXB.load(xmlContent);
		assertEquals("mpegts", ffprobeJAXB.getFormat().orElseThrow().formatName());
		assertEquals(1, ffprobeJAXB.getAudioStreams().count());
		assertEquals(1, ffprobeJAXB.getVideoStreams().count());
	}

	File makeOutputFile(final String baseFileName) throws IOException {
		final var outputFile = new File("target/e2e-export", baseFileName);
		runApp(
				"--temp", "target/e2e-temp",
				"-i", rawData.archive().getPath(),
				"-f", "ffprobexml",
				"-e", "target/e2e-export",
				"--export-base-filename", "mpg");
		return outputFile;
	}

}
