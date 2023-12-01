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

import static media.mexm.mediadeepa.components.CLIRunner.EXIT_CODE_GENERATE_DOC;
import static media.mexm.mediadeepa.components.CLIRunner.PROP_EXPORTDOCUMENTATION_MANPAGE;
import static media.mexm.mediadeepa.components.CLIRunner.PROP_EXPORTDOCUMENTATION_README;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
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
	@MockBean
	DocumentationExporter documentationExporter;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();

		/**
		 * Spring Boot test limitation: startup is before BeforeEach
		 */
		reset(appCommand, appSessionService);
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(appCommand, appSessionService, documentationExporter);
	}

	@AfterEach
	@BeforeEach
	void resetProp() {
		System.getProperties().remove(PROP_EXPORTDOCUMENTATION_MANPAGE);
		System.getProperties().remove(PROP_EXPORTDOCUMENTATION_README);
	}

	@Test
	void testRun_makeMan() throws Exception {
		System.setProperty(PROP_EXPORTDOCUMENTATION_MANPAGE, "something");
		c.run();
		verify(documentationExporter, times(1)).exportManPage(new File("something"));
		assertEquals(EXIT_CODE_GENERATE_DOC, c.getExitCode());
	}

	@Test
	void testRun_makeMd() throws Exception {
		System.setProperty(PROP_EXPORTDOCUMENTATION_README, "something");
		c.run();
		verify(documentationExporter, times(1)).exportReadmeProjectMarkdown(new File("something"));
		assertEquals(EXIT_CODE_GENERATE_DOC, c.getExitCode());
	}

}
