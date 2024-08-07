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

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;
import tv.hd3g.jobkit.engine.BackgroundServiceEvent;
import tv.hd3g.jobkit.engine.ExecutionEvent;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.watchfolder.FolderActivity;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;
import tv.hd3g.jobkit.watchfolder.WatchedFilesInMemoryDb;
import tv.hd3g.jobkit.watchfolder.Watchfolders;
import tv.hd3g.transfertfiles.CachedFileAttributes;
import tv.hd3g.transfertfiles.local.LocalFile;

@Slf4j
public class WorkingSession {// TODO test

	private final List<File> inputRegularFiles;
	private final List<File> inputRegularDirs;
	private final Consumer<File> onFoundFile;

	private final WatchedFilesInMemoryDb db;
	private final JobKitEngine jobKitEngine;

	public WorkingSession(final List<String> rawInput,
						  final Consumer<File> onFoundFile,
						  final Consumer<File> validateInputFile,
						  final boolean limitOneFile,
						  final Runnable onMoreThanLimitOneFile) {
		this.onFoundFile = onFoundFile;

		db = new WatchedFilesInMemoryDb();// TODO set max deep;
		final var scheduledExecutor = Executors.newScheduledThreadPool(1);// TODO provided
		jobKitEngine = new JobKitEngine(scheduledExecutor, new ExecutionEvent() {}, new BackgroundServiceEvent() {}); // TODO provided

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

		if (limitOneFile
			&& inputRegularFiles.size() > 1
			|| inputRegularDirs.isEmpty() == false) {
			onMoreThanLimitOneFile.run();
		}

		if (inputRegularFiles.size() + inputRegularDirs.size() > 1) {
			inputRegularFiles.forEach(f -> log.info("Prepare to work on \"{}\"", f.getAbsolutePath()));
			inputRegularDirs.forEach(f -> log.info("Prepare to work on \"{}\" directory", f.getAbsolutePath()));
		}

		inputRegularFiles.forEach(validateInputFile::accept);

		// TODO willcard process, only here
	}

	public void startWork() {
		inputRegularFiles.forEach(onFoundFile::accept);

		if (inputRegularDirs.isEmpty()) {
			return;
		}

		final var allObservedFolders = inputRegularDirs.stream()
				.map(dir -> {
					final var observedFolder = new ObservedFolder();
					observedFolder.setTargetFolder(dir.getAbsolutePath());
					return observedFolder;
				})
				.toList();

		final var lockToEnd = new CountDownLatch(allObservedFolders.size());

		class Founded implements FolderActivity {

			@Override
			public void onAfterScan(final ObservedFolder observedFolder,
									final Duration scanTime,
									final WatchedFiles scanResult) throws IOException {
				final var foundedAndUpdated = scanResult.foundedAndUpdated();
				log.info("Start to work with {} file(s) found on \"{}\" directory",
						foundedAndUpdated.size(),
						observedFolder.getTargetFolder());

				foundedAndUpdated.stream()
						.map(CachedFileAttributes::getAbstractFile)
						.map(f -> (LocalFile) f)
						.map(LocalFile::getInternalFile)
						.forEach(onFoundFile::accept);

				lockToEnd.countDown();
			}

			@Override
			public void onScanErrorFolder(final ObservedFolder observedFolder, final Exception e) throws IOException {
				log.error("Can't scan {}", observedFolder.getTargetFolder(), e);
				lockToEnd.countDown();
			}
		}

		final var w = new Watchfolders(
				allObservedFolders,
				new Founded(),
				Duration.ofDays(MAX_VALUE),
				jobKitEngine,
				"internalwatch",
				"internalrun",
				() -> db);
		w.startScans();

		try {
			lockToEnd.await();
		} catch (final InterruptedException e) {
			log.error("Can't to wait to scan dir(s)", e);
			Thread.currentThread().interrupt();
		}
		w.stopScans();
	}

}
