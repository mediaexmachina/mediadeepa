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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

class E2EGraphicTest extends E2EUtils {

	private static final String MPG = "mpg_";
	E2ERawOutDataFiles rawData;
	ThreadLocal<float[]> hsbvalsThreadLocal;

	@BeforeEach
	void init() {
		hsbvalsThreadLocal = ThreadLocal.withInitial(() -> new float[3]);
	}

	@TestFactory
	Stream<DynamicTest> testTable() throws IOException {
		rawData = prepareMpgForSimpleE2ETests();
		if (rawData == null) {
			return Stream.empty();
		}

		final var tests = new LinkedHashMap<String, Executable>();

		final var IMAGE_SIZE_FULL_HEIGHT = defaultAppConfig.getGraphicConfig().getImageSizeFullSize();
		final var IMAGE_SIZE_HALF_HEIGHT = defaultAppConfig.getGraphicConfig().getImageSizeHalfSize();

		doAllTests(tests, IMAGE_SIZE_FULL_HEIGHT, IMAGE_SIZE_HALF_HEIGHT, false);
		doAllTests(tests, IMAGE_SIZE_FULL_HEIGHT, IMAGE_SIZE_HALF_HEIGHT, true);
		return tests.entrySet().stream()
				.map(entry -> dynamicTest(entry.getKey(), entry.getValue()));
	}

	private void doAllTests(final LinkedHashMap<String, Executable> tests,
							final Dimension IMAGE_SIZE_FULL_HEIGHT,
							final Dimension IMAGE_SIZE_HALF_HEIGHT,
							final boolean jpg) throws IOException {
		final var testName = jpg ? " JPEG" : " PNG";
		final var graphicConf = defaultAppConfig.getGraphicConfig();

		tests.put("LUFS" + testName,
				checkImageGraphic(makeOutputFile(jpg,
						MPG + graphicConf.getLufsGraphicFilename()), IMAGE_SIZE_FULL_HEIGHT));
		tests.put("LUFS TPK" + testName,
				checkImageGraphic(makeOutputFile(jpg,
						MPG + graphicConf.getLufsTPKGraphicFilename()), IMAGE_SIZE_FULL_HEIGHT));
		tests.put("Audio phase" + testName,
				checkImageGraphic(makeOutputFile(jpg,
						MPG + graphicConf.getAPhaseGraphicFilename()), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Audio entropy" + testName,
				checkImageGraphic(makeOutputFile(jpg,
						MPG + graphicConf.getEntropyGraphicFilename()), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Audio flat-factor" + testName,
				checkImageGraphic(makeOutputFile(jpg,
						MPG + graphicConf.getFlatnessGraphicFilename()), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Audio noise-floor" + testName,
				checkImageGraphic(makeOutputFile(jpg,
						MPG + graphicConf.getNoiseFloorGraphicFilename()), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Audio peak-level" + testName,
				checkImageGraphic(makeOutputFile(jpg,
						MPG + graphicConf.getPeakLevelGraphicFilename()), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Audio DC offset" + testName,
				checkImageGraphic(makeOutputFile(jpg,
						MPG + graphicConf.getDcOffsetGraphicFilename()), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Video SITI" + testName,
				checkImageGraphic(makeOutputFile(jpg,
						MPG + graphicConf.getSitiGraphicFilename()), IMAGE_SIZE_FULL_HEIGHT));
		tests.put("Video block" + testName,
				checkImageGraphic(makeOutputFile(jpg,
						MPG + graphicConf.getBlockGraphicFilename()), IMAGE_SIZE_FULL_HEIGHT));
		tests.put("Video blur" + testName,
				checkImageGraphic(makeOutputFile(jpg,
						MPG + graphicConf.getBlurGraphicFilename()), IMAGE_SIZE_FULL_HEIGHT));
		tests.put("Video idet" + testName,
				checkImageGraphic(makeOutputFile(jpg,
						MPG + graphicConf.getItetGraphicFilename()), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Video crop" + testName,
				checkImageGraphic(makeOutputFile(jpg,
						MPG + graphicConf.getCropGraphicFilename()), IMAGE_SIZE_FULL_HEIGHT));
		tests.put("Events" + testName,
				checkImageGraphic(makeOutputFile(jpg,
						MPG + graphicConf.getEventsGraphicFilename()), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Video bitrate" + testName,
				checkImageGraphic(makeOutputFile(jpg,
						MPG + graphicConf.getVBitrateGraphicFilename()), IMAGE_SIZE_FULL_HEIGHT));
		tests.put("Audio bitrate" + testName,
				checkImageGraphic(makeOutputFile(jpg,
						MPG + graphicConf.getABitrateGraphicFilename()), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Video frame duration" + testName,
				checkImageGraphic(makeOutputFile(jpg,
						MPG + graphicConf.getVFrameDurationGraphicFilename()), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Video GOP counts" + testName,
				checkImageGraphic(makeOutputFile(jpg,
						MPG + graphicConf.getGopCountGraphicFilename()), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Video GOP sizes" + testName,
				checkImageGraphic(makeOutputFile(jpg,
						MPG + graphicConf.getGopSizeGraphicFilename()), IMAGE_SIZE_FULL_HEIGHT));
	}

	File makeOutputFile(final boolean jpg, final String baseFileName) throws IOException {
		final var outputFile = new File("target/e2e-export", baseFileName);
		runApp(
				"--temp", "target/e2e-temp",
				"--import", rawData.archive().getPath(),
				"-f", "graphic",
				"-e", "target/e2e-export",
				"--export-base-filename", "mpg",
				jpg ? "--graphic-jpg" : null);
		return outputFile;
	}

	private static record HSV(float hue, float sat, float value) {
		static HSV get(final float[] hsbvals) {
			return new HSV(hsbvals[0], hsbvals[1], hsbvals[2]);
		}
	}

	private Executable checkImageGraphic(final File outputFile,
										 final Dimension expectedImageSize) {
		return () -> checkImageGraphicInternal(outputFile, expectedImageSize);
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
