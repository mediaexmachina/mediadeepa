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

import static org.apache.commons.io.FileUtils.forceDelete;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import media.mexm.mediadeepa.cli.AppCommand;
import media.mexm.mediadeepa.service.AppSessionService;

@SpringBootTest
class CLIRunnerDocExportTest {

	@Autowired
	CLIRunner c;

	@MockBean
	AppCommand appCommand;
	@MockBean
	AppSessionService appSessionService;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		reset(appCommand, appSessionService);
	}

	@BeforeAll
	@AfterAll
	static void all() {
		System.getProperties().remove("exportdocumentation.manpage");
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(appCommand, appSessionService);
	}

	@Test
	void testRun_makeMan() throws Exception {
		final var tempMan = File.createTempFile("mediadeepa-test", ".man");
		forceDelete(tempMan);
		System.setProperty("exportdocumentation.manpage", tempMan.getPath());
		c.run();

		assertThat(tempMan)
				.exists()
				.size()
				.isGreaterThan(10)
				.returnToFile()
				.content()
				.contains("ffmpeg", "mediadeepa", "\\-\\-export");
		forceDelete(tempMan);
		assertEquals(0, c.getExitCode());
	}

}
