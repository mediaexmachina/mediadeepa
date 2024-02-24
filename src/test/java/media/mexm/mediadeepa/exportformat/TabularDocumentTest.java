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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.components.OutputFileSupplier;
import net.datafaker.Faker;

class TabularDocumentTest {

	final static Faker faker = Faker.instance();

	TabularDocument doc;
	String fileName;
	File exportDirectory;
	List<String> row;
	String head;
	byte[] document;
	String fileExt;
	String prefixFileName;
	File expectedOutputFile;

	@Mock
	TabularDocumentExporter exporter;
	@Mock
	Object object;
	@Mock
	OutputFileSupplier outputFileSupplier;
	@Mock
	DataResult result;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		exportDirectory = new File("target/tmp-test/" + faker.numerify("exportDirectory###"));
		fileName = faker.numerify("filename###");
		doc = new TabularDocument(exporter, fileName);

		row = List.of(faker.numerify("item###"));
		head = faker.numerify("head###");
		document = faker.random().nextRandomBytes(faker.random().nextInt(10, 100));
		fileExt = faker.numerify("ext###");
		prefixFileName = faker.numerify("prefix###");
		expectedOutputFile = new File(exportDirectory.getPath(), prefixFileName + "_" + fileName + "." + fileExt);

		when(exporter.getDocument(any(), any())).thenReturn(document);
		when(exporter.getDocumentFileExtension()).thenReturn(fileExt);
		when(exporter.getNumberUtils()).thenReturn(new NumberUtils());
		when(exporter.formatNumberHighPrecision(anyFloat()))
				.thenAnswer(d -> String.valueOf(d.getArgument(0, Float.class)));
		when(outputFileSupplier.makeOutputFile(eq(result), any())).thenReturn(expectedOutputFile);
	}

	@AfterEach
	void ends() {
		FileUtils.deleteQuietly(exportDirectory);
		verifyNoMoreInteractions(exporter, object);
	}

	@Test
	void testHead() {
		assertEquals(doc, doc.head(List.of(head)));
		assertThrows(IllegalArgumentException.class, () -> doc.head(List.of()));// NOSONAR 5778
	}

	@Test
	void testRowListOfString() {
		doc.head(List.of(""));
		doc.row(row);
		assertEquals(expectedOutputFile, doc.exportToFile(outputFileSupplier, result).get());

		verify(exporter, times(1)).getDocument(List.of(""), List.of(row));
		verify(exporter, times(1)).getDocumentFileExtension();
	}

	@Test
	void testRowListOfString_empty_noHeader() {
		doc.row((List<String>) null);
		doc.row(List.of());
		assertTrue(doc.exportToFile(outputFileSupplier, result).isEmpty());
	}

	@Test
	void testRowListOfString_noHeader() {
		final var empty = List.of("");
		assertThrows(IllegalArgumentException.class, () -> doc.row(empty));
		assertTrue(doc.exportToFile(outputFileSupplier, result).isEmpty());
	}

	@Test
	void testRowObjectArray() throws MalformedURLException {
		final var head = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9");
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
				Float.NaN,
				Float.NEGATIVE_INFINITY,
				Float.POSITIVE_INFINITY,
				numb,
				url));

		final var row = List.of(
				"",
				str,
				"00:00:00.005",
				"3.14157",
				"",
				"-144",
				"144",
				String.valueOf(numb),
				url.toString());

		assertEquals(expectedOutputFile, doc.exportToFile(outputFileSupplier, result).get());

		verify(exporter, times(1)).getDocument(head, List.of(row));
		verify(exporter, times(1)).getDocumentFileExtension();
		verify(exporter, atLeast(1)).getNumberUtils();
		verify(exporter, atLeast(1)).formatNumberHighPrecision(anyFloat());
	}

	@Test
	void testExportToFile_empty() {
		assertTrue(doc.exportToFile(outputFileSupplier, result).isEmpty());
	}

	@Test
	void testExportToFile() {
		doc.head(List.of(head));
		doc.row(row);
		assertEquals(expectedOutputFile, doc.exportToFile(outputFileSupplier, result).get());
		verify(exporter, times(1)).getDocument(List.of(head), List.of(row));
		verify(exporter, times(1)).getDocumentFileExtension();
	}

}
