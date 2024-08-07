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
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkingSession {// TODO test

	private final List<File> inputFiles;

	public WorkingSession(final List<String> rawInputFiles,
						  final Consumer<File> validateInputFile,
						  final boolean limitOneFile,
						  final Runnable onMoreThanLimitOneFile) {
		inputFiles = Optional.ofNullable(rawInputFiles)
				.orElse(List.of())
				.stream()
				.map(File::new)
				.toList();

		if (limitOneFile && inputFiles.size() > 1) {
			onMoreThanLimitOneFile.run();
			return;
		}

		inputFiles.forEach(validateInputFile::accept);
	}

	public void startWork(final Consumer<File> onFoundFile) {
		if (inputFiles.size() > 1) {
			inputFiles.forEach(f -> log.info("Prepare to work on {}", f.getAbsolutePath()));
		}
		inputFiles.forEach(onFoundFile::accept);
	}

}
