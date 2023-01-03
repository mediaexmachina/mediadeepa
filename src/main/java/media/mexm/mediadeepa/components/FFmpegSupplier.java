/*
 * This file is part of mediadeepa.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2023
 *
 */
package media.mexm.mediadeepa.components;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tv.hd3g.fflauncher.FFmpeg;
import tv.hd3g.fflauncher.FFprobe;
import tv.hd3g.processlauncher.cmdline.Parameters;

@Component
public class FFmpegSupplier {
	@Autowired
	private String ffmpegExecName;
	@Autowired
	private String ffprobeExecName;

	public FFmpeg createFFmpeg() {
		return new FFmpeg(ffmpegExecName, new Parameters());
	}

	public FFprobe createFFprobe() {
		return new FFprobe(ffprobeExecName, new Parameters());
	}

}
