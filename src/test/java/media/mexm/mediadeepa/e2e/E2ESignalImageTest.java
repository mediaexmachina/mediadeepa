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
 * Copyright (C) Media ex Machina 2024
 *
 */
package media.mexm.mediadeepa.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.File;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class E2ESignalImageTest extends E2EUtils {

	@TestFactory
	Stream<DynamicTest> testSignalImage() {
		rawData = prepareMpgForSimpleE2ETests();
		if (rawData == null) {
			return Stream.empty();
		}

		return Stream.of(dynamicTest("wavform", this::testWavform));
	}

	void testWavform() {
		final var outputFile = makeOutputFile("mpg_waveform.png", "signalimage");
		assertTrue(outputFile.exists());
		assertTrue(outputFile.length() > 0);
	}

	File makeOutputFile(final String baseFileName, final String format) {
		final var outputFile = new File("target/e2e-export", baseFileName);
		runApp(
				"--temp", "target/e2e-temp",
				"-i", rawData.archive().getPath(),
				"-f", format,
				"-e", "target/e2e-export",
				"--export-base-filename", "mpg");
		return outputFile;
	}

}
