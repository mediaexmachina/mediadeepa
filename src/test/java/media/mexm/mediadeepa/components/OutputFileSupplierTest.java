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
 * Copyright (C) Media ex Machina 2024
 *
 */
package media.mexm.mediadeepa.components;

import static java.io.File.separator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import media.mexm.mediadeepa.cli.AppCommand;
import media.mexm.mediadeepa.cli.ExportToCmd;
import media.mexm.mediadeepa.cli.OutputCmd;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.service.AppSessionService;
import net.datafaker.Faker;
import picocli.CommandLine;

@SpringBootTest
class OutputFileSupplierTest {
	static Faker faker = net.datafaker.Faker.instance();

	@Autowired
	OutputFileSupplier ofs;
	@Autowired
	AppConfig appConfig;

	@MockBean
	DocumentationExporter documentationExporter;
	@MockBean
	CLIRunner cliRunner;
	@MockBean
	CommandLine commandLine;
	@MockBean
	AppCommand appCommand;
	@MockBean
	AppSessionService appSessionService;

	@Mock
	DataResult result;
	@Mock
	OutputCmd outputCmd;
	@Mock
	ExportToCmd exportToCmd;

	String suffix;
	String baseFileName;
	File export;
	String inputFile;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();

		suffix = faker.numerify("suffix###");
		baseFileName = faker.numerify("baseFileName###");

		inputFile = faker.numerify("inputDir###")
					+ separator
					+ faker.numerify("inputFile###")
					+ "." + faker.numerify("ext###");
		when(appCommand.getInput()).thenReturn(List.of(inputFile));
		when(appCommand.getOutputCmd()).thenReturn(outputCmd);
		when(outputCmd.getExportToCmd()).thenReturn(exportToCmd);

		export = new File(faker.numerify("export###"));
		when(exportToCmd.getExport()).thenReturn(export);
	}

	@AfterEach
	void ends() {
		verify(appCommand, atLeast(0)).getInput();
		verify(appCommand, atLeast(0)).getInputList();
		verify(appCommand, atLeastOnce()).getOutputCmd();
		verify(outputCmd, atLeastOnce()).getExportToCmd();
		verify(exportToCmd, atLeastOnce()).getExport();
		verify(exportToCmd, atLeastOnce()).getBaseFileName();
		verify(result, atLeast(0)).isInMultipleSourcesSet();

		verifyNoMoreInteractions(result, appCommand, outputCmd, exportToCmd);

		appConfig.setAddSourceExtToOutputDirectories(false);
	}

	@Test
	void testMakeOutputFile_noBaseFileName() {
		assertEquals(new File(export, suffix), ofs.makeOutputFile(result, suffix));
	}

	@Test
	void testMakeOutputFile_simpleBaseFileName() {
		when(exportToCmd.getBaseFileName()).thenReturn(baseFileName);
		assertEquals(new File(export, baseFileName + "_" + suffix), ofs.makeOutputFile(result, suffix));
	}

	@ParameterizedTest
	@ValueSource(strings = { "_", " ", "-", "|" })
	void testMakeOutputFile_separatedBaseFileName(final String sep) {
		when(exportToCmd.getBaseFileName()).thenReturn(baseFileName + sep);
		assertEquals(new File(export, baseFileName + sep + suffix), ofs.makeOutputFile(result, suffix));
	}

	@Nested
	class MultipleSources {

		String inputFileName;

		@BeforeEach
		void init() {
			inputFileName = FilenameUtils.getBaseName(inputFile);
			when(appCommand.getInput()).thenReturn(List.of(inputFile, inputFile));
			when(result.getSource()).thenReturn(inputFile);
			when(result.isInMultipleSourcesSet()).thenReturn(true);
		}

		@AfterEach
		void ends() {
			verify(result, atLeastOnce()).getSource();
			verify(result, atLeastOnce()).isInMultipleSourcesSet();
			appConfig.setAddSourceExtToOutputDirectories(false);
		}

		@Test
		void testMakeOutputFile_noBaseFileName() {
			assertEquals(new File(export, inputFileName + "_" + suffix),
					ofs.makeOutputFile(result, suffix));
		}

		@Test
		void testMakeOutputFile_simpleBaseFileName() {
			when(exportToCmd.getBaseFileName()).thenReturn(baseFileName);
			assertEquals(new File(export, inputFileName + "_" + baseFileName + "_" + suffix),
					ofs.makeOutputFile(result, suffix));
		}

		@ParameterizedTest
		@ValueSource(strings = { "_", " ", "-", "|" })
		void testMakeOutputFile_separatedBaseFileName(final String sep) {
			when(exportToCmd.getBaseFileName()).thenReturn(baseFileName + sep);
			assertEquals(new File(export, inputFileName + "_" + baseFileName + sep + suffix),
					ofs.makeOutputFile(result, suffix));
		}

	}

	@Nested
	class MultipleSourcesWithExt extends MultipleSources {

		@Override
		@BeforeEach
		void init() {
			super.init();
			inputFileName = FilenameUtils.getName(inputFile).replace(".", "-");
			appConfig.setAddSourceExtToOutputDirectories(true);
		}

	}
}
