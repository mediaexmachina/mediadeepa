/*
 * This file is part of mediadeepa.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2024
 *
 */
package media.mexm.mediadeepa.exportformat.components;

import static java.util.stream.Collectors.toUnmodifiableMap;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static media.mexm.mediadeepa.exportformat.ProcessingHandledData.WAVEFORM;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.components.OutputFileSupplier;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormat;
import media.mexm.mediadeepa.exportformat.ImageArtifact;
import media.mexm.mediadeepa.exportformat.ProcessingHandledData;
import media.mexm.mediadeepa.rendererengine.SignalImageRendererEngine;

@Component
public class SignalImageExportFormat implements ExportFormat {

	@Autowired
	private OutputFileSupplier outputFileSupplier;
	@Autowired
	private List<SignalImageRendererEngine> engines;

	@Override
	public Set<ProcessingHandledData> canHandleProcessingData() {
		return Set.of(WAVEFORM);
	}

	@Override
	public String getFormatName() {
		return "signalimage";
	}

	@Override
	public String getFormatLongName() {
		return "Signal image representation";
	}

	@Override
	public String getFormatDescription() {
		return "Draw an image from raw signal like audio wavform";
	}

	@Override
	public Set<String> getInternalProducedFileNames() {
		return engines.stream()
				.map(SignalImageRendererEngine::getDefaultInternalFileName)
				.collect(toUnmodifiableSet());
	}

	private record Result(String name, File content) {
	}

	@Override
	public Map<String, File> exportResult(final DataResult result) {
		return engines.stream()
				.map(e -> signalToFile(result, e))
				.flatMap(Optional::stream)
				.collect(toUnmodifiableMap(f -> f.name, f -> f.content));
	}

	private Optional<Result> signalToFile(final DataResult result, final SignalImageRendererEngine r) {
		final var oIa = r.makeimagePNG(result);
		if (oIa.isEmpty()) {
			return Optional.empty();
		}
		final var ia = oIa.get();
		final var name = ia.name();
		final var data = ia.data();
		final var outFile = outputFileSupplier.makeOutputFile(result, r.getDefaultInternalFileName());

		try {
			FileUtils.writeByteArrayToFile(outFile, data, false);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write file", e);
		}

		return Optional.ofNullable(new Result(name, outFile));
	}

	@Override
	public Optional<byte[]> makeSingleExport(final DataResult result, final String internalFileName) {
		return engines.stream()
				.filter(e -> e.getDefaultInternalFileName().equals(internalFileName))
				.findFirst()
				.flatMap(e -> e.makeimagePNG(result))
				.map(ImageArtifact::data);
	}

}
