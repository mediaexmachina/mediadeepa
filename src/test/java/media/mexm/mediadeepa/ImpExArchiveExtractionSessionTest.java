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
package media.mexm.mediadeepa;

import static media.mexm.mediadeepa.ImpExArchiveExtractionSession.NEWLINE;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import media.mexm.mediadeepa.ImpExArchiveExtractionSession.ExtractedFileEntry;
import net.datafaker.Faker;
import tv.hd3g.fflauncher.recipes.MediaAnalyserSessionFilterContext;

class ImpExArchiveExtractionSessionTest {
	static Faker faker = net.datafaker.Faker.instance();

	ImpExArchiveExtractionSession s;
	String internalFileName;
	String content;
	List<String> lines;
	ExtractedFileEntry efEntry;
	List<ExtractedFileEntry> efEntries;
	Map<String, String> versions;
	List<MediaAnalyserSessionFilterContext> filters;
	File zipFile;

	@BeforeEach
	void init() throws Exception {
		s = new ImpExArchiveExtractionSession();
		internalFileName = faker.numerify("internalFileName###");
		content = faker.numerify("content###");
		lines = List.of(faker.numerify("line###"), faker.numerify("line###"));
		efEntry = new ExtractedFileEntry(internalFileName, content);
		efEntries = List.of(new ExtractedFileEntry(internalFileName,
				lines.get(0) + NEWLINE + lines.get(1)));
		versions = Map.of(
				faker.numerify("key0-###"), faker.numerify("value0-###"),
				faker.numerify("key1-###"), faker.numerify("value1-###"));
		filters = List.of(new MediaAnalyserSessionFilterContext(
				faker.numerify("type###"),
				faker.numerify("name###"),
				faker.numerify("setup###"),
				faker.numerify("className###")));
		zipFile = File.createTempFile("mediadeepa-test", ".zip");
	}

	@AfterEach
	void ends() {
		FileUtils.deleteQuietly(zipFile);
	}

	@Test
	void testAddStringString() {
		s.add(internalFileName, (String) null);
		s.add(internalFileName, "");
		s.add(internalFileName, content);
		final var entries = s.getEntries().toList();
		assertEquals(List.of(efEntry), entries);
	}

	@Test
	void testAddStringListOfString() {
		s.add(internalFileName, (List<String>) null);
		s.add(internalFileName, List.of());
		s.add(internalFileName, lines);
		final var entries = s.getEntries().toList();
		assertEquals(efEntries, entries);
	}

	@Test
	void testGetEntries() {
		assertEquals(0, s.getEntries().count());
	}

	private String checkEntryJson() {
		final var item = s.getEntries().toList().get(0);
		assertEquals(internalFileName, item.internalFileName());
		return item.content();
	}

	@Test
	void testAddVersion() {
		s.addVersion(internalFileName, versions);
		assertEquals(1, s.getEntries().count());
		assertThat(checkEntryJson()).startsWith("{").endsWith("}");
	}

	@Test
	void testGetVersions() {
		assertEquals(Map.of(), s.getVersions(internalFileName));
		s.addVersion(internalFileName, versions);
		assertEquals(versions, s.getVersions(internalFileName));
	}

	@Test
	void testAddFilterContext() {
		s.addFilterContext(internalFileName, filters);
		assertEquals(1, s.getEntries().count());
		assertThat(checkEntryJson()).startsWith("[").endsWith("]");
	}

	@Test
	void testGetFilterContext() {
		assertThat(s.getFilterContext(internalFileName)).isEmpty();
		s.addFilterContext(internalFileName, filters);
		final var result = s.getFilterContext(internalFileName);
		assertEquals(filters, result);
	}

	@Test
	void testWriteEmpty() {
		assertThat(zipFile).exists().size().isEqualTo(0);
		s.saveToZip(zipFile);
		assertThat(zipFile).doesNotExist();
	}

	@Test
	void testReadWrite() throws IOException {
		s.add(internalFileName, content);
		forceDelete(zipFile);
		s.saveToZip(zipFile);
		assertThat(zipFile).exists().size().isGreaterThan(1);

		s = new ImpExArchiveExtractionSession();
		final var notFile = new File("");
		assertThrows(UncheckedIOException.class, () -> s.readFromZip(notFile));
		s.readFromZip(zipFile);
		final var entries = s.getEntries().toList();
		assertEquals(List.of(efEntry), entries);
	}

}
