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
package media.mexm.mediadeepa.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.concurrent.Callable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import net.datafaker.Faker;

class AppCommandTest {
	static Faker faker = net.datafaker.Faker.instance();

	AppCommand c;

	@Mock
	Callable<Integer> doCall;
	int returnCode;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		c = new AppCommand();
		returnCode = faker.random().nextInt();
		when(doCall.call()).thenReturn(returnCode);
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(doCall);
	}

	@Test
	void testCall() throws Exception {
		c.setDoCall(doCall);
		assertEquals(returnCode, c.call());
		verify(doCall, times(1)).call();

	}

}
