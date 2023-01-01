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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.springframework.boot.SpringApplication;

import media.mexm.mediadeepa.App;

public interface E2ERunners {

	default void runApp(final String... params) {
		assertEquals(
				0,
				SpringApplication.exit(SpringApplication.run(App.class, params)),
				"App exit code must return 0");
	}

	default void extractRawTXT(final E2ERawOutDataFiles rawData) throws IOException {
		if (rawData.allOutExists()) {
			return;
		}
		runApp("-i", rawData.mediaFile().getPath(), "-c",
				"--temp", "target/e2e-temp",
				"--extract-alavfi", rawData.outAlavfi().getPath(),
				"--extract-vlavfi", rawData.outVlavfi().getPath(),
				"--extract-stderr", rawData.outStderr().getPath(),
				"--extract-probeheaders", rawData.outProbeheaders().getPath(),
				"--extract-probesummary", rawData.outProbesummary().getPath(),
				"--extract-container", rawData.outContainer().getPath());
	}

	default void processTXT(final File mediaFile) throws IOException {
		runApp("-i", mediaFile.getPath(), "-c",
				"--temp", "target/e2e-temp",
				"-f", "txt",
				"-e", "target/e2e-process",
				"--export-base-filename", FilenameUtils.getExtension(mediaFile.getName()));
	}

	default void importRawTXTToProcess(final E2ERawOutDataFiles rawData) throws IOException {
		final var expectedFile = new File(
				"target/e2e-export/" + rawData.getExtension() + "_ffprobe.xml");
		if (expectedFile.exists()) {
			return;
		}
		if (rawData.outVlavfi().exists()) {
			runApp(
					"--temp", "target/e2e-temp",
					"--import-lavfi", rawData.outAlavfi().getPath(),
					"--import-lavfi", rawData.outVlavfi().getPath(),
					"--import-stderr", rawData.outStderr().getPath(),
					"--import-probeheaders", rawData.outProbeheaders().getPath(),
					"--import-container", rawData.outContainer().getPath(),
					"-f", "txt",
					"-e", "target/e2e-export",
					"--export-base-filename", rawData.getExtension());
		} else {
			runApp(
					"--temp", "target/e2e-temp",
					"--import-lavfi", rawData.outAlavfi().getPath(),
					"--import-stderr", rawData.outStderr().getPath(),
					"--import-probeheaders", rawData.outProbeheaders().getPath(),
					"--import-container", rawData.outContainer().getPath(),
					"-f", "txt",
					"-e", "target/e2e-export",
					"--export-base-filename", rawData.getExtension());
		}
	}

}
