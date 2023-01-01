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

import static java.lang.Float.NEGATIVE_INFINITY;
import static java.lang.Float.NaN;
import static java.lang.Float.POSITIVE_INFINITY;
import static media.mexm.mediadeepa.exportformat.TabularDocument.durationToString;
import static media.mexm.mediadeepa.exportformat.TabularDocument.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import net.datafaker.Faker;

class TabularDocumentTest {

	final static Faker faker = Faker.instance();

	TabularDocument doc;
	String fileName;
	File exportDirectory;
	List<String> row;
	String head;
	byte[] document;

	@Mock
	TabularDocumentExporter exporter;
	@Mock
	Object object;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		doc = new TabularDocument(exporter);
		fileName = faker.numerify("filename###");
		exportDirectory = new File(faker.numerify("exportDirectory###"));
		row = List.of(faker.numerify("item###"));
		head = faker.numerify("head###");
		document = faker.random().nextRandomBytes(faker.random().nextInt(10, 100));

		when(exporter.getDocument(any(), any())).thenReturn(document);
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(exporter, object);
	}

	@Test
	void testHead() {
		assertEquals(doc, doc.head(head));
		assertThrows(IllegalArgumentException.class, () -> doc.head());
	}

	@Test
	void testRowListOfString() {
		doc.head("");
		doc.row(row);
		doc.exportToFile(fileName, exportDirectory);

		verify(exporter, times(1)).getDocument(List.of(""), List.of(row));
		verify(exporter, times(1)).save(document, fileName, exportDirectory);
	}

	@Test
	void testRowListOfString_empty_noHeader() {
		doc.row((List<String>) null);
		doc.row(List.of());
		doc.exportToFile(fileName, exportDirectory);
		verifyNoInteractions(exporter);
	}

	@Test
	void testRowListOfString_noHeader() {
		doc.row(List.of(""));
		doc.exportToFile(fileName, exportDirectory);
		verifyNoInteractions(exporter);
	}

	@Test
	void testRowObjectArray() throws MalformedURLException {
		final var head = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9" };
		doc.head(head);

		final var str = faker.numerify("item###");
		final var duration = Duration.ofMillis(5);
		final var flo = 3.14157f;
		final var numb = faker.random().nextInt();
		final var url = new URL("http://localhost");

		assertEquals(doc, doc.row(
				null,
				str,
				duration,
				flo,
				NaN,
				NEGATIVE_INFINITY,
				POSITIVE_INFINITY,
				numb,
				url));

		final var row = List.of(
				"",
				str,
				"00:00:00.005",
				"3.14158",
				"",
				"-144",
				"144",
				String.valueOf(numb),
				url.toString());

		doc.exportToFile(fileName, exportDirectory);

		verify(exporter, times(1)).getDocument(List.of(head), List.of(row));
		verify(exporter, times(1)).save(document, fileName, exportDirectory);
	}

	@Test
	void testToList() {
		final var v0 = faker.numerify("item###");
		final var v1 = faker.numerify("item###");
		final var v2 = faker.numerify("item###");
		assertEquals(List.of(v0, v1, v2), toList(v0, v1, null, v2));
	}

	@Test
	void testExportToFile_empty() {
		doc.exportToFile(fileName, exportDirectory);
		verifyNoInteractions(exporter);
	}

	@Test
	void testExportToFile() {
		doc.head(head);
		doc.row(row);
		doc.exportToFile(fileName, exportDirectory);
		verify(exporter, times(1)).getDocument(List.of(head), List.of(row));
		verify(exporter, times(1)).save(document, fileName, exportDirectory);
	}

	@Test
	void testDurationToString() {
		assertEquals("", durationToString(null));
		assertEquals("00:00:00", durationToString(Duration.ZERO));
		assertEquals("00:00:00.009", durationToString(Duration.ofMillis(9)));
		assertEquals("00:00:00.099", durationToString(Duration.ofMillis(99)));
		assertEquals("00:00:00.999", durationToString(Duration.ofMillis(999)));
		assertEquals("00:00:59", durationToString(Duration.ofSeconds(59)));
		assertEquals("00:59:00", durationToString(Duration.ofMinutes(59)));
		assertEquals("10:00:00", durationToString(Duration.ofHours(10)));
		assertEquals("48:00:00", durationToString(Duration.ofDays(2)));
	}

}
