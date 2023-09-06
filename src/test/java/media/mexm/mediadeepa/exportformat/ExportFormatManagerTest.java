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
package media.mexm.mediadeepa.exportformat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import net.datafaker.Faker;

class ExportFormatManagerTest {

	final static Faker faker = Faker.instance();

	ExportFormatManager m;

	String name;
	@Mock
	ExportFormat exportFormat;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		name = faker.numerify("name###");
		m = new ExportFormatManager();
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(exportFormat);
	}

	@Test
	void testRegister() {
		m.register(name, exportFormat);
		assertThrows(IllegalArgumentException.class, () -> m.register(name, exportFormat));
	}

	@Test
	void testIsFormatExists() {
		assertFalse(m.isFormatExists(name));
		m.register(name, exportFormat);
		assertTrue(m.isFormatExists(name));
		verifyNoInteractions(exportFormat);
	}

	@Test
	void testGetExportFormat() {
		assertThrows(IllegalArgumentException.class, () -> m.getExportFormat(name));
		m.register(name, exportFormat);
		assertEquals(exportFormat, m.getExportFormat(name));
	}

	@Test
	void testGetRegisted() {
		final var r = m.getRegisted();
		assertEquals(Map.of(), r);
		assertThrows(UnsupportedOperationException.class,
				() -> r.put(name, name));

		final var longName = faker.numerify("longName###");
		when(exportFormat.getFormatLongName()).thenReturn(longName);

		m.register(name, exportFormat);
		assertEquals(Map.of(name, longName), m.getRegisted());
		verify(exportFormat, times(1)).getFormatLongName();
	}

}
