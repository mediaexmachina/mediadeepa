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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import j2html.tags.specialized.DivTag;

class SimpleKeyValueReportEntryTest extends BaseReportEntryTest {

	String value;

	@BeforeEach
	void init() {
		value = faker.numerify("value###");
		entry = new SimpleKeyValueReportEntry(key, value);
	}

	@Test
	void testToDomContent() {
		final var dom = entry.toDomContent();
		assertNotNull(dom);
		assertThat(dom).isInstanceOf(DivTag.class);
	}

	@Test
	void testIsEmpty() {
		assertFalse(entry.isEmpty());
	}

	@Test
	void testGetFromRatio() {
		final var entry = SimpleKeyValueReportEntry.getFromRatio(key, 10, 20);
		assertEquals("1:2 (0.5)", entry.value());
	}

}
