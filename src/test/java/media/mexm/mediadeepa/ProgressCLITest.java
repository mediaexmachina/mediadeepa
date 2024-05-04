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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.datafaker.Faker;

class ProgressCLITest {

	static final String NL = System.lineSeparator();
	static Faker faker = net.datafaker.Faker.instance();

	ProgressCLI c;

	double value;
	float speed;
	ByteArrayOutputStream outputStream;
	PrintStream printStream;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		value = 0.3333;
		speed = 0.6666f;
		outputStream = new ByteArrayOutputStream();
		printStream = new PrintStream(outputStream);
		c = new ProgressCLI(printStream);
	}

	@Test
	void testDisplayProgress_simple() {
		c.displayProgress(value, speed);
		assertThat(outputStream).asString()
				.contains(" |======              | 33% ETA 00:00:00, x0.7");
	}

	@Test
	void testDisplayProgress_lastIsSame() {
		c.displayProgress(value, speed);
		c.displayProgress(value, speed);
		assertThat(outputStream).asString()
				.contains(" |======              | 33% ETA 00:00:00, x0.7");
	}

	@Test
	void testEnd() {
		c.end();
		assertThat(outputStream).asString()
				.contains(" |====================|100%, total time:");
	}

	@Test
	void testEnd_twice() {
		c.end();
		c.end();
		assertThat(outputStream).asString()
				.contains(" |====================|100%, total time:");
	}

	@Test
	void testDisplayProgress_rude_start() {
		c.displayProgress(0, speed);
		assertThat(outputStream).asString()
				.contains(" |                    |");
	}

	@Test
	void testDisplayProgress_rude_end() {
		c.displayProgress(1, speed);
		assertThat(outputStream).asString()
				.contains(" |====================|100%, total time:");
	}

}
