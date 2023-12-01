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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.UncheckedIOException;

import org.commonmark.node.Heading;
import org.commonmark.node.Paragraph;
import org.commonmark.node.StrongEmphasis;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import media.mexm.mediadeepa.components.CLIRunner;
import media.mexm.mediadeepa.components.DocumentationExporter;

@SpringBootTest
class DocumentParserServiceTest {

	@MockBean
	CLIRunner cliRunner;
	@Autowired
	DocumentParserService documentParserService;
	@MockBean
	DocumentationExporter documentationExporter;

	@Test
	void testMarkdownParse() {
		final var node = documentParserService.markdownParse("""
				# Title
				**bold**
				""");
		assertThat(node)
				.isNotNull();
		assertThat(node.getFirstChild())
				.isNotNull().isInstanceOf(Heading.class);
		assertThat(node.getLastChild())
				.isNotNull().isInstanceOf(Paragraph.class);
		assertThat(node.getLastChild().getFirstChild())
				.isNotNull().isInstanceOf(StrongEmphasis.class);
	}

	@Test
	void testHtmlRender() {
		final var node = documentParserService.markdownParse("""
				# Title
				**bold**
				""");
		assertEquals("""
				<h1>Title</h1>
				<p><strong>bold</strong></p>
				""", documentParserService.htmlRender(node));
	}

	@Test
	void testGetDocContent() {
		final var doc = documentParserService.getDocContent("about.md");
		assertThat(doc).isNotNull().isNotBlank();
		assertThrows(UncheckedIOException.class, () -> documentParserService.getDocContent("nope"));
	}

}
