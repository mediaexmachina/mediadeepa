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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.datafaker.Faker;

class NumericReportEntryTest {

	static Faker faker = Faker.instance();

	static class NumericReportEntryImpl implements NumericReportEntry {

		@Override
		public String key() {
			return faker.numerify("###key") + "(s)";
		}

		@Override
		public String unit() {
			return faker.numerify("###unit") + "(s)";
		}
	}

	NumericReportEntryImpl nre;

	@BeforeEach
	void init() {
		nre = new NumericReportEntryImpl();
	}

	@Test
	void testGetKeyWithPlurial() {
		assertThat(nre.getKeyWithPlurial(false)).endsWith("key");
		assertThat(nre.getKeyWithPlurial(true)).endsWith("keys");
	}

	@Test
	void testGetUnitWithPlurial() {
		assertThat(nre.getUnitWithPlurial(false)).endsWith("unit");
		assertThat(nre.getUnitWithPlurial(true)).endsWith("units");
	}

	@Test
	void testGetWithPlurial() {
		assertThat(nre.getWithPlurial(null, false)).isEmpty();
		assertThat(nre.getWithPlurial(null, true)).isEmpty();
		assertThat(nre.getWithPlurial("", false)).isEmpty();
		assertThat(nre.getWithPlurial("", true)).isEmpty();

		assertThat(nre.getWithPlurial("key", false)).isEqualTo("key");
		assertThat(nre.getWithPlurial("key", true)).isEqualTo("key");
		assertThat(nre.getWithPlurial("keys", false)).isEqualTo("keys");
		assertThat(nre.getWithPlurial("keys", true)).isEqualTo("keys");
		assertThat(nre.getWithPlurial("key(s)", false)).isEqualTo("key");
		assertThat(nre.getWithPlurial("key(s)", true)).isEqualTo("keys");
	}

}
