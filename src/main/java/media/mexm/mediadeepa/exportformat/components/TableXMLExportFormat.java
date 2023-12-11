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

import static media.mexm.mediadeepa.exportformat.components.TableSQLiteExportFormat.cleanNameToFieldName;

import java.io.File;
import java.util.List;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

@Component
@Slf4j
public class TableXMLExportFormat extends TableExportFormat implements OutputStreamProvider {

	private static final String REPORT = "report";

	private final AppConfig appConfig;
	private final XMLEventFactory xml;

	public TableXMLExportFormat(@Autowired final List<TableRendererEngine> engines,
								@Autowired final AppConfig appConfig,
								@Autowired final NumberUtils numberUtils) {
		super(engines, numberUtils);
		this.appConfig = appConfig;
		xml = XMLEventFactory.newInstance();
	}

	@Override
	public String getFormatName() {
		return "xml";
	}

	@Override
	public String getFormatLongName() {
		return "XML Document";
	}

	@Override
	public File save(final DataResult result, final List<Table> tables, final ExportToCmd exportToCmd) {
		final var outputFile = exportToCmd.makeOutputFile(appConfig.getXmltableFileName());
		try {
			log.debug("Start export {} tables to {}...", tables.size(), outputFile);

			final var writer = XMLOutputFactory.newInstance()
					.createXMLEventWriter(createOutputStream(outputFile));
			writer.add(xml.createStartDocument());
			writer.add(xml.createStartElement("", null, REPORT));

			for (var posTable = 0; posTable < tables.size(); posTable++) {
				makeTable(tables, writer, posTable);
			}

			writer.add(xml.createEndElement("", null, REPORT));
			writer.add(xml.createEndDocument());

			writer.close();
		} catch (final XMLStreamException e) {
			throw new IllegalStateException("Can't write XML file", e);
		}

		return outputFile;
	}

	private void makeTable(final List<Table> tables,
						   final XMLEventWriter writer,
						   final int posTable) throws XMLStreamException {
		final var table = tables.get(posTable);
		final var rows = table.getRows();
		log.debug("Add {} rows to \"{}\" sheet in XML", rows.size(), table.getTableName());

		writer.add(xml.createStartElement("", null, "table"));
		writer.add(xml.createAttribute("", null, "name", cleanNameToFieldName(table.getTableName())));
		writer.add(xml.createAttribute("", null, "long-name", table.getTableName()));

		writer.add(xml.createStartElement("", null, "headers"));
		final var header = table.getHeader();
		final var rowTypes = table.getRowTypes();
		for (var pos = 0; pos < header.size(); pos++) {
			final var headerLongName = header.get(pos);
			final var type = rowTypes.get(pos);
			writer.add(xml.createStartElement("", null, "header"));
			writer.add(xml.createAttribute("", null, "name", cleanNameToFieldName(headerLongName)));
			writer.add(xml.createAttribute("", null, "long-name", headerLongName));

			if (type == TableCellString.class) {
				writer.add(xml.createAttribute("", null, "type", "string"));
			} else if (type == TableCellFloat.class) {
				writer.add(xml.createAttribute("", null, "type", "float"));
			} else if (type == TableCellLong.class) {
				writer.add(xml.createAttribute("", null, "type", "long"));
			} else if (type == TableCellInteger.class) {
				writer.add(xml.createAttribute("", null, "type", "int"));
			} else {
				writer.add(xml.createAttribute("", null, "type", "undefinited"));
			}
			writer.add(xml.createEndElement("", null, "header"));
		}
		writer.add(xml.createEndElement("", null, "headers"));

		for (var posRow = 0; posRow < rows.size(); posRow++) {
			makeRow(writer, header, rows, posRow);
		}

		writer.add(xml.createEndElement("", null, "table"));
	}

	private void makeRow(final XMLEventWriter writer,
						 final List<String> header,
						 final List<Row> rows,
						 final int posRow) throws XMLStreamException {
		final var row = rows.get(posRow);
		writer.add(xml.createStartElement("", null, "entry"));

		final var cells = row.getCells();
		for (var posCell = 0; posCell < cells.size(); posCell++) {
			final var cell = cells.get(posCell);
			final var headerName = cleanNameToFieldName(header.get(posCell));

			if (cell instanceof final TableCellString cellContent) {
				writer.add(xml.createAttribute("", null, headerName, cellContent.value()));
			} else if (cell instanceof final TableCellFloat cellContent) {
				writer.add(xml.createAttribute("", null, headerName,
						numberUtils.formatDecimalSimple5En(cellContent.value())));
			} else if (cell instanceof final TableCellLong cellContent2) {
				writer.add(xml.createAttribute("", null, headerName,
						String.valueOf(cellContent2.value())));
			} else if (cell instanceof final TableCellInteger cellContent3) {
				writer.add(xml.createAttribute("", null, headerName,
						String.valueOf(cellContent3.value())));
			} else if (cell instanceof TableCellNull) {
				writer.add(xml.createAttribute("", null, headerName, ""));
			} else {
				throw new IllegalArgumentException("Can't manage type " + cell.getClass());
			}
		}
		writer.add(xml.createEndElement("", null, "entry"));
	}

}
