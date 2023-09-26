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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class E2ECSVTest extends E2EUtils {

	@Test
	void testCSV_classic() throws IOException {
		final var rawData = prepareMpgForSimpleE2ETests();
		if (rawData == null) {
			return;
		}

		final var outputFileMediaSum = new File("target/e2e-export", "mpg_media-summary.csv");
		if (outputFileMediaSum.exists() == false) {
			runApp(
					"--temp", "target/e2e-temp",
					"--import-lavfi", rawData.outAlavfi().getPath(),
					"--import-lavfi", rawData.outVlavfi().getPath(),
					"--import-stderr", rawData.outStderr().getPath(),
					"--import-probeheaders", rawData.outProbeheaders().getPath(),
					"--import-container", rawData.outContainer().getPath(),
					"-f", "csv",
					"-e", "target/e2e-export",
					"--export-base-filename", "mpg");
		}
		var lines = readLines(outputFileMediaSum);
		assertEquals(List.of(
				"Type,Value",
				"Stream,video: mpeg2video 352×288 Main/Main with B frames @ 25 fps yuv420p/colRange:TV",
				"Stream,audio: mp2 s16p stereo @ 48000 Hz [256 kbps]",
				"Format,\"MPEG-PS (MPEG-2 Program Stream), 00:00:56, 17 MB\""), lines);

		final var outputFileEbur128 = new File("target/e2e-export", "mpg_audio-ebur128-summary.csv");
		lines = readLines(outputFileEbur128);

		assertEquals(2, lines.size());
		assertThat(lines.get(0)).isEqualTo(
				"Integrated,Integrated Threshold,Loudness Range,Loudness Range Threshold,Loudness Range Low,Loudness Range High,Sample Peak,True Peak");
		assertThat(lines.get(1)).startsWith(
				"-24.5,-35.3,17.3,-45.1,-36.2,-18.9,-17.6,-17.6");
		/** "True Peak = -17.8" Not enough stable with some ffmpeg builds */

		final var csvCount = Stream.of(new File("target/e2e-export").listFiles())
				.filter(f -> f.getName().startsWith("mpg_")
							 && f.getName().endsWith(".csv"))
				.count();
		assertEquals(20, csvCount);
	}

	@Test
	void testCSV_frenchFlavor() throws IOException {
		final var rawData = prepareMovForSimpleE2ETests();
		if (rawData == null) {
			return;
		}

		final var outputFileMediaSum = new File("target/e2e-export", "movfr_media-summary.csv");
		if (outputFileMediaSum.exists() == false) {
			runApp(
					"--temp", "target/e2e-temp",
					"--import-lavfi", rawData.outAlavfi().getPath(),
					"--import-lavfi", rawData.outVlavfi().getPath(),
					"--import-stderr", rawData.outStderr().getPath(),
					"--import-probeheaders", rawData.outProbeheaders().getPath(),
					"--import-container", rawData.outContainer().getPath(),
					"-f", "csvfr",
					"-e", "target/e2e-export",
					"--export-base-filename", "movfr");
		}

		final var outputFileEbur128 = new File("target/e2e-export", "movfr_audio-ebur128-summary.csv");
		final var lines = readLines(outputFileEbur128);
		assertEquals(2, lines.size());
		assertThat(lines.get(0)).isEqualTo(
				"Integrated;Integrated Threshold;Loudness Range;Loudness Range Threshold;Loudness Range Low;Loudness Range High;Sample Peak;True Peak");
		assertThat(lines.get(1)).startsWith(
				"-24,4;-35,1;17,2;-45;-36,1;-18,8;-18;-17,");
		/** "True Peak = -17,8" Not enough stable with some ffmpeg builds */

		final var csvCount = Stream.of(new File("target/e2e-export").listFiles())
				.filter(f -> f.getName().startsWith("movfr_")
							 && f.getName().endsWith(".csv"))
				.count();
		assertEquals(19, csvCount);
	}

}
