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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.datafaker.Faker;

class TableDocumentTest {
	final static Faker faker = Faker.instance();

	TableDocument t;
	String tableName;
	String head;
	String sValue;
	Object objectValue;
	float fValue;
	int iValue;
	long lValue;

	@BeforeEach
	void init() {
		t = new TableDocument();
		tableName = faker.numerify("tableName###");
		head = faker.numerify("head###");
		sValue = faker.numerify("value###");
		objectValue = new Object();
		fValue = faker.random().nextFloat();
		iValue = faker.random().nextInt();
		lValue = faker.random().nextLong();
	}

	@Test
	void testGetTables() {
		assertEquals(List.of(), t.getTables());
		assertThrows(UnsupportedOperationException.class, () -> t.getTables().add(null));// NOSONAR 5778
	}

	@Test
	void testCreateTable() {
		final var table = t.createTable(tableName);
		assertNotNull(table);
		assertEquals(tableName, table.getTableName());
	}

	@Test
	void testHeaders() {
		final var table = t.createTable(tableName);
		assertEquals(table, table.head(List.of(head)));
		assertEquals(List.of(head), table.getHeader());
		assertThrows(UnsupportedOperationException.class, () -> table.getHeader().add(""));// NOSONAR 5778
		assertThrows(IllegalArgumentException.class, () -> table.head(List.of()));// NOSONAR 5778
	}

	@Test
	void testRow() {
		final var table = t.createTable(tableName);
		final var row = table.addRow();
		assertNotNull(row);
		assertEquals(List.of(row), table.getRows());
		assertThrows(UnsupportedOperationException.class, () -> table.getRows().add(null));// NOSONAR 5778
	}

	@Test
	void testCell() {
		final var table = t.createTable(tableName);
		table.head(List.of("", "", "", "", "", ""));

		final var row = table.addRow();
		assertEquals(row, row.addCell(Duration.ofSeconds(10)));
		assertEquals(row, row.addCell(sValue));
		assertEquals(row, row.addCell(fValue));
		assertEquals(row, row.addCell(iValue));
		assertEquals(row, row.addCell(lValue));
		assertEquals(row, row.addOptionalToString(objectValue));

		final var cells = row.getCells();
		assertNotNull(cells);
		assertEquals(List.of(
				new TableCellString("00:00:10"),
				new TableCellString(sValue),
				new TableCellFloat(fValue),
				new TableCellInteger(iValue),
				new TableCellLong(lValue),
				new TableCellString(objectValue.toString())), cells);

		assertEquals(List.of(
				TableCellString.class,
				TableCellString.class,
				TableCellFloat.class,
				TableCellInteger.class,
				TableCellLong.class,
				TableCellString.class),
				table.getRowTypes());
	}

	@Test
	void testCellNulls() {
		final var table = t.createTable(tableName);
		table.head(List.of("", "", "", ""));

		final var row = table.addRow();
		assertEquals(row, row.addCell((Duration) null));
		assertEquals(row, row.addCell((String) null));
		assertEquals(row, row.addCell((Number) null));
		assertEquals(row, row.addOptionalToString(null));

		final var cells = row.getCells();
		assertNotNull(cells);
		assertEquals(List.of(
				TableCellNull.INSTANCE,
				TableCellNull.INSTANCE,
				TableCellNull.INSTANCE,
				TableCellNull.INSTANCE), cells);

		assertThrows(IllegalStateException.class, () -> row.addCell(""));
		assertThrows(UnsupportedOperationException.class, () -> cells.add(null));

		assertEquals(List.of(
				TableCellString.class,
				TableCellString.class,
				TableCellFloat.class,
				TableCellString.class),
				table.getRowTypes());
	}

	@Test
	void testCellFloats() {
		final var table = t.createTable(tableName);
		table.head(List.of("", "", "", ""));

		final var row = table.addRow();
		assertEquals(row, row.addCell(Float.NaN));
		assertEquals(row, row.addCell(Float.NEGATIVE_INFINITY));
		assertEquals(row, row.addCell(Float.POSITIVE_INFINITY));
		assertEquals(row, row.addCell(42d));

		final var cells = row.getCells();
		assertNotNull(cells);
		assertEquals(List.of(
				TableCellNull.INSTANCE,
				new TableCellFloat(-144),
				new TableCellFloat(144),
				new TableCellFloat(42)), cells);

		assertEquals(List.of(
				TableCellFloat.class,
				TableCellFloat.class,
				TableCellFloat.class,
				TableCellFloat.class),
				table.getRowTypes());
	}

	@Test
	void testGetEmptyTables() {
		final var table = t.createTable(tableName);
		assertEquals(List.of(), t.getTables());

		table.addRow();
		assertEquals(List.of(), t.getTables());
	}

}
