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
package media.mexm.mediadeepa.exportformat.tables;

import static java.util.Locale.US;
import static media.mexm.mediadeepa.components.CLIRunner.makeOutputFileName;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;

import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.tables.TableDocument.Table;

@Slf4j
public class TableODSExportFormat extends TableExportFormat {

	public static final String SUFFIX_FILE_NAME = "media-datas.ods";

	@Override
	public String getFormatLongName() {
		return "OpenDocument Spreadsheet";
	}

	// TODO readme

	@Override
	public void save(final DataResult result,
					 final List<Table> tables,
					 final File exportDirectory,
					 final String baseFileName) {
		try (final var sd = OdfSpreadsheetDocument.newSpreadsheetDocument()) {
			sd.setLocale(US);
			final var now = System.currentTimeMillis();
			log.debug("Start export {} tables to {} ODS file...", tables.size(), baseFileName);

			tables.forEach(table -> makeTable(sd, table, baseFileName));

			log.debug("Export done to {} ODS file in {} sec",
					baseFileName,
					Duration.ofMillis(System.currentTimeMillis() - now).toSeconds());

			final var outputFile = new File(exportDirectory, makeOutputFileName(baseFileName, SUFFIX_FILE_NAME));
			log.info("Save to {}", outputFile);
			sd.save(outputFile);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't manage SpreadsheetDocument", e);
		} catch (final Exception e) {
			log.error("Can't manage SpreadsheetDocument", e);
		}
	}

	private static void makeTable(final OdfSpreadsheetDocument sd, final Table table, final String baseFileName) {
		final var rows = table.getRows();
		final var header = table.getHeader();
		final var w = header.size();
		final var h = rows.size() + 1;

		log.debug("Create table {} ({} x {} cells) on {}",
				table.getTableName(), w, h, baseFileName);
		final var odftable = OdfTable.newTable(sd, 1, w);
		odftable.setTableName(table.getTableName());

		for (var pos = 0; pos < header.size(); pos++) {
			final var odfCell = odftable.getCellByPosition(pos, 0);
			odfCell.setStringValue(header.get(pos));
		}

		for (var posRow = 0; posRow < rows.size(); posRow++) {
			final var odfRow = odftable.getRowByIndex(posRow + 1);
			final var cells = rows.get(posRow).getCells();
			for (var posCell = 0; posCell < cells.size(); posCell++) {
				final var odfCell = odfRow.getCellByIndex(posCell);
				final var cell = cells.get(posCell);

				if (cell instanceof final TableCellString cellContent) {
					odfCell.setStringValue(cellContent.value());
				} else if (cell instanceof final TableCellFloat cellContent) {
					odfCell.setDoubleValue((double) cellContent.value());
				} else if (cell instanceof final TableCellLong cellContent2) {
					odfCell.setDoubleValue((double) cellContent2.value());
				} else if (cell instanceof final TableCellInteger cellContent3) {
					odfCell.setDoubleValue((double) cellContent3.value());
				} else if (cell instanceof TableCellNull) {
					/**
					 * Skip cell creation
					 */
				} else {
					throw new IllegalArgumentException("Can't manage type " + cell.getClass());
				}
			}
		}

		odftable.getColumnList().forEach(c -> c.setUseOptimalWidth(true));
	}

}
