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

import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.synchronizedList;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.service.AppSessionService;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.watchfolder.FolderActivity;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;
import tv.hd3g.jobkit.watchfolder.WatchedFilesInMemoryDb;
import tv.hd3g.jobkit.watchfolder.Watchfolders;
import tv.hd3g.transfertfiles.CachedFileAttributes;
import tv.hd3g.transfertfiles.local.LocalFile;

@Slf4j
public class WorkingSession {

	private final AppSessionService appService;
	private final List<File> inputRegularFiles;
	private final List<File> inputRegularDirs;
	private final List<File> processedFiles;
	private final boolean multipleSources;

	public WorkingSession(final List<String> rawInput,
						  final AppSessionService appService,
						  final boolean limitOneFile,
						  final Runnable onMoreThanLimitOneFile) {
		this.appService = appService;
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
		processedFiles = synchronizedList(new ArrayList<>());

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

		// TODO AFTER willcard process, only here
	}

	private void onFoundFile(final File inputFile) {
		appService.fileWork(inputFile, multipleSources);
		processedFiles.add(inputFile);
	}

	/**
	 * Will be blocking on scan, and no limit blocking during watch dir.
	 */
	public void startWork(final JobKitEngine jobKitEngine,
						  final String spoolNameWatchfolder,
						  final int maxDeep) {
		inputRegularFiles.forEach(this::onFoundFile);

		if (inputRegularDirs.isEmpty()) {
			return;
		}

		final var allObservedFolders = inputRegularDirs.stream()
				.map(dir -> {
					final var observedFolder = new ObservedFolder();
					observedFolder.setTargetFolder(dir.getAbsolutePath());
					observedFolder.setLabel(dir.getName() + " (" + dir.getAbsolutePath() + ")");
					observedFolder.setMinFixedStateTime(Duration.ZERO);// TODO AFTER set by conf (regular)
					observedFolder.setTimeBetweenScans(Duration.ofSeconds(5));// TODO AFTER set by conf (regular)
					return observedFolder;
				})
				.toList();

		class Founded implements FolderActivity {

			@Override
			public void onBeforeScan(final ObservedFolder observedFolder) throws IOException {
				log.debug("Start directory scan on {}", observedFolder.getLabel());
			}

			@Override
			public void onAfterScan(final ObservedFolder observedFolder,
									final Duration scanTime,
									final WatchedFiles scanResult) throws IOException {
				final var foundedAndUpdated = scanResult.foundedAndUpdated();

				if (foundedAndUpdated.isEmpty() == false) {
					log.info("Start to work with {} file(s) found on \"{}\" directory",
							foundedAndUpdated.size(),
							observedFolder.getTargetFolder());

					foundedAndUpdated.stream()
							.map(CachedFileAttributes::getAbstractFile)
							.map(f -> (LocalFile) f)
							.map(LocalFile::getInternalFile)
							.forEach(f -> onFoundFile(f));
				}
			}

			@Override
			public void onScanErrorFolder(final ObservedFolder observedFolder, final Exception e) throws IOException {
				log.error("Can't scan {}", observedFolder.getTargetFolder(), e);
			}
		}

		log.debug("Set recursive to {} deep dir", maxDeep);

		final var w = new Watchfolders(
				allObservedFolders,
				new Founded(),
				Duration.ofDays(MAX_VALUE),
				jobKitEngine,
				spoolNameWatchfolder,
				spoolNameWatchfolder,
				() -> new WatchedFilesInMemoryDb(maxDeep));

		/**
		 * 1st to Setup the WF database
		 */
		w.queueManualScan();

		// TODO AFTER just w.startScans(); (regular)
		w.queueManualScan();

		try {
			jobKitEngine.getSpooler()
					.getExecutor(spoolNameWatchfolder)
					.waitToEndQueue(Runnable::run)
					.get();
		} catch (InterruptedException | ExecutionException e) {
			Thread.currentThread().interrupt();
			log.error("Can't wait the work ends", e);
		}

		if (processedFiles.isEmpty()) {
			log.warn("No file founded in provided path(s): {}", inputRegularDirs);
		} else if (processedFiles.size() > 1) {
			log.info("Working file count: {}", processedFiles.size());
		}
	}

}
