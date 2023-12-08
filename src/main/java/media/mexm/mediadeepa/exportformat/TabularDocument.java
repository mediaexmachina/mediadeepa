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
package media.mexm.mediadeepa.exportformat;

import static java.lang.Float.NEGATIVE_INFINITY;
import static java.lang.Float.POSITIVE_INFINITY;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.cli.ExportToCmd;

/**
 * Manage structured header/cols/row document
 */
@Slf4j
public class TabularDocument {
	private final List<List<String>> lines;
	private List<String> header;
	private final TabularDocumentExporter exporter;
	private final String baseFileName;

	public TabularDocument(final TabularDocumentExporter exporter, final String baseFileName) {
		this.exporter = Objects.requireNonNull(exporter);
		this.baseFileName = Objects.requireNonNull(baseFileName, "\"baseFileName\" can't to be null");
		lines = new ArrayList<>();
		header = List.of();
	}

	public TabularDocument head(final List<String> header) {
		this.header = Objects.requireNonNull(header, "\"header\" can't to be null");
		if (header.isEmpty()) {
			throw new IllegalArgumentException("Empty header");
		}
		return this;
	}

	public void row(final List<String> item) {
		if (item == null || item.isEmpty()) {
			return;
		} else if (header.size() != item.size()) {
			throw new IllegalArgumentException(
					"Invalid item count (" + item.size() + "), header count is " + header.size() + ". Idem=" + item);
		}
		lines.add(item);
	}

	public TabularDocument row(final Object... item) {
		row(Optional.ofNullable(item).stream()
				.flatMap(Stream::of)
				.map(o -> {
					if (o == null) {
						return "";
					} else if (o instanceof final String str) {
						return str;
					} else if (o instanceof final Duration d) {
						return exporter.getNumberUtils().durationToString(d);
					} else if (o instanceof final Float oF) {
						if (oF.isNaN()) {
							return "";
						} else if (oF == NEGATIVE_INFINITY) {
							return "-144";
						} else if (oF == POSITIVE_INFINITY) {
							return "144";
						}
						return exporter.formatNumberHighPrecision(oF);
					} else if (o instanceof Number) {
						return String.valueOf(o);
					} else {
						return o.toString();
					}
				})
				.toList());
		return this;
	}

	public Optional<File> exportToFile(final ExportToCmd exportToCmd) {
		final var outfile = exportToCmd.makeOutputFile(baseFileName + "." + exporter.getDocumentFileExtension());
		if (lines.isEmpty()) {
			log.trace("Nothing to export for {}", outfile.getName());
			return Optional.empty();
		}

		try {
			FileUtils.writeByteArrayToFile(outfile, exporter.getDocument(header, lines));
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write file", e);
		}
		return Optional.ofNullable(outfile);
	}

}
