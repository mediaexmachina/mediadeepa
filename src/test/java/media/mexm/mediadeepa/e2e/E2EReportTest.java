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

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import media.mexm.mediadeepa.ConstStrings;

class E2EReportTest extends E2EUtils implements ConstStrings {

	@Test
	void testHTML() throws IOException {
		rawData = prepareTsForSimpleE2ETests();
		if (rawData == null) {
			return;
		}
		final var outputFile = makeOutputFile("mpg_report.html");
		assertTrue(outputFile.exists());
		assertThat(outputFile.length()).isGreaterThan(30_000);
		final var content = E2EUtils.readLines(outputFile);
		assertThat(content).hasSizeGreaterThan(2);
		assertThat(content.get(0)).isEqualTo("<!DOCTYPE html>");
		assertThat(content.get(1)).startsWithIgnoringCase("<html lang=\"en\">");
		assertThat(content.get(content.size() - 1)).endsWithIgnoringCase("</html>");

		final var strContent = content.stream().collect(joining(" "));
		Stream.of(
				ABOUT_THIS_DOCUMENT,
				AUDIO_BITRATE,
				AUDIO_FRAMES,
				AUDIO_MEDIA_FILE_INFORMATION,
				BLACK_BORDERS_CROP_DETECTION,
				DOCUMENT_CREATION_DATE,
				FFMPEG_FILTERS_USED_IN_THIS_MEASURE,
				IMAGE_AND_MOTION_COMPLEXITY,
				IMAGE_BLUR_DETECTION,
				IMAGE_COMPRESSION_ARTIFACT_DETECTION,
				INTERLACING_DETECTION,
				LOUDNESS_EBU_R128,
				MEDIADEEPA_REPORT_DOCUMENT,
				MEDIA_CONTAINER,
				PHASE_CORRELATION,
				SIGNAL_STATS,
				SPATIAL_INFORMATION,
				STREAM_PACKETS,
				TARGET_SOURCE,
				TEMPORAL_INFORMATION,
				VIDEO_BITRATE,
				VIDEO_COMPRESSION_GROUP_OF_PICTURES,
				VIDEO_MEDIA_FILE_INFORMATION,
				VIDEO_FRAMES,
				"Black frames events",
				"Freeze (static) frames events",
				"Event")
				.forEach(f -> assertThat(strContent).contains(f));
	}

	File makeOutputFile(final String baseFileName) throws IOException {
		final var outputFile = new File("target/e2e-export", baseFileName);
		runApp(
				"--temp", "target/e2e-temp",
				"-i", rawData.archive().getPath(),
				"-f", "report",
				"-e", "target/e2e-export",
				"--export-base-filename", "mpg");
		return outputFile;
	}

}
