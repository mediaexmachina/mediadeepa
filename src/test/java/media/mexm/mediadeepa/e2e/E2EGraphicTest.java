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

import static media.mexm.mediadeepa.exportformat.graphic.DataGraphic.IMAGE_SIZE_FULL_HEIGHT;
import static media.mexm.mediadeepa.exportformat.graphic.DataGraphic.IMAGE_SIZE_HALF_HEIGHT;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.ABITRATE_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.A_PHASE_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.BLOCK_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.BLUR_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.CROP_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.DC_OFFSET_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.ENTROPY_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.EVENTS_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.FLATNESS_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.GOP_COUNT_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.GOP_SIZES_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.IDET_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.LUFS_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.LUFS_TPK_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.NOISE_FLOOR_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.PEAK_LEVEL_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.SITI_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.VBITRATE_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.VFRAMEDURATION_SUFFIX_FILE_NAME;
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
		tests.put("LUFS", checkImageGraphic(makeOutputFile(
				MPG + LUFS_SUFFIX_FILE_NAME), IMAGE_SIZE_FULL_HEIGHT));
		tests.put("LUFS TPK", checkImageGraphic(makeOutputFile(
				MPG + LUFS_TPK_SUFFIX_FILE_NAME), IMAGE_SIZE_FULL_HEIGHT));
		tests.put("Audio phase", checkImageGraphic(makeOutputFile(
				MPG + A_PHASE_SUFFIX_FILE_NAME), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Audio entropy", checkImageGraphic(makeOutputFile(
				MPG + ENTROPY_SUFFIX_FILE_NAME), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Audio flat-factor", checkImageGraphic(makeOutputFile(
				MPG + FLATNESS_SUFFIX_FILE_NAME), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Audio noise-floor", checkImageGraphic(makeOutputFile(
				MPG + NOISE_FLOOR_SUFFIX_FILE_NAME), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Audio peak-level", checkImageGraphic(makeOutputFile(
				MPG + PEAK_LEVEL_SUFFIX_FILE_NAME), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Audio DC offset", checkImageGraphic(makeOutputFile(
				MPG + DC_OFFSET_SUFFIX_FILE_NAME), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Video SITI", checkImageGraphic(makeOutputFile(
				MPG + SITI_SUFFIX_FILE_NAME), IMAGE_SIZE_FULL_HEIGHT));
		tests.put("Video block", checkImageGraphic(makeOutputFile(
				MPG + BLOCK_SUFFIX_FILE_NAME), IMAGE_SIZE_FULL_HEIGHT));
		tests.put("Video blur", checkImageGraphic(makeOutputFile(
				MPG + BLUR_SUFFIX_FILE_NAME), IMAGE_SIZE_FULL_HEIGHT));
		tests.put("Video idet", checkImageGraphic(makeOutputFile(
				MPG + IDET_SUFFIX_FILE_NAME), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Video crop", checkImageGraphic(makeOutputFile(
				MPG + CROP_SUFFIX_FILE_NAME), IMAGE_SIZE_FULL_HEIGHT));
		tests.put("Events", checkImageGraphic(makeOutputFile(
				MPG + EVENTS_SUFFIX_FILE_NAME), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Video bitrate", checkImageGraphic(makeOutputFile(
				MPG + VBITRATE_SUFFIX_FILE_NAME), IMAGE_SIZE_FULL_HEIGHT));
		tests.put("Audio bitrate", checkImageGraphic(makeOutputFile(
				MPG + ABITRATE_SUFFIX_FILE_NAME), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Video frame duration", checkImageGraphic(makeOutputFile(
				MPG + VFRAMEDURATION_SUFFIX_FILE_NAME), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Video GOP counts", checkImageGraphic(makeOutputFile(
				MPG + GOP_COUNT_SUFFIX_FILE_NAME), IMAGE_SIZE_HALF_HEIGHT));
		tests.put("Video GOP sizes", checkImageGraphic(makeOutputFile(
				MPG + GOP_SIZES_SUFFIX_FILE_NAME), IMAGE_SIZE_FULL_HEIGHT));
		return tests.entrySet().stream()
				.map(entry -> dynamicTest(entry.getKey(), entry.getValue()));
	}

	File makeOutputFile(final String baseFileName) throws IOException {
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
				"-f", "graphic",
				"-e", "target/e2e-export",
				"--export-base-filename", "mpg");
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
