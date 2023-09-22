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

import static media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.IMAGE_SIZE_FULL_HEIGHT;
import static media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.IMAGE_SIZE_HALF_HEIGHT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class E2EGraphicTest extends E2EUtils {

	ThreadLocal<float[]> hsbvalsThreadLocal;

	@BeforeEach
	void init() {
		hsbvalsThreadLocal = ThreadLocal.withInitial(() -> new float[3]);
	}

	// TODO generic test for all graphics in one

	@Test
	void testLUFS() throws IOException {
		final var rawData = prepareMovForSimpleE2ETests();
		if (rawData == null) {
			return;
		}

		final var outputFileLUFS = new File("target/e2e-export", "mov_lufs-events.jpg");
		final var outputFileTPK = new File("target/e2e-export", "mov_lufs-tpk-events.jpg");
		if (outputFileLUFS.exists() == false
			|| outputFileTPK.exists() == false) {
			runApp(
					"--temp", "target/e2e-temp",
					"--import-lavfi", rawData.outAlavfi().getPath(),
					"--import-lavfi", rawData.outVlavfi().getPath(),
					"--import-stderr", rawData.outStderr().getPath(),
					"--import-probeheaders", rawData.outProbeheaders().getPath(),
					"--import-container", rawData.outContainer().getPath(),
					"-f", "lufs",
					"-e", "target/e2e-export",
					"--export-base-filename", "mov");
		}

		var image = ImageIO.read(outputFileLUFS);
		assertEquals(IMAGE_SIZE_FULL_HEIGHT.getWidth(), image.getWidth());
		assertEquals(IMAGE_SIZE_FULL_HEIGHT.getHeight(), image.getHeight());
		checkImageGraphic(image);

		image = ImageIO.read(outputFileTPK);
		assertEquals(IMAGE_SIZE_FULL_HEIGHT.getWidth(), image.getWidth());
		assertEquals(IMAGE_SIZE_FULL_HEIGHT.getHeight(), image.getHeight());
		checkImageGraphic(image);
	}

	@Test
	void testAPhase() throws IOException {
		final var rawData = prepareMovForSimpleE2ETests();
		if (rawData == null) {
			return;
		}

		final var outputFile = new File("target/e2e-export", "mov_audio-phase.jpg");
		if (outputFile.exists() == false) {
			runApp(
					"--temp", "target/e2e-temp",
					"--import-lavfi", rawData.outAlavfi().getPath(),
					"--import-lavfi", rawData.outVlavfi().getPath(),
					"--import-stderr", rawData.outStderr().getPath(),
					"--import-probeheaders", rawData.outProbeheaders().getPath(),
					"--import-container", rawData.outContainer().getPath(),
					"-f", "graphic",
					"-e", "target/e2e-export",
					"--export-base-filename", "mov");
		}

		final var image = ImageIO.read(outputFile);
		assertEquals(IMAGE_SIZE_HALF_HEIGHT.getWidth(), image.getWidth());
		assertEquals(IMAGE_SIZE_HALF_HEIGHT.getHeight(), image.getHeight());
		checkImageGraphic(image);
	}

	private static record HSV(float hue, float sat, float value) {
		static HSV get(final float[] hsbvals) {
			return new HSV(hsbvals[0], hsbvals[1], hsbvals[2]);
		}
	}

	private void checkImageGraphic(final BufferedImage image) {
		final var allColors = IntStream.range(0, image.getWidth())
				.parallel()
				.mapToObj(posX -> IntStream
						.range(0, image.getHeight())
						.mapToObj(posY -> image.getRGB(posX, posY)))
				.flatMap(t -> t)
				.distinct()
				.map(Color::new)
				.map(c -> {
					final var hsbvals = hsbvalsThreadLocal.get();
					Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsbvals);
					return HSV.get(hsbvals);
				})
				.toList();

		assertThat(allColors).hasSizeGreaterThan(1000);

		final var greyCount = allColors.parallelStream().filter(hsv -> hsv.sat == 0f).count();
		assertThat(greyCount).isGreaterThan(100);

		final var fullSat = allColors.parallelStream()
				.filter(hsv -> hsv.value > 0.1f)
				.filter(hsv -> hsv.sat == 1f)
				.count();
		assertThat(fullSat).isGreaterThan(50);

		final var greyCols = allColors.parallelStream()
				.map(HSV::value)
				.mapToInt(v -> Math.round(v * 10))
				.distinct()
				.summaryStatistics();
		assertEquals(10, greyCols.getMax());
		assertEquals(0, greyCols.getMin());
		assertEquals(5d, greyCols.getAverage());

		final var hueCols = (int) allColors.parallelStream()
				.filter(hsv -> hsv.value > .7f)
				.filter(hsv -> hsv.sat >= .9f)
				.map(HSV::hue)
				.map(v -> Math.round(v * 50))
				.distinct()
				.count();
		assertThat(hueCols).isGreaterThanOrEqualTo(2);
	}

}
