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
package media.mexm.mediadeepa.rendererengine.components;

import static java.util.function.Predicate.not;
import static media.mexm.mediadeepa.exportformat.ReportSectionCategory.ABOUT;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.RunnedJavaCmdLine;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.KeyPreValueReportEntry;
import media.mexm.mediadeepa.exportformat.ReportDocument;
import media.mexm.mediadeepa.exportformat.ReportSection;
import media.mexm.mediadeepa.exportformat.SimpleKeyValueReportEntry;
import media.mexm.mediadeepa.exportformat.TableDocument;
import media.mexm.mediadeepa.exportformat.TabularDocument;
import media.mexm.mediadeepa.exportformat.TabularExportFormat;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;

@Component
public class AboutMeasureRendererEngine implements
										ReportRendererEngine,
										TableRendererEngine,
										TabularRendererEngine,
										ConstStrings {

	@Autowired
	private RunnedJavaCmdLine runnedJavaCmdLine;

	public static final List<String> HEAD_ABOUT = List.of(TYPE, NAME, SETUP, JAVA_CLASS);
	public static final List<String> HEAD_APP_ABOUT = List.of(TYPE, VALUE);

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		final var tAbout = new TabularDocument(tabularExportFormat, "about").head(HEAD_APP_ABOUT);
		result.getVersions().entrySet().forEach(entry -> tAbout.row(entry.getKey(), entry.getValue()));

		runnedJavaCmdLine.makeArchiveCommandline()
				.ifPresent(cmdLine -> tAbout.row(ANALYSIS_CREATED_BY, cmdLine));
		tAbout.row(REPORT_CREATED_BY, runnedJavaCmdLine.makeFullExtendedCommandline());

		final var oTFilters = result.getMediaAnalyserResult()
				.map(maResult -> {
					final var tFilters = new TabularDocument(tabularExportFormat, "filters")
							.head(HEAD_ABOUT);
					maResult.filters()
							.forEach(f -> tFilters.row(
									f.type(),
									f.name(),
									f.setup(),
									f.className()));
					return tFilters;
				});

		return Stream.concat(Stream.of(tAbout), oTFilters.stream()).toList();
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		result.getMediaAnalyserResult()
				.ifPresent(maResult -> {
					final var aboutMeasure = tableDocument.createTable("Filters").head(HEAD_ABOUT);
					maResult.filters()
							.forEach(f -> aboutMeasure.addRow()
									.addCell(f.type())
									.addCell(f.name())
									.addCell(f.setup())
									.addCell(f.className()));
				});

		final var t = tableDocument.createTable("About app").head(HEAD_APP_ABOUT);
		result.getVersions().entrySet().forEach(entry -> t.addRow().addCell(entry.getKey()).addCell(entry.getValue()));

		runnedJavaCmdLine.makeArchiveCommandline()
				.ifPresent(cmdLine -> t.addRow().addCell(ANALYSIS_CREATED_BY).addCell(cmdLine));
		t.addRow().addCell(REPORT_CREATED_BY).addCell(runnedJavaCmdLine.makeFullExtendedCommandline());
	}

	@Override
	public void addToReport(final DataResult result, final ReportDocument document) {
		result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::filters)
				.filter(not(Collection::isEmpty))
				.ifPresent(filters -> {
					final var section = new ReportSection(ABOUT, FFMPEG_FILTERS_USED_IN_THIS_MEASURE);
					filters.stream()
							.sorted((l, r) -> l.name().compareTo(r.name()))
							.map(filter -> new SimpleKeyValueReportEntry(filter.name(), filter.setup()))
							.forEach(section::add);
					document.add(section);
				});

		final var section = new ReportSection(ABOUT, COMMAND_LINES_USED_TO_CREATE_THIS_REPORT);
		runnedJavaCmdLine.makeArchiveCommandline()
				.ifPresent(cmdLine -> section.add(new KeyPreValueReportEntry(ANALYSIS_CREATED_BY, cmdLine)));
		section.add(new KeyPreValueReportEntry(REPORT_CREATED_BY,
				runnedJavaCmdLine.makeFullExtendedCommandline()));
		document.add(section);
	}

}
