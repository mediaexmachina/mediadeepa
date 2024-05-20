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
package media.mexm.mediadeepa;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static java.lang.Integer.MAX_VALUE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.io.FileUtils.forceMkdirParent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import tv.hd3g.fflauncher.recipes.MediaAnalyserSessionFilterContext;

@Slf4j
public class ImpExArchiveExtractionSession {

	private static final String CAN_T_READ_FROM_JSON = "Can't read from json";
	static final String NEWLINE = "\n";
	public static final int TEN_MB = 0xFFFFFF;

	private final ObjectMapper objectMapper;
	private final LinkedHashMap<String, String> contentItems;

	public ImpExArchiveExtractionSession() {
		this(new ObjectMapper());
		objectMapper.setLocale(ENGLISH);
		objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
		objectMapper.configure(INDENT_OUTPUT, true);
		objectMapper.configure(WRITE_DATES_AS_TIMESTAMPS, true);
	}

	public ImpExArchiveExtractionSession(final ObjectMapper objectMapper) {
		this.objectMapper = Objects.requireNonNull(objectMapper, "\"objectMapper\" can't to be null");
		contentItems = new LinkedHashMap<>();
	}

	public void add(final String internalFileName, final List<String> lines) {
		if (lines == null || lines.isEmpty()) {
			return;
		}
		add(internalFileName, lines.stream().collect(joining(NEWLINE)));
	}

	public record ExtractedFileEntry(String internalFileName, String content) {
	}

	public Stream<ExtractedFileEntry> getEntries() {
		return contentItems.entrySet()
				.stream()
				.filter(e -> e.getValue() != null)
				.filter(e -> e.getValue().isEmpty() == false)
				.map(e -> new ExtractedFileEntry(e.getKey(), e.getValue()));
	}

	public void add(final String internalFileName, final String content) {
		if (content == null || content.isEmpty()) {
			return;
		}
		contentItems.put(internalFileName, content);
	}

	private void add(final String internalFileName, final Object item) {
		try {
			add(internalFileName, objectMapper.writeValueAsString(item));
		} catch (final JsonProcessingException e) {
			throw new IllegalArgumentException("Can't produce json", e);
		}
	}

	public void addFilterContext(final String internalFileName, final List<MediaAnalyserSessionFilterContext> filters) {
		add(internalFileName, filters);
	}

	public List<MediaAnalyserSessionFilterContext> getFilterContext(final String internalFileName) {
		if (contentItems.containsKey(internalFileName) == false) {
			return List.of();
		}
		try {
			return objectMapper.readValue(contentItems.get(internalFileName),
					new TypeReference<List<MediaAnalyserSessionFilterContext>>() {});
		} catch (final JsonProcessingException e) {
			throw new IllegalArgumentException(CAN_T_READ_FROM_JSON, e);
		}
	}

	public void addVersion(final String internalFileName, final Map<String, String> versions) {
		add(internalFileName, versions);
	}

	public Map<String, String> getVersions(final String internalFileName) {
		if (contentItems.containsKey(internalFileName) == false) {
			return Map.of();
		}
		try {
			return objectMapper.readValue(contentItems.get(internalFileName),
					new TypeReference<Map<String, String>>() {});
		} catch (final JsonProcessingException e) {
			throw new IllegalArgumentException(CAN_T_READ_FROM_JSON, e);
		}
	}

	private Optional<String> getFullTextContent(final String internalFileName) {
		return Optional.ofNullable(contentItems.get(internalFileName));
	}

	public Optional<String> getFFmpegCommandLine(final String internalFileName) {
		return getFullTextContent(internalFileName);
	}

	public Optional<String> getFFprobeCommandLine(final String internalFileName) {
		return getFullTextContent(internalFileName);
	}

	public void addRunnedJavaCmdLine(final String internalFileName, final RunnedJavaCmdLine runnedJavaCmdLine) {
		add(internalFileName, runnedJavaCmdLine);
	}

	public RunnedJavaCmdLine getRunnedJavaCmdLine(final String internalFileName) {
		if (contentItems.containsKey(internalFileName) == false) {
			return new RunnedJavaCmdLine();
		}
		try {
			return objectMapper.readValue(contentItems.get(internalFileName),
					new TypeReference<RunnedJavaCmdLine>() {});
		} catch (final JsonProcessingException e) {
			throw new IllegalArgumentException(CAN_T_READ_FROM_JSON, e);
		}
	}

	public ImpExArchiveExtractionSession readFromZip(final File zipFile) {
		final var buffer = new byte[TEN_MB];
		final var maxReadZipEntry = Integer.parseInt(System.getProperty(
				"mediadeepa.maxReadZipEntry",
				/**
				 * Approx. 2 GB
				 */
				String.valueOf(MAX_VALUE)));

		log.info("Open and load {} zip file", zipFile);
		try (var zipIn = new ZipInputStream(
				new BufferedInputStream(new FileInputStream(zipFile), TEN_MB))) {
			ZipEntry zEntry;
			final var sb = new StringBuilder(10_000);
			while ((zEntry = zipIn.getNextEntry()) != null) {
				if (sb.length() > maxReadZipEntry) {
					log.warn(
							"Max zip entry allowed to readed: {} bytes. Change it with \"mediadeepa.maxReadZipEntry\" system property",
							maxReadZipEntry);
					throw new IllegalStateException("Max zip entry readed from an archive file");
				}
				int len;
				sb.setLength(0);
				while ((len = zipIn.read(buffer)) > 0) {
					sb.append(new String(buffer, 0, len));
				}
				add(zEntry.getName(), sb.toString());
			}
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't open/read input file as ZIP file", e);
		}
		return this;
	}

	public void saveToZip(final File zipFile) {
		if (zipFile.exists()) {
			log.info("Overwrite {} file", zipFile);
			try {
				FileUtils.forceDelete(zipFile);
			} catch (final IOException e) {
				throw new UncheckedIOException("Can't overwrite", e);
			}
		}

		final var entries = getEntries().iterator();
		if (entries.hasNext() == false) {
			log.warn("Nothing to export in archive file...");
			return;
		}

		log.info("Save to archive file {}", zipFile);
		try {
			forceMkdirParent(zipFile);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't create sub dirs", e);
		}

		try (var fileOut = new BufferedOutputStream(new FileOutputStream(zipFile), TEN_MB)) {
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
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write ZIP file", e);
		}
	}

}
