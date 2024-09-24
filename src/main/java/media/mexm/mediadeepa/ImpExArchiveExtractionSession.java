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
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.io.FilenameUtils.removeExtension;

import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
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
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.exportformat.VideoImageSnapshots;
import tv.hd3g.fflauncher.recipes.MediaAnalyserSessionFilterContext;
import tv.hd3g.fflauncher.recipes.wavmeasure.MeasuredWav;

@Slf4j
public class ImpExArchiveExtractionSession {

	public static final String DATAS_ZIP_DIR = "data/";
	private static final String CAN_T_READ_FROM_JSON = "Can't read from json";
	static final String NEWLINE = "\n";
	public static final int TEN_MB = 0xFFFFFF;

	private final ObjectMapper objectMapper;
	private final LinkedHashMap<String, String> contentItems;
	private final LinkedHashMap<String, byte[]> contentDatas;

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
		contentDatas = new LinkedHashMap<>();
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

	public void addMeasuredWav(final String internalFileName, final MeasuredWav measuredWav) {
		add(internalFileName, measuredWav);
	}

	public void addVideoImageSnapshots(final String internalFileNameSignificantJson,
									   final String internalFileNameSignificantJPG,
									   final String internalFileNameStripJPG,
									   final VideoImageSnapshots videoImageSnapshots) {
		add(internalFileNameSignificantJson, videoImageSnapshots.imageSize());
		contentDatas.put(internalFileNameSignificantJPG, videoImageSnapshots.significantImageData());

		final var stripImages = videoImageSnapshots.stripImagesData();
		final var stripBaseName = removeExtension(internalFileNameStripJPG);
		final var stripExt = getExtension(internalFileNameStripJPG);

		IntStream.range(0, stripImages.size())
				.forEach(i -> contentDatas.put(stripBaseName + i + "." + stripExt, stripImages.get(i)));

	}

	public Optional<VideoImageSnapshots> getVideoImageSnapshots(final String internalFileNameSignificantJson,
																final String internalFileNameSignificantJPG,
																final String internalFileNameStripJPG) {
		if (contentItems.containsKey(internalFileNameSignificantJson) == false
			|| contentDatas.containsKey(internalFileNameSignificantJPG) == false) {
			return Optional.empty();
		}

		final Dimension imageSize;
		try {
			imageSize = objectMapper.readValue(contentItems.get(internalFileNameSignificantJson),
					new TypeReference<Dimension>() {});
		} catch (final JsonProcessingException e) {
			throw new IllegalArgumentException(CAN_T_READ_FROM_JSON, e);
		}

		final var significantImageData = contentDatas.get(internalFileNameSignificantJPG);

		final var stripBaseName = removeExtension(internalFileNameStripJPG);
		final var stripExt = getExtension(internalFileNameStripJPG);

		final var stripImagesData = IntStream.range(0, contentDatas.size())
				.mapToObj(i -> stripBaseName + i + "." + stripExt)
				.filter(contentDatas::containsKey)
				.map(contentDatas::get)
				.toList();

		return Optional.ofNullable(new VideoImageSnapshots(significantImageData, imageSize, stripImagesData));
	}

	public Optional<MeasuredWav> getMeasuredWav(final String internalFileName) {
		if (contentItems.containsKey(internalFileName) == false) {
			return Optional.empty();
		}
		try {
			return Optional.ofNullable(objectMapper.readValue(contentItems.get(internalFileName),
					new TypeReference<MeasuredWav>() {}));
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
		final var maxReadZipEntry = Integer.parseInt(System.getProperty(
				"mediadeepa.maxReadZipEntry",
				/**
				 * Approx. 2 GB
				 */
				String.valueOf(MAX_VALUE - 1)));

		log.info("Open and load {} zip file", zipFile);
		try (var zipIn = new ZipInputStream(
				new BufferedInputStream(new FileInputStream(zipFile),
						TEN_MB > (int) zipFile.length() ? (int) zipFile.length() : TEN_MB))) {
			ZipEntry zEntry;
			final var byteArray = new ByteArrayOutputStream(TEN_MB);

			int val;
			while ((zEntry = zipIn.getNextEntry()) != null) {
				if (zEntry.getName().equals(DATAS_ZIP_DIR)) {
					continue;
				}

				while ((val = zipIn.read()) > -1) {
					byteArray.write(val);

					if (byteArray.size() >= maxReadZipEntry) {
						throw new IOException("Zip entry (" + zEntry.getName() + ") is too big: " + maxReadZipEntry);
					}
				}

				if (zEntry.getName().startsWith(DATAS_ZIP_DIR)) {
					contentDatas.put(
							zEntry.getName().substring(DATAS_ZIP_DIR.length()),
							byteArray.toByteArray());
				} else {
					add(zEntry.getName(), new String(byteArray.toByteArray(), UTF_8));
				}

				byteArray.reset();
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
		if (entries.hasNext() == false && contentDatas.isEmpty()) {
			log.warn("Nothing to export in archive file...");
			return;
		}

		log.info("Save to archive file {}", zipFile);
		try {
			forceMkdirParent(zipFile);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't create sub dirs", e);
		}

		final var rawEntries = contentDatas.entrySet().iterator();

		try (var fileOut = new BufferedOutputStream(new FileOutputStream(zipFile), TEN_MB)) {
			try (var zipOut = new ZipOutputStream(fileOut)) {
				while (entries.hasNext()) {
					final var entry = entries.next();
					log.debug("Add to zip {} ({} chars)", entry.internalFileName(), entry.content().length());
					zipOut.putNextEntry(new ZipEntry(entry.internalFileName()));
					zipOut.write(entry.content().getBytes(UTF_8));
					zipOut.closeEntry();
				}

				if (rawEntries.hasNext()) {
					zipOut.putNextEntry(new ZipEntry(DATAS_ZIP_DIR));
				}
				while (rawEntries.hasNext()) {
					final var entry = rawEntries.next();
					zipOut.putNextEntry(new ZipEntry(DATAS_ZIP_DIR + entry.getKey()));
					zipOut.write(entry.getValue());
					zipOut.closeEntry();
				}

				zipOut.flush();
			}
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write ZIP file", e);
		}
	}

}
