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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class DocumentParserServiceImpl implements DocumentParserService {

	@Autowired
	private Parser markdownParser;
	@Autowired
	private HtmlRenderer htmlRenderer;

	@Override
	public Node markdownParse(final String mdDocument) {
		return markdownParser.parse(mdDocument);
	}

	@Override
	public String htmlRender(final Node document) {
		return htmlRenderer.render(document);
	}

	@Override
	public String getDocContent(final String fileName) {
		final var r = new ClassPathResource("doc/en/" + fileName, this.getClass().getClassLoader());
		try {
			return r.getContentAsString(StandardCharsets.UTF_8);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't found/open " + fileName, e);
		}
	}

}
