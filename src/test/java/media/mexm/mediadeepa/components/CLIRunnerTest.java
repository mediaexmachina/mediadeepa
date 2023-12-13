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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.Callable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import media.mexm.mediadeepa.cli.AppCommand;
import media.mexm.mediadeepa.service.AppSessionService;
import media.mexm.mediadeepa.service.DocumentParserService;
import net.datafaker.Faker;
import picocli.CommandLine;

@SpringBootTest
class CLIRunnerTest {

	@Autowired
	CLIRunner c;

	@MockBean
	CommandLine commandLine;
	@MockBean
	AppCommand appCommand;
	@MockBean
	AppSessionService appSessionService;
	@MockBean
	DocumentParserService documentParserService;
	@MockBean
	DocumentationExporter documentationExporter;
	@Captor
	ArgumentCaptor<Callable<Integer>> doCallCaptor;
	int returnCode;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		returnCode = Faker.instance().random().nextInt();
		when(appSessionService.runCli()).thenReturn(returnCode);
		when(commandLine.execute(any())).thenReturn(returnCode);
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(
				commandLine,
				appCommand,
				appSessionService,
				documentParserService,
				documentationExporter);
	}

	@Test
	void testRun() throws Exception {
		verify(commandLine, times(2)).setParameterExceptionHandler(any());
		verify(appCommand, times(1)).setDoCall(doCallCaptor.capture());
		assertEquals(returnCode, doCallCaptor.getValue().call());
		verify(appSessionService, times(1)).runCli();
		verify(commandLine, times(1)).execute(any());

		/**
		 * Spring Boot test limitation: startup is before BeforeEach
		 */
		assertEquals(0, c.getExitCode());
	}

}
