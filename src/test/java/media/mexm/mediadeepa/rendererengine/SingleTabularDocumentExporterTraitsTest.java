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
 * Copyright (C) Media ex Machina 2024
 *
 */
package media.mexm.mediadeepa.rendererengine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.TabularDocument;
import media.mexm.mediadeepa.exportformat.TabularExportFormat;
import net.datafaker.Faker;

class SingleTabularDocumentExporterTraitsTest {

	static Faker faker = net.datafaker.Faker.instance();

	String singleUniqTabularDocumentBaseFileName;
	SingleTabularDocumentExporterTraitsImpl s;

	@Mock
	TabularDocument tabularDocument;
	@Mock
	DataResult dataResult;
	@Mock
	TabularExportFormat tabularExportFormat;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		singleUniqTabularDocumentBaseFileName = faker.numerify("singleUniqTabularDocumentBaseFileName###");
		s = new SingleTabularDocumentExporterTraitsImpl();
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(
				tabularDocument,
				dataResult,
				tabularExportFormat);
	}

	class SingleTabularDocumentExporterTraitsImpl implements SingleTabularDocumentExporterTraits {

		@Override
		public List<TabularDocument> toTabularDocument(final DataResult result,
													   final TabularExportFormat t) {
			assertEquals(dataResult, result);
			assertEquals(tabularExportFormat, t);
			return List.of(tabularDocument, mock(TabularDocument.class));
		}

		@Override
		public String getSingleUniqTabularDocumentBaseFileName() {
			return singleUniqTabularDocumentBaseFileName;
		}

	}

	@Test
	void testToSingleTabularDocument() {
		final var internalTabularBaseFileName = faker.numerify("internalTabularBaseFileName###");
		final var oTD = s.toSingleTabularDocument(internalTabularBaseFileName, dataResult, tabularExportFormat);
		assertThat(oTD).contains(tabularDocument);
	}

	@Test
	void testGetInternalTabularBaseFileNames() {
		assertThat(s.getInternalTabularBaseFileNames()).isEqualTo(Set.of(singleUniqTabularDocumentBaseFileName));
	}

}
