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

import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.ABITRATE_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.A_PHASE_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.BLOCK_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.BLUR_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.CROP_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.DC_OFFSET_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.ENTROPY_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.EVENTS_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.FLAT_FACTOR_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.IDET_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.LUFS_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.LUFS_TPK_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.NOISE_FLOOR_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.PEAK_LEVEL_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.SITI_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.VBITRATE_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.GraphicExportFormat.VFRAMEDURATION_SUFFIX_FILE_NAME;
import static media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.IMAGE_SIZE_FULL_HEIGHT;
import static media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.IMAGE_SIZE_HALF_HEIGHT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class E2EGraphicTest extends E2EUtils {

	private static final String MOV = "mov_";
	E2ERawOutDataFiles rawData;
	ThreadLocal<float[]> hsbvalsThreadLocal;

	@BeforeEach
	void init() {
		hsbvalsThreadLocal = ThreadLocal.withInitial(() -> new float[3]);
	}

	@TestFactory
	Stream<DynamicTest> testTable() throws IOException {
		rawData = prepareMovForSimpleE2ETests();
		if (rawData == null) {
			return Stream.empty();
		}
		return Stream.of(
				dynamicTest("LUFS",
						() -> checkImageGraphic(makeOutputFile(MOV + LUFS_SUFFIX_FILE_NAME),
								IMAGE_SIZE_FULL_HEIGHT)),
				dynamicTest("LUFS TPK",
						() -> checkImageGraphic(makeOutputFile(MOV + LUFS_TPK_SUFFIX_FILE_NAME),
								IMAGE_SIZE_FULL_HEIGHT)),
				dynamicTest("Audio phase",
						() -> checkImageGraphic(makeOutputFile(MOV + A_PHASE_SUFFIX_FILE_NAME),
								IMAGE_SIZE_HALF_HEIGHT)),
				dynamicTest("Audio entropy",
						() -> checkImageGraphic(makeOutputFile(MOV + ENTROPY_SUFFIX_FILE_NAME),
								IMAGE_SIZE_HALF_HEIGHT)),
				dynamicTest("Audio flat-factor",
						() -> checkImageGraphic(makeOutputFile(MOV + FLAT_FACTOR_SUFFIX_FILE_NAME),
								IMAGE_SIZE_HALF_HEIGHT)),
				dynamicTest("Audio noise-floor",
						() -> checkImageGraphic(makeOutputFile(MOV + NOISE_FLOOR_SUFFIX_FILE_NAME),
								IMAGE_SIZE_HALF_HEIGHT)),
				dynamicTest("Audio peak-level",
						() -> checkImageGraphic(makeOutputFile(MOV + PEAK_LEVEL_SUFFIX_FILE_NAME),
								IMAGE_SIZE_HALF_HEIGHT)),
				dynamicTest("Audio DC offset",
						() -> checkImageGraphic(makeOutputFile(MOV + DC_OFFSET_SUFFIX_FILE_NAME),
								IMAGE_SIZE_HALF_HEIGHT)),
				dynamicTest("Video SITI",
						() -> checkImageGraphic(makeOutputFile(MOV + SITI_SUFFIX_FILE_NAME),
								IMAGE_SIZE_FULL_HEIGHT)),
				dynamicTest("Video block",
						() -> checkImageGraphic(makeOutputFile(MOV + BLOCK_SUFFIX_FILE_NAME),
								IMAGE_SIZE_FULL_HEIGHT)),
				dynamicTest("Video blur",
						() -> checkImageGraphic(makeOutputFile(MOV + BLUR_SUFFIX_FILE_NAME),
								IMAGE_SIZE_FULL_HEIGHT)),
				dynamicTest("Video idet",
						() -> checkImageGraphic(makeOutputFile(MOV + IDET_SUFFIX_FILE_NAME),
								IMAGE_SIZE_HALF_HEIGHT)),
				dynamicTest("Video crop",
						() -> checkImageGraphic(makeOutputFile(MOV + CROP_SUFFIX_FILE_NAME),
								IMAGE_SIZE_FULL_HEIGHT)),
				dynamicTest("Events",
						() -> checkImageGraphic(makeOutputFile(MOV + EVENTS_SUFFIX_FILE_NAME),
								IMAGE_SIZE_HALF_HEIGHT)),
				dynamicTest("Video bitrate",
						() -> checkImageGraphic(makeOutputFile(MOV + VBITRATE_SUFFIX_FILE_NAME),
								IMAGE_SIZE_FULL_HEIGHT)),
				dynamicTest("Audio bitrate",
						() -> checkImageGraphic(makeOutputFile(MOV + ABITRATE_SUFFIX_FILE_NAME),
								IMAGE_SIZE_HALF_HEIGHT)),
				dynamicTest("Video frame duration",
						() -> checkImageGraphic(makeOutputFile(MOV + VFRAMEDURATION_SUFFIX_FILE_NAME),
								IMAGE_SIZE_HALF_HEIGHT)));
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
				"--export-base-filename", "mov");
		return outputFile;
	}

	private static record HSV(float hue, float sat, float value) {
		static HSV get(final float[] hsbvals) {
			return new HSV(hsbvals[0], hsbvals[1], hsbvals[2]);
		}
	}

	private void checkImageGraphic(final File outputFile,
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

		assertThat(allColors).hasSizeGreaterThan(1000);

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
