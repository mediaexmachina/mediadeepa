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
package media.mexm.mediadeepa.components;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.datafaker.Faker;

class NumberUtilsTest {

	NumberUtils nu;

	@BeforeEach
	void init() {
		nu = new NumberUtils();
	}

	@Test
	void testFormatDecimalSimple1En() {
		assertEquals("123456.9", nu.formatDecimalSimple1En(123456.88888));
	}

	@Test
	void testFormatDecimalSimple5En() {
		assertEquals("123456.98765", nu.formatDecimalSimple5En(123456.987654));
	}

	@Test
	void testFormatDecimalSimple1Fr() {
		assertEquals("123456,9", nu.formatDecimalSimple1Fr(123456.88888));
	}

	@Test
	void testFormatDecimalSimple5Fr() {
		assertEquals("123456,98765", nu.formatDecimalSimple5Fr(123456.987654));
	}

	@Test
	void testFormatDecimalFull1En() {
		assertEquals("123,456.9", nu.formatDecimalFull1En(123456.88888));
	}

	@Test
	void testFormatFixedFullEn() {
		assertEquals("123,457", nu.formatFixedFullEn(123456.88888));
	}

	@Test
	void testFormatDecimalFull3En() {
		assertEquals("123,456.889", nu.formatDecimalFull3En(123456.88888));
	}

	@Test
	void testValueToString() {
		assertEquals("?", nu.valueToString(null));
		assertEquals("5", nu.valueToString(5));
		assertEquals("5", nu.valueToString(5l));
		assertEquals("123,456.9", nu.valueToString(123456.88888f));
		assertEquals("?", nu.valueToString(Float.NaN));
		assertEquals("-144", nu.valueToString(Float.NEGATIVE_INFINITY));
		assertEquals("144", nu.valueToString(Float.POSITIVE_INFINITY));
		assertEquals("123,456.9", nu.valueToString(123456.88888d));
		assertEquals("?", nu.valueToString(Double.NaN));
		assertEquals("-144", nu.valueToString(Double.NEGATIVE_INFINITY));
		assertEquals("144", nu.valueToString(Double.POSITIVE_INFINITY));
		assertEquals("5", nu.valueToString((short) 5));
	}

	@Test
	void testDurationToString() {
		assertEquals("", nu.durationToString(null));
		assertEquals("00:00:00", nu.durationToString(Duration.ZERO));
		assertEquals("00:00:00.009", nu.durationToString(Duration.ofMillis(9)));
		assertEquals("00:00:00.099", nu.durationToString(Duration.ofMillis(99)));
		assertEquals("00:00:00.999", nu.durationToString(Duration.ofMillis(999)));
		assertEquals("00:00:59", nu.durationToString(Duration.ofSeconds(59)));
		assertEquals("00:59:00", nu.durationToString(Duration.ofMinutes(59)));
		assertEquals("10:00:00", nu.durationToString(Duration.ofHours(10)));
		assertEquals("48:00:00", nu.durationToString(Duration.ofDays(2)));
	}

	@Test
	void testSecToMs() {
		final var f = Faker.instance().random().nextFloat();
		assertEquals(f * 1000, nu.secToMs(f));
	}

	@Test
	void testGetTimeDerivative() {
		final var result = nu.getTimeDerivative(Stream.of(1f, 2f, 10f, 3f, 4f), 5);
		assertArrayEquals(new double[] { 1, 8, -7, 1, 0 }, result);
	}

}
