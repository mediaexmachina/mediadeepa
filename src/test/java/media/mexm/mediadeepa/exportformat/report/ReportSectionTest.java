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
package media.mexm.mediadeepa.exportformat.report;

import static j2html.TagCreator.div;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import j2html.tags.specialized.SectionTag;
import net.datafaker.Faker;

class ReportSectionTest {

	static Faker faker = net.datafaker.Faker.instance();

	ReportSection reportSection;
	ReportSectionCategory category;
	String title;

	@Mock
	ReportEntry entry;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		category = faker.options().option(ReportSectionCategory.class);
		title = faker.numerify("title###");
		reportSection = new ReportSection(category, title);
	}

	void ends() {
		verifyNoMoreInteractions(entry);
	}

	@Test
	void testToDomContent() {
		assertThat(reportSection.toDomContent()).isInstanceOf(SectionTag.class);
	}

	@Test
	void testAddReportEntryArray() {
		assertEquals(reportSection, reportSection.add(entry));
	}

	@Test
	void testAddCollectionOfT() {
		assertEquals(reportSection, reportSection.add(Set.of(entry)));
	}

	@Test
	void testIsNotEmpty() {
		assertFalse(reportSection.isNotEmpty());
	}

	@Test
	void testGetSectionAnchorName() {
		assertThat(reportSection.getSectionAnchorName())
				.startsWith(category.toString().toLowerCase())
				.endsWith(title.toLowerCase())
				.doesNotContain(" ");
	}

	@Nested
	class WithEntry {

		String value;

		@BeforeEach
		void init() throws Exception {
			value = faker.numerify("value###");
			when(entry.isEmpty()).thenReturn(false);
			when(entry.toDomContent()).thenReturn(div(value));
			reportSection.add(entry);
		}

		@Test
		void testToDomContent() {
			assertThat(reportSection.toDomContent())
					.isInstanceOf(SectionTag.class)
					.asString().contains(value);
			verify(entry, atLeast(1)).isEmpty();
			verify(entry, atLeast(1)).toDomContent();
		}

		@Test
		void testIsNotEmpty() {
			assertTrue(reportSection.isNotEmpty());
			verify(entry, atLeast(1)).isEmpty();
		}

	}
}
