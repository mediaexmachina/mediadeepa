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
 * Copyright (C) hdsdi3g for hd3g.tv 2024
 *
 */
package media.mexm.mediadeepa.e2e;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

class E2EDirectoryTest extends E2EUtils {

	@Test
	void testOneDir() {
		final var inputs = ALL_MEDIA_FILE_NAME.stream()
				.filter(File::exists)
				.map(E2EUtils::prepareForSimpleE2ETests)
				.toList();
		if (inputs.isEmpty()) {
			return;
		}

		final var parentDir = inputs.get(0).archive().getParentFile().getAbsolutePath();

		runApp(
				"--temp", "target/e2e-temp",
				"-i", parentDir,
				"-f", "ffprobexml",
				"-e", "target/e2e-export",
				"--export-base-filename", "directory");

		// TODO check result...
	}

	@Test
	void testNoFiles_emptyDir() throws IOException {// NOSONAR S2699
		final var empty = new File("target/e2e-temp/always-empty");
		FileUtils.forceMkdir(empty);
		FileUtils.cleanDirectory(empty);

		runApp(
				"-i", empty.getPath(),
				"-f", "ffprobexml",
				"-e", "target/e2e-export",
				"--export-base-filename", "directory");
	}

	// TODO test 2 files
	// TODO test 2 dir (one empty and one full)

}
