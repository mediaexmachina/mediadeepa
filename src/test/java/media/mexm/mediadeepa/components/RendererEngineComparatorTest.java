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

import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.AboutMeasureRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.BlockRendererEngine;

class RendererEngineComparatorTest {

	RendererEngineComparator c;

	@Mock
	BlockRendererEngine lower;
	@Mock
	AboutMeasureRendererEngine higher;
	@Mock
	ReportRendererEngine newer;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		c = new RendererEngineComparator();
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
