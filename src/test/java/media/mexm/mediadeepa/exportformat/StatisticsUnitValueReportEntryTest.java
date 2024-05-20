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

import static media.mexm.mediadeepa.ConstStrings.LABEL_AVERAGE;
import static media.mexm.mediadeepa.ConstStrings.LABEL_MAXIMUM;
import static media.mexm.mediadeepa.ConstStrings.LABEL_MEDIAN;
import static media.mexm.mediadeepa.ConstStrings.LABEL_MINIMUM;
import static media.mexm.mediadeepa.exportformat.report.StatisticsUnitValueReportEntry.createFromInteger;
import static media.mexm.mediadeepa.exportformat.report.StatisticsUnitValueReportEntry.createFromLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import j2html.tags.specialized.DivTag;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.exportformat.report.StatisticsUnitValueReportEntry;
import net.datafaker.Faker;

class StatisticsUnitValueReportEntryTest {
	static Faker faker = net.datafaker.Faker.instance();

	StatisticsUnitValueReportEntry stat;

	String key;
	String unit;
	String[] contains;

	@Deprecated
	NumberUtils numberUtils;

	@BeforeEach
	void init() throws Exception {
		numberUtils = new NumberUtils();
		key = faker.numerify("key###");
		unit = faker.numerify("unit###");
		contains = new String[] {
								  key, unit + "s",
								  "12", "50", "77", "200",
								  LABEL_AVERAGE, LABEL_MAXIMUM, LABEL_MEDIAN, LABEL_MINIMUM };
	}

	/*	@Test
		void testCreateFromDouble() {
			stat = createFromDouble(key, Stream.of(12d, 25d, 50d, 100d, 200d), unit + "(s)", "#");
			assertThat(stat.toDomContent(numberUtils)).isInstanceOf(DivTag.class)
					.asString().contains(contains);
			assertFalse(stat.isEmpty());
			assertEquals(key, stat.key());
			assertEquals(unit + "(s)", stat.unit());
		}
	
		@Test
		void testCreateFromFloat() {
			stat = createFromFloat(key, Stream.of(12f, 25f, 50f, 100f, 200f), unit + "(s)");
			assertThat(stat.toDomContent(numberUtils)).isInstanceOf(DivTag.class)
					.asString().contains(contains);
			assertFalse(stat.isEmpty());
			assertEquals(key, stat.key());
			assertEquals(unit + "(s)", stat.unit());
		}*/

	@Test
	void testCreateFromInteger() {
		stat = createFromInteger(key, Stream.of(12, 25, 50, 100, 200), unit + "(s)", numberUtils);
		assertThat(stat.toDomContent(numberUtils)).isInstanceOf(DivTag.class)
				.asString().contains(contains);
		assertFalse(stat.isEmpty());
		assertEquals(key, stat.key());
		assertEquals(unit + "(s)", stat.unit());
	}

	@Test
	void testCreateFromLong() {
		stat = createFromLong(key, Stream.of(12l, 25l, 50l, 100l, 200l), unit + "(s)", numberUtils);
		assertThat(stat.toDomContent(numberUtils)).isInstanceOf(DivTag.class)
				.asString().contains(contains);
		assertFalse(stat.isEmpty());
		assertEquals(key, stat.key());
		assertEquals(unit + "(s)", stat.unit());
	}

}
