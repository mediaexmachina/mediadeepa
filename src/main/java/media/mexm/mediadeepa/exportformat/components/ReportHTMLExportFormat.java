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
import static org.apache.commons.io.FileUtils.write;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.components.OutputFileSupplier;
import media.mexm.mediadeepa.components.RendererEngineComparator;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormat;
import media.mexm.mediadeepa.exportformat.report.ReportDocument;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;

@Component
public class ReportHTMLExportFormat implements ExportFormat, ConstStrings {

	@Autowired
	private List<ReportRendererEngine> engines;
	@Autowired
	private RendererEngineComparator rendererEngineComparator;
	@Autowired
	private AppConfig appConfig;
	@Autowired
	private NumberUtils numberUtils;
	@Autowired
	private OutputFileSupplier outputFileSupplier;

	@Value("classpath:html-report-style.css")
	private Resource cssHTMLReportResource;

	@Override
	public String getFormatName() {
		return "report";
	}

	@Override
	public String getFormatLongName() {
		return "HTML document report";
	}

	@Override
	public String getFormatDescription() {
		return "with file and signal stats, event detection, codecs, GOP stats...";
	}

	@Override
	public Map<String, File> exportResult(final DataResult result) {
		try {
			final var outFile = outputFileSupplier.makeOutputFile(
					result, appConfig.getReportConfig().getHtmlFilename());
			write(outFile,
					new ReportDocument(result, engines, rendererEngineComparator)
							.toHTML(cssHTMLReportResource, numberUtils),
					UTF_8, false);

			return Map.of("html_report", outFile);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write html file", e);
		}
	}

	@Override
	public Optional<byte[]> makeSingleExport(final DataResult result,
											 final String internalFileName) {
		return Optional.ofNullable(
				new ReportDocument(result, engines, rendererEngineComparator)
						.toHTML(cssHTMLReportResource, numberUtils)
						.getBytes(UTF_8));
	}

	@Override
	public Set<String> getInternalProducedFileNames() {
		return Set.of(appConfig.getReportConfig().getHtmlFilename());
	}

}
