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
 * Copyright (C) hdsdi3g for hd3g.tv 2024
 *
 */
package media.mexm.mediadeepa.exportformat.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.awt.Dimension;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import j2html.TagCreator;
import j2html.tags.DomContent;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.exportformat.ImageArtifact;
import net.datafaker.Faker;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class ImageReportEntryTest {

	@Fake
	String name;
	@Fake(min = 100, max = 1000)
	int width;
	@Fake(min = 100, max = 1000)
	int height;
	@Fake
	String contentType;
	@Fake(min = 100, max = 1000)
	int dataSize;
	@Fake
	String alt;
	@Mock
	DomContentProvider caption;
	@Fake
	String captionText;
	@Mock
	NumberUtils numberUtils;
	@Mock
	JsonGenerator gen;
	@Mock
	SerializerProvider provider;

	Dimension size;
	byte[] data;
	DomContent domcontent;
	ImageArtifact image;

	ImageReportEntry ire;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		domcontent = TagCreator.span(captionText);
		when(caption.toDomContent(numberUtils)).thenReturn(domcontent);

		size = new Dimension(width, height);
		data = Faker.instance().random().nextRandomBytes(dataSize);
		image = new ImageArtifact(name, size, contentType, "A FIXED VALUE".getBytes());
		ire = new ImageReportEntry(image, caption, 1000);
	}

	@Test
	void testMakeEmbeddedHTMLImage() {
		final var img = ire.makeEmbeddedHTMLImage(alt);
		assertThat(img.toString()).contains(alt, "src=\"data:" + contentType, "width", "height");
	}

	@Test
	void testIsEmpty() {
		assertFalse(ire.isEmpty());
	}

	@Test
	void testToDomContent() {
		assertThat(ire.toDomContent(numberUtils).render())
				.contains("src=\"data:" + contentType, "width", "height", image.name(), domcontent.render());
		verify(caption, atLeast(1)).toDomContent(numberUtils);
	}

	@Test
	void testHashCode() {
		assertEquals(ire.hashCode(), Objects.hash(caption));
	}

	@Test
	void testEquals() {
		assertEquals(ire, new ImageReportEntry(null, caption, -1));
	}

	@Test
	void testToString() {
		assertThat(ire.toString()).isNotEmpty();
	}

}
