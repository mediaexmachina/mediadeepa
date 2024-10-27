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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static media.mexm.mediadeepa.exportformat.ProcessingHandledData.CONTAINER_ANALYSIS;
import static media.mexm.mediadeepa.exportformat.ProcessingHandledData.MEDIA_ANALYSIS;
import static org.apache.commons.io.FilenameUtils.getBaseName;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import media.mexm.mediadeepa.components.OutputFileSupplier;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;

public abstract class TabularExportFormat implements ExportFormat, TabularDocumentExporter {

	protected final List<TabularRendererEngine> engines;
	protected final OutputFileSupplier outputFileSupplier;

	protected TabularExportFormat(final List<TabularRendererEngine> engines,
								  final OutputFileSupplier outputFileSupplier) {
		this.engines = Objects.requireNonNull(engines, "\"engines\" can't to be null");
		this.outputFileSupplier = Objects.requireNonNull(outputFileSupplier, "\"outputFileSupplier\" can't to be null");
	}

	@Override
	public Set<ProcessingHandledData> canHandleProcessingData() {
		return Set.of(MEDIA_ANALYSIS, CONTAINER_ANALYSIS);
	}

	@Override
	public Set<String> getInternalProducedFileNames() {
		return engines.stream()
				.map(TabularRendererEngine::getInternalTabularBaseFileNames)
				.flatMap(Set::stream)
				.distinct()
				.map(name -> name + "." + getDocumentFileExtension())
				.collect(toUnmodifiableSet());
	}

	@Override
	public Optional<byte[]> makeSingleExport(final DataResult result,
											 final String internalFileName) {
		final var internalBaseFileName = getBaseName(internalFileName);
		return engines.stream()
				.filter(en -> en.getInternalTabularBaseFileNames().contains(internalBaseFileName))
				.findFirst()
				.map(en -> en.toSingleTabularDocument(internalBaseFileName, result, this))
				.flatMap(identity())
				.flatMap(TabularDocument::exportToBytes);
	}

	@Override
	public Map<String, File> exportResult(final DataResult result) {
		return engines.stream()
				.map(en -> en.toTabularDocument(result, this))
				.flatMap(List::stream)
				.map(tabular -> tabular.exportToFile(outputFileSupplier, result))
				.flatMap(Optional::stream)
				.collect(toUnmodifiableMap(f -> getBaseName(f.getName()), f -> f));
	}

}
