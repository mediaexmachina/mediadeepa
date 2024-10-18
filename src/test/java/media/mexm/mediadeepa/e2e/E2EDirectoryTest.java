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

import static java.util.function.Predicate.not;
import static org.apache.commons.io.FileUtils.cleanDirectory;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.forceMkdirParent;
import static org.apache.commons.io.FileUtils.touch;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class E2EDirectoryTest extends E2EUtils {

	static final File empty = new File("target/e2e-temp/always-empty");
	static final File outDir = new File("target/e2e-export-directory");

	static final File ignoreMe = new File("target/e2e/ignore-me.zip");
	static final File ignoreMe2 = new File("target/e2e/ignore-me2/again-ignore-me.zip");
	static final File ignoreMe3 = new File("target/e2e/ignore-me.nope");
	static final File ignoreMe4 = new File("target/e2e/again-ignore-me.zip");

	@BeforeAll
	static void load() throws IOException {
		forceMkdir(empty);
		cleanDirectory(empty);
		forceMkdir(outDir);
		cleanDirectory(outDir);

		final var ignoreMeList = new File[] { ignoreMe, ignoreMe2, ignoreMe3, ignoreMe4 };
		for (var pos = 0; pos < ignoreMeList.length; pos++) {
			if (ignoreMeList[pos].exists()) {
				forceDelete(ignoreMeList[pos]);
			}
		}
	}

	List<E2ERawOutDataFiles> inputs;

	String directoryTestBaseName;

	@BeforeEach
	void init() {
		inputs = ALL_MEDIA_FILE_NAME.stream()
				.filter(File::exists)
				.map(E2EUtils::prepareForSimpleE2ETests)
				.toList();
	}

	@Test
	void testNoFiles_emptyDir() {// NOSONAR S2699
		directoryTestBaseName = "testNoFiles_emptyDir";
		runApp(
				"-i", empty.getPath(),
				"-f", "ffprobexml",
				"-e", outDir.getPath(),
				"--export-base-filename", directoryTestBaseName);
	}

	@Test
	void testOneDir() throws IOException {
		if (inputs.isEmpty()) {
			return;
		}
		directoryTestBaseName = "testOneDir";

		/**
		 * Test not recursive by default
		 */
		final var ignoreMe5 = new File(outDir, "ignore-me/ignore-me.txt");
		forceMkdirParent(ignoreMe5);
		touch(ignoreMe5);

		runApp(
				"--temp", "target/e2e-temp",
				"-i", getParentDir(),
				"-f", "ffprobexml",
				"-e", outDir.getPath(),
				"--export-base-filename", directoryTestBaseName);

		checkAllOutExists(directoryTestBaseName);
	}

	@Test
	void testTwoDir() {
		if (inputs.isEmpty()) {
			return;
		}
		directoryTestBaseName = "testTwoDir";

		runApp(
				"--temp", "target/e2e-temp",
				"-i", empty.getPath(),
				"-i", getParentDir(),
				"-f", "ffprobexml",
				"-e", outDir.getPath(),
				"--export-base-filename", directoryTestBaseName);

		checkAllOutExists(directoryTestBaseName);
	}

	@Test
	void testTwoZip() {
		if (inputs.size() < 2) {
			return;
		}
		directoryTestBaseName = "testTwoZip";
		final var zip0 = inputs.get(0).archive().getAbsolutePath();
		final var zip1 = inputs.get(1).archive().getAbsolutePath();

		runApp(
				"--temp", "target/e2e-temp",
				"-i", zip0,
				"-i", zip1,
				"-f", "ffprobexml",
				"-e", outDir.getPath(),
				"--export-base-filename", directoryTestBaseName);

		final var results = getTargetedResults(directoryTestBaseName)
				.limit(2)
				.filter(not(File::exists))
				.toList();

		assertThat(results).isEmpty();
	}

	@Test
	void testTwoZipTwoDir() {
		if (inputs.size() < 2) {
			return;
		}
		directoryTestBaseName = "testTwoZipTwoDir";
		final var zip0 = inputs.get(0).archive().getAbsolutePath();
		final var zip1 = inputs.get(1).archive().getAbsolutePath();

		runApp(
				"--temp", "target/e2e-temp",
				"-i", zip0,
				"-i", zip1,
				"-i", empty.getPath(),
				"-i", getParentDir(),
				"-f", "ffprobexml",
				"-e", outDir.getPath(),
				"--export-base-filename", directoryTestBaseName);

		checkAllOutExists(directoryTestBaseName);
	}

	@Test
	void testTwoMedias() {
		if (inputs.size() < 2) {
			return;
		}
		directoryTestBaseName = "testTwoMedias";
		final var media0 = inputs.get(0).mediaFile().getAbsolutePath();
		final var media1 = inputs.get(1).mediaFile().getAbsolutePath();

		runApp(
				"--temp", "target/e2e-temp",
				"-i", media0,
				"-i", media1,
				"-fo", "astats",
				"-vn",
				"-f", "ffprobexml",
				"-e", outDir.getPath(),
				"--export-base-filename", directoryTestBaseName);

		final var results = inputs.stream()
				.limit(2)
				.map(E2ERawOutDataFiles::mediaFile)
				.map(File::getName)
				.map(f -> f.replace(".", "-") + "_" + directoryTestBaseName + "_ffprobe.xml")
				.map(f -> new File(outDir, f))
				.filter(not(File::exists))
				.toList();

		assertThat(results).isEmpty();
	}

	@Test
	void testFilters() throws IOException {
		if (inputs.isEmpty()) {
			return;
		}
		directoryTestBaseName = "testFilters";

		touch(ignoreMe);
		forceMkdirParent(ignoreMe2);
		touch(ignoreMe2);

		runApp(
				"--temp", "target/e2e-temp",
				"-i", "target",
				"-r",
				"--include-ext", "zip",
				"--include-ext", "xml",
				"--exclude-file", "ignore-me.*",
				"--exclude-dir", "e2e-*",
				"--exclude-dir", "*sources*",
				"--exclude-dir", "*classes*",
				"--exclude-dir", "*surefire*",
				"--exclude-dir", "*maven*",
				"--exclude-dir", "*test*",
				"--exclude-dir", "*site*",
				"--exclude-path", "e2e/ignore-me2",
				"-f", "ffprobexml",
				"-e", outDir.getPath(),
				"--export-base-filename", directoryTestBaseName);

		forceDelete(ignoreMe);
		forceDelete(ignoreMe2.getParentFile());
		checkAllOutExists(directoryTestBaseName);
	}

	@Test
	void testFilters2() throws IOException {
		if (inputs.isEmpty()) {
			return;
		}
		directoryTestBaseName = "testFilters2";

		touch(ignoreMe3);
		touch(ignoreMe4);

		runApp(
				"--temp", "target/e2e-temp",
				"-i", "target",
				"-r",
				"--include-file", "demo-render-*.zip",
				"--include-file", "container-only-avi",
				"--include-file", "ignore-me*",
				"--include-dir", "e2e*",
				"--exclude-ext", "nope",
				"-f", "ffprobexml",
				"-e", outDir.getPath(),
				"--export-base-filename", directoryTestBaseName);

		forceDelete(ignoreMe3);
		forceDelete(ignoreMe4);
		checkAllOutExists(directoryTestBaseName);
	}

	private void checkAllOutExists(final String exportBaseFilename) {
		final var results = getTargetedResults(exportBaseFilename)
				.filter(not(File::exists))
				.toList();
		assertThat(results).isEmpty();
	}

	private String getParentDir() {
		return inputs.get(0).archive().getParentFile().getAbsolutePath();
	}

	private Stream<File> getTargetedResults(final String exportBaseFilename) {
		return inputs.stream()
				.map(E2ERawOutDataFiles::mediaFile)
				.map(File::getName)
				.map(f -> f.replace(".", "-") + "_" + exportBaseFilename + "_ffprobe.xml")
				.map(f -> new File(outDir, f));
	}

}
