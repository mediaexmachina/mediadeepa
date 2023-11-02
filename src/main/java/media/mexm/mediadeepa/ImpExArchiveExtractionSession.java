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
package media.mexm.mediadeepa;

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.joining;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class ImpExArchiveExtractionSession {

	private static final String ENTRY_STR = "entry";
	static final String NEWLINE = "\n";
	private final LinkedHashMap<String, String> contentItems;

	public ImpExArchiveExtractionSession() {
		contentItems = new LinkedHashMap<>();
	}

	public void add(final String internalFileName, final List<String> lines) {
		if (lines == null || lines.isEmpty()) {
			return;
		}
		add(internalFileName, lines.stream().collect(joining(NEWLINE)));
	}

	public record ExtractedFileEntry(String internalFileName, String content) {
	}

	public Stream<ExtractedFileEntry> getEntries() {
		return contentItems.entrySet()
				.stream()
				.filter(e -> e.getValue() != null)
				.filter(e -> e.getValue().isEmpty() == false)
				.map(e -> new ExtractedFileEntry(e.getKey(), e.getValue()));
	}

	public void add(final String internalFileName, final String content) {
		if (content == null || content.isEmpty()) {
			return;
		}
		contentItems.put(internalFileName, content);
	}

	public void addVersion(final String internalFileName, final Map<String, String> versions) {
		try {
			final var xml = XMLEventFactory.newInstance();
			final var sw = new StringWriter();
			final var writer = XMLOutputFactory.newInstance()
					.createXMLEventWriter(sw);

			writer.add(xml.createStartDocument());
			writer.add(xml.createStartElement("", null, "map"));

			final var vIterator = versions.entrySet().iterator();
			while (vIterator.hasNext()) {
				final var entry = vIterator.next();
				writer.add(xml.createStartElement("", null, ENTRY_STR));
				writer.add(xml.createAttribute("", null, "key", entry.getKey()));
				writer.add(xml.createAttribute("", null, "value", entry.getValue()));
				writer.add(xml.createEndElement("", null, ENTRY_STR));
			}

			writer.add(xml.createEndElement("", null, "map"));
			writer.add(xml.createEndDocument());
			writer.close();
			add(internalFileName, sw.toString());
		} catch (XMLStreamException | FactoryConfigurationError e) {
			throw new IllegalArgumentException("Can't produce version", e);
		}
	}

	public Map<String, String> getVersions(final String internalFileName) {
		if (contentItems.containsKey(internalFileName) == false) {
			return Map.of();
		}

		try {
			final var sr = new StringReader(contentItems.get(internalFileName));
			final var factory = XMLInputFactory.newInstance();
			factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
			factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
			final var reader = factory.createXMLEventReader(sr);

			final var versions = new LinkedHashMap<String, String>();

			XMLEvent nextEvent;
			while (reader.hasNext()) {
				nextEvent = reader.nextEvent();
				if (nextEvent instanceof final StartElement element
					&& ENTRY_STR.equals(element.getName().getLocalPart())) {
					versions.put(
							element.getAttributeByName(new QName("key")).getValue(),
							element.getAttributeByName(new QName("value")).getValue());
				}
			}

			return unmodifiableMap(versions);
		} catch (XMLStreamException | FactoryConfigurationError e) {
			throw new IllegalArgumentException("Can't produce version", e);
		}
	}
}
