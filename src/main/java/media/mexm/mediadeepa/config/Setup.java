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
package media.mexm.mediadeepa.config;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import media.mexm.mediadeepa.ConfigurationCrawler;
import media.mexm.mediadeepa.KeyPressToExit;
import media.mexm.mediadeepa.LoggerConfiguration;
import media.mexm.mediadeepa.ProgressCLI;
import media.mexm.mediadeepa.RunnedJavaCmdLine;
import media.mexm.mediadeepa.cli.AppCommand;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;
import tv.hd3g.fflauncher.FFmpeg;
import tv.hd3g.fflauncher.FFprobe;
import tv.hd3g.fflauncher.about.FFAbout;
import tv.hd3g.fflauncher.progress.ProgressListener;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;

@Configuration
@ComponentScan(basePackages = { "tv.hd3g.commons.version.mod" })
public class Setup {

	@Autowired
	private AppConfig appConfig;

	@Bean
	ExecutableFinder getExecutableFinder() {
		return new ExecutableFinder();
	}

	@Bean(name = "ffmpegAbout")
	FFAbout getFFmpegAbout(final ExecutableFinder executableFinder) {
		return new FFmpeg(appConfig.getFfmpegExecName(), new Parameters()).getAbout(executableFinder);
	}

	@Bean(name = "ffprobeAbout")
	FFAbout getFFprobeAbout(final ExecutableFinder executableFinder) {
		return new FFprobe(appConfig.getFfmpegExecName(), new Parameters()).getAbout(executableFinder);
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
	Supplier<ProgressCLI> createProgressCLI() {
		return () -> new ProgressCLI(System.out);// NOSONAR S106
	}

	@Bean
	CommandLine getCommandLine(final AppCommand appCommand, final IFactory factory) {
		return new CommandLine(appCommand, factory);
	}

	@Bean
	AppCommand getAppCommand() {
		return new AppCommand();
	}

	@Bean
	Parser getMarkdownParser() {
		return Parser.builder().build();
	}

	@Bean
	HtmlRenderer getHtmlRenderer() {
		return HtmlRenderer.builder().build();
	}

	@Bean
	ConfigurationCrawler getConfigurationCrawler() {
		return new ConfigurationCrawler(appConfig);
	}

	@Bean
	LoggerConfiguration getLoggerConfiguration() {
		return new LoggerConfiguration(appConfig);
	}

	@Bean
	RunnedJavaCmdLine runnedJavaCmdLine() {
		return new RunnedJavaCmdLine();
	}

}
