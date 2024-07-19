/*
 * This file is part of mediadeepa.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2023
 *
 */
package media.mexm.mediadeepa.exportformat;

import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.components.OutputFileSupplier;
import media.mexm.mediadeepa.exportformat.components.TabularTXTExportFormat;
import net.datafaker.Faker;

class TabularTXTExportFormatTest {
	static final Faker faker = Faker.instance();

	TabularTXTExportFormat t;
	NumberUtils numberUtils;
	@Mock
	OutputFileSupplier outputFileSupplier;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		numberUtils = new NumberUtils();
		t = new TabularTXTExportFormat(List.of(), numberUtils, outputFileSupplier);
	}

	@Test
	void testGetFormatLongName() {
		assertThat(t.getFormatLongName()).hasSizeGreaterThan(0);
	}

	@Test
	void testGetDocumentFileExtension() {
		assertEquals("txt", t.getDocumentFileExtension());
	}

	@Test
	void testFormatToString() {
		assertEquals("42", t.formatNumberHighPrecision(42f));
		assertEquals("4.2", t.formatNumberHighPrecision(4.2f));
	}

	@Test
	void testGetDocument() {
		final var head = faker.numerify("head###");
		final var row = List.of(faker.numerify("item###"), faker.numerify("item###"));

		final var doc = t.getDocument(List.of(head), List.of(row, row));
		assertEquals(head + lineSeparator()
					 + row.get(0) + "\t" + row.get(1) + lineSeparator()
					 + row.get(0) + "\t" + row.get(1) + lineSeparator(),
				new String(doc, UTF_8));
	}

}
