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
package media.mexm.mediadeepa.exportformat.components;

import static com.opencsv.ICSVParser.DEFAULT_ESCAPE_CHARACTER;
import static com.opencsv.ICSVParser.DEFAULT_QUOTE_CHARACTER;
import static com.opencsv.ICSVParser.DEFAULT_SEPARATOR;
import static com.opencsv.ICSVWriter.RFC4180_LINE_END;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.StringWriter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.opencsv.CSVWriterBuilder;

import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.components.OutputFileSupplier;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;

@Component
public class TabularCSVExportFormat extends TabularTXTExportFormat {

	public TabularCSVExportFormat(@Autowired final List<TabularRendererEngine> engines,
								  @Autowired final NumberUtils numberUtils,
								  @Autowired final OutputFileSupplier outputFileSupplier) {
		super(engines, numberUtils, outputFileSupplier);
	}

	@Override
	public String getFormatName() {
		return "csv";
	}

	@Override
	public String getFormatLongName() {
		return "Classic CSV files";
	}

	@Override
	public String getDocumentFileExtension() {
		return "csv";
	}

	@Override
	public String getFormatDescription() {
		return "comma separated, \"`.`\" decimal separator";
	}

	@Override
	public byte[] getDocument(final List<String> header, final List<List<String>> lines) {
		final var writer = new StringWriter();
		final var csvWriter = prepareCSVWriter(new CSVWriterBuilder(writer)).build();

		final var nextLine = new String[header.size()];
		toArray(header, nextLine);
		csvWriter.writeNext(nextLine, false);

		lines.forEach(l -> {
			toArray(l, nextLine);
			csvWriter.writeNext(nextLine, false);
		});

		csvWriter.flushQuietly();
		return writer.toString().getBytes(UTF_8);
	}

	protected CSVWriterBuilder prepareCSVWriter(final CSVWriterBuilder csvWriter) {
		return csvWriter
				.withSeparator(DEFAULT_SEPARATOR)
				.withQuoteChar(DEFAULT_QUOTE_CHARACTER)
				.withEscapeChar(DEFAULT_ESCAPE_CHARACTER)
				.withLineEnd(RFC4180_LINE_END);
	}

	private static void toArray(final List<String> data, final String[] array) {
		for (var pos = 0; pos < array.length; pos++) {
			array[pos] = data.get(pos);
		}
	}

}
