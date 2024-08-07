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

import org.junit.jupiter.api.Test;

class E2EDirectoryTest extends E2EUtils {

	@Test
	void test() {
		final var inputs = ALL_MEDIA_FILE_NAME.stream()
				.filter(File::exists)
				.map(E2ERawOutDataFiles::create)
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

	}

}
