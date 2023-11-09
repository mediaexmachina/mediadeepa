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
package media.mexm.mediadeepa.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.datafaker.Faker;

class ExportToCmdTest {
	static Faker faker = net.datafaker.Faker.instance();

	ExportToCmd etc;
	String suffix;
	String baseFileName;
	File export;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		etc = new ExportToCmd();
		suffix = faker.numerify("suffix###");
		baseFileName = faker.numerify("baseFileName###");
		export = new File(faker.numerify("export###"));
		etc.setExport(export);
	}

	@Test
	void testMakeOutputFile() throws Exception {
		assertEquals(new File(export, suffix), etc.makeOutputFile(suffix));

		etc.setBaseFileName(baseFileName);
		assertEquals(new File(export, baseFileName + "_" + suffix), etc.makeOutputFile(suffix));

		etc.setBaseFileName(baseFileName + "_");
		assertEquals(new File(export, baseFileName + "_" + suffix), etc.makeOutputFile(suffix));

		etc.setBaseFileName(baseFileName + " ");
		assertEquals(new File(export, baseFileName + " " + suffix), etc.makeOutputFile(suffix));

		etc.setBaseFileName(baseFileName + "-");
		assertEquals(new File(export, baseFileName + "-" + suffix), etc.makeOutputFile(suffix));

		etc.setBaseFileName(baseFileName + "|");
		assertEquals(new File(export, baseFileName + "|" + suffix), etc.makeOutputFile(suffix));
	}

}
