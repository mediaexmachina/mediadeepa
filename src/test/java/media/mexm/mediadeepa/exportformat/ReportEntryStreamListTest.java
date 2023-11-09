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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import j2html.tags.specialized.DivTag;
import media.mexm.mediadeepa.components.NumberUtils;
import net.datafaker.Faker;

class ReportEntryStreamListTest {

	static Faker faker = net.datafaker.Faker.instance();

	ReportEntryStreamList report;
	String id;
	String codecType;
	int index;

	@Deprecated
	NumberUtils numberUtils;

	@BeforeEach
	void init() {
		numberUtils = new NumberUtils();
		report = new ReportEntryStreamList();
		id = faker.numerify("id###");
		codecType = faker.numerify("codecType###");
		index = faker.random().nextInt();
	}

	@Test
	void testAddStream() {
		assertNotNull(report.addStream(id, codecType, index));
	}

	@Test
	void testGetFirstItemsToDisplay() {
		assertEquals(Set.of(), report.getFirstItemsToDisplay());
	}

	@Test
	void testToDomContent() {
		assertThat(report.toDomContent(numberUtils)).isInstanceOf(DivTag.class);
	}

	@Test
	void testIsEmpty() {
		assertTrue(report.isEmpty());
	}

	@Nested
	class Added {

		String key0;
		String key1;
		String value0A;
		String value0B;
		String value1C;

		@BeforeEach
		void init() throws Exception {
			key0 = faker.numerify("key0###");
			key1 = faker.numerify("key1###");
			value0A = faker.numerify("valueA###");
			value0B = faker.numerify("valueB###");
			value1C = faker.numerify("valueC###");

			report.addStream(id, codecType, index).add(key0, value0A);
			report.addStream(id, codecType, index).add(key0, value0B).add(key1, value1C);
		}

		@Test
		void testGetFirstItemsToDisplay() {
			assertEquals(Set.of(key0), report.getFirstItemsToDisplay());
		}

		@Test
		void testToDomContent() {
			assertThat(report.toDomContent(numberUtils)).isInstanceOf(DivTag.class)
					.asString().contains(key0, key1, value0A, value0B, value1C);
		}

		@Test
		void testIsEmpty() {
			assertFalse(report.isEmpty());
		}
	}

}
