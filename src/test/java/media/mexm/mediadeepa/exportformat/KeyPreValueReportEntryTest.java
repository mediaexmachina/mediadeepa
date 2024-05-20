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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import j2html.tags.specialized.DivTag;
import media.mexm.mediadeepa.exportformat.report.KeyPreValueReportEntry;

class KeyPreValueReportEntryTest extends BaseReportEntryTest {

	String value;

	@BeforeEach
	void init() throws Exception {
		value = faker.numerify("value###");
		entry = new KeyPreValueReportEntry(key, value);
	}

	@Test
	void testToDomContent() {
		final var dom = entry.toDomContent(null);
		assertNotNull(dom);
		assertThat(dom).isInstanceOf(DivTag.class);
	}

	@Test
	void testIsEmpty() {
		assertFalse(entry.isEmpty());
	}

}
