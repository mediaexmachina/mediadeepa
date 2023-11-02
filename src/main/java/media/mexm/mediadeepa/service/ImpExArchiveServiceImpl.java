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
package media.mexm.mediadeepa.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.forceMkdirParent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.ImpExArchiveExtractionSession;
import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ExtractTo;
import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ImportFrom;

@Slf4j
@Service
public class ImpExArchiveServiceImpl implements ImpExArchiveService {

	private static final int TEN_MB = 0xFFFFFF;

	@Override
	public void saveTo(final ExtractTo extractTo, final ImpExArchiveExtractionSession session) throws IOException {
		final var f = extractTo.getArchiveFile();
		if (f.exists()) {
			log.info("Overwrite {} file", f);
			FileUtils.forceDelete(f);
		}

		final var entries = session.getEntries().iterator();
		if (entries.hasNext() == false) {
			log.warn("Nothing to export in archive file...");
			return;
		}

		final var archiveFile = extractTo.getArchiveFile();
		log.info("Save to archive file {}", archiveFile);
		forceMkdirParent(archiveFile);

		try (var fileOut = new BufferedOutputStream(new FileOutputStream(archiveFile), TEN_MB)) {
			try (var zipOut = new ZipOutputStream(fileOut)) {
				while (entries.hasNext()) {
					final var entry = entries.next();
					log.debug("Add to zip {} ({} chars)", entry.internalFileName(), entry.content().length());
					zipOut.putNextEntry(new ZipEntry(entry.internalFileName()));
					zipOut.write(entry.content().getBytes(UTF_8));
					zipOut.closeEntry();
				}
				zipOut.flush();
			}
		}
	}

	@Override
	public ImpExArchiveExtractionSession loadFrom(final ImportFrom importFrom) throws IOException {
		final var session = new ImpExArchiveExtractionSession();

		final var buffer = new byte[TEN_MB];
		try (var zipIn = new ZipInputStream(
				new BufferedInputStream(new FileInputStream(importFrom.getArchiveFile()), TEN_MB))) {
			ZipEntry zEntry;
			final var sb = new StringBuilder(10_000);
			while ((zEntry = zipIn.getNextEntry()) != null) {
				int len;
				sb.setLength(0);
				while ((len = zipIn.read(buffer)) > 0) {
					sb.append(new String(buffer, 0, len));
				}
				session.add(zEntry.getName(), sb.toString());
			}
		}
		return session;
	}

}
