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
 * Copyright (C) hdsdi3g for hd3g.tv 2024
 *
 */
package media.mexm.mediadeepa.workingsession;

import static java.io.File.pathSeparator;
import static java.lang.Integer.MAX_VALUE;
import static java.time.Duration.ofMillis;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.cli.ScanDirCmd;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.service.AppSessionService;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;
import tv.hd3g.jobkit.watchfolder.WatchedFilesInMemoryDb;
import tv.hd3g.jobkit.watchfolder.Watchfolders;

@Slf4j
public class WorkingSession {

	private final AppConfig appConfig;
	private final ScanDirCmd scanDirCmd;
	private final FoundedFiles foundedFiles;
	private final List<File> inputRegularFiles;
	private final List<File> inputRegularDirs;
	private final WorkingSessionResult workingSessionResult;
	private final boolean multipleSources;

	public WorkingSession(final AppConfig appConfig,
						  final ScanDirCmd scanDirCmd,
						  final List<String> rawInput,
						  final AppSessionService appService,
						  final boolean limitOneFile,
						  final Runnable onMoreThanLimitOneFile) {
		this.appConfig = appConfig;
		this.scanDirCmd = scanDirCmd;

		final var inputFiles = Optional.ofNullable(rawInput)
				.orElse(List.of())
				.stream()
				.map(File::new)
				.toList();

		inputRegularDirs = inputFiles.stream()
				.filter(File::isDirectory)
				.toList();
		inputRegularFiles = inputFiles.stream()
				.filter(File::isFile)
				.toList();
		workingSessionResult = new WorkingSessionResult();

		if (limitOneFile
			&& (inputRegularFiles.size() > 1
				|| inputRegularDirs.isEmpty() == false)) {
			onMoreThanLimitOneFile.run();
		}

		multipleSources = inputRegularFiles.size() > 1 || inputRegularDirs.isEmpty() == false;
		if (multipleSources) {
			inputRegularFiles.forEach(f -> log.info("Prepare to work on \"{}\"", f.getAbsolutePath()));
			inputRegularDirs.forEach(f -> log.info("Prepare to work on \"{}\" directory", f.getAbsolutePath()));
		}

		inputRegularFiles.forEach(appService::validateInputFile);

		foundedFiles = new FoundedFiles(appService, workingSessionResult, multipleSources);
	}

	private Set<String> toSet(final List<String> l) {
		return Optional.ofNullable(l)
				.orElse(List.of())
				.stream()
				.distinct()
				.collect(toUnmodifiableSet());
	}

	private ObservedFolder makeObservedFolder(final File rootDirectory) {
		final var observedFolder = new ObservedFolder();
		observedFolder.setRecursive(scanDirCmd.isRecursive());
		observedFolder.setTargetFolder(rootDirectory.getAbsolutePath());
		observedFolder.setLabel(rootDirectory.getName() + " (" + rootDirectory.getAbsolutePath() + ")");

		final var timeBetweenScans = Duration.ofSeconds(
				scanDirCmd.getTimeBetweenScans() > 0 ? scanDirCmd.getTimeBetweenScans() : -1);
		observedFolder.setTimeBetweenScans(timeBetweenScans);

		if (timeBetweenScans.isPositive()) {
			observedFolder.setMinFixedStateTime(ofMillis(appConfig.getScanDir().getMinFixedStateTime()));
		} else {
			observedFolder.setMinFixedStateTime(Duration.ofSeconds(-1));
		}

		observedFolder.setIgnoreFiles(Stream.of(appConfig.getScanDir().getIgnoreFiles()
				.split(pathSeparator))
				.distinct()
				.collect(toUnmodifiableSet()));
		observedFolder.setRetryAfterTimeFactor(appConfig.getScanDir().getRetryAfterTimeFactor());
		observedFolder.setAllowedExtentions(toSet(scanDirCmd.getAllowedExtentions()));
		observedFolder.setBlockedExtentions(toSet(scanDirCmd.getBlockedExtentions()));
		observedFolder.setIgnoreRelativePaths(toSet(scanDirCmd.getIgnoreRelativePaths()));
		observedFolder.setAllowedFileNames(toSet(scanDirCmd.getAllowedFileNames()));
		observedFolder.setAllowedDirNames(toSet(scanDirCmd.getAllowedDirNames()));
		observedFolder.setBlockedFileNames(toSet(scanDirCmd.getBlockedFileNames()));
		observedFolder.setBlockedDirNames(toSet(scanDirCmd.getBlockedDirNames()));
		observedFolder.setAllowedLinks(scanDirCmd.isAllowedLinks());
		observedFolder.setAllowedHidden(scanDirCmd.isAllowedHidden());
		return observedFolder;
	}

	/**
	 * Will be blocking on scan, and no limit blocking during watch dir.
	 */
	public void startWork(final JobKitEngine jobKitEngine,
						  final String spoolNameWatchfolder) {
		inputRegularFiles.forEach(foundedFiles::onFoundFile);

		if (inputRegularDirs.isEmpty()) {
			workingSessionResult.checkPublishedFilePresence();
			return;
		}

		final var allObservedFolders = inputRegularDirs.stream()
				.map(this::makeObservedFolder)
				.toList();

		final var maxDeep = scanDirCmd.isRecursive() ? appConfig.getScanDir().getDepthScanDirectories()
													 : 0;
		log.debug("Set recursive to {} deep dir", maxDeep);

		final var watchfolders = new Watchfolders(
				allObservedFolders,
				foundedFiles,
				Duration.ofDays(MAX_VALUE),
				jobKitEngine,
				spoolNameWatchfolder,
				spoolNameWatchfolder,
				() -> new WatchedFilesInMemoryDb(maxDeep));

		/**
		 * 1st to Setup the WF database
		 */
		manageFoundedFiles(watchfolders.manualScan());

		if (scanDirCmd.getTimeBetweenScans() > 0) {
			log.info("Start to scan directories, every {} second(s)...", scanDirCmd.getTimeBetweenScans());
			watchfolders.startScans();
			while (true) {
				Thread.onSpinWait();
			}
		} else {
			manageFoundedFiles(watchfolders.manualScan());
		}

		if (workingSessionResult.isEmpty()) {
			log.warn("No file founded in provided path(s): {}", inputRegularDirs);
		} else if (workingSessionResult.size() > 1) {
			log.info("Working file count: {}", workingSessionResult.size());
		}
		workingSessionResult.checkPublishedFilePresence();
	}

	private void manageFoundedFiles(final Map<ObservedFolder, WatchedFiles> manualScanResult) {
		manualScanResult.forEach((k, v) -> {
			try {
				if (log.isTraceEnabled()) {
					log.trace("Founded file {} from [{}]", v, k);
				} else {
					log.debug("Founded {}/{} file(s) from [{}]",
							v.foundedAndUpdated().size(), v.totalFiles(), k.getTargetFolder());
				}
				foundedFiles.onAfterScan(k, Duration.ZERO, v);
			} catch (final IOException e) {
				log.error("Can't process founded files", e);
			}
		});
	}

}
