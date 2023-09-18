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

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

class E2EGraphicTest extends E2EUtils {

	@Test
	void testLUFS() throws IOException {
		final var rawData = prepareMovForSimpleE2ETests();
		if (rawData == null) {
			return;
		}

		final var outputFileLUFS = new File("target/e2e-export", "mov_lufs-events.jpg");
		// XXX if (outputFileLUFS.exists() == false) {
		runApp(
				"--temp", "target/e2e-temp",
				"--import-lavfi", rawData.outAlavfi().getPath(),
				"--import-lavfi", rawData.outVlavfi().getPath(),
				"--import-stderr", rawData.outStderr().getPath(),
				"--import-probeheaders", rawData.outProbeheaders().getPath(),
				"--import-container", rawData.outContainer().getPath(),
				"-f", "lufs",
				"-e", "target/e2e-export",
				"--export-base-filename", "mov");
		// }
	}

}
