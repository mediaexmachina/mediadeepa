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

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.service.AppSessionService;
import tv.hd3g.jobkit.watchfolder.FolderActivity;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;
import tv.hd3g.transfertfiles.CachedFileAttributes;
import tv.hd3g.transfertfiles.local.LocalFile;

/**
 * This can be runned outside JobKit
 */
@Slf4j
class FoundedFiles implements FolderActivity {

	private final AppSessionService appService;
	private final WorkingSessionResult workingSessionResult;
	private final boolean multipleSources;

	FoundedFiles(final AppSessionService appService, final WorkingSessionResult workingSessionResult,
				 final boolean multipleSources) {
		this.appService = appService;
		this.workingSessionResult = workingSessionResult;
		this.multipleSources = multipleSources;
	}

	void onFoundFile(final File inputFile) {
		workingSessionResult.afterWork(inputFile, appService.fileWork(inputFile, multipleSources));
	}

	@Override
	public void onBeforeScan(final ObservedFolder observedFolder) throws IOException {
		log.debug("Directory scan on {}", observedFolder.getLabel());
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
					.forEach(this::onFoundFile);
		} else {
			log.trace("Empty updated files on \"{}\" directory", observedFolder.getTargetFolder());
		}
	}

	@Override
	public void onScanErrorFolder(final ObservedFolder observedFolder, final Exception e) throws IOException {
		log.error("Can't scan {}", observedFolder.getTargetFolder(), e);
	}

}
