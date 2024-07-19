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
 * Copyright (C) Media ex Machina 2024
 *
 */
package media.mexm.mediadeepa.exportformat;

import static java.lang.String.valueOf;
import static media.mexm.mediadeepa.exportformat.ImageArtifact.IMAGE_JPEG;
import static media.mexm.mediadeepa.exportformat.ImageArtifact.IMAGE_PNG;
import static media.mexm.mediadeepa.exportformat.ImageArtifact.renderChartToJPEGImage;
import static media.mexm.mediadeepa.exportformat.ImageArtifact.renderChartToPNGImage;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jfree.chart.plot.PlotOrientation.VERTICAL;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import net.datafaker.Faker;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class ImageArtifactTest {

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

	Dimension size;
	byte[] data;

	ImageArtifact ia;

	@BeforeEach
	void init() {
		size = new Dimension(width, height);
		data = Faker.instance().random().nextRandomBytes(dataSize);
		ia = new ImageArtifact(name, size, contentType, data);
	}

	@Test
	void testIsPNG() {
		assertFalse(ia.isPNG());
		ia = new ImageArtifact(name, size, IMAGE_PNG, data);
		assertTrue(ia.isPNG());
	}

	@Test
	void testIsJPEG() {
		assertFalse(ia.isJPEG());
		ia = new ImageArtifact(name, size, IMAGE_JPEG, data);
		assertTrue(ia.isJPEG());
	}

	@Test
	void testWriteToFile() throws IOException {
		final var file = File.createTempFile("mediadeepa", "temp");
		forceDelete(file);
		ia.writeToFile(file);
		assertArrayEquals(data, readFileToByteArray(file));
		forceDelete(file);
	}

	@Nested
	class Chart {
		JFreeChart chart;

		@BeforeEach
		void init() {
			chart = ChartFactory.createStackedXYAreaChart(
					"", "", "", new DefaultTableXYDataset(), VERTICAL, true, false, false);
		}

		@Test
		void testRenderChartToJPEGImage() {
			ia = renderChartToJPEGImage(name, chart, size, 0.5f);
			assertEquals(name, ia.name());
			assertEquals(size, ia.size());
			assertTrue(ia.isJPEG());
			assertThat(ia.data()).isNotEmpty();
		}

		@Test
		void testRenderChartToPNGImage() {
			ia = renderChartToPNGImage(name, chart, size);
			assertEquals(name, ia.name());
			assertEquals(size, ia.size());
			assertTrue(ia.isPNG());
			assertThat(ia.data()).isNotEmpty();
		}

	}

	@Test
	void testMakeEmbeddedHTMLImage() {
		ia = new ImageArtifact(name, size, contentType, "A FIXED VALUE".getBytes());

		final var img = ia.makeEmbeddedHTMLImage(alt);
		assertEquals(
				"<img alt=\"" + alt +
					 "\" width=\"" + width / 2 +
					 "\" height=\"" + height / 2
					 + "\" src=\"data:" + contentType + ";base64,QSBGSVhFRCBWQUxVRQ==\">",
				img.toString());
	}

	@Test
	void testToString() {
		assertThat(ia.toString()).contains(valueOf(data.length));
	}

	@Test
	void testHashCode() {
		assertEquals(new ImageArtifact(name, null, null, data).hashCode(), ia.hashCode());
	}

	@Test
	void testEquals() {
		assertEquals(new ImageArtifact(name, null, null, data), ia);
	}

}
