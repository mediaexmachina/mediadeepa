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

import static java.io.File.pathSeparatorChar;
import static org.apache.commons.io.FileUtils.forceMkdirParent;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

class E2ESingleExportTest extends E2EUtils {

	File outputFile;

	@Test
	void testSimpleExport() throws IOException {
		rawData = prepareMpgForSimpleE2ETests();
		if (rawData == null) {
			return;
		}

		outputFile = new File("target/e2e-single-export", "testSimpleExportLUFS-notjpg.jpg");
		forceMkdirParent(outputFile);
		if (outputFile.exists()) {
			return;
		}

		runApp(
				"--temp", "target/e2e-temp",
				"-i", rawData.archive().getPath(),
				"--single-export", "audio-loudness" + pathSeparatorChar + outputFile.getAbsolutePath());

		assertThat(outputFile).exists().size().isNotZero();
		// TODO check is PNG
	}

	// TODO test jpg "--graphic-jpg"
	// TODO test "container-video-gop.csv" (not fr)
	// TODO test "container-video-gop.txt"
	// TODO test "report.html"
	// TODO test "media-datas.xml";
	// TODO test "ffprobe.xml";

}
