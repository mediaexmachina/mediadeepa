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
 * Copyright (C) Media ex Machina 2023
 *
 */
package media.mexm.mediadeepa.exportformat;

import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import java.util.List;

public class TabularTXTExportFormat extends TabularExportFormat {

	@Override
	public byte[] getDocument(final List<String> header, final List<List<String>> lines) {
		final var head = header.stream().collect(joining("\t"));
		final var doc = lines.stream()
				.map(r -> r.stream().collect(joining("\t")))
				.collect(joining(lineSeparator()));
		final var document = head + lineSeparator() + doc;
		return document.getBytes(UTF_8);
	}

}
