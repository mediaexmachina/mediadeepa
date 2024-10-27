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
package media.mexm.mediadeepa.exportformat.components;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.empty;
import static media.mexm.mediadeepa.exportformat.ProcessingHandledData.SNAPSHOT_IMAGE;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.components.OutputFileSupplier;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormat;
import media.mexm.mediadeepa.exportformat.ImageArtifact;
import media.mexm.mediadeepa.exportformat.ProcessingHandledData;
import media.mexm.mediadeepa.rendererengine.components.SignificantSnapshotImageRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.StripSnapshotImagesRendererEngine;

@Component
@Slf4j
public class SnapshotImageExportFormat implements ExportFormat {

	@Autowired
	private OutputFileSupplier outputFileSupplier;
	@Autowired
	private SignificantSnapshotImageRendererEngine significantEngine;
	@Autowired
	private StripSnapshotImagesRendererEngine stripEngine;
	@Autowired
	private AppConfig appConfig;

	@Override
	public Set<ProcessingHandledData> canHandleProcessingData() {
		return Set.of(SNAPSHOT_IMAGE);
	}

	@Override
	public String getFormatName() {
		return "snapshots";
	}

	@Override
	public String getFormatLongName() {
		return "Image snapshots";
	}

	@Override
	public String getFormatDescription() {
		return "Extract image snapshots (significant and strip) from media file";
	}

	private void write(final File file, final byte[] data) {
		try {
			writeByteArrayToFile(file, data, false);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write file", e);
		}
	}

	@Override
	public Map<String, File> exportResult(final DataResult result) {
		final var producedResult = new LinkedHashMap<String, File>();

		final var significantImageResult = significantEngine.makeimage(result);
		if (significantImageResult.isPresent()
			&& appConfig.getSnapshotImageConfig().isExportSignificant()) {
			final var internalFileName = significantEngine.getDefaultInternalFileName();
			final var outFile = outputFileSupplier.makeOutputFile(result, internalFileName);
			write(outFile, significantImageResult.get().data());
			producedResult.put("significantImage", outFile);
		}

		final var stripImagesResult = stripEngine.makeimages(result);
		if (stripImagesResult.isPresent()) {
			final var internalFileName = stripEngine.getDefaultInternalFileName();
			final var images = stripImagesResult.get();

			IntStream.range(0, images.size()).forEach(i -> {
				final var outFile = outputFileSupplier.makeOutputFile(result, format(internalFileName, i + 1));
				write(outFile, images.get(i).data());
				producedResult.put("stripImage" + i, outFile);
			});

		}

		return unmodifiableMap(producedResult);
	}

	@Override
	public Set<String> getInternalProducedFileNames() {
		return Set.of(significantEngine.getDefaultInternalFileName(), stripEngine.getDefaultInternalFileName());
	}

	@Override
	public Optional<byte[]> makeSingleExport(final DataResult result, final String internalFileName) {
		if (internalFileName.equals(significantEngine.getDefaultInternalFileName())) {
			return significantEngine.makeimage(result)
					.map(ImageArtifact::data);
		}
		if (internalFileName.equals(stripEngine.getDefaultInternalFileName())) {
			log.error("Can't extract strip images as single export");
		}

		return empty();
	}

}
