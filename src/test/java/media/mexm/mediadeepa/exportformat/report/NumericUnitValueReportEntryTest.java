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

import static media.mexm.mediadeepa.exportformat.report.NumericUnitValueReportEntry.valueToString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import j2html.tags.specialized.DivTag;

class NumericUnitValueReportEntryTest extends BaseReportEntryTest {

	Number value;
	String unit;

	@BeforeEach
	void init() {
		value = faker.random().nextDouble(2, 1000);
		unit = faker.numerify("unit###");
		entry = new NumericUnitValueReportEntry(key, value, unit);
	}

	@Test
	void testToDomContent() {
		final var dom = entry.toDomContent();
		assertNotNull(dom);
		assertThat(dom).isInstanceOf(DivTag.class);
	}

	@Test
	void testToDomContent_empty() {
		entry = new NumericUnitValueReportEntry(key, 0, unit);
		final var dom = entry.toDomContent();
		assertNotNull(dom);
		assertThat(dom).isInstanceOf(DivTag.class);
		assertThat(dom.toString()).doesNotContain(">0<");
		assertThat(dom.toString()).contains(">no<");
	}

	@Test
	void testToDomContent_single() {
		entry = new NumericUnitValueReportEntry(key, 1, unit + "(s)");
		final var dom = entry.toDomContent();
		assertNotNull(dom);
		assertThat(dom).isInstanceOf(DivTag.class);
		assertThat(dom.toString()).contains(unit);
		assertThat(dom.toString()).doesNotContain("(s)");
		assertThat(dom.toString()).doesNotContain(unit + "s");
	}

	@Test
	void testToDomContent_multiple() {
		entry = new NumericUnitValueReportEntry(key, 2, unit + "(s)");
		final var dom = entry.toDomContent();
		assertNotNull(dom);
		assertThat(dom).isInstanceOf(DivTag.class);
		assertThat(dom.toString()).contains(unit + "s");
		assertThat(dom.toString()).doesNotContain("(s)");
	}

	@Test
	void testIsEmpty() {
		assertFalse(entry.isEmpty());
	}

	@Test
	void testIsEmpty_nan() {
		assertTrue(new NumericUnitValueReportEntry(key, Double.NaN, unit).isEmpty());
		assertTrue(new NumericUnitValueReportEntry(key, Float.NaN, unit).isEmpty());
		assertTrue(new NumericUnitValueReportEntry(key, null, unit).isEmpty());
	}

	@Test
	void testValueToString() {
		assertEquals("?", valueToString(null));
		assertEquals("12,345", valueToString(12345l));
		assertEquals("12,345", valueToString(12345));
		assertEquals("?", valueToString(Float.NaN));
		assertEquals("-144", valueToString(Float.NEGATIVE_INFINITY));
		assertEquals("144", valueToString(Float.POSITIVE_INFINITY));
		assertEquals("12,345.7", valueToString(12345.67f));
		assertEquals("12,345.6", valueToString(12345.61f));
		assertEquals("?", valueToString(Double.NaN));
		assertEquals("-144", valueToString(Double.NEGATIVE_INFINITY));
		assertEquals("144", valueToString(Double.POSITIVE_INFINITY));
		assertEquals("12,345.7", valueToString(12345.67d));
		assertEquals("12,345.6", valueToString(12345.61d));
		assertEquals("5", valueToString((short) 5));
	}

}
