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

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class DocumentationExporterTest {

	@Autowired
	DocumentationExporter documentationExporter;

	@MockBean
	CLIRunner cliRunner;

	@Test
	void testExportManPage() throws IOException {
		final var tempMan = File.createTempFile("mediadeepa-test", ".man");
		forceDelete(tempMan);
		documentationExporter.exportManPage(tempMan);

		assertThat(tempMan)
				.exists()
				.size()
				.isGreaterThan(10)
				.returnToFile()
				.content()
				.contains("ffmpeg", "mediadeepa", "\\-\\-export");
		forceDelete(tempMan);
	}

	@Test
	void testExportReadme() throws IOException {
		final var tempReadme = File.createTempFile("mediadeepa-test", ".md");
		forceDelete(tempReadme);
		documentationExporter.exportReadmeProjectMarkdown(tempReadme);

		assertThat(tempReadme)
				.exists()
				.size()
				.isGreaterThan(10)
				.returnToFile()
				.content()
				.contains("ffmpeg", "mediadeepa", "sonarcloud");
		forceDelete(tempReadme);
	}

	@Test
	void testExportWebpage() throws IOException {
		final var tempWebsite = File.createTempFile("mediadeepa-test", ".html");
		if (tempWebsite.exists()) {
			forceDelete(tempWebsite);
		}
		documentationExporter.exportProjectPage(tempWebsite);

		assertThat(tempWebsite)
				.exists()
				.size()
				.isGreaterThan(10)
				.returnToFile()
				.content()
				.contains("ffmpeg", "mediadeepa", "sonarcloud", "xlsx", "-h");
		forceDelete(tempWebsite);
	}

}
