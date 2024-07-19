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

import static java.lang.Math.round;
import static java.lang.System.lineSeparator;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static media.mexm.mediadeepa.e2e.E2ESpecificMediaFile.getFromMediaFile;
import static org.apache.commons.io.FilenameUtils.wildcardMatch;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class E2ETextTest extends E2EUtils {

	File e2eExportDir;

	public E2ETextTest() {
		e2eExportDir = new File("target/e2e-export");
	}

	@TestFactory
	Stream<DynamicTest> testTXT() {
		return ALL_MEDIA_FILE_NAME.stream()
				.filter(File::exists)
				.map(TestFullTXT::new)
				.map(TestFullTXT::toDynamicTest);
	}

	class TestFullTXT {

		final File mediafile;
		final String ext;
		final E2ERawOutDataFiles rawData;

		TestFullTXT(final File mediafile) {
			this.mediafile = mediafile;
			rawData = E2ERawOutDataFiles.create(mediafile);
			ext = rawData.getExtension();
		}

		DynamicTest toDynamicTest() {
			return dynamicTest(ext, this::checks);
		}

		void checks() throws IOException {
			extractRawTXT(rawData);
			FileUtils.forceMkdir(e2eExportDir);
			checkZipArchive();

			runApp(
					"--temp", "target/e2e-temp",
					"-i", rawData.archive().getPath(),
					"-f", "txt",
					"-e", "target/e2e-export",
					"--export-base-filename", rawData.getExtension());

			checkProcessTXT_ebur128Summary();
			checkProcessTXT_ebur128();
			checkProcessTXT_audioStats();
			checkProcessTXT_audioPhaseMeter();
			checkProcessTXT_audioConsts();
			checkProcessTXT_audioFrames();
			checkProcessTXT_containerPackets();
			checkProcessTXT_events();
			checkProcessTXT_mediaSummary();

			if (rawData.hasVideo()) {
				checkProcessTXT_containerVideoConst();
				checkProcessTXT_containerVideoFrames();
				checkProcessTXT_blockDectect();
				checkProcessTXT_blurDectect();
				checkProcessTXT_cropDectect();
				checkProcessTXT_interlaceDectect();
				checkProcessTXT_siti();
				checkProcessTXT_sitiStats();
			}

			var listExport = makeListExportTXT(e2eExportDir);
			if (listExport.isEmpty()) {
				processTXT();
				listExport = makeListExportTXT(e2eExportDir);
				assertFalse(listExport.isEmpty(), "Can't found exported TXT files for " + ext);
			}

			final var fExportIterator = listExport.iterator();
			while (fExportIterator.hasNext()) {
				final var fExport = fExportIterator.next();

				final var fProcess = new File("target/e2e-process", fExport.getName());
				if (fProcess.exists() == false) {
					processTXT();
				}
				assertTrue(fProcess.exists(), "Can't found " + fProcess);

				final var lExport = FileUtils.readLines(fExport, UTF_8);
				final var lProcess = FileUtils.readLines(fProcess, UTF_8);
				assertEquals(lExport.size(), lProcess.size(), fExport.getName());

				for (var pos = 0; pos < lExport.size(); pos++) {
					assertEquals(lExport.get(pos), lProcess.get(pos),
							"Difference in L" + (pos + 1) + " on " + fExport.getPath());
				}
			}

			final var filterFile = new File("target/e2e-process/" + ext + "_filters.txt");
			assertTrue(filterFile.exists(), "Can't found " + filterFile);
			final var lines = readLines(filterFile);
			assertTableWith(4, lines);
			assertEquals(4, lines.stream().filter(l -> l.startsWith("audio")).count());
			if (ext.equals("wav") == false) {
				assertEquals(7, lines.stream().filter(l -> l.startsWith("video")).count());
				assertEquals(12, lines.size());
			} else {
				assertEquals(5, lines.size());
			}

		}

		private void processTXT() {
			runApp("-i", mediafile.getPath(), "-c",
					"--temp", "target/e2e-temp",
					"-f", "txt",
					"-e", "target/e2e-process",
					"--export-base-filename", FilenameUtils.getExtension(mediafile.getName()));
		}

		private List<File> makeListExportTXT(final File e2eExportDir) {
			return Stream.of(e2eExportDir.listFiles())
					.filter(f -> f.getName().startsWith(ext + "_"))
					.filter(f -> f.getName().endsWith(".txt"))
					.filter(f -> f.getName().endsWith("filters.txt") == false)
					.filter(f -> f.getName().endsWith("media-summary.txt") == false)
					.filter(f -> f.getName().endsWith("_about.txt") == false)
					.toList();
		}

		void checkProcessTXT_containerVideoFrames() throws IOException {
			final var f = getProcessedTXTFromRaw(rawData, "container-video-frames.txt");
			final var lines = readLines(f);
			assertEquals(V_FRAME_COUNT + 1, lines.size());
			assertTableWith(14, lines);
			assertEquals(List.of("0\t"),
					lines.stream().skip(1).map(l -> l.substring(0, "0\t".length())).distinct().toList());
		}

		void checkProcessTXT_containerVideoConst() throws IOException {
			final var specificMediaFile = getFromMediaFile(rawData.mediaFile());
			final var f = getProcessedTXTFromRaw(rawData, "container-video-consts.txt");
			final var lines = readLines(f);
			assertTableWith(19, lines);
			switch (specificMediaFile) {
			case AVI:
				assertThat(lines.size()).isIn(2, V_FRAME_COUNT + 1);
				assertEquals(List.of(
						"352", "288", "1:1", "0", "0", "yuv420p",
						"", "", "", "", "0", "0", "0", "0", "0", "0", "1", "0.02", "9990"),
						getSplitedLine(lines, 1));
				break;
			case MKV:
				assertThat(lines.size()).isIn(2, V_FRAME_COUNT + 1);
				assertEquals(List.of(
						"352", "288", "1:1", "0", "0", "yuv420p",
						"", "", "", "", "0", "0", "0", "0", "0", "0", "40", "0.04", "706"),
						getSplitedLine(lines, 1));
				break;
			case MOV:
				assertThat(lines.size()).isIn(2, V_FRAME_COUNT + 1);
				assertEquals(List.of(
						"352", "288", "1:1", "0", "0", "yuv420p",
						"", "", "", "", "0", "0", "0", "0", "0", "0", "640", "0.04", "36"),
						getSplitedLine(lines, 1));
				break;
			case MPG:
				assertThat(lines.size()).isIn(2, V_FRAME_COUNT + 1);
				assertEquals(List.of(
						"352", "288", "1:1", "0", "0", "yuv420p",
						"tv", "", "", "", "48600", "0.54", "48600", "0.54", "48600", "0.54", "3600",
						"0.04", "30"),
						getSplitedLine(lines, 1));
				break;
			case TS:
				assertThat(lines.size()).isIn(2, V_FRAME_COUNT + 1);
				assertEquals(List.of(
						"352", "288", "1:1", "0", "0", "yuv420p",
						"tv", "", "", "", "129600", "1.44", "129600", "1.44", "129600", "1.44",
						"3600",
						"0.04", "564"),
						getSplitedLine(lines, 1));
				break;
			case MXF:
				assertThat(lines.size()).isIn(2, V_FRAME_COUNT + 1);
				assertEquals(List.of(
						"352", "288", "1:1", "0", "0", "yuv420p",
						"tv", "", "", "", "0", "0", "0", "0", "0", "0", "1", "0.04", "8192"),
						getSplitedLine(lines, 1));
				break;
			default:
				break;
			}
		}

		void checkProcessTXT_mediaSummary() throws IOException {
			final var specificMediaFile = getFromMediaFile(rawData.mediaFile());
			final var f = getProcessedTXTFromRaw(rawData, "media-summary.txt");
			final var lines = readLines(f);
			assertTableWith(2, lines);

			switch (specificMediaFile) {
			case AVI, MKV, MOV, MPG, TS:
				assertEquals(4, lines.size());
				break;
			case WAV:
				assertEquals(3, lines.size());
				break;
			case MXF:
				assertEquals(5, lines.size());
				break;
			}

		}

		void checkProcessTXT_events() throws IOException {
			final var specificMediaFile = getFromMediaFile(rawData.mediaFile());
			final var f = getProcessedTXTFromRaw(rawData, "events.txt");
			final var lines = readLines(f);
			assertTableWith(5, lines);
			switch (specificMediaFile) {
			case AVI, MKV, MOV:
				assertEquals(List.of("freeze", "", "00:00:00", "00:00:05", "00:00:05"),
						getSplitedLine(lines, 1));
				assertEquals(List.of("black", "", "00:00:05", "00:00:10", "00:00:05"),
						getSplitedLine(lines, 2));
				assertEquals(List.of("freeze", "", "00:00:05", "00:00:10", "00:00:05"),
						getSplitedLine(lines, 3));
				assertEquals(List.of("black", "", "00:00:20", "00:00:21", "00:00:01"),
						getSplitedLine(lines, 4));
				assertEquals(List.of("silence", "2", "00:00:36", "00:00:46", "00:00:10"),
						getSplitedLine(lines, 5));
				assertEquals(6, lines.size());
				break;
			case MPG, TS:
				assertEquals(List.of("freeze", "", "00:00:00.010", "00:00:05.010", "00:00:05"),
						getSplitedLine(lines, 1));
				assertEquals(List.of("black", "", "00:00:05.010", "00:00:10.010", "00:00:05"),
						getSplitedLine(lines, 2));
				assertEquals(List.of("freeze", "", "00:00:05.010", "00:00:10.010", "00:00:05"),
						getSplitedLine(lines, 3));
				assertEquals(List.of("black", "", "00:00:20.010", "00:00:21.010", "00:00:01"),
						getSplitedLine(lines, 4));
				assertEquals(5, lines.size());
				break;
			case WAV:
				assertEquals(List.of("silence", "2", "00:00:36", "00:00:46", "00:00:10"),
						getSplitedLine(lines, 1));
				assertEquals(2, lines.size());
				break;
			case MXF:
				assertEquals(List.of("freeze", "", "00:00:00", "00:00:05", "00:00:05"),
						getSplitedLine(lines, 1));
				assertEquals(List.of("black", "", "00:00:05", "00:00:10", "00:00:05"),
						getSplitedLine(lines, 2));
				assertEquals(List.of("freeze", "", "00:00:05", "00:00:10", "00:00:05"),
						getSplitedLine(lines, 3));
				assertEquals(List.of("black", "", "00:00:20", "00:00:21", "00:00:01"),
						getSplitedLine(lines, 4));
				assertEquals(5, lines.size());
				break;
			}
		}

		void checkProcessTXT_containerPackets() throws IOException {
			final var specificMediaFile = getFromMediaFile(rawData.mediaFile());
			final var f = getProcessedTXTFromRaw(rawData, "container-packets.txt");
			final var lines = readLines(f);
			assertTableWith(11, lines);
			if (rawData.hasVideo()) {
				if (specificMediaFile == E2ESpecificMediaFile.MXF) {
					assertEquals(V_FRAME_COUNT + specificMediaFile.alavfiFrameCount * 2 + 1, lines.size());
				} else {
					assertEquals(V_FRAME_COUNT + specificMediaFile.alavfiFrameCount + 1, lines.size());
				}
			} else {
				assertThat(lines.size()).isBetween(10, (int) specificMediaFile.alavfiFrameCount + 2);
			}
		}

		void checkProcessTXT_audioFrames() throws IOException {
			final var specificMediaFile = getFromMediaFile(rawData.mediaFile());
			final var f = getProcessedTXTFromRaw(rawData, "container-audio-frames.txt");
			final var lines = readLines(f);
			assertTableWith(12, lines);
			assertThat(lines.size()).isBetween(10, (int) specificMediaFile.containerAudioCount + 2);
		}

		void checkProcessTXT_audioConsts() throws IOException {
			final var f = getProcessedTXTFromRaw(rawData, "container-audio-consts.txt");
			final var lines = readLines(f);
			assertTableWith(12, lines);
			assertEquals(2, lines.size());
		}

		void checkProcessTXT_audioPhaseMeter() throws IOException {
			final var specificMediaFile = getFromMediaFile(rawData.mediaFile());
			final var f = getProcessedTXTFromRaw(rawData, "audio-phase-meter.txt");
			final var lines = readLines(f);
			assertTableWith(4, lines);
			assertThat(lines.size()).isBetween(10, (int) specificMediaFile.alavfiFrameCount + 2);
		}

		void checkProcessTXT_audioStats() throws IOException {
			final var specificMediaFile = getFromMediaFile(rawData.mediaFile());
			final var f = getProcessedTXTFromRaw(rawData, "audio-stats.txt");
			final var lines = readLines(f);
			assertTableWith(List.of(32), lines);
			assertThat(lines.size()).isBetween(60, (int) specificMediaFile.containerAudioCount * 2 + 2);
		}

		void checkProcessTXT_ebur128() throws IOException {
			final var f = getProcessedTXTFromRaw(rawData, "audio-ebur128.txt");
			final var lines = readLines(f);
			assertTableWith(9, lines);
			assertEquals(561, lines.size());
		}

		void checkProcessTXT_ebur128Summary() throws IOException {
			final var specificMediaFile = getFromMediaFile(rawData.mediaFile());
			final var f = getProcessedTXTFromRaw(rawData, "audio-ebur128-summary.txt");
			final var lines = readLines(f);
			assertTableWith(6, lines);

			final var lineHeader = Arrays.asList(lines.get(0).split("\t"));
			final var lineValues = Arrays.asList(lines.get(1).split("\t"));
			final var floatValues = IntStream.range(0, lineHeader.size())
					.mapToObj(i -> new KV<>(lineHeader.get(i), Float.valueOf(lineValues.get(i))))
					.collect(toUnmodifiableMap(KV::key, KV::value));

			assertEquals(-24, round(floatValues.get("Integrated")));
			if (specificMediaFile == E2ESpecificMediaFile.MXF) {
				assertEquals(20, round(floatValues.get("Loudness Range")));
				assertEquals(-39, round(floatValues.get("Loudness Range Low")));
				assertEquals(-19, round(floatValues.get("Loudness Range High")));
				assertEquals(-21, round(floatValues.get("Sample Peak")));
				assertEquals(-21, round(floatValues.get("True Peak")));
			} else {
				assertEquals(17, round(floatValues.get("Loudness Range")));
				assertEquals(-36, round(floatValues.get("Loudness Range Low")));
				assertEquals(-19, round(floatValues.get("Loudness Range High")));
				assertThat(floatValues.get("Sample Peak")).isCloseTo(-17, offset(2f));
				assertThat(floatValues.get("True Peak")).isCloseTo(-17, offset(2f));
			}
		}

		void checkProcessTXT_sitiStats() throws IOException {
			final var f = getProcessedTXTFromRaw(rawData, "video-siti-stats-ITU-T_P-910.txt");
			final var lines = readLines(f);
			assertTableWith(4, lines);
			assertEquals(3, lines.size());
			assertEquals(1, lines.stream().filter(l -> l.startsWith("Spatial Info")).count());
			assertEquals(1, lines.stream().filter(l -> l.startsWith("Temporal Info")).count());
		}

		void checkProcessTXT_siti() throws IOException {
			final var f = getProcessedTXTFromRaw(rawData, "video-siti-ITU-T_P-910.txt");
			final var lines = readLines(f);
			assertTableWith(5, lines);
			assertEquals(V_FRAME_COUNT + 1, lines.size());
		}

		void checkProcessTXT_interlaceDectect() throws IOException {
			final var f = getProcessedTXTFromRaw(rawData, "video-interlace-detect.txt");
			final var lines = readLines(f);
			assertTableWith(17, lines);
			assertEquals(V_FRAME_COUNT + 1, lines.size());
		}

		void checkProcessTXT_cropDectect() throws IOException {
			final var f = getProcessedTXTFromRaw(rawData, "video-crop-detect.txt");
			final var lines = readLines(f);
			assertTableWith(11, lines);
			assertEquals(V_FRAME_COUNT + 1, lines.size());
		}

		void checkProcessTXT_blurDectect() throws IOException {
			final var f = getProcessedTXTFromRaw(rawData, "video-blur-detect.txt");
			final var lines = readLines(f);
			/**
			 * Blur can output some empty cols (as "-nan" values returned by filter), here on black frames.
			 */
			assertTableWith(List.of(4, 3), lines);
			assertEquals(V_FRAME_COUNT + 1, lines.size());
		}

		void checkProcessTXT_blockDectect() throws IOException {
			final var f = getProcessedTXTFromRaw(rawData, "video-block-detect.txt");
			final var lines = readLines(f);
			assertTableWith(4, lines);
			assertEquals(V_FRAME_COUNT + 1, lines.size());
		}

		void checkZipArchive() throws IOException {
			final var archive = rawData.archive();
			assertThat(archive).exists().size().isGreaterThan(1);

			final var names = new HashSet<String>();
			try (var zipIn = new ZipInputStream(new FileInputStream(archive))) {
				ZipEntry zEntry;
				while ((zEntry = zipIn.getNextEntry()) != null) {
					names.add(zEntry.getName());
				}
			}

			if (rawData.hasVideo()) {
				assertThat(names).containsOnly(
						zippedTxtFileNames.getVersionJson(),
						zippedTxtFileNames.getCommandLineJson(),
						zippedTxtFileNames.getFiltersJson(),
						zippedTxtFileNames.getContainerXml(),
						zippedTxtFileNames.getFfprobeXml(),
						zippedTxtFileNames.getLavfiTxtBase() + "0.txt",
						zippedTxtFileNames.getLavfiTxtBase() + "1.txt",
						zippedTxtFileNames.getMeasuredWavJson(),
						zippedTxtFileNames.getSummaryTxt(),
						zippedTxtFileNames.getFfmpegCommandLineTxt(),
						zippedTxtFileNames.getFfprobeCommandLineTxt(),
						zippedTxtFileNames.getSourceNameTxt());
			} else {
				assertThat(names).containsOnly(
						zippedTxtFileNames.getVersionJson(),
						zippedTxtFileNames.getCommandLineJson(),
						zippedTxtFileNames.getFiltersJson(),
						zippedTxtFileNames.getContainerXml(),
						zippedTxtFileNames.getFfprobeXml(),
						zippedTxtFileNames.getLavfiTxtBase() + "0.txt",
						zippedTxtFileNames.getMeasuredWavJson(),
						zippedTxtFileNames.getSummaryTxt(),
						zippedTxtFileNames.getFfmpegCommandLineTxt(),
						zippedTxtFileNames.getFfprobeCommandLineTxt(),
						zippedTxtFileNames.getSourceNameTxt());
			}
		}
	}

	@Test
	void testLongFile() throws IOException {
		final var rawData = E2ERawOutDataFiles.create(MEDIA_FILE_NAME_LONG);
		if (MEDIA_FILE_NAME_LONG.exists() == false) {
			return;
		}
		final var exportBaseFilename = "long-mkv";
		extractRawTXT(rawData);
		assertTrue(rawData.allOutExists());

		runApp(() -> new File("target/e2e-export", "long-mkv_about.txt").exists(),
				"--temp", "target/e2e-temp",
				"-i", rawData.archive().getPath(),
				"-f", "txt",
				"-e", "target/e2e-export",
				"--export-base-filename", exportBaseFilename);

		assertEquals(144001,
				countLinesExportDir("long-mkv_audio-ebur128.txt"));
		assertEquals(2,
				countLinesExportDir("long-mkv_audio-ebur128-summary.txt"));
		assertThat(countLinesExportDir("long-mkv_audio-phase-meter.txt")).isBetween(100_000, 600_002);
		assertThat(countLinesExportDir("long-mkv_audio-stats.txt")).isBetween(288_003, 1_200_003);
		assertEquals(2,
				countLinesExportDir("long-mkv_container-audio-consts.txt"));
		assertEquals(600002,
				countLinesExportDir("long-mkv_container-audio-frames.txt"));
		assertEquals(960002,
				countLinesExportDir("long-mkv_container-packets.txt"));
		assertEquals(2,
				countLinesExportDir("long-mkv_container-video-consts.txt"));
		assertEquals(360001,
				countLinesExportDir("long-mkv_container-video-frames.txt"));
		assertEquals(36001,
				countLinesExportDir("long-mkv_container-video-gop.txt"));
		assertEquals(360001,
				countLinesExportDir("long-mkv_video-block-detect.txt"));
		assertEquals(360001,
				countLinesExportDir("long-mkv_video-blur-detect.txt"));
		assertEquals(360001,
				countLinesExportDir("long-mkv_video-crop-detect.txt"));
		assertEquals(360001,
				countLinesExportDir("long-mkv_video-interlace-detect.txt"));
		assertEquals(360001,
				countLinesExportDir("long-mkv_video-siti-ITU-T_P-910.txt"));
		assertEquals(3,
				countLinesExportDir("long-mkv_video-siti-stats-ITU-T_P-910.txt"));
		assertTrue(countLinesExportDir("long-mkv_about.txt") > 0);
	}

	static final int EXPECTED_OUT_FILES_COUNT_SOURCE_VID = 16;
	static final int EXPECTED_OUT_FILES_COUNT_SOURCE_AUD = 8;

	@Nested
	class Multiple {

		List<E2ERawOutDataFiles> inputs;
		File exportDir;
		List<String> params;
		String exportBaseFilename;

		@AfterAll
		static void open() {
			System.clearProperty("mediadeepa.addSourceExtToOutputDirectories");
		}

		@BeforeEach
		void init() throws IOException {
			System.setProperty("mediadeepa.addSourceExtToOutputDirectories", "true");

			inputs = ALL_MEDIA_FILE_NAME.stream()
					.filter(File::exists)
					.map(E2ERawOutDataFiles::create)
					.toList();
			if (inputs.isEmpty() == false) {
				for (final var rawData : inputs) {
					extractRawTXT(rawData);
					assertTrue(rawData.allOutExists());
				}
			}

			exportDir = new File("target/e2e-export");
			FileUtils.forceMkdir(exportDir);
		}

		@Test
		void testMultipleInputs() throws IOException {// NOSONAR S2699
			if (inputs.isEmpty()) {
				return;
			}

			exportBaseFilename = "e2e-multiple";
			params = new ArrayList<>(List.of(
					"--temp", "target/e2e-temp",
					"-f", "txt",
					"-e", exportDir.getPath(),
					"--export-base-filename", exportBaseFilename));
			params.addAll(inputs.stream()
					.map(E2ERawOutDataFiles::archive)
					.map(File::getPath)
					.flatMap(fileName -> Stream.of("-i", fileName))
					.toList());
		}

		@Test
		void testListInput() throws IOException {// NOSONAR S2699
			if (inputs.isEmpty()) {
				return;
			}

			final var textList = inputs.stream()
					.map(E2ERawOutDataFiles::archive)
					.map(File::getPath)
					.collect(joining(lineSeparator()));
			final var inputList = new File(exportDir, "input-list.txt");
			FileUtils.write(inputList, textList + lineSeparator(), defaultCharset());

			exportBaseFilename = "e2e-list-input";
			params = new ArrayList<>(List.of(
					"--temp", "target/e2e-temp",
					"-f", "txt",
					"-e", exportDir.getPath(),
					"--export-base-filename", exportBaseFilename,
					"--input-list", inputList.getPath()));
		}

		@AfterEach
		void ends() {
			if (inputs.isEmpty()) {
				return;
			}

			final var expectedFileCount = (int) inputs.stream()
					.filter(E2ERawOutDataFiles::hasVideo)
					.count() * EXPECTED_OUT_FILES_COUNT_SOURCE_VID + (int) inputs.stream()
							.filter(not(E2ERawOutDataFiles::hasVideo))
							.count() * EXPECTED_OUT_FILES_COUNT_SOURCE_AUD;
			final var realFileCountBefore = getCurrentFileCountMultipleInputs();

			runApp(() -> realFileCountBefore >= expectedFileCount,
					params.toArray(new String[params.size()]));

			final var realFileCountAfter = getCurrentFileCountMultipleInputs();
			assertThat(realFileCountAfter).isGreaterThanOrEqualTo(expectedFileCount);

			System.clearProperty("mediadeepa.addSourceExtToOutputDirectories");
		}

		private int getCurrentFileCountMultipleInputs() {
			return (int) inputs.stream()
					.map(E2ERawOutDataFiles::mediaFile)
					.map(File::getName)
					.map(sourceName -> sourceName + "_" + exportBaseFilename)
					.flatMap(fileNamePrefix -> Stream.of(
							exportDir.listFiles(f -> wildcardMatch(f.getName(), fileNamePrefix + "*"))))
					.filter(f -> f.length() > 0)
					.count();
		}
	}

	@Test
	void extractAndProcessContainerOnly() {
		if (MEDIA_FILE_NAME_AVI.exists() == false) {
			return;
		}

		final var zipFile = new File("target/e2e/container-only-avi.zip");
		runApp(() -> zipFile.exists(),
				"--temp", "target/e2e-temp",
				"-i", MEDIA_FILE_NAME_AVI.getPath(),
				"-c", "-mn",
				"--extract", zipFile.getPath());

		assertThat(zipFile).exists().size().isGreaterThan(100);

		final var expectedResult = new File("target/e2e-export/container-only_media-summary.txt");
		runApp(() -> expectedResult.exists(),
				"--temp", "target/e2e-temp",
				"-i", zipFile.getPath(),
				"-f", "txt",
				"-e", "target/e2e-export",
				"--export-base-filename", "container-only");

		assertThat(expectedResult).exists().size().isGreaterThan(80);
	}

}
