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
package media.mexm.mediadeepa.rendererengine.components;

import static media.mexm.mediadeepa.rendererengine.components.AboutMeasureRendererEngine.ABOUT_NAME;
import static media.mexm.mediadeepa.rendererengine.components.AboutMeasureRendererEngine.FILTERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import media.mexm.mediadeepa.RunnedJavaCmdLine;
import media.mexm.mediadeepa.cli.AppCommand;
import media.mexm.mediadeepa.components.CLIRunner;
import media.mexm.mediadeepa.components.DocumentationExporter;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.TabularExportFormat;
import media.mexm.mediadeepa.service.AppSessionService;
import net.datafaker.Faker;
import picocli.CommandLine;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;

@SpringBootTest
class AboutMeasureRendererEngineTest {
	static Faker faker = net.datafaker.Faker.instance();

	@Autowired
	AboutMeasureRendererEngine aboutMeasureRendererEngine;

	@MockBean
	CLIRunner c;
	@MockBean
	CommandLine commandLine;
	@MockBean
	AppCommand appCommand;
	@MockBean
	AppSessionService appSessionService;
	@MockBean
	DocumentationExporter documentationExporter;
	@MockBean
	RunnedJavaCmdLine runnedJavaCmdLine;

	@Mock
	TabularExportFormat tabularExportFormat;
	@Mock
	DataResult result;
	@Mock
	MediaAnalyserResult mediaAnalyserResult;
	String internalTabularBaseFileName;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		internalTabularBaseFileName = faker.numerify("internalTabularBaseFileName###");
		when(runnedJavaCmdLine.makeArchiveCommandline()).thenReturn(Optional.empty());
		when(result.getVersions()).thenReturn(Map.of());
		when(result.getMediaAnalyserResult()).thenReturn(Optional.ofNullable(mediaAnalyserResult));
		when(mediaAnalyserResult.filters()).thenReturn(Set.of());
	}

	@AfterEach
	void end() {
		verifyNoMoreInteractions(
				tabularExportFormat,
				result,
				runnedJavaCmdLine,
				mediaAnalyserResult);
	}

	@Test
	void testToSingleTabularDocument_nothing() {
		final var std = aboutMeasureRendererEngine.toSingleTabularDocument(
				internalTabularBaseFileName,
				result,
				tabularExportFormat);
		assertThat(std).isEmpty();
	}

	@Test
	void testToSingleTabularDocument_about() {
		final var std = aboutMeasureRendererEngine.toSingleTabularDocument(
				ABOUT_NAME,
				result,
				tabularExportFormat);
		assertThat(std).isNotEmpty();

		verify(runnedJavaCmdLine, times(1)).makeFullExtendedCommandline();
		verify(runnedJavaCmdLine, times(1)).makeArchiveCommandline();
		verify(result, times(1)).getVersions();
	}

	@Test
	void testToSingleTabularDocument_filters() {
		final var std = aboutMeasureRendererEngine.toSingleTabularDocument(
				FILTERS,
				result,
				tabularExportFormat);
		assertThat(std).isNotEmpty();

		verify(result, times(1)).getMediaAnalyserResult();
		verify(mediaAnalyserResult, times(1)).filters();
	}

}
