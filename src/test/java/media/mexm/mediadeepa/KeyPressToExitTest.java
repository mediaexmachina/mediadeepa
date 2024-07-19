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
package media.mexm.mediadeepa;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.datafaker.Faker;

class KeyPressToExitTest {
	static Faker faker = net.datafaker.Faker.instance();

	ByteArrayInputStream source;
	TestKeyPressToExit p;

	static class TestKeyPressToExit extends KeyPressToExit {

		CountDownLatch latch;

		public TestKeyPressToExit(final InputStream source) {
			super(source);
			latch = new CountDownLatch(1);
		}

		@Override
		protected void exit() {
			latch.countDown();
		}
	}

	@BeforeEach
	void init() {
		source = new ByteArrayInputStream("\n".getBytes());
		p = new TestKeyPressToExit(source);
	}

	@Test
	void testRun() throws InterruptedException {
		p.run();
		assertTrue(p.latch.await(1, SECONDS));
	}

	@Test
	void testStart() throws InterruptedException {
		p.start();
		assertTrue(p.latch.await(1, SECONDS));
	}

}
