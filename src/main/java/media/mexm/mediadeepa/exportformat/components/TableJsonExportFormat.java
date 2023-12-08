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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.cli.ExportToCmd;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.OutputStreamProvider;
import media.mexm.mediadeepa.exportformat.TableCellFloat;
import media.mexm.mediadeepa.exportformat.TableCellInteger;
import media.mexm.mediadeepa.exportformat.TableCellLong;
import media.mexm.mediadeepa.exportformat.TableCellNull;
import media.mexm.mediadeepa.exportformat.TableCellString;
import media.mexm.mediadeepa.exportformat.TableDocument.Table;
import media.mexm.mediadeepa.exportformat.TableDocument.Table.Row;
import media.mexm.mediadeepa.exportformat.TableExportFormat;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;

@Slf4j
@Component
public class TableJsonExportFormat extends TableExportFormat implements OutputStreamProvider {

	private final JsonFactory jsonFactory;
	private final AppConfig appConfig;

	public TableJsonExportFormat(@Autowired final List<TableRendererEngine> engines,
								 @Autowired final AppConfig appConfig,
								 @Autowired final NumberUtils numberUtils) {
		super(engines, numberUtils);
		jsonFactory = new JsonFactory();
		this.appConfig = appConfig;
	}

	@Override
	public String getFormatName() {
		return "json";
	}

	@Override
	public String getFormatLongName() {
		return "JSON Document";
	}

	@Override
	public File save(final DataResult result, final List<Table> tables, final ExportToCmd exportToCmd) {
		final var outputFile = exportToCmd.makeOutputFile(appConfig.getJsonTableFileName());
		log.debug("Start export {} tables to {}...", tables.size(), outputFile);

		try (final var json = jsonFactory.createGenerator(createOutputStream(outputFile), JsonEncoding.UTF8)) {
			json.writeStartObject();
			json.writeFieldName("report");
			json.writeStartObject();
			for (var posTable = 0; posTable < tables.size(); posTable++) {
				makeTable(tables, json, posTable);
			}
			json.writeEndObject();
			json.writeEndObject();
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write Json file", e);
		}

		return outputFile;
	}

	private void makeTable(final List<Table> tables,
						   final JsonGenerator json,
						   final int posTable) throws IOException {
		final var table = tables.get(posTable);
		final var rows = table.getRows();
		final var header = table.getHeader();

		log.debug("Add {} rows to \"{}\" sheet in XML", rows.size(), table.getTableName());
		json.writeFieldName(table.getTableName());

		json.writeStartArray();
		for (var posRow = 0; posRow < rows.size(); posRow++) {
			makeRow(json, rows, header, posRow);
		}
		json.writeEndArray();
	}

	private void makeRow(final JsonGenerator json,
						 final List<Row> rows,
						 final List<String> header,
						 final int posRow) throws IOException {
		final var row = rows.get(posRow);
		json.writeStartObject();

		final var cells = row.getCells();
		for (var posCell = 0; posCell < cells.size(); posCell++) {
			final var cell = cells.get(posCell);
			final var headerName = header.get(posCell);

			if (cell instanceof final TableCellString cellContent) {
				json.writeStringField(headerName, cellContent.value());
			} else if (cell instanceof final TableCellFloat cellContent) {
				json.writeNumberField(headerName, cellContent.value());
			} else if (cell instanceof final TableCellLong cellContent2) {
				json.writeNumberField(headerName, cellContent2.value());
			} else if (cell instanceof final TableCellInteger cellContent3) {
				json.writeNumberField(headerName, cellContent3.value());
			} else if (cell instanceof TableCellNull) {
				json.writeNullField(headerName);
			} else {
				throw new IllegalArgumentException("Can't manage type " + cell.getClass());
			}
		}
		json.writeEndObject();
	}

}
