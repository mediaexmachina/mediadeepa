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

import static media.mexm.mediadeepa.ImpExArchiveExtractionSession.TEN_MB;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.components.OutputFileSupplier;
import media.mexm.mediadeepa.exportformat.TableDocument.Table;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;

@Slf4j
public abstract class TableExportFormat implements ExportFormat {

	protected final List<TableRendererEngine> engines;
	protected final NumberUtils numberUtils;
	protected final OutputFileSupplier outputFileSupplier;

	protected TableExportFormat(final List<TableRendererEngine> engines,
								final NumberUtils numberUtils,
								final OutputFileSupplier outputFileSupplier) {
		this.engines = Objects.requireNonNull(engines, "\"engines\" can't to be null");
		this.numberUtils = Objects.requireNonNull(numberUtils, "\"numberUtils\" can't to be null");
		this.outputFileSupplier = Objects.requireNonNull(outputFileSupplier, "\"outputFileSupplier\" can't to be null");
	}

	public abstract String getInternalFileName();

	public abstract void makeDocument(DataResult result,
									  List<Table> tables,
									  OutputStream outputStream);

	@Override
	public Map<String, File> exportResult(final DataResult result) {
		final var tableDocument = new TableDocument(numberUtils);
		engines.forEach(en -> en.addToTable(result, tableDocument));

		final var outputFile = outputFileSupplier.makeOutputFile(result, getInternalFileName());
		log.debug("Start export {} tables to {}...", tableDocument.getTables().size(), outputFile);

		try (var outputStream = new BufferedOutputStream(new FileOutputStream(outputFile), 0XFFFFFF)) {
			makeDocument(result, tableDocument.getTables(), outputStream);
		} catch (final FileNotFoundException e) {
			throw new UncheckedIOException("Can't create file", e);
		} catch (final IOException e1) {
			throw new UncheckedIOException("Can't close stream file", e1);
		}

		return Map.of("tables", outputFile);
	}

	@Override
	public Optional<byte[]> makeSingleExport(final DataResult result,
											 final String internalFileName) {
		final var tableDocument = new TableDocument(numberUtils);
		engines.forEach(en -> en.addToTable(result, tableDocument));

		final var bias = new ByteArrayOutputStream(TEN_MB);
		makeDocument(result, tableDocument.getTables(), bias);
		return Optional.ofNullable(bias.toByteArray());
	}

	@Override
	public Set<String> getInternalProducedFileNames() {
		return Set.of(getInternalFileName());
	}

}
