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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import media.mexm.mediadeepa.ImpExArchiveExtractionSession.ExtractedFileEntry;
import net.datafaker.Faker;

class ImpExArchiveExtractionSessionTest {
	static Faker faker = net.datafaker.Faker.instance();

	ImpExArchiveExtractionSession s;
	String internalFileName;
	String content;
	List<String> lines;
	ExtractedFileEntry efEntry;
	List<ExtractedFileEntry> efEntries;
	Map<String, String> versions;

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
	}

	@Test
	void testAddStringString() {
		s.add(internalFileName, content);
		final var entries = s.getEntries().toList();
		assertEquals(List.of(efEntry), entries);
	}

	@Test
	void testAddStringListOfString() {
		s.add(internalFileName, lines);
		final var entries = s.getEntries().toList();
		assertEquals(efEntries, entries);
	}

	@Test
	void testGetEntries() {
		assertEquals(0, s.getEntries().count());
	}

	@Test
	void testAddVersion() {
		s.addVersion(internalFileName, versions);
		assertEquals(1, s.getEntries().count());
		final var item = s.getEntries().toList().get(0);
		assertEquals(internalFileName, item.internalFileName());
		assertThat(item.content()).startsWith("<?xml version='1.0'?><map>");
		assertThat(item.content()).endsWith("</map>");
	}

	@Test
	void testGetVersions() {
		s.addVersion(internalFileName, versions);
		assertEquals(versions, s.getVersions(internalFileName));
	}

}
