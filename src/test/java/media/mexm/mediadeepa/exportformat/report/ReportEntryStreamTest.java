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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import j2html.tags.specialized.DivTag;
import media.mexm.mediadeepa.components.NumberUtils;
import net.datafaker.Faker;

class ReportEntryStreamTest {

	static Faker faker = net.datafaker.Faker.instance();

	ReportEntryStream res;

	String key;
	String value;
	String unit;
	Number nValue;
	String byDefault;
	String id;
	String codecType;
	int index;

	@Mock
	ReportEntryStreamList reportList;

	NumberUtils numberUtils;

	@BeforeEach
	void init() throws Exception {
		numberUtils = new NumberUtils();
		openMocks(this).close();
		key = faker.numerify("key###");
		value = faker.numerify("value###");
		unit = faker.numerify("unit###");
		byDefault = faker.numerify("byDefault###");
		id = faker.numerify("id###");
		codecType = faker.numerify("codecType###");
		index = faker.random().nextInt();
		nValue = 123_456.758f;
		res = new ReportEntryStream(id, codecType, index, reportList);
	}

	void ends() {
		verifyNoMoreInteractions(reportList);
	}

	@Test
	void testAddStringString() {
		assertEquals(res, res.add(key, value));
		assertEquals(Set.of(key), res.getAllItemKeys());
		assertFalse(res.isEmpty());
		assertThat(res.toDomContent(numberUtils)).isInstanceOf(DivTag.class)
				.asString().contains(key, value);
	}

	@Test
	void testAddStringStringString() {
		res.add(key, value, byDefault);
		assertEquals(Set.of(key), res.getAllItemKeys());
		assertFalse(res.isEmpty());
		assertThat(res.toDomContent(numberUtils)).isInstanceOf(DivTag.class)
				.asString().contains(key, value);
	}

	@Test
	void testAddStringStringString_default() {
		res.add(key, byDefault, byDefault);
		assertEquals(Set.of(), res.getAllItemKeys());
		assertTrue(res.isEmpty());
		assertThat(res.toDomContent(numberUtils)).isInstanceOf(DivTag.class)
				.asString().doesNotContain(key, byDefault);
	}

	@Test
	void testAddStringStreamOfString() {
		res.add(key, Stream.of(value));
		assertEquals(Set.of(key), res.getAllItemKeys());
		assertFalse(res.isEmpty());
		assertThat(res.toDomContent(numberUtils)).isInstanceOf(DivTag.class)
				.asString().contains(key, value);
	}

	@Test
	void testAddStringNumberString() {
		res.add(key, nValue, unit);
		assertEquals(Set.of(key), res.getAllItemKeys());
		assertFalse(res.isEmpty());
		assertThat(res.toDomContent(numberUtils)).isInstanceOf(DivTag.class)
				.asString().contains(key, "123,456.8", unit);
	}

	@Test
	void testIsEmpty() {
		assertTrue(res.isEmpty());
	}

}
