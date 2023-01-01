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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.datafaker.Faker;

class TabularTXTExportFormatTest {
	final static Faker faker = Faker.instance();

	TabularTXTExportFormat t;

	@Test
	void testGetDocument() {
		t = new TabularTXTExportFormat();
		final var head = faker.numerify("head###");
		final var row = List.of(faker.numerify("item###"), faker.numerify("item###"));

		final var doc = t.getDocument(List.of(head), List.of(row, row));
		assertEquals(new String(doc, UTF_8), head + lineSeparator()
											 + row.get(0) + "\t" + row.get(1) + lineSeparator()
											 + row.get(0) + "\t" + row.get(1));
	}

}
