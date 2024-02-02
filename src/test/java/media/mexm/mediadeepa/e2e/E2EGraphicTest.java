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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

class E2EGraphicTest extends E2EUtils {

	private static final String MPG = "mpg_";
	ThreadLocal<float[]> hsbvalsThreadLocal;

	@BeforeEach
	void init() {
		hsbvalsThreadLocal = ThreadLocal.withInitial(() -> new float[3]);
	}

	@TestFactory
	Stream<DynamicTest> testGraphics() throws IOException {
		rawData = prepareMpgForSimpleE2ETests();
		if (rawData == null) {
			return Stream.empty();
		}

		final var tests = new LinkedHashMap<String, Executable>();
		tests.putAll(doAllTests(false));
		tests.putAll(doAllTests(true));
		return tests.entrySet().stream()
				.map(entry -> dynamicTest(entry.getKey(), entry.getValue()));
	}

	private Map<String, Executable> doAllTests(final boolean jpg) throws IOException {
		final var fullHeight = defaultAppConfig.getGraphicConfig().getImageSizeFullSize();
		final var halfHeight = defaultAppConfig.getGraphicConfig().getImageSizeHalfSize();
		final var testName = jpg ? " JPEG" : " PNG";
		final var ext = "." + testName.trim().toLowerCase();
		final var graphicConf = defaultAppConfig.getGraphicConfig();

		final var tests = new LinkedHashMap<String, Executable>();
		tests.put("LUFS" + testName,
				makeAndCheckGraphic(jpg, MPG + graphicConf.getLufsGraphicFilename() + ext, fullHeight));
		tests.put("LUFS TPK" + testName,
				makeAndCheckGraphic(jpg, MPG + graphicConf.getLufsTPKGraphicFilename() + ext, fullHeight));
		tests.put("Audio phase" + testName,
				makeAndCheckGraphic(jpg, MPG + graphicConf.getAPhaseGraphicFilename() + ext, halfHeight));
		tests.put("Audio entropy" + testName,
				makeAndCheckGraphic(jpg, MPG + graphicConf.getEntropyGraphicFilename() + ext, halfHeight));
		tests.put("Audio flat-factor" + testName,
				makeAndCheckGraphic(jpg, MPG + graphicConf.getFlatnessGraphicFilename() + ext, halfHeight));
		tests.put("Audio noise-floor" + testName,
				makeAndCheckGraphic(jpg, MPG + graphicConf.getNoiseFloorGraphicFilename() + ext, halfHeight));
		tests.put("Audio peak-level" + testName,
				makeAndCheckGraphic(jpg, MPG + graphicConf.getPeakLevelGraphicFilename() + ext, halfHeight));
		tests.put("Audio DC offset" + testName,
				makeAndCheckGraphic(jpg, MPG + graphicConf.getDcOffsetGraphicFilename() + ext, halfHeight));
		tests.put("Video SITI" + testName,
				makeAndCheckGraphic(jpg, MPG + graphicConf.getSitiGraphicFilename() + ext, fullHeight));
		tests.put("Video block" + testName,
				makeAndCheckGraphic(jpg, MPG + graphicConf.getBlockGraphicFilename() + ext, fullHeight));
		tests.put("Video blur" + testName,
				makeAndCheckGraphic(jpg, MPG + graphicConf.getBlurGraphicFilename() + ext, fullHeight));
		tests.put("Video idet" + testName,
				makeAndCheckGraphic(jpg, MPG + graphicConf.getItetGraphicFilename() + ext, halfHeight));
		tests.put("Video crop" + testName,
				makeAndCheckGraphic(jpg, MPG + graphicConf.getCropGraphicFilename() + ext, fullHeight));
		tests.put("Events" + testName,
				makeAndCheckGraphic(jpg, MPG + graphicConf.getEventsGraphicFilename() + ext, halfHeight));
		tests.put("Video bitrate" + testName,
				makeAndCheckGraphic(jpg, MPG + graphicConf.getVBitrateGraphicFilename() + ext, fullHeight));
		tests.put("Audio bitrate" + testName,
				makeAndCheckGraphic(jpg, MPG + graphicConf.getABitrateGraphicFilename() + ext, halfHeight));
		tests.put("Video frame duration" + testName,
				makeAndCheckGraphic(jpg, MPG + graphicConf.getVFrameDurationGraphicFilename() + ext, halfHeight));
		tests.put("Video GOP counts" + testName,
				makeAndCheckGraphic(jpg, MPG + graphicConf.getGopCountGraphicFilename() + ext, halfHeight));
		tests.put("Video GOP sizes" + testName,
				makeAndCheckGraphic(jpg, MPG + graphicConf.getGopSizeGraphicFilename() + ext, fullHeight));
		return tests;
	}

	private Executable makeAndCheckGraphic(final boolean jpg,
										   final String baseFileName,
										   final Dimension expectedImageSize) {
		return () -> {
			final var outputFile = new File("target/e2e-export", baseFileName);
			if (outputFile.exists() == false) {
				runApp(
						"--temp", "target/e2e-temp",
						"-i", rawData.archive().getPath(),
						"-f", "graphic",
						"-e", "target/e2e-export",
						"--export-base-filename", "mpg",
						jpg ? "--graphic-jpg" : null);
				checkImageGraphicInternal(outputFile, expectedImageSize);
			}
		};
	}

	private static record HSV(float hue, float sat, float value) {
		static HSV get(final float[] hsbvals) {
			return new HSV(hsbvals[0], hsbvals[1], hsbvals[2]);
		}
	}

	private void checkImageGraphicInternal(final File outputFile,
										   final Dimension expectedImageSize) throws IOException {
		final var image = ImageIO.read(outputFile);
		assertEquals(expectedImageSize.getWidth(), image.getWidth());
		assertEquals(expectedImageSize.getHeight(), image.getHeight());

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

		assertThat(allColors).hasSizeGreaterThan(100);

		final var greyCount = allColors.parallelStream().filter(hsv -> hsv.sat == 0f).count();
		assertThat(greyCount).isGreaterThan(100);

		final var fullSat = allColors.parallelStream()
				.filter(hsv -> hsv.value > 0.1f)
				.filter(hsv -> hsv.sat == 1f)
				.count();
		assertThat(fullSat).isGreaterThan(20);

		final var greyCols = allColors.parallelStream()
				.map(HSV::value)
				.mapToInt(v -> Math.round(v * 10))
				.distinct()
				.summaryStatistics();
		assertEquals(10, greyCols.getMax());
		assertEquals(0, greyCols.getMin());
		assertEquals(5d, greyCols.getAverage());
	}

}
