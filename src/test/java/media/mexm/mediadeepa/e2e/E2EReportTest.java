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

import static java.util.stream.Collectors.joining;
import static media.mexm.mediadeepa.exportformat.report.HTMLExportFormat.SUFFIX_FILE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class E2EReportTest extends E2EUtils {

	E2ERawOutDataFiles rawData;

	@Test
	void testHTML() throws IOException {
		rawData = prepareTsForSimpleE2ETests();
		if (rawData == null) {
			return;
		}
		final var outputFile = makeOutputFile("mpg_" + SUFFIX_FILE_NAME);
		assertTrue(outputFile.exists());
		assertThat(outputFile.length()).isGreaterThan(30_000);
		final var content = E2EUtils.readLines(outputFile);
		assertThat(content).hasSizeGreaterThan(2);
		assertThat(content.get(0)).isEqualTo("<!DOCTYPE html>");
		assertThat(content.get(1)).startsWithIgnoringCase("<html lang=\"en\">");
		assertThat(content.get(content.size() - 1)).endsWithIgnoringCase("</html>");

		final var strContent = content.stream().collect(joining(" "));
		Stream.of(
				"Loudness EBU-R128",
				"Phase correlation",
				"Signal stats",
				"Image and motion quality",
				"Black frames events",
				"Freeze (static) frames events",
				"Interlacing detection",
				"Black borders / crop detection",
				"Media container",
				"Audio media file information",
				"Video media file information",
				"Video frames",
				"Video compression group-of-pictures",
				"Audio frames",
				"Stream packets",
				"About this document")
				.forEach(f -> assertThat(strContent).contains(f));
	}

	File makeOutputFile(final String baseFileName) throws IOException {
		final var outputFile = new File("target/e2e-export", baseFileName);
		if (outputFile.exists() == false) {
			runApp(
					"--temp", "target/e2e-temp",
					"--import-lavfi", rawData.outAlavfi().getPath(),
					"--import-lavfi", rawData.outVlavfi().getPath(),
					"--import-stderr", rawData.outStderr().getPath(),
					"--import-probeheaders", rawData.outProbeheaders().getPath(),
					"--import-container", rawData.outContainer().getPath(),
					"-f", "html",
					"-e", "target/e2e-export",
					"--export-base-filename", "mpg");
		}
		return outputFile;
	}

}
