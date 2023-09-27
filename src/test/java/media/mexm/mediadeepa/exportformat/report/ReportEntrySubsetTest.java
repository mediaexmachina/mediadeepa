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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import j2html.tags.specialized.DivTag;
import net.datafaker.Faker;

class ReportEntrySubsetTest {

	static Faker faker = net.datafaker.Faker.instance();

	ReportEntrySubset report;
	String value;

	@Mock
	ReportEntry entry;
	@Mock
	ReportSection addToSection;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		value = faker.numerify("value###");
		when(entry.isEmpty()).thenReturn(false);
		when(entry.toDomContent()).thenReturn(div(value));
		report = new ReportEntrySubset(List.of(entry));
	}

	void ends() {
		verifyNoMoreInteractions(entry, addToSection);
	}

	@Test
	void testToDomContent() {
		assertThat(report.toDomContent())
				.isInstanceOf(DivTag.class)
				.asString().contains(value);
	}

	@Test
	void testIsEmpty() {
		assertFalse(report.isEmpty());
		verify(entry, atLeast(1)).isEmpty();
	}

	@Test
	void testToEntrySubset() {
		ReportEntrySubset.toEntrySubset(Stream.of(entry), addToSection);
		verify(addToSection, times(1)).add(any(ReportEntrySubset.class));
	}

}
