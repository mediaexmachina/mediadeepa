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

import static java.lang.Float.NEGATIVE_INFINITY;
import static java.lang.Float.POSITIVE_INFINITY;
import static media.mexm.mediadeepa.exportformat.tabular.TabularDocument.durationToString;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Manage structured multi table name/header/cols/row document
 */
@Slf4j
public class TableDocument {

	private final List<Table> tables;

	public TableDocument() {
		tables = new ArrayList<>();
	}

	public class Table {

		@Getter
		private final String tableName;
		private List<String> header;
		private final List<Row> rows;
		private final List<Class<? extends TableCell>> rowTypes;

		private Table(final String tableName) {
			this.tableName = Objects.requireNonNull(tableName, "\"tableName\" can't to be null");
			rows = new ArrayList<>();
			rowTypes = new ArrayList<>();
		}

		private boolean isEmpty() {
			return rows.isEmpty() || rows.stream().allMatch(Row::isEmpty);
		}

		public Table head(final List<String> header) {
			this.header = Objects.requireNonNull(header, "\"header\" can't to be null");
			if (header.isEmpty()) {
				throw new IllegalArgumentException("Empty header");
			}
			return this;
		}

		public class Row {

			private final List<TableCell> cells;

			private Row() {
				cells = new ArrayList<>();
			}

			private boolean isEmpty() {
				return cells.isEmpty();
			}

			private void checkFull() {
				if (cells.size() == header.size()) {
					throw new IllegalStateException("Can't add new item in row. Table size=" + header.size());
				}
			}

			public Row addCell(final String value) {
				checkFull();
				if (value == null) {
					cells.add(TableCellNull.INSTANCE);
				} else {
					cells.add(new TableCellString(value));
				}
				importCellType(TableCellString.class);
				return this;
			}

			public Row addOptionalToString(final Object value) {
				if (value == null) {
					cells.add(TableCellNull.INSTANCE);
					importCellType(TableCellString.class);
				} else {
					addCell(value.toString());
				}
				return this;
			}

			public Row addCell(final Number value) {
				checkFull();
				if (value == null) {
					cells.add(TableCellNull.INSTANCE);
					importCellType(TableCellFloat.class);
				} else if (value instanceof final Long l) {
					cells.add(new TableCellLong(l));
					importCellType(TableCellLong.class);
				} else if (value instanceof final Integer i) {
					cells.add(new TableCellInteger(i));
					importCellType(TableCellInteger.class);
				} else if (value instanceof final Float f) {
					if (f.isNaN()) {
						cells.add(TableCellNull.INSTANCE);
					} else if (f == NEGATIVE_INFINITY) {
						cells.add(new TableCellFloat(-144));
					} else if (f == POSITIVE_INFINITY) {
						cells.add(new TableCellFloat(144));
					} else {
						cells.add(new TableCellFloat(f));
					}
					importCellType(TableCellFloat.class);
				} else {
					cells.add(new TableCellFloat(value.floatValue()));
					importCellType(TableCellFloat.class);
				}
				return this;
			}

			public Row addCell(final Duration value) {
				checkFull();
				if (value == null) {
					cells.add(TableCellNull.INSTANCE);
				} else {
					cells.add(new TableCellString(durationToString(value)));
				}
				importCellType(TableCellString.class);
				return this;
			}

			public List<TableCell> getCells() {
				return Collections.unmodifiableList(cells);
			}

			private void importCellType(final Class<? extends TableCell> type) {
				if (header.size() == rowTypes.size()) {
					return;
				}
				rowTypes.add(type);
			}
		}

		public Row addRow() {
			final var l = new Row();
			rows.add(l);
			return l;
		}

		public List<String> getHeader() {
			return Collections.unmodifiableList(header);
		}

		public List<Row> getRows() {
			return Collections.unmodifiableList(rows);
		}

		public List<Class<? extends TableCell>> getRowTypes() {
			if (header.size() != rowTypes.size()) {
				log.error("Row types: {} (need {})", rowTypes, header.size());
				throw new IllegalStateException("Invalid (missing) some row types");
			}
			return Collections.unmodifiableList(rowTypes);
		}
	}

	public Table createTable(final String tableName) {
		final var t = new Table(tableName);
		tables.add(t);
		return t;
	}

	public List<Table> getTables() {
		return tables.stream().filter(Predicate.not(Table::isEmpty)).toList();
	}

}
