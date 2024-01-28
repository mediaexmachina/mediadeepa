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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import media.mexm.mediadeepa.cli.ExportToCmd;
import media.mexm.mediadeepa.components.CLIRunner;
import media.mexm.mediadeepa.components.DocumentationExporter;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.components.DemoExportFormat;
import net.datafaker.Faker;

@SpringBootTest
class MediaAnalyticsTransformerServiceTest {
	static Faker faker = net.datafaker.Faker.instance();

	@Autowired
	MediaAnalyticsTransformerService matService;
	@Autowired
	DemoExportFormat demoExportFormat;

	@MockBean
	CLIRunner cliRunner;
	@MockBean
	DocumentationExporter documentationExporter;
	@Mock
	DataResult dataResult;

	String notExistsFormat;
	ExportToCmd exportToCmd;
	String formatName;
	String formatLongName;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		notExistsFormat = faker.numerify("notExistsFormat###");
		formatName = demoExportFormat.getFormatName();
		formatLongName = demoExportFormat.getFormatLongName();
		exportToCmd = new ExportToCmd();
		exportToCmd.setFormat(Set.of(formatName));
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(dataResult, documentationExporter);
	}

	@Test
	void testIsExportFormatExists() {
		assertFalse(matService.isExportFormatExists(notExistsFormat));
		assertFalse(matService.isExportFormatExists(formatLongName));
		assertTrue(matService.isExportFormatExists(formatName));
		assertTrue(matService.isExportFormatExists(formatName.toUpperCase()));
	}

	@Test
	void testExportAnalytics() {
		matService.exportAnalytics(dataResult, exportToCmd);
		assertThat(demoExportFormat.getCapturedResults())
				.isEqualTo(List.of(dataResult));
	}

}
