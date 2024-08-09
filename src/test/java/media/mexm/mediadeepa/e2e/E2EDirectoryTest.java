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
import static org.apache.commons.io.FileUtils.delete;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.forceMkdirParent;
import static org.apache.commons.io.FileUtils.touch;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class E2EDirectoryTest extends E2EUtils {

	static final File empty = new File("target/e2e-temp/always-empty");

	@BeforeAll
	static void load() throws IOException {
		forceMkdir(empty);
		cleanDirectory(empty);
	}

	List<E2ERawOutDataFiles> inputs;

	@BeforeEach
	void init() {
		inputs = ALL_MEDIA_FILE_NAME.stream()
				.filter(File::exists)
				.map(E2EUtils::prepareForSimpleE2ETests)
				.toList();

		getTargetedResults()
				.filter(File::exists)
				.forEach(f -> {
					try {
						delete(f);
					} catch (final IOException e) {
						throw new UncheckedIOException("Can't delete", e);
					}
				});
	}

	@Test
	void testNoFiles_emptyDir() {// NOSONAR S2699
		runApp(
				"-i", empty.getPath(),
				"-f", "ffprobexml",
				"-e", "target/e2e-export",
				"--export-base-filename", "directory");
	}

	@Test
	void testOneDir() throws IOException {
		if (inputs.isEmpty()) {
			return;
		}

		/**
		 * Test not recursive by default
		 */
		final var ignoreMe = new File("target/e2e-export/ignore-me/ignore-me.txt");
		forceMkdirParent(ignoreMe);
		touch(ignoreMe);

		runApp(
				"--temp", "target/e2e-temp",
				"-i", getParentDir(),
				"-f", "ffprobexml",
				"-e", "target/e2e-export",
				"--export-base-filename", "directory");

		checkAllOutExists();
	}

	@Test
	void testTwoDir() {
		if (inputs.isEmpty()) {
			return;
		}

		runApp(
				"--temp", "target/e2e-temp",
				"-i", empty.getPath(),
				"-i", getParentDir(),
				"-f", "ffprobexml",
				"-e", "target/e2e-export",
				"--export-base-filename", "directory");

		checkAllOutExists();
	}

	@Test
	void testTwoZip() {
		if (inputs.size() < 2) {
			return;
		}
		final var zip0 = inputs.get(0).archive().getAbsolutePath();
		final var zip1 = inputs.get(1).archive().getAbsolutePath();

		runApp(
				"--temp", "target/e2e-temp",
				"-i", zip0,
				"-i", zip1,
				"-f", "ffprobexml",
				"-e", "target/e2e-export",
				"--export-base-filename", "directory");

		final var results = getTargetedResults()
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
		final var zip0 = inputs.get(0).archive().getAbsolutePath();
		final var zip1 = inputs.get(1).archive().getAbsolutePath();

		runApp(
				"--temp", "target/e2e-temp",
				"-i", zip0,
				"-i", zip1,
				"-i", empty.getPath(),
				"-i", getParentDir(),
				"-f", "ffprobexml",
				"-e", "target/e2e-export",
				"--export-base-filename", "directory");

		checkAllOutExists();
	}

	@Test
	void testTwoMedias() {
		if (inputs.size() < 2) {
			return;
		}
		final var media0 = inputs.get(0).mediaFile().getAbsolutePath();
		final var media1 = inputs.get(1).mediaFile().getAbsolutePath();

		runApp(
				"--temp", "target/e2e-temp",
				"-i", media0,
				"-i", media1,
				"-fo", "astats",
				"-vn",
				"-f", "ffprobexml",
				"-e", "target/e2e-export",
				"--export-base-filename", "directory-media");

		final var results = inputs.stream()
				.limit(2)
				.map(E2ERawOutDataFiles::mediaFile)
				.map(File::getName)
				.map(f -> f.replace(".", "-") + "_directory-media_ffprobe.xml")
				.map(f -> new File("target/e2e-export/" + f))
				.filter(not(File::exists))
				.toList();

		assertThat(results).isEmpty();
	}

	/*
	TODO test + recursive
		setIgnoreFiles
		setAllowedExtentions
		setBlockedExtentions
		setIgnoreRelativePaths
		setAllowedFileNames
		setAllowedDirNames
		setBlockedFileNames
		setBlockedDirNames
		setAllowedLinks
		setAllowedHidden
	*/

	private void checkAllOutExists() {
		final var results = getTargetedResults()
				.filter(not(File::exists))
				.toList();
		assertThat(results).isEmpty();
	}

	private String getParentDir() {
		return inputs.get(0).archive().getParentFile().getAbsolutePath();
	}

	private Stream<File> getTargetedResults() {
		return inputs.stream()
				.map(E2ERawOutDataFiles::mediaFile)
				.map(File::getName)
				.map(f -> f.replace(".", "-") + "_directory_ffprobe.xml")
				.map(f -> new File("target/e2e-export/" + f));
	}

}
