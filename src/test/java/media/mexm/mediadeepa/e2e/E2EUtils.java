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
import static org.apache.commons.compress.utils.FileNameUtils.getExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.boot.SpringApplication;

import media.mexm.mediadeepa.App;

abstract class E2EUtils {

	static final int V_FRAME_COUNT = 1400;

	static final File TARGET_DIR = new File(".demo-media-files");
	static final File MEDIA_FILE_NAME_AVI = new File(TARGET_DIR, "demo-render.avi");
	static final File MEDIA_FILE_NAME_MKV = new File(TARGET_DIR, "demo-render.mkv");
	static final File MEDIA_FILE_NAME_MOV = new File(TARGET_DIR, "demo-render.mov");
	static final File MEDIA_FILE_NAME_MPG = new File(TARGET_DIR, "demo-render.mpg");
	static final File MEDIA_FILE_NAME_MXF = new File(TARGET_DIR, "demo-render.mxf");
	static final File MEDIA_FILE_NAME_TS = new File(TARGET_DIR, "demo-render.ts");
	static final File MEDIA_FILE_NAME_WAV = new File(TARGET_DIR, "demo-render.wav");
	static final File MEDIA_FILE_NAME_LONG = new File(TARGET_DIR, "long.mkv");

	static final List<File> ALL_MEDIA_FILE_NAME = List.of(
			MEDIA_FILE_NAME_AVI,
			MEDIA_FILE_NAME_MKV,
			MEDIA_FILE_NAME_MOV,
			MEDIA_FILE_NAME_MPG,
			MEDIA_FILE_NAME_MXF,
			MEDIA_FILE_NAME_TS,
			MEDIA_FILE_NAME_WAV);

	static {
		App.setDefaultProps();
		System.setProperty("mediadeepa.disableKeyPressExit", "true");
	}

	static void runApp(final String... params) {
		assertEquals(
				0,
				SpringApplication.exit(SpringApplication.run(App.class, params)),
				"App exit code must return 0");
	}

	static void extractRawTXT(final E2ERawOutDataFiles rawData) throws IOException {
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

	static void processTXT(final File mediaFile) throws IOException {
		runApp("-i", mediaFile.getPath(), "-c",
				"--temp", "target/e2e-temp",
				"-f", "txt",
				"-e", "target/e2e-process",
				"--export-base-filename", FilenameUtils.getExtension(mediaFile.getName()));
	}

	static void importRawTXTToProcess(final E2ERawOutDataFiles rawData) throws IOException {
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

	static void assertEqualsNbLines(final long expected, final List<String> lines, final String what) {
		assertEquals(expected,
				lines.stream().filter(l -> l.startsWith(what)).count(), what);
	}

	static File getProcessedTXTFromRaw(final E2ERawOutDataFiles rawData, final String baseFileName) {
		return new File(
				"target/e2e-export/" + getExtension(rawData.mediaFile().getName()) + "_" + baseFileName);
	}

	static List<String> readLines(final File f) throws IOException {
		assertTrue(f.exists(), "Can't found " + f);
		return FileUtils.readLines(f, UTF_8);
	}

	static void assertTableWith(final int expect, final List<String> file) {
		final var real = file.stream().map(l -> l.split("\t").length).distinct().toList();
		assertEquals(List.of(expect), real, "Expected table width " + expect + ", real is " + real);
	}

	static void assertTableWith(final List<Integer> expect, final List<String> file) {
		final var real = file.stream().map(l -> l.split("\t").length).distinct().toList();
		assertEquals(expect, real, "Expected table width " + expect + ", real is " + real);
	}

	static List<String> getSplitedLine(final List<String> lines, final int linePos) {
		return List.of(lines.get(linePos).split("\t"));
	}

	static int countLinesExportDir(final String fileName) throws IOException {
		try (var br = new BufferedReader(new FileReader(new File("target/e2e-export", fileName)))) {
			var count = 0;
			while (br.readLine() != null) {
				count++;
			}
			return count;
		}
	}

	E2ERawOutDataFiles prepareMovForSimpleE2ETests() throws IOException {
		final var rawData = E2ERawOutDataFiles.create(MEDIA_FILE_NAME_MOV);
		if (MEDIA_FILE_NAME_MOV.exists() == false) {
			return null;
		}
		extractRawTXT(rawData);
		assertTrue(rawData.allOutExists());
		return rawData;
	}

}
