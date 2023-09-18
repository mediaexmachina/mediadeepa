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

import static java.util.stream.Collectors.joining;
import static media.mexm.mediadeepa.components.CLIRunner.makeOutputFileName;

import java.io.File;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.StringJoiner;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.JournalMode;
import org.sqlite.SQLiteConfig.SynchronousMode;
import org.sqlite.SQLiteConfig.TempStore;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.tables.TableDocument.Table;

@Slf4j
public class TableSQLiteExportFormat extends TableExportFormat {

	public static final String SUFFIX_FILE_NAME = "media-datas.sqlite";

	@Override
	public String getFormatLongName() {
		return "SQLite database";
	}

	@Override
	public void save(final DataResult result,
					 final List<Table> tables,
					 final File exportDirectory,
					 final String baseFileName) {
		final var outputFile = new File(exportDirectory, makeOutputFileName(baseFileName, SUFFIX_FILE_NAME));
		final var url = "jdbc:sqlite:" + outputFile.getPath().replace('\\', '/');

		final var sqliteConfig = new SQLiteConfig();
		sqliteConfig.enableFullSync(false);
		sqliteConfig.enableLoadExtension(false);
		sqliteConfig.setJournalMode(JournalMode.OFF);
		sqliteConfig.setSynchronous(SynchronousMode.OFF);
		sqliteConfig.setTempStore(TempStore.MEMORY);

		try (final var connection = sqliteConfig.createConnection(url)) {
			try (final var statement = connection.createStatement()) {
				statement.setQueryTimeout(240);

				tables.forEach(table -> {
					try {
						makeTableOnSQL(table, statement);
					} catch (final SQLException e) {
						throw new IllegalStateException("Can't create/update SQLlite table " + table.getTableName(), e);
					}
				});
			}
		} catch (final SQLException e) {
			throw new IllegalStateException("Can't operate SQLlite file", e);
		}
	}

	private static void makeTableOnSQL(final Table table, final Statement statement) throws SQLException {
		final var tableName = cleanNameToFieldName(table.getTableName());
		statement.executeUpdate("DROP TABLE IF EXISTS \"" + tableName + "\"");// NOSONAR S2077

		final var sqlFieldsCreate = new StringJoiner(", ");
		sqlFieldsCreate.add("\"id\" INTEGER");

		final var header = table.getHeader();
		final var rowTypes = table.getRowTypes();
		for (var posHeader = 0; posHeader < header.size(); posHeader++) {
			final var type = rowTypes.get(posHeader);
			final var headerName = cleanNameToFieldName(header.get(posHeader));

			if (type == TableCellString.class) {
				sqlFieldsCreate.add("\"" + headerName + "\" TEXT");
			} else if (type == TableCellFloat.class) {
				sqlFieldsCreate.add("\"" + headerName + "\" REAL");
			} else if (type == TableCellLong.class
					   || type == TableCellInteger.class) {
				sqlFieldsCreate.add("\"" + headerName + "\" INTEGER");
			} else {
				throw new IllegalArgumentException("Can't manage type " + type);
			}
		}

		sqlFieldsCreate.add("PRIMARY KEY(\"id\" AUTOINCREMENT)");

		final var sqlCreate = "CREATE TABLE \"" + tableName + "\" (" + sqlFieldsCreate.toString() + ");";
		log.debug("Execute SQL: {}", sqlCreate);
		statement.executeUpdate(sqlCreate);// NOSONAR S2077

		final var sqlFields = header.stream()
				.map(TableSQLiteExportFormat::cleanNameToFieldName)
				.collect(joining(", "));

		pushRowsToSQL(table, statement, tableName, sqlFields);
	}

	private static void pushRowsToSQL(final Table table,
									  final Statement statement,
									  final String tableName,
									  final String sqlFields) throws SQLException {
		final var rows = table.getRows();
		log.debug("Add {} rows to \"{}\" sheet in SQLite", rows.size(), table.getTableName());

		var batchSize = 0;
		final var maxBatch = 10000;
		for (var posRow = 0; posRow < rows.size(); posRow++) {
			if (batchSize++ == maxBatch) {
				log.debug("SQL executeBatch {}", batchSize - 1);
				statement.executeBatch();
				batchSize = 0;
			}

			final var sqlValues = new StringJoiner(",");
			final var cells = rows.get(posRow).getCells();

			for (var posCell = 0; posCell < cells.size(); posCell++) {
				final var cell = cells.get(posCell);
				if (cell instanceof final TableCellString cellContent) {
					sqlValues.add("\"" + cellContent.value() + "\"");
				} else if (cell instanceof final TableCellFloat cellContent) {
					sqlValues.add(String.valueOf(cellContent.value()));
				} else if (cell instanceof final TableCellLong cellContent2) {
					sqlValues.add(String.valueOf(cellContent2.value()));
				} else if (cell instanceof final TableCellInteger cellContent3) {
					sqlValues.add(String.valueOf(cellContent3.value()));
				} else if (cell instanceof TableCellNull) {
					sqlValues.add("NULL");
				} else {
					throw new IllegalArgumentException("Can't manage type " + cell.getClass());
				}
			}
			final var sqlAdd = "INSERT INTO \"" + tableName
							   + "\" (" + sqlFields
							   + ") VALUES (" + sqlValues.toString() + ")";
			log.trace("SQL {}" + sqlAdd);
			statement.addBatch(sqlAdd);// NOSONAR S2077
		}
		if (batchSize > 0) {
			log.debug("SQL executeBatch {}", batchSize);
			statement.executeBatch();
		}
	}

	public static String cleanNameToFieldName(final String name) {
		return name.trim()
				.toLowerCase()
				.replace(' ', '_')
				.replace('-', '_')
				.replace('/', '_');
	}

}
