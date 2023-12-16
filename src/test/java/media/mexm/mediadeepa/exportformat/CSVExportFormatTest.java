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
package media.mexm.mediadeepa.exportformat;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.exportformat.components.TabularCSVExportFormat;
import net.datafaker.Faker;

class CSVExportFormatTest {
	final static Faker faker = Faker.instance();

	TabularCSVExportFormat c;
	NumberUtils numberUtils;

	@BeforeEach
	void init() {
		numberUtils = new NumberUtils();
		c = new TabularCSVExportFormat(List.of(), numberUtils);
	}

	@Test
	void testGetFormatLongName() {
		assertThat(c.getFormatLongName()).hasSizeGreaterThan(0);
	}

	@Test
	void testGetDocumentFileExtension() {
		assertEquals("csv", c.getDocumentFileExtension());
	}

	@Test
	void testGetDocument() {
		final var head = List.of(faker.numerify("head###"), faker.numerify("head###"), faker.numerify("head###"));
		final var row = List.of(faker.numerify("item###"), faker.numerify("#.##"), faker.numerify("###"));
		final var doc = c.getDocument(head, List.of(row, row));

		assertEquals(head.get(0) + "," + head.get(1) + "," + head.get(2) + "\r\n"
					 + row.get(0) + "," + row.get(1) + "," + row.get(2) + "\r\n"
					 + row.get(0) + "," + row.get(1) + "," + row.get(2) + "\r\n",
				new String(doc, UTF_8));
	}

}
