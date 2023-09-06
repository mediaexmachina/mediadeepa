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
package media.mexm.mediadeepa.exportformat.tabular;

import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import java.text.DecimalFormat;
import java.util.List;

public class TabularTXTExportFormat extends TabularExportFormat {

	protected final DecimalFormat highFormat;
	protected final DecimalFormat lowFormat;

	public TabularTXTExportFormat() {
		highFormat = TabularDocumentExporter.getENHighDecimalFormat();
		lowFormat = TabularDocumentExporter.getENLowDecimalFormat();
	}

	@Override
	public String getFormatLongName() {
		return "Values separated by tabs in text files";
	}

	@Override
	public String getDocumentFileExtension() {
		return "txt";
	}

	@Override
	public String formatToString(final float value, final boolean lowPrecison) {
		if (lowPrecison) {
			return lowFormat.format(value);
		} else {
			return highFormat.format(value);
		}
	}

	@Override
	public byte[] getDocument(final List<String> header, final List<List<String>> lines) {
		final var head = header.stream().collect(joining("\t"));
		final var doc = lines.stream()
				.map(r -> r.stream().collect(joining("\t")))
				.collect(joining(lineSeparator()));
		final var document = head + lineSeparator() + doc + lineSeparator();
		return document.getBytes(UTF_8);
	}

}
