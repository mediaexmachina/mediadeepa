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

import static media.mexm.mediadeepa.exportformat.ReportEntrySubset.toEntrySubset;
import static media.mexm.mediadeepa.exportformat.ReportSectionCategory.VIDEO;
import static media.mexm.mediadeepa.exportformat.StatisticsUnitValueReportEntry.createFromFloat;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ReportDocument;
import media.mexm.mediadeepa.exportformat.ReportSection;
import media.mexm.mediadeepa.exportformat.TableDocument;
import media.mexm.mediadeepa.exportformat.TabularDocument;
import media.mexm.mediadeepa.exportformat.TabularExportFormat;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.SingleTabularDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdSiti;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdValue;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;

@Component
public class SITIReportRendererEngine implements
									  TableRendererEngine,
									  TabularRendererEngine,
									  ReportRendererEngine,
									  ConstStrings,
									  SingleTabularDocumentExporterTraits {

	@Autowired
	private NumberUtils numberUtils;

	public static final List<String> HEAD_SITI_REPORT = List.of(TYPE, AVERAGE, MAX, MIN);

	@Override
	public String getSingleUniqTabularDocumentBaseFileName() {
		return "video-siti-stats-ITU-T_P-910";
	}

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		return result.getMediaAnalyserResult()
				.map(maResult -> {
					final var lavfiMetadatas = maResult.lavfiMetadatas();
					final var sitiStats = new TabularDocument(tabularExportFormat,
							getSingleUniqTabularDocumentBaseFileName())
									.head(HEAD_SITI_REPORT);
					if (lavfiMetadatas.getSitiReport().isEmpty() == false) {
						final var stats = lavfiMetadatas.computeSitiStats();
						sitiStats
								.row(SPATIAL_INFO,
										stats.si().getAverage(),
										stats.si().getMax(),
										stats.si().getMin())
								.row(TEMPORAL_INFO,
										stats.ti().getAverage(),
										stats.ti().getMax(),
										stats.ti().getMin());
					}
					return sitiStats;
				})
				.stream()
				.toList();
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		result.getMediaAnalyserResult()
				.ifPresent(maResult -> {
					final var lavfiMetadatas = maResult.lavfiMetadatas();
					if (lavfiMetadatas.getSitiReport().isEmpty()) {
						return;
					}
					final var sitiStats = tableDocument.createTable("SITI Stats").head(HEAD_SITI_REPORT);
					final var stats = lavfiMetadatas.computeSitiStats();
					sitiStats
							.addRow()
							.addCell(SPATIAL_INFO)
							.addCell(stats.si().getAverage())
							.addCell(stats.si().getMax())
							.addCell(stats.si().getMin());
					sitiStats.addRow()
							.addCell(TEMPORAL_INFO)
							.addCell(stats.ti().getAverage())
							.addCell(stats.ti().getMax())
							.addCell(stats.ti().getMin());
				});
	}

	@Override
	public void addToReport(final DataResult result, final ReportDocument document) {
		result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.ifPresent(lavfiMetadatas -> {
					final var sitiReport = lavfiMetadatas.getSitiReport();
					final var section = new ReportSection(VIDEO, IMAGE_AND_MOTION_COMPLEXITY);
					toEntrySubset(Stream.of(
							createFromFloat(
									SPATIAL_INFORMATION,
									sitiReport.stream()
											.map(LavfiMtdValue::value)
											.map(LavfiMtdSiti::si),
									"", numberUtils::formatDecimalFull1En),
							createFromFloat(
									TEMPORAL_INFORMATION,
									sitiReport.stream()
											.map(LavfiMtdValue::value)
											.map(LavfiMtdSiti::ti),
									"", numberUtils::formatDecimalFull1En)), section);
					document.add(section);
				});
	}
}
