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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class E2ESnapshotTest extends E2EUtils {
	static final String MEDIADEEPA_SNAPSHOT_STRIP_COUNT = "mediadeepa.snapshotImageConfig.stripExpectedCount";
	static final String MEDIADEEPA_SNAPSHOT_STRIP_NAME = "mediadeepa.snapshotImageConfig.stripJpegFilename";
	static final String MEDIADEEPA_SNAPSHOT_EXPORT_SIGNIFICANT = "mediadeepa.snapshotImageConfig.exportSignificant";
	static final String MEDIADEEPA_SNAPSHOT_SIGNIFICANT_NAME = "mediadeepa.snapshotImageConfig.significantJpegFilename";

	final File baseOutputDir = new File("target/e2e-export");

	private static void doReset() {
		System.clearProperty(MEDIADEEPA_SNAPSHOT_STRIP_COUNT);
		System.clearProperty(MEDIADEEPA_SNAPSHOT_STRIP_NAME);
		System.clearProperty(MEDIADEEPA_SNAPSHOT_EXPORT_SIGNIFICANT);
		System.clearProperty(MEDIADEEPA_SNAPSHOT_SIGNIFICANT_NAME);
	}

	@BeforeEach
	void init() {
		doReset();
	}

	@AfterEach
	void ends() {
		doReset();
	}

	@AfterAll
	static void afterAll() {
		doReset();
	}

	@Test
	void testExtractAllZip() {
		rawData = prepareMovForSimpleE2ETests();
		if (rawData == null) {
			return;
		}
		final var exportBaseFilename = "snap-zip";
		final var expectedFiles = IntStream.range(0, 7)
				.mapToObj(i -> {
					final var suffix = i > 0 ? i : "";
					return new File(baseOutputDir, exportBaseFilename + "_snapshot" + suffix + ".jpg");
				})
				.toList();

		if (expectedFiles.stream().allMatch(File::exists)) {
			return;
		}

		runApp(
				"--temp", "target/e2e-temp",
				"-i", rawData.archive().getPath(),
				"-f", "snapshots",
				"-e", baseOutputDir.getPath(),
				"--export-base-filename", exportBaseFilename);

		checkIsMissing(expectedFiles);
	}

	private static void checkIsMissing(final List<File> expectedFiles) {
		final var missing = expectedFiles.stream()
				.filter(Predicate.not(File::exists))
				.map(File::getName)
				.collect(Collectors.joining(", "));
		assertThat(missing).isEmpty();
	}

	@Test
	void testMakeTooMany() {
		rawData = prepareMovForSimpleE2ETests();
		if (rawData == null) {
			return;
		}
		final var maxImageCount = 55;
		final var exportBaseFilename = "snap-toomany";
		final var expectedFiles = IntStream.range(1, maxImageCount)
				.mapToObj(i -> new File(baseOutputDir, exportBaseFilename + "_strip" + i + ".jpg"))
				.toList();

		if (expectedFiles.stream().allMatch(File::exists)) {
			return;
		}

		System.setProperty(MEDIADEEPA_SNAPSHOT_STRIP_COUNT, "100");
		System.setProperty(MEDIADEEPA_SNAPSHOT_STRIP_NAME, "strip%d.jpg");
		System.setProperty(MEDIADEEPA_SNAPSHOT_EXPORT_SIGNIFICANT, "false");
		System.setProperty(MEDIADEEPA_SNAPSHOT_SIGNIFICANT_NAME, "impossible.jpg");

		runApp(
				"--temp", "target/e2e-temp",
				"-i", rawData.mediaFile().getPath(),
				"-mn",
				"-f", "snapshots",
				"-e", baseOutputDir.getPath(),
				"--export-base-filename", exportBaseFilename);

		checkIsMissing(expectedFiles);

		assertThat(new File(baseOutputDir, exportBaseFilename + "_impossible.jpg"))
				.doesNotExist();
		assertThat(new File(baseOutputDir, exportBaseFilename + "_strip" + (maxImageCount + 1) + ".jpg"))
				.doesNotExist();
	}

	@Test
	void testMakeOnlySignificant() {
		rawData = prepareMovForSimpleE2ETests();
		if (rawData == null) {
			return;
		}
		final var exportBaseFilename = "snap";
		final var significantName = "only-significant.jpg";
		final var expectedOut = new File(baseOutputDir, exportBaseFilename + "_" + significantName);

		if (expectedOut.exists()) {
			return;
		}

		System.setProperty(MEDIADEEPA_SNAPSHOT_STRIP_COUNT, "0");
		System.setProperty(MEDIADEEPA_SNAPSHOT_SIGNIFICANT_NAME, significantName);

		runApp(
				"--temp", "target/e2e-temp",
				"-i", rawData.mediaFile().getPath(),
				"-mn",
				"-f", "snapshots",
				"-e", baseOutputDir.getPath(),
				"--export-base-filename", exportBaseFilename);

		assertThat(expectedOut).exists();
		assertThat(new File(baseOutputDir, exportBaseFilename + "_snapshot.jpg")).doesNotExist();
	}
}
