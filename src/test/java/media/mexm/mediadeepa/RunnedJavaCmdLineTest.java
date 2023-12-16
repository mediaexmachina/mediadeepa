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
package media.mexm.mediadeepa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import net.datafaker.Faker;

class RunnedJavaCmdLineTest {
	static Faker faker = Faker.instance();

	RunnedJavaCmdLine r;

	String appRunner;
	String args;
	String hostname;
	String username;
	String workingDir;
	String makeFullExtendedCommandline;

	@Mock
	RunnedJavaCmdLine archiveJavaCmdLine;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();

		r = new RunnedJavaCmdLine();
		appRunner = faker.numerify("appRunner###");
		args = faker.numerify("args###");
		hostname = faker.numerify("hostname###");
		username = faker.numerify("username###");
		workingDir = faker.numerify("workingDir###");
		makeFullExtendedCommandline = faker.numerify("makeFullExtendedCommandline###");
	}

	@Test
	void testMakeFullExtendedCommandline() {
		r.setAppRunner(appRunner);
		r.setArgs(args);
		r.setHostname(hostname);
		r.setUsername(username);
		r.setWorkingDir(workingDir);

		final var full = r.makeFullExtendedCommandline();
		assertThat(full).contains(
				appRunner,
				args,
				hostname,
				username,
				workingDir);
	}

	@Test
	void testMakeArchiveCommandline() {
		r.setArchiveJavaCmdLine(archiveJavaCmdLine);
		when(archiveJavaCmdLine.makeFullExtendedCommandline()).thenReturn(makeFullExtendedCommandline);
		assertEquals(Optional.ofNullable(makeFullExtendedCommandline), r.makeArchiveCommandline());
		verify(archiveJavaCmdLine, atLeast(1)).makeFullExtendedCommandline();
		verifyNoMoreInteractions(archiveJavaCmdLine);
	}

	@Test
	void testSetup() {
		r.setup(new String[] { args, args }, appRunner);
		assertEquals(r.getAppRunner(), appRunner);
		assertEquals(r.getArgs(), args + " " + args);
		assertThat(r.getHostname()).isNotBlank();
		assertThat(r.getUsername()).isNotBlank();
		assertThat(r.getWorkingDir()).isNotBlank();
	}

	@Test
	void testEscape() {
		assertEquals(args, RunnedJavaCmdLine.escape(args));
		assertEquals(args + "\\\"", RunnedJavaCmdLine.escape(args + "\""));
		assertEquals("\"" + args + " " + appRunner + "\"",
				RunnedJavaCmdLine.escape(args + " " + appRunner));
	}

}
