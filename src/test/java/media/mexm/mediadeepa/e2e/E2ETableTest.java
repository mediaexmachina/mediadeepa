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
package media.mexm.mediadeepa.e2e;

import static com.fasterxml.jackson.core.JsonToken.FIELD_NAME;
import static com.fasterxml.jackson.core.JsonToken.START_OBJECT;
import static com.fasterxml.jackson.core.JsonToken.VALUE_STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mockito.Mock;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.fasterxml.jackson.core.JsonFactory;

class E2ETableTest extends E2EUtils {

	E2ERawOutDataFiles rawData;

	@Mock
	DefaultHandler defaultHandler;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(defaultHandler);
	}

	@TestFactory
	Stream<DynamicTest> testTable() throws IOException {
		rawData = prepareMovForSimpleE2ETests();
		if (rawData == null) {
			return Stream.empty();
		}

		return Stream.of(
				dynamicTest("XLSX", this::testXLSX),
				dynamicTest("SQLite", this::testSQLite),
				dynamicTest("XML", this::testXML),
				dynamicTest("json", this::testJSON));
	}

	File makeOutputFile(final String baseFileName, final String format) throws IOException {
		final var outputFile = new File("target/e2e-export", baseFileName);
		if (outputFile.exists()) {
			return outputFile;
		}
		runApp(
				"--temp", "target/e2e-temp",
				"--import-lavfi", rawData.outAlavfi().getPath(),
				"--import-lavfi", rawData.outVlavfi().getPath(),
				"--import-stderr", rawData.outStderr().getPath(),
				"--import-probeheaders", rawData.outProbeheaders().getPath(),
				"--import-container", rawData.outContainer().getPath(),
				"-f", format,
				"-e", "target/e2e-export",
				"--export-base-filename", "mov");
		return outputFile;
	}

	void testXLSX() throws IOException {
		final var outputFile = makeOutputFile("mov_media-datas.xlsx", "xlsx");
		assertTrue(outputFile.exists());
		assertTrue(outputFile.length() > 0);
	}

	void testSQLite() throws IOException {
		final var outputFile = makeOutputFile("mov_media-datas.sqlite", "sqlite");
		assertTrue(outputFile.exists());
		assertTrue(outputFile.length() > 0);
	}

	void testXML() throws IOException, SAXException, ParserConfigurationException {
		final var outputFile = makeOutputFile("mov_media-datas.xml", "xml");
		assertTrue(outputFile.exists());
		assertTrue(outputFile.length() > 0);

		final var factory = SAXParserFactory.newInstance();
		final var saxParser = factory.newSAXParser();
		saxParser.parse(outputFile, defaultHandler);
		verify(defaultHandler, times(1)).startDocument();
		verify(defaultHandler, times(1)).endDocument();
		verify(defaultHandler, times(1)).declaration("1.0", "UTF-8", null);
		verify(defaultHandler, times(1)).setDocumentLocator(any());

		verify(defaultHandler, times(1)).startElement(eq(""), eq(""), eq("report"), any());
		verify(defaultHandler, times(18)).startElement(eq(""), eq(""), eq("table"), any());
		verify(defaultHandler, times(18)).startElement(eq(""), eq(""), eq("headers"), any());
		verify(defaultHandler, times(156)).startElement(eq(""), eq(""), eq("header"), any());
		verify(defaultHandler, atLeast(23000)).startElement(eq(""), eq(""), eq("entry"), any());
		verify(defaultHandler, atLeast(1)).endElement(anyString(), anyString(), anyString());
	}

	void testJSON() throws IOException {
		rawData = prepareMovForSimpleE2ETests();
		final var outputFile = makeOutputFile("mov_media-datas.json", "json");
		assertTrue(outputFile.exists());
		assertTrue(outputFile.length() > 0);

		final var jsonFactory = new JsonFactory();
		final var jParser = jsonFactory.createParser(outputFile);

		assertEquals(START_OBJECT, jParser.nextToken());
		assertEquals(FIELD_NAME, jParser.nextToken());

		assertEquals("appVersion", jParser.currentName());
		assertThat(jParser.getText()).isNotBlank();
		assertEquals(VALUE_STRING, jParser.nextToken());

		assertEquals(FIELD_NAME, jParser.nextToken());
		assertEquals("report", jParser.currentName());

		assertEquals(START_OBJECT, jParser.nextToken());
		assertEquals(FIELD_NAME, jParser.nextToken());
	}

}
