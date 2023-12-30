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
package media.mexm.mediadeepa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import media.mexm.mediadeepa.KeyPressToExit;
import media.mexm.mediadeepa.cli.AppCommand;
import media.mexm.mediadeepa.cli.ExportToCmd;
import media.mexm.mediadeepa.cli.ExtractToCmd;
import media.mexm.mediadeepa.cli.ImportFromCmd;
import media.mexm.mediadeepa.cli.ProcessFileCmd;
import media.mexm.mediadeepa.components.CLIRunner;
import media.mexm.mediadeepa.components.DocumentationExporter;
import media.mexm.mediadeepa.config.AppConfig;
import net.datafaker.Faker;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.UsageMessageSpec;
import picocli.CommandLine.ParameterException;
import tv.hd3g.commons.version.EnvironmentVersion;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

@SpringBootTest
class AppSessionServiceTest {
	static Faker faker = net.datafaker.Faker.instance();

	@Autowired
	AppSessionServiceImpl appSessionService;
	@Autowired
	AppCommand appCommand;
	@Autowired
	AppConfig appConfig;

	@MockBean
	CLIRunner cliRunner;
	@MockBean
	CommandLine commandLine;
	@MockBean
	FFmpegService ffmpegService;
	@MockBean
	EnvironmentVersion environmentVersion;
	@MockBean
	ScheduledExecutorService scheduledExecutorService;
	@MockBean
	ExecutableFinder executableFinder;
	@MockBean
	KeyPressToExit keyPressToExit;
	@MockBean
	MediaAnalyticsTransformerService mediaAnalyticsTransformerService;
	@MockBean
	DocumentationExporter documentationExporter;

	@Mock
	PrintWriter pw;
	@Mock
	CommandSpec commandSpec;
	@Mock
	UsageMessageSpec usageMessageSpec;

	ProcessFileCmd processFileCmd;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		appCommand.setVersion(false);
		appCommand.setOptions(false);
		appCommand.setAutocomplete(false);

		when(commandLine.getOut()).thenReturn(pw);
		when(commandLine.getCommandSpec()).thenReturn(commandSpec);
		when(commandSpec.usageMessage()).thenReturn(usageMessageSpec);
		when(usageMessageSpec.hidden()).thenReturn(true);

		when(ffmpegService.getVersions()).thenReturn(Map.of());
		when(ffmpegService.getMtdFiltersAvaliable()).thenReturn(Map.of());
		when(mediaAnalyticsTransformerService.getExportFormatInformation()).thenReturn(Map.of());

		processFileCmd = new ProcessFileCmd();
		processFileCmd.setInput(File.createTempFile("mediadeepa", ".tmp"));
		appCommand.setProcessFileCmd(null);
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(
				ffmpegService,
				environmentVersion,
				scheduledExecutorService,
				executableFinder,
				keyPressToExit,
				mediaAnalyticsTransformerService,
				documentationExporter);
	}

	@Test
	void testRunCli_nothing() throws IOException {
		assertThrows(ParameterException.class, () -> appSessionService.runCli());
	}

	@Test
	void testRunCli_version() throws IOException {
		appCommand.setVersion(true);
		assertEquals(0, appSessionService.runCli());
		verify(environmentVersion, atLeastOnce()).appVersion();
	}

	@Test
	void testRunCli_options() throws IOException {
		appCommand.setOptions(true);
		assertEquals(0, appSessionService.runCli());
		verify(ffmpegService, atLeastOnce()).getVersions();
		verify(ffmpegService, atLeastOnce()).getMtdFiltersAvaliable();
		verify(executableFinder, atLeastOnce()).get(appConfig.getFfmpegExecName());
		verify(executableFinder, atLeastOnce()).get(appConfig.getFfprobeExecName());
		verify(mediaAnalyticsTransformerService, atLeastOnce()).getExportFormatInformation();
	}

	@Test
	void testRunCli_autocomplete() throws IOException {
		appCommand.setAutocomplete(true);
		assertEquals(0, appSessionService.runCli());
		verify(environmentVersion, atLeastOnce()).appVersion();
		verify(commandLine, atLeastOnce()).getCommandSpec();
	}

	@Test
	void testRunCli_process_nothingToDo() throws IOException {
		appCommand.setProcessFileCmd(processFileCmd);
		assertThrows(ParameterException.class, () -> appSessionService.runCli());
	}

	@Test
	void testRunCli_cantCumulateOptions() throws IOException {
		appCommand.setProcessFileCmd(new ProcessFileCmd());
		appCommand.setExtractToCmd(new ExtractToCmd());
		appCommand.setExportToCmd(new ExportToCmd());
		appCommand.setImportFromCmd(new ImportFromCmd());
		assertThrows(ParameterException.class, () -> appSessionService.runCli());
	}

	@Test
	void testRunCli_cantCombinateImportExtractOptions() throws IOException {
		appCommand.setImportFromCmd(new ImportFromCmd());
		appCommand.setExtractToCmd(new ExtractToCmd());
		assertThrows(ParameterException.class, () -> appSessionService.runCli());
	}

	@Test
	void testRunCli_exportFormatEmpty() throws IOException {
		appCommand.setProcessFileCmd(processFileCmd);
		final var exportToCmd = new ExportToCmd();
		exportToCmd.setExport(new File(""));
		appCommand.setExportToCmd(exportToCmd);
		assertThrows(ParameterException.class, () -> appSessionService.runCli());

		exportToCmd.setFormat(Set.of());
		assertThrows(ParameterException.class, () -> appSessionService.runCli());

		exportToCmd.setFormat(Set.of("Something"));
		assertThrows(ParameterException.class, () -> appSessionService.runCli());
	}

	@Test
	void testValidateInputFile() {
		assertThrows(ParameterException.class, () -> appSessionService.validateInputFile(null));

		final var notFoundFile = new File(faker.numerify("file###"));
		assertThrows(ParameterException.class,
				() -> appSessionService.validateInputFile(notFoundFile));

		final var dirFile = File.listRoots()[0];
		assertTrue(dirFile.exists());
		assertThrows(ParameterException.class,
				() -> appSessionService.validateInputFile(dirFile));
	}

	@Test
	void testValidateOutputFile() {
		assertThrows(ParameterException.class, () -> appSessionService.validateOutputFile(null));

		final var dirFile = File.listRoots()[0];
		assertTrue(dirFile.exists());
		assertThrows(ParameterException.class,
				() -> appSessionService.validateOutputFile(dirFile));
	}

	@Test
	void testValidateOutputDir() {
		assertThrows(ParameterException.class, () -> appSessionService.validateOutputDir(null));

		final var dirFile = new File("../../../../../../../../../../../../../../../../../../../../../?dirnotexists");
		assertFalse(dirFile.exists());
		assertThrows(UncheckedIOException.class,
				() -> appSessionService.validateOutputDir(dirFile));

		final var regularFile = processFileCmd.getInput();
		assertTrue(regularFile.exists());
		assertThrows(ParameterException.class,
				() -> appSessionService.validateOutputDir(regularFile));
	}

}
