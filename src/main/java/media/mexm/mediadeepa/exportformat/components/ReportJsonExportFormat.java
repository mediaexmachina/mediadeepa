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

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;
import static media.mexm.mediadeepa.exportformat.ProcessingHandledData.CONTAINER_ANALYSIS;
import static media.mexm.mediadeepa.exportformat.ProcessingHandledData.MEDIA_ANALYSIS;
import static media.mexm.mediadeepa.exportformat.report.JsonContentProvider.createModule;
import static org.apache.commons.io.FileUtils.write;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.components.OutputFileSupplier;
import media.mexm.mediadeepa.components.RendererEngineComparator;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormat;
import media.mexm.mediadeepa.exportformat.ProcessingHandledData;
import media.mexm.mediadeepa.exportformat.report.ReportDocument;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;

@Component
public class ReportJsonExportFormat implements ExportFormat, ConstStrings {

	@Autowired
	private List<ReportRendererEngine> engines;
	@Autowired
	private RendererEngineComparator rendererEngineComparator;
	@Autowired
	private AppConfig appConfig;
	@Autowired
	private OutputFileSupplier outputFileSupplier;
	private ObjectMapper objectMapper;

	@PostConstruct
	void afterPropertiesSet() {
		objectMapper = new ObjectMapper();
		objectMapper.setLocale(ENGLISH);
		objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, true);
		objectMapper.configure(INDENT_OUTPUT, appConfig.getReportConfig().isJsonIdentOutput());
		objectMapper.configure(WRITE_DATES_AS_TIMESTAMPS, false);
		objectMapper.configure(WRITE_DATES_WITH_CONTEXT_TIME_ZONE, true);
		objectMapper.findAndRegisterModules();
		objectMapper.registerModule(createModule());
	}

	@Override
	public String getFormatName() {
		return "jsonreport";
	}

	@Override
	public String getFormatLongName() {
		return "JSON document report";
	}

	@Override
	public String getFormatDescription() {
		return "with the same informations as html report";
	}

	@Override
	public Set<ProcessingHandledData> canHandleProcessingData() {
		return Set.of(MEDIA_ANALYSIS, CONTAINER_ANALYSIS);
	}

	private String getJson(final Object object) {
		try {
			return objectMapper.writeValueAsString(object);
		} catch (final JsonProcessingException e) {
			throw new IllegalStateException("Trouble with " + object.getClass().getName(), e);
		}
	}

	@Override
	public Map<String, File> exportResult(final DataResult result) {
		try {
			final var outFile = outputFileSupplier.makeOutputFile(
					result, appConfig.getReportConfig().getJsonFilename());
			write(outFile,
					getJson(new ReportDocument(result, engines, rendererEngineComparator)),
					UTF_8, false);

			return Map.of("json_report", outFile);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write json file", e);
		}
	}

	@Override
	public Optional<byte[]> makeSingleExport(final DataResult result,
											 final String internalFileName) {
		return Optional.ofNullable(
				getJson(new ReportDocument(result, engines, rendererEngineComparator)).getBytes(UTF_8));
	}

	@Override
	public Set<String> getInternalProducedFileNames() {
		return Set.of(appConfig.getReportConfig().getJsonFilename());
	}

}
