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

import java.io.File;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

public enum E2ESpecificMediaFile {

	AVI(
			2625,
			2625,
			"avi",
			1,
			List.of(
					"AVI (Audio Video Interleaved), 00:00:56, 48 MB",
					"video: ffv1 352×288 @ 50 fps [5735 kbps] yuv420p (2800 frms)",
					"audio: pcm_s16le 2 channels @ 48000 Hz [1536 kbps]")),

	MKV(
			2625,
			2625,
			"matroska,webm",
			1,
			List.of(
					"Matroska / WebM, 00:00:56, 48 MB",
					"video: ffv1 352×288 @ 25 fps yuv420p",
					"audio: pcm_s16le 2 channels @ 48000 Hz [1536 kbps]")),

	MOV(
			2625,
			2625,
			"mov,mp4,m4a,3gp,3g2,mj2",
			1,
			List.of(
					"QuickTime / MOV, 00:00:56, 48 MB",
					"video: ffv1 352×288 @ 25 fps [5731 kbps] yuv420p (1400 frms) default stream",
					"audio: pcm_s16le stereo @ 48000 Hz [1536 kbps] default stream")),

	MPG(
			2334,
			2334,
			"mpeg",
			0,
			List.of(
					"MPEG-PS (MPEG-2 Program Stream), 00:00:56, 18 MB",
					"video: mpeg2video 352×288 Main/Main with B frames @ 25 fps yuv420p/colRange:TV",
					"audio: mp2 s16p stereo @ 48000 Hz [256 kbps]")),

	MXF(
			1400,
			2800,
			"mxf",
			0,
			List.of(
					"MXF (Material eXchange Format), 00:00:56, 29 MB",
					"video: mpeg2video 352×288 Main/Main with B frames @ 25 fps yuv420p/colRange:TV",
					"audio: pcm_s16le mono @ 48000 Hz [768 kbps]",
					"audio: pcm_s16le mono @ 48000 Hz [768 kbps]")),
	TS(
			2334,
			2334,
			"mpegts",
			0,
			List.of(
					"MPEG-TS (MPEG-2 Transport Stream), 00:00:56, 19 MB, 1 program(s)",
					"video: mpeg2video 352×288 Main/Main with B frames @ 25 fps yuv420p/colRange:TV",
					"audio: mp2 stereo @ 48000 Hz [256 kbps]")),

	WAV(
			2625,
			2625,
			"wav",
			1,
			List.of(
					"WAV / WAVE (Waveform Audio), 00:00:56, 10 MB",
					"audio: pcm_s16le 2 channels @ 48000 Hz [1536 kbps]"));

	final long alavfiFrameCount;
	final long containerAudioCount;
	final List<String> probesummary;
	final String formatName;
	final long alavfiSilenceDetectCount;

	E2ESpecificMediaFile(
						 final long alavfiFrameCount,
						 final long containerAudioCount,
						 final String formatName,
						 final long alavfiSilenceDetectCount,
						 final List<String> probesummary) {
		this.alavfiFrameCount = alavfiFrameCount;
		this.containerAudioCount = containerAudioCount;
		this.formatName = formatName;
		this.alavfiSilenceDetectCount = alavfiSilenceDetectCount;
		this.probesummary = probesummary;

	}

	static E2ESpecificMediaFile getFromMediaFile(final File mediaFile) {
		final var ext = FilenameUtils.getExtension(mediaFile.getName());
		return switch (ext) {
		case "avi" -> AVI;
		case "mkv" -> MKV;
		case "mov" -> MOV;
		case "mpg" -> MPG;
		case "mxf" -> MXF;
		case "ts" -> TS;
		case "wav" -> WAV;
		default -> throw new IllegalArgumentException("Unexpected value: " + ext);
		};
	}

}
