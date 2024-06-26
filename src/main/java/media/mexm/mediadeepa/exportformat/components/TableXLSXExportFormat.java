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

import static org.apache.poi.ss.usermodel.CellType.BLANK;
import static org.apache.poi.ss.usermodel.CellType.NUMERIC;
import static org.apache.poi.ss.usermodel.CellType.STRING;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.components.OutputFileSupplier;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.TableCellFloat;
import media.mexm.mediadeepa.exportformat.TableCellInteger;
import media.mexm.mediadeepa.exportformat.TableCellLong;
import media.mexm.mediadeepa.exportformat.TableCellNull;
import media.mexm.mediadeepa.exportformat.TableCellString;
import media.mexm.mediadeepa.exportformat.TableDocument.Table;
import media.mexm.mediadeepa.exportformat.TableExportFormat;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;

@Slf4j
@Component
public class TableXLSXExportFormat extends TableExportFormat {

	private final AppConfig appConfig;

	public TableXLSXExportFormat(@Autowired final List<TableRendererEngine> engines,
								 @Autowired final AppConfig appConfig,
								 @Autowired final NumberUtils numberUtils,
								 @Autowired final OutputFileSupplier outputFileSupplier) {
		super(engines, numberUtils, outputFileSupplier);
		this.appConfig = appConfig;
	}

	@Override
	public String getFormatName() {
		return "xlsx";
	}

	@Override
	public String getFormatLongName() {
		return "XLSX Spreadsheet";
	}

	@Override
	public String getFormatDescription() {
		return "simple raw data export, one tabs by export result";
	}

	@Override
	public void makeDocument(final DataResult result,
							 final List<Table> tables,
							 final OutputStream outputStream) {
		try (final var wb = new SXSSFWorkbook()) {
			wb.setCompressTempFiles(false);
			tables.forEach(table -> makeTableOnWorkbook(wb, table));
			wb.write(outputStream);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't close Workbook", e);
		}
	}

	private static void makeTableOnWorkbook(final SXSSFWorkbook wb, final Table table) {
		final var sSheet = wb.createSheet(table.getTableName());

		final var sRowHeader = sSheet.createRow(0);
		final var headers = table.getHeader();
		for (var pos = 0; pos < headers.size(); pos++) {
			final var sCellHeader = sRowHeader.createCell(pos, STRING);
			sCellHeader.setCellValue(headers.get(pos));
		}

		final var rows = table.getRows();
		log.debug("Add {} rows to \"{}\" sheet in XLSX", rows.size(), table.getTableName());

		for (var posRow = 0; posRow < rows.size(); posRow++) {
			final var sRow = sSheet.createRow(posRow + 1);
			final var cells = rows.get(posRow).getCells();

			for (var posCell = 0; posCell < cells.size(); posCell++) {
				final var sCell = sRow.createCell(posCell);
				final var cell = cells.get(posCell);

				if (cell instanceof final TableCellString cellContent) {
					sCell.setCellType(STRING);
					sCell.setCellValue(cellContent.value());
				} else if (cell instanceof final TableCellFloat cellContent) {
					sCell.setCellType(NUMERIC);
					sCell.setCellValue(cellContent.value());
				} else if (cell instanceof final TableCellLong cellContent2) {
					sCell.setCellType(NUMERIC);
					sCell.setCellValue(cellContent2.value());
				} else if (cell instanceof final TableCellInteger cellContent3) {
					sCell.setCellType(NUMERIC);
					sCell.setCellValue(cellContent3.value());
				} else if (cell instanceof TableCellNull) {
					sCell.setCellType(BLANK);
				} else {
					throw new IllegalArgumentException("Can't manage type " + cell.getClass());
				}
			}
		}

		sSheet.trackAllColumnsForAutoSizing();
		for (var pos = 0; pos < headers.size(); pos++) {
			sSheet.autoSizeColumn(pos);
		}
		sSheet.untrackAllColumnsForAutoSizing();

		try {
			log.trace("Flush export \"{}\" sheet in XLSX", table.getTableName());
			sSheet.flushRows();
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't flush " + table.getTableName(), e);
		}
	}

	@Override
	public String getInternalFileName() {
		return appConfig.getXslxtableFileName();
	}

}
