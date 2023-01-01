/*
 * This file is part of mediadeepa.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2023
 *
 */
package media.mexm.mediadeepa.exportformat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import net.datafaker.Faker;

class TabularDocumentExporterTest {
	final static Faker faker = Faker.instance();

	static class TabularDocumentExporterImpl implements TabularDocumentExporter {

		@Override
		public byte[] getDocument(final List<String> header, final List<List<String>> lines) {
			return null;
		}

	}

	@Test
	void testSave() throws IOException {
		final var t = new TabularDocumentExporterImpl();
		final var document = faker.random().nextRandomBytes(faker.random().nextInt(10, 100));
		final var f = File.createTempFile("test-mediadeepa", ".tmp");
		FileUtils.forceDelete(f);
		assertFalse(f.exists());

		t.save(document, f.getName(), f.getParentFile());
		assertTrue(f.exists());
		assertTrue(Arrays.equals(document, FileUtils.readFileToByteArray(f)));
		FileUtils.forceDelete(f);
		assertFalse(f.exists());
	}

}
