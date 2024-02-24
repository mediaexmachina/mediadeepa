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
package media.mexm.mediadeepa.exportformat.components;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.apache.commons.io.FilenameUtils.getBaseName;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.cli.AppCommand;
import media.mexm.mediadeepa.components.OutputFileSupplier;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormat;
import media.mexm.mediadeepa.rendererengine.GraphicRendererEngine;

@Component
public class GraphicExportFormat implements ExportFormat {

	@Autowired
	private List<GraphicRendererEngine> engines;
	@Autowired
	private AppConfig appConfig;
	@Autowired
	private AppCommand appCommand;
	@Autowired
	private OutputFileSupplier outputFileSupplier;

	@Override
	public Map<String, File> exportResult(final DataResult result) {
		return engines.stream()
				.map(engine -> engine.toGraphic(result))
				.flatMap(List::stream)
				.map(f -> f.save(appCommand, appConfig, outputFileSupplier, result))
				.collect(toUnmodifiableMap(
						f -> getBaseName(f.getName()),
						identity()));
	}

	@Override
	public Set<String> getInternalProducedFileNames() {
		return engines.stream()
				.map(GraphicRendererEngine::getGraphicInternalProducedBaseFileNames)
				.flatMap(Set::stream)
				.distinct()
				.collect(toUnmodifiableSet());
	}

	@Override
	public Optional<byte[]> makeSingleExport(final DataResult result,
											 final String internalFileName) {
		final var baseFileName = getBaseName(internalFileName);
		return engines.stream()
				.filter(engine -> engine.getGraphicInternalProducedBaseFileNames().contains(baseFileName))
				.findFirst()
				.flatMap(engine -> engine.toSingleGraphic(baseFileName, result))
				.map(ga -> ga.getRawData(appCommand, appConfig));
	}

	@Override
	public String getFormatName() {
		return "graphic";
	}

	@Override
	public String getFormatLongName() {
		return "Data graphical representation";
	}

	@Override
	public String getFormatDescription() {
		final var result = """
				audio Loudness (integrated, short-term, momentary, true-peak)
				audio phase correlation (+/- 180°)
				audio DC offset, signal entropy, flatness, noise floor, peak level, silence/mono events
				video quality as spatial information (SI) and temporal information
				video block/blur/black/interlacing/crop/freeze detection
				video and audio iterate frames
				video frame duration stability
				video GOP (group of picture) size (number of frames by GOP, and by frame type)
				video GOP frame size, by frame type, by GOP, by frame number
				""";
		return result.lines().collect(joining(", "));
	}

}
