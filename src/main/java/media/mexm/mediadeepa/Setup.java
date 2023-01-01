/*
 * This file is part of mediadeepa.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either command 3 of the License, or
 * any later command.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2023
 *
 */
package media.mexm.mediadeepa;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import media.mexm.mediadeepa.exportformat.ExportFormatManager;
import tv.hd3g.fflauncher.FFmpeg;
import tv.hd3g.fflauncher.FFprobe;
import tv.hd3g.fflauncher.about.FFAbout;
import tv.hd3g.fflauncher.progress.ProgressListener;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;

@Configuration
@ComponentScan(basePackages = { "tv.hd3g.commons.version.mod" })
public class Setup {

	@Value("${mediadeepa.ffmpegExecName:ffmpeg}")
	private String ffmpegExecName;
	@Value("${mediadeepa.ffmpegExecName:ffprobe}")
	private String ffprobeExecName;

	@Bean
	ExecutableFinder getExecutableFinder() {
		return new ExecutableFinder();
	}

	@Bean(name = "ffmpegExecName")
	String getFFmpegExecName(final ExecutableFinder executableFinder) {
		return ffmpegExecName;
	}

	@Bean(name = "ffprobeExecName")
	String getFFprobeExecName(final ExecutableFinder executableFinder) {
		return ffprobeExecName;
	}

	@Bean(name = "ffmpegAbout")
	FFAbout getFFmpegAbout(final ExecutableFinder executableFinder) {
		return new FFmpeg(ffmpegExecName, new Parameters()).getAbout(executableFinder);
	}

	@Bean(name = "ffprobeAbout")
	FFAbout getFFprobeAbout(final ExecutableFinder executableFinder) {
		return new FFprobe(ffprobeExecName, new Parameters()).getAbout(executableFinder);
	}

	@Bean
	ScheduledExecutorService getMaxExecTimeScheduler() {
		return Executors.newSingleThreadScheduledExecutor();
	}

	@Bean
	ProgressListener getProgressListener() {
		return new ProgressListener();
	}

	@Bean
	KeyPressToExit getKeyPressToExit() {
		return new KeyPressToExit(System.in);
	}

	@Bean
	ExportFormatManager getExportFormatManager() {
		return new ExportFormatManager();
	}

	@Bean
	Supplier<ProgressCLI> createProgressCLI() {
		return () -> new ProgressCLI(System.out);// NOSONAR S106
	}

}
