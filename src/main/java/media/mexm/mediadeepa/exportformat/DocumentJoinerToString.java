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
 * Copyright (C) hdsdi3g for hd3g.tv 2023
 *
 */
package media.mexm.mediadeepa.exportformat;

import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DocumentJoinerToString implements DocumentJoiner<String> {
	private static Logger log = LogManager.getLogger();

	@Override
	public String emptyDocument() {
		return "";
	}

	@Override
	public String assembleDocument(final List<String> h, final List<List<String>> l) {
		final var head = h.stream().collect(joining("\t"));
		final var doc = l.stream()
				.map(r -> r.stream().collect(joining("\t")))
				.collect(joining(lineSeparator()));
		return head + lineSeparator() + doc;
	}

	@Override
	public void save(final String fileName, final File exportDirectory, final String document) {
		if (document.isEmpty()) {
			log.debug("Nothing to save to {}", fileName);
			return;
		}
		log.info("Save to {}", fileName);
		try {
			FileUtils.write(
					new File(exportDirectory, fileName),
					document,
					UTF_8,
					false);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write file", e);
		}
	}

}
