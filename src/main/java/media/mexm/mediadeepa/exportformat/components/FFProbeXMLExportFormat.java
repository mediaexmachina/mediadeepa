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
 * Copyright (C) Media ex Machina 2023
 *
 */
package media.mexm.mediadeepa.exportformat.components;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.apache.commons.io.FilenameUtils.getBaseName;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.cli.ExportToCmd;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormat;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;

@Component
public class FFProbeXMLExportFormat implements ExportFormat {

	@Autowired
	private AppConfig appConfig;

	@Override
	public String getFormatName() {
		return "ffprobexml";
	}

	@Override
	public String getFormatLongName() {
		return "Media file headers on FFprobe XML";
	}

	@Override
	public Map<String, File> exportResult(final DataResult result, final ExportToCmd exportToCmd) {
		return result.getFFprobeResult()
				.map(ffprobeJABX -> {
					final var outFile = exportToCmd.makeOutputFile(appConfig.getFfprobexmlFileName());
					try {
						FileUtils.write(
								outFile,
								ffprobeJABX.getXmlContent(),
								UTF_8,
								false);
					} catch (final IOException e) {
						throw new UncheckedIOException("Can't save file", e);
					}
					return outFile;
				})
				.stream()
				.collect(toUnmodifiableMap(f -> getBaseName(f.getName()), identity()));
	}

	@Override
	public Optional<byte[]> makeSingleExport(final DataResult result,
											 final String internalFileName) {
		return result.getFFprobeResult()
				.map(FFprobeJAXB::getXmlContent)
				.map(f -> f.getBytes(UTF_8));
	}

	@Override
	public Set<String> getInternalProducedFileNames() {
		return Set.of(appConfig.getFfprobexmlFileName());
	}

}
