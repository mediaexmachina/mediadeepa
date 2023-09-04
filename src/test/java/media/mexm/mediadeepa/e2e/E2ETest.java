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
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static media.mexm.mediadeepa.e2e.E2ESpecificMediaFile.getFromMediaFile;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import media.mexm.mediadeepa.App;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;

class E2ETest extends E2EUtils implements E2ERunners {

	@TestFactory
	Stream<DynamicTest> testTXT() {
		return ALL_MEDIA_FILE_NAME.stream()
				.filter(File::exists)
				.flatMap(this::applyTXT)
				.filter(Objects::nonNull);
	}

	Stream<DynamicTest> applyTXT(final File mediaFile) {
		final var rawData = E2ERawOutDataFiles.create(mediaFile);
		final var ext = rawData.getExtension();
		final var first = Stream.of(
				dynamicTest(ext + " to rawTXT", () -> extractRawTXT(rawData)),
				rawData.hasVideo()
								   ? dynamicTest(ext + " rawTXT checkRawVlavfi",
										   () -> checkRawVlavfi(rawData))
								   : null,
				dynamicTest(ext + " rawTXT checkRawAlavfi",
						() -> checkRaw_Alavfi(rawData)),
				dynamicTest(ext + " rawTXT checkRawStdErr",
						() -> checkRaw_StdErr(rawData)),
				dynamicTest(ext + " rawTXT checkRawProbeHeaders",
						() -> checkRaw_ProbeHeaders(rawData)),
				dynamicTest(ext + " rawTXT checkRawProbeSummary",
						() -> checkRaw_ProbeSummary(rawData)),
				dynamicTest(ext + " rawTXT checkRawContainer",
						() -> checkRaw_Container(rawData)),
				dynamicTest(ext + " rawTXT to processTXT",
						() -> importRawTXTToProcess(rawData)),
				dynamicTest(ext + " processTXT ebur128Summary",
						() -> checkProcessTXT_ebur128Summary(rawData)),
				dynamicTest(ext + " processTXT ebur128",
						() -> checkProcessTXT_ebur128(rawData)),
				dynamicTest(ext + " processTXT audioStats",
						() -> checkProcessTXT_audioStats(rawData)),
				dynamicTest(ext + " processTXT audioPhaseMeter",
						() -> checkProcessTXT_audioPhaseMeter(rawData)),
				dynamicTest(ext + " processTXT audioConsts",
						() -> checkProcessTXT_audioConsts(rawData)),
				dynamicTest(ext + " processTXT audioFrames",
						() -> checkProcessTXT_audioFrames(rawData)),
				dynamicTest(ext + " processTXT containerPackets",
						() -> checkProcessTXT_containerPackets(rawData)),
				dynamicTest(ext + " processTXT events",
						() -> checkProcessTXT_events(rawData)),
				dynamicTest(ext + " processTXT ffprobeXML",
						() -> checkProcessTXT_ffprobeXML(rawData)),
				dynamicTest(ext + " processTXT stderr",
						() -> checkProcessTXT_stderr(rawData)),
				rawData.hasVideo()
								   ? dynamicTest(ext + " processTXT containerVideoConst",
										   () -> checkProcessTXT_containerVideoConst(rawData))
								   : null,
				rawData.hasVideo()
								   ? dynamicTest(ext + " processTXT containerVideoFrames",
										   () -> checkProcessTXT_containerVideoFrames(rawData))
								   : null,
				rawData.hasVideo()
								   ? dynamicTest(ext + " processTXT blockDectect",
										   () -> checkProcessTXT_blockDectect(rawData))
								   : null,
				rawData.hasVideo()
								   ? dynamicTest(ext + " processTXT blurDectect",
										   () -> checkProcessTXT_blurDectect(rawData))
								   : null,
				rawData.hasVideo()
								   ? dynamicTest(ext + " processTXT cropDectect",
										   () -> checkProcessTXT_cropDectect(rawData))
								   : null,
				rawData.hasVideo()
								   ? dynamicTest(ext + " processTXT interlaceDectect",
										   () -> checkProcessTXT_interlaceDectect(rawData))
								   : null,
				rawData.hasVideo()
								   ? dynamicTest(ext + " processTXT siti",
										   () -> checkProcessTXT_siti(rawData))
								   : null,
				rawData.hasVideo()
								   ? dynamicTest(ext + " processTXT sitiStats",
										   () -> checkProcessTXT_sitiStats(rawData))
								   : null,
				hasNotAlreadyProcessTXT(ext)
											 ? dynamicTest(
													 ext + " direct to process", () -> processTXT(mediaFile))
											 : null);
		return Stream.concat(first, checkProcessTXT(ext));
	}

	boolean hasNotAlreadyProcessTXT(final String ext) {
		return new File("target/e2e-process/" + ext + "_filters.txt").exists() == false;
	}

	Stream<DynamicTest> checkProcessTXT(final String ext) {
		final var e2eExportDir = new File("target/e2e-export");
		try {
			FileUtils.forceMkdir(e2eExportDir);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't create " + e2eExportDir, e);
		}

		final var listExport = Stream.of(e2eExportDir.listFiles())
				.filter(f -> f.getName().startsWith(ext + "_"))
				.toList();

		return Stream.concat(
				listExport.stream().map(fExport -> {
					final var fProcess = new File("target/e2e-process", fExport.getName());
					return dynamicTest(ext + " check processTXT " + getBaseName(fProcess.getName()),
							() -> {
								assertTrue(fProcess.exists(), "Can't found " + fProcess);

								final var lExport = FileUtils.readLines(fExport, UTF_8);
								final var lProcess = FileUtils.readLines(fProcess, UTF_8);
								assertEquals(lExport.size(), lProcess.size());

								for (var pos = 0; pos < lExport.size(); pos++) {
									assertEquals(lExport.get(pos), lProcess.get(pos), "Difference in L" + pos + 1);
								}
							});
				}),
				Stream.of(dynamicTest(ext + " check processTXT filter",
						() -> {
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
						})));
	}

	void checkRaw_ProbeSummary(final E2ERawOutDataFiles rawData) throws IOException {
		final var specificMediaFile = rawData.getSpecificMediaFile();
		final var lines = readLines(rawData.outProbesummary());
		assertEquals(lines, specificMediaFile.probesummary);
	}

	void checkRaw_Container(final E2ERawOutDataFiles rawData) throws IOException {
		final var specificMediaFile = rawData.getSpecificMediaFile();
		final var lines = readLines(rawData.outContainer())
				.stream()
				.map(String::trim)
				.toList();
		if (rawData.hasVideo()) {
			assertEqualsNbLines(1400, lines, "<packet codec_type=\"video\"");
			assertEqualsNbLines(1400, lines, "<frame media_type=\"video\"");
		}
		assertEqualsNbLines(specificMediaFile.containerAudioCount, lines, "<packet codec_type=\"audio\"");
		assertEqualsNbLines(specificMediaFile.containerAudioCount, lines, "<frame media_type=\"audio\"");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", lines.get(0));
		assertEquals("<ffprobe>", lines.get(1));
		assertEquals("<packets_and_frames>", lines.get(2));
		assertEquals("</packets_and_frames>", lines.get(lines.size() - 2));
		assertEquals("</ffprobe>", lines.get(lines.size() - 1));
	}

	void checkRaw_ProbeHeaders(final E2ERawOutDataFiles rawData) throws IOException {
		final var specificMediaFile = rawData.getSpecificMediaFile();
		assertTrue(rawData.outProbeheaders().exists());
		final var xmlContent = FileUtils.readFileToString(rawData.outProbeheaders(), UTF_8);
		final var warns = new ArrayList<String>();
		final var f = new FFprobeJAXB(xmlContent, w -> warns.add(w));
		assertEquals(List.of(), warns);

		switch (specificMediaFile) {
		case AVI:
		case MKV:
		case MOV:
		case MPG:
		case TS:
			assertEquals(1, f.getVideoStreams().count());
			assertEquals(1, f.getAudiosStreams().count());
			break;
		case WAV:
			assertEquals(1, f.getAudiosStreams().count());
			break;
		case MXF:
			assertEquals(1, f.getVideoStreams().count());
			assertEquals(2, f.getAudiosStreams().count());
			break;
		}
		assertEquals(specificMediaFile.formatName, f.getFormat().getFormatName());
	}

	void checkRaw_StdErr(final E2ERawOutDataFiles rawData) throws IOException {
		final var lines = readLines(rawData.outStderr()).stream()
				.map(String::trim)
				.toList();
		assertTrue(561 <= lines.stream().filter(l -> l.startsWith("[Parsed_ebur128_")).count(), "[Parsed_ebur128_");

		if (rawData.hasVideo()) {
			assertEqualsNbLines(1, lines, "[Parsed_blockdetect_");
		}
	}

	void checkRaw_Alavfi(final E2ERawOutDataFiles rawData) throws IOException {
		final var specificMediaFile = rawData.getSpecificMediaFile();

		final var lines = readLines(rawData.outAlavfi());
		assertEqualsNbLines(specificMediaFile.alavfiFrameCount, lines, "lavfi.aphasemeter.phase");
		assertEqualsNbLines(specificMediaFile.alavfiFrameCount, lines, "frame:");
		assertEqualsNbLines(specificMediaFile.alavfiFrameCount, lines, "lavfi.astats.1.DC_offset");
		assertEqualsNbLines(specificMediaFile.alavfiFrameCount, lines, "lavfi.astats.1.Peak_level");
		assertEqualsNbLines(specificMediaFile.alavfiFrameCount, lines, "lavfi.astats.1.Flat_factor");
		assertEqualsNbLines(specificMediaFile.alavfiFrameCount, lines, "lavfi.astats.1.Peak_count");
		assertEqualsNbLines(specificMediaFile.alavfiFrameCount * 2, lines, "lavfi.astats.1.Noise_floor");
		assertEqualsNbLines(specificMediaFile.alavfiFrameCount, lines, "lavfi.astats.1.Noise_floor_count");
		assertEqualsNbLines(specificMediaFile.alavfiFrameCount, lines, "lavfi.astats.1.Entropy");
		assertEqualsNbLines(specificMediaFile.alavfiFrameCount, lines, "lavfi.astats.2.DC_offset");
		assertEqualsNbLines(specificMediaFile.alavfiFrameCount, lines, "lavfi.astats.2.Peak_level");
		assertEqualsNbLines(specificMediaFile.alavfiFrameCount, lines, "lavfi.astats.2.Flat_factor");
		assertEqualsNbLines(specificMediaFile.alavfiFrameCount, lines, "lavfi.astats.2.Peak_count");
		assertEqualsNbLines(specificMediaFile.alavfiFrameCount * 2, lines, "lavfi.astats.2.Noise_floor");
		assertEqualsNbLines(specificMediaFile.alavfiFrameCount, lines, "lavfi.astats.2.Noise_floor_count");
		assertEqualsNbLines(specificMediaFile.alavfiFrameCount, lines, "lavfi.astats.2.Entropy");
		assertEqualsNbLines(specificMediaFile.alavfiSilenceDetectCount, lines, "lavfi.silence_start.2=36");
		assertEqualsNbLines(specificMediaFile.alavfiSilenceDetectCount, lines, "lavfi.silence_end.2=46");
		assertEqualsNbLines(specificMediaFile.alavfiSilenceDetectCount, lines, "lavfi.silence_duration.2=10");
	}

	void checkRawVlavfi(final E2ERawOutDataFiles rawData) throws IOException {
		final var lines = readLines(rawData.outVlavfi());
		assertEqualsNbLines(1400, lines, "frame:");
		assertEqualsNbLines(1400, lines, "lavfi.block");
		assertEqualsNbLines(1400, lines, "lavfi.blur");
		assertTrue(lines.stream().filter(l -> l.startsWith("lavfi.cropdetect")).count() > 1400);
		assertEqualsNbLines(1400, lines, "lavfi.siti.si");
		assertEqualsNbLines(1400, lines, "lavfi.siti.si");
		assertEqualsNbLines(1, lines, "lavfi.freezedetect.freeze_start=0");
		assertEqualsNbLines(1, lines, "lavfi.freezedetect.freeze_start=5");
		assertEqualsNbLines(2, lines, "lavfi.freezedetect.freeze_duration=5");
		assertEqualsNbLines(1, lines, "lavfi.freezedetect.freeze_end=5");
		assertEqualsNbLines(1, lines, "lavfi.freezedetect.freeze_end=10");
		assertEqualsNbLines(1, lines, "lavfi.black_start=5");
		assertEqualsNbLines(1, lines, "lavfi.black_end=10");
		assertEqualsNbLines(1, lines, "lavfi.black_start=20");
		assertEqualsNbLines(1, lines, "lavfi.black_end=21");
		assertEqualsNbLines(1400, lines, "lavfi.idet.single.current_frame");
		assertEqualsNbLines(1400, lines, "lavfi.idet.single.tff");
		assertEqualsNbLines(1400, lines, "lavfi.idet.single.bff");
		assertEqualsNbLines(1400, lines, "lavfi.idet.single.progressive");
		assertEqualsNbLines(1400, lines, "lavfi.idet.single.undetermined");
		assertEqualsNbLines(1400, lines, "lavfi.idet.multiple.current_frame");
		assertEqualsNbLines(1400, lines, "lavfi.idet.multiple.tff");
		assertEqualsNbLines(1400, lines, "lavfi.idet.multiple.bff");
		assertEqualsNbLines(1400, lines, "lavfi.idet.multiple.progressive");
		assertEqualsNbLines(1400, lines, "lavfi.idet.multiple.undetermined");
		assertEqualsNbLines(1400, lines, "lavfi.idet.repeated.current_frame");
		assertEqualsNbLines(1400, lines, "lavfi.idet.repeated.neither");
		assertEqualsNbLines(1400, lines, "lavfi.idet.repeated.top");
		assertEqualsNbLines(1400, lines, "lavfi.idet.repeated.bottom");
	}

	void checkProcessTXT_containerVideoFrames(final E2ERawOutDataFiles rawData) throws IOException {
		final var f = getProcessedTXTFromRaw(rawData, "container-video-frames.txt");
		final var lines = readLines(f);
		assertEquals(V_FRAME_COUNT + 1, lines.size());
		assertTableWith(15, lines);
		assertEquals(List.of("VIDEO\t"),
				lines.stream().skip(1).map(l -> l.substring(0, "VIDEO\t".length())).distinct().toList());
	}

	void checkProcessTXT_containerVideoConst(final E2ERawOutDataFiles rawData) throws IOException {
		final var specificMediaFile = getFromMediaFile(rawData.mediaFile());
		final var f = getProcessedTXTFromRaw(rawData, "container-video-consts.txt");
		final var lines = readLines(f);
		assertTableWith(21, lines);
		switch (specificMediaFile) {
		case AVI:
			assertEquals(2, lines.size());
			assertEquals(List.of(
					"352", "288", "1:1", "0", "0", "yuv420p",
					"", "", "", "", "0", "0", "0", "0", "0", "0", "0", "0", "1", "0.02", "9990"),
					getSplitedLine(lines, 1));
			break;
		case MKV:
			assertEquals(2, lines.size());
			assertEquals(List.of(
					"352", "288", "1:1", "0", "0", "yuv420p",
					"", "", "", "", "0", "0", "0", "0", "0", "0", "0", "0", "40", "0.04", "706"),
					getSplitedLine(lines, 1));
			break;
		case MOV:
			assertEquals(2, lines.size());
			assertEquals(List.of(
					"352", "288", "1:1", "0", "0", "yuv420p",
					"", "", "", "", "0", "0", "0", "0", "0", "0", "0", "0", "640", "0.04", "36"),
					getSplitedLine(lines, 1));
			break;
		case MPG:
			assertEquals(V_FRAME_COUNT + 1, lines.size());
			assertEquals(List.of(
					"352", "288", "1:1", "0", "0", "yuv420p",
					"tv", "", "", "", "0", "0", "48600", "0.54001", "48600", "0.54001", "48600", "0.54001", "3600",
					"0.04", "30"),
					getSplitedLine(lines, 1));
			break;
		case TS:
			assertEquals(V_FRAME_COUNT + 1, lines.size());
			assertEquals(List.of(
					"352", "288", "1:1", "0", "0", "yuv420p",
					"tv", "", "", "", "0", "0", "129600", "1.44001", "129600", "1.44001", "129600", "1.44001",
					"3600",
					"0.04", "564"),
					getSplitedLine(lines, 1));
			break;
		case MXF:
			assertEquals(V_FRAME_COUNT + 1, lines.size());
			assertEquals(List.of(
					"352", "288", "1:1", "0", "0", "yuv420p",
					"tv", "", "", "", "0", "0", "0", "0", "0", "0", "0", "0", "1", "0.04", "8192"),
					getSplitedLine(lines, 1));
			break;
		default:
			break;
		}
	}

	void checkProcessTXT_stderr(final E2ERawOutDataFiles rawData) {
		final var f = getProcessedTXTFromRaw(rawData, "rawstderrfilters.txt");
		assertTrue(f.exists());
	}

	void checkProcessTXT_ffprobeXML(final E2ERawOutDataFiles rawData) throws IOException {
		final var f = getProcessedTXTFromRaw(rawData, "ffprobe.xml");
		final var probe = readFileToString(f, UTF_8);
		assertTrue(probe.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
									+ System.lineSeparator()
									+ "<ffprobe>"));
		assertTrue(probe.endsWith("</ffprobe>"));
	}

	void checkProcessTXT_events(final E2ERawOutDataFiles rawData) throws IOException {
		final var specificMediaFile = getFromMediaFile(rawData.mediaFile());
		final var f = getProcessedTXTFromRaw(rawData, "events.txt");
		final var lines = readLines(f);
		assertTableWith(5, lines);
		switch (specificMediaFile) {
		case AVI:
		case MKV:
		case MOV:
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
		case MPG:
		case TS:
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

	void checkProcessTXT_containerPackets(final E2ERawOutDataFiles rawData) throws IOException {
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
			assertEquals(specificMediaFile.alavfiFrameCount + 1, lines.size());
		}
	}

	void checkProcessTXT_audioFrames(final E2ERawOutDataFiles rawData) throws IOException {
		final var specificMediaFile = getFromMediaFile(rawData.mediaFile());
		final var f = getProcessedTXTFromRaw(rawData, "container-audio-frames.txt");
		final var lines = readLines(f);
		assertTableWith(13, lines);
		assertEquals(specificMediaFile.containerAudioCount + 1, lines.size());
		assertEquals(List.of("AUDIO\t"),
				lines.stream().skip(1).map(l -> l.substring(0, "AUDIO\t".length())).distinct().toList());
	}

	void checkProcessTXT_audioConsts(final E2ERawOutDataFiles rawData) throws IOException {
		final var f = getProcessedTXTFromRaw(rawData, "container-audio-consts.txt");
		final var lines = readLines(f);
		assertTableWith(12, lines);
		assertEquals(2, lines.size());
	}

	void checkProcessTXT_audioPhaseMeter(final E2ERawOutDataFiles rawData) throws IOException {
		final var specificMediaFile = getFromMediaFile(rawData.mediaFile());
		final var f = getProcessedTXTFromRaw(rawData, "audio-phase-meter.txt");
		final var lines = readLines(f);
		assertTableWith(4, lines);
		assertEquals(specificMediaFile.alavfiFrameCount + 1, lines.size());
	}

	void checkProcessTXT_audioStats(final E2ERawOutDataFiles rawData) throws IOException {
		final var specificMediaFile = getFromMediaFile(rawData.mediaFile());
		final var f = getProcessedTXTFromRaw(rawData, "audio-stats.txt");
		final var lines = readLines(f);
		assertTableWith(List.of(12, 11), lines);
		if (specificMediaFile == E2ESpecificMediaFile.MXF) {
			assertEquals(specificMediaFile.containerAudioCount + 1, lines.size());
		} else {
			assertEquals(specificMediaFile.containerAudioCount * 2 + 1, lines.size());
		}
	}

	void checkProcessTXT_ebur128(final E2ERawOutDataFiles rawData) throws IOException {
		final var f = getProcessedTXTFromRaw(rawData, "audio-ebur128.txt");
		final var lines = readLines(f);
		assertTableWith(11, lines);
		assertEquals(561, lines.size());
	}

	void checkProcessTXT_ebur128Summary(final E2ERawOutDataFiles rawData) throws IOException {
		final var specificMediaFile = getFromMediaFile(rawData.mediaFile());
		final var f = getProcessedTXTFromRaw(rawData, "audio-ebur128-summary.txt");
		final var lines = readLines(f);
		assertTableWith(2, lines);
		final var floatValues = lines.stream().skip(1)
				.map(l -> List.of(l.split("\t")))
				.map(l -> new KV<>(l.get(0), Float.valueOf(l.get(1))))
				.collect(toUnmodifiableMap(KV::key, KV::value));
		assertEquals(-24, round(floatValues.get("Integrated")));
		if (specificMediaFile == E2ESpecificMediaFile.MXF) {
			assertEquals(-36, round(floatValues.get("Integrated Threshold")));
			assertEquals(20, round(floatValues.get("Loudness Range")));
			assertEquals(-45, round(floatValues.get("Loudness Range Threshold")));
			assertEquals(-39, round(floatValues.get("Loudness Range Low")));
			assertEquals(-19, round(floatValues.get("Loudness Range High")));
			assertEquals(-21, round(floatValues.get("Sample Peak")));
			assertEquals(-21, round(floatValues.get("True Peak")));
		} else {
			assertEquals(-35, round(floatValues.get("Integrated Threshold")));
			assertEquals(17, round(floatValues.get("Loudness Range")));
			assertEquals(-45, round(floatValues.get("Loudness Range Threshold")));
			assertEquals(-36, round(floatValues.get("Loudness Range Low")));
			assertEquals(-19, round(floatValues.get("Loudness Range High")));
			assertEquals(-18, round(floatValues.get("Sample Peak")));
			assertEquals(-18, round(floatValues.get("True Peak")));
		}
	}

	void checkProcessTXT_sitiStats(final E2ERawOutDataFiles rawData) throws IOException {
		final var f = getProcessedTXTFromRaw(rawData, "video-siti-stats-ITU-T_P-910.txt");
		final var lines = readLines(f);
		assertTableWith(4, lines);
		assertEquals(3, lines.size());
		assertEquals(1, lines.stream().filter(l -> l.startsWith("Spatial Info")).count());
		assertEquals(1, lines.stream().filter(l -> l.startsWith("Temporal Info")).count());
	}

	void checkProcessTXT_siti(final E2ERawOutDataFiles rawData) throws IOException {
		final var f = getProcessedTXTFromRaw(rawData, "video-siti-ITU-T_P-910.txt");
		final var lines = readLines(f);
		assertTableWith(5, lines);
		assertEquals(V_FRAME_COUNT + 1, lines.size());
	}

	void checkProcessTXT_interlaceDectect(final E2ERawOutDataFiles rawData) throws IOException {
		final var f = getProcessedTXTFromRaw(rawData, "video-interlace-detect.txt");
		final var lines = readLines(f);
		assertTableWith(17, lines);
		assertEquals(V_FRAME_COUNT + 1, lines.size());
	}

	void checkProcessTXT_cropDectect(final E2ERawOutDataFiles rawData) throws IOException {
		final var f = getProcessedTXTFromRaw(rawData, "video-crop-detect.txt");
		final var lines = readLines(f);
		assertTableWith(11, lines);
		/**
		 * Crop dectect seams to skip the 2 first frames
		 */
		assertEquals(V_FRAME_COUNT - 1, lines.size());
	}

	void checkProcessTXT_blurDectect(final E2ERawOutDataFiles rawData) throws IOException {
		final var f = getProcessedTXTFromRaw(rawData, "video-blur-detect.txt");
		final var lines = readLines(f);
		/**
		 * Blur can output some empty cols (as "-nan" values returned by filter), here on black frames.
		 */
		assertTableWith(List.of(4, 3), lines);
		assertEquals(V_FRAME_COUNT + 1, lines.size());
	}

	void checkProcessTXT_blockDectect(final E2ERawOutDataFiles rawData) throws IOException {
		final var f = getProcessedTXTFromRaw(rawData, "video-block-detect.txt");
		final var lines = readLines(f);
		assertTableWith(4, lines);
		assertEquals(V_FRAME_COUNT + 1, lines.size());
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

		if (new File("target/e2e-export", "long-mkv_ffprobe.xml").exists() == false) {
			runApp(
					"--temp", "target/e2e-temp",
					"--import-lavfi", rawData.outAlavfi().getPath(),
					"--import-lavfi", rawData.outVlavfi().getPath(),
					"--import-stderr", rawData.outStderr().getPath(),
					"--import-probeheaders", rawData.outProbeheaders().getPath(),
					"--import-container", rawData.outContainer().getPath(),
					"-f", "txt",
					"-e", "target/e2e-export",
					"--export-base-filename", exportBaseFilename);
		}

		assertEquals(144001,
				countLinesExportDir("long-mkv_audio-ebur128.txt"));
		assertEquals(9,
				countLinesExportDir("long-mkv_audio-ebur128-summary.txt"));
		assertEquals(600002,
				countLinesExportDir("long-mkv_audio-phase-meter.txt"));
		assertEquals(1200003,
				countLinesExportDir("long-mkv_audio-stats.txt"));
		assertEquals(2,
				countLinesExportDir("long-mkv_container-audio-consts.txt"));
		assertEquals(600002,
				countLinesExportDir("long-mkv_container-audio-frames.txt"));
		assertEquals(960002,
				countLinesExportDir("long-mkv_container-packets.txt"));
		assertEquals(360001,
				countLinesExportDir("long-mkv_container-video-consts.txt"));
		assertEquals(360001,
				countLinesExportDir("long-mkv_container-video-frames.txt"));
		assertEquals(36001,
				countLinesExportDir("long-mkv_container-video-gop.txt"));
		assertEquals(360001,
				countLinesExportDir("long-mkv_video-block-detect.txt"));
		assertEquals(360001,
				countLinesExportDir("long-mkv_video-blur-detect.txt"));
		assertEquals(359999,
				countLinesExportDir("long-mkv_video-crop-detect.txt"));
		assertEquals(360001,
				countLinesExportDir("long-mkv_video-interlace-detect.txt"));
		assertEquals(360001,
				countLinesExportDir("long-mkv_video-siti-ITU-T_P-910.txt"));
		assertEquals(3,
				countLinesExportDir("long-mkv_video-siti-stats-ITU-T_P-910.txt"));
		assertTrue(countLinesExportDir("long-mkv_ffprobe.xml") > 0);
	}

	@Nested
	@ExtendWith(OutputCaptureExtension.class)
	class CmdUtilsTest {

		@ParameterizedTest
		@ValueSource(strings = { "-h", "--help" })
		void testShowHelp(final String param, final CapturedOutput output) {
			runApp(param);
			assertThat(output.getOut()).startsWith("Usage: mediadeepa");
			assertThat(output.getErr()).isEmpty();
		}

		@Test
		void testShowDefaultHelp(final CapturedOutput output) {
			assertEquals(
					2,
					SpringApplication.exit(SpringApplication.run(App.class)),
					"App exit code must return 2");
			assertThat(output.getOut()).isEmpty();
			assertThat(output.getErr()).contains("Usage: mediadeepa");
		}

		@ParameterizedTest
		@ValueSource(strings = { "-v", "--version" })
		void testShowVersion(final String param, final CapturedOutput output) {
			runApp(param);
			assertThat(output.getOut()).startsWith("Media Deep Analysis");
			assertThat(output.getOut()).contains("Media ex Machina", "Copyright", "GNU");
			assertThat(output.getErr()).isEmpty();
		}

		@ParameterizedTest
		@ValueSource(strings = { "-o", "--options" })
		void testShowOptions(final String param, final CapturedOutput output) {
			runApp(param);
			assertThat(output.getOut()).contains(
					"ffmpeg",
					"ffprobe",
					"ebur128",
					"cropdetect",
					"metadata");
			assertThat(output.getErr()).isEmpty();
		}

		@Test
		void testShowAutocomplete(final CapturedOutput output) {
			runApp("--autocomplete");
			assertThat(output.getOut()).startsWith("#!/usr/bin/env bash");
			assertThat(output.getOut()).contains("mediadeepa", "picocli");
			assertThat(output.getErr()).isEmpty();
		}

	}
}
