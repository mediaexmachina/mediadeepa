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

import static java.util.Collections.synchronizedList;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkingSessionResult {

	private final List<WorkingResult> results;

	public WorkingSessionResult() {
		results = synchronizedList(new ArrayList<>());
	}

	public boolean isEmpty() {
		return results.isEmpty();
	}

	public int size() {
		return results.size();
	}

	public void afterWork(final File inputFile, final Map<String, File> fileWork) {
		results.add(new WorkingResult(requireNonNull(inputFile), requireNonNull(fileWork)));
	}

	public record WorkingResult(File inputFile, Map<String, File> fileWork) {

		public void checkPublishedFilePresence() {
			log.debug("Start to check {} files produced for {}...", fileWork.size(), inputFile.getName());

			fileWork.forEach((name, outFile) -> {
				if (outFile.exists() && outFile.length() > 0l) {
					log.trace("File exists and not empty: {} [{}]", outFile.getName(), name);
				} else {
					throw new UncheckedIOException(new IOException(
							"Can't found/empty expected file " + outFile + " [" + name + "]"));
				}
			});

		}
	}

	public void checkPublishedFilePresence() {
		results.forEach(WorkingResult::checkPublishedFilePresence);
	}

}
