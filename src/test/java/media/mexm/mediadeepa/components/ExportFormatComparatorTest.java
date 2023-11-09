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
package media.mexm.mediadeepa.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.openMocks;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import media.mexm.mediadeepa.exportformat.ExportFormat;
import media.mexm.mediadeepa.exportformat.components.HTMLExportFormat;
import media.mexm.mediadeepa.exportformat.components.TableXMLExportFormat;

class ExportFormatComparatorTest {

	ExportFormatComparator c;

	@Mock
	TableXMLExportFormat lower;
	@Mock
	HTMLExportFormat higher;
	@Mock
	ExportFormat newer;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		c = new ExportFormatComparator();
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(lower, higher, newer);
	}

	@Test
	void testCompare() {
		assertEquals(-1, c.compare(lower, higher));
		assertEquals(1, c.compare(higher, lower));
		assertEquals(-1, c.compare(lower, newer));
		assertEquals(-1, c.compare(higher, newer));
		assertEquals(1, c.compare(newer, lower));
		assertEquals(1, c.compare(newer, higher));
	}

}
