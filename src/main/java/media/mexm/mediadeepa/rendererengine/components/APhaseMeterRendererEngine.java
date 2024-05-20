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
package media.mexm.mediadeepa.rendererengine.components;

import static java.util.function.Predicate.not;
import static media.mexm.mediadeepa.exportformat.DataGraphic.FULL_PINK;
import static media.mexm.mediadeepa.exportformat.DataGraphic.THIN_STROKE;
import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.AUDIO;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.GraphicArtifact;
import media.mexm.mediadeepa.exportformat.RangeAxis;
import media.mexm.mediadeepa.exportformat.TableDocument;
import media.mexm.mediadeepa.exportformat.TabularDocument;
import media.mexm.mediadeepa.exportformat.TabularExportFormat;
import media.mexm.mediadeepa.exportformat.TimedDataGraphic;
import media.mexm.mediadeepa.exportformat.report.ReportDocument;
import media.mexm.mediadeepa.exportformat.report.ReportSection;
import media.mexm.mediadeepa.exportformat.report.StatisticsUnitValueReportEntry;
import media.mexm.mediadeepa.rendererengine.GraphicRendererEngine;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.SingleGraphicDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.SingleTabularDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMetadataFilterParser;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdValue;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;

@Component
public class APhaseMeterRendererEngine implements
									   ReportRendererEngine,
									   TableRendererEngine,
									   TabularRendererEngine,
									   GraphicRendererEngine,
									   ConstStrings,
									   SingleTabularDocumentExporterTraits,
									   SingleGraphicDocumentExporterTraits {

	@Autowired
	private AppConfig appConfig;
	@Autowired
	private NumberUtils numberUtils;

	public static final List<String> HEAD_APHASE = List.of(FRAME, PTS, PTS_TIME, VALUE);

	@Override
	public String getSingleUniqTabularDocumentBaseFileName() {
		return "audio-phase-meter";
	}

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		return result.getMediaAnalyserResult()
				.map(maResult -> {
					final var lavfiMetadatas = maResult.lavfiMetadatas();
					final var aPhaseMeter = new TabularDocument(tabularExportFormat,
							getSingleUniqTabularDocumentBaseFileName()).head(HEAD_APHASE);
					lavfiMetadatas.getAPhaseMeterReport()
							.forEach(a -> aPhaseMeter.row(a.frame(), a.pts(), a.ptsTime(), a.value()));
					return aPhaseMeter;
				})
				.stream()
				.toList();
	}

	@Override
	public void addToTable(final DataResult result,
						   final TableDocument doc) {
		result.getMediaAnalyserResult()
				.ifPresent(maResult -> {
					final var aPhaseMeter = doc.createTable("Audio phase").head(HEAD_APHASE);
					final var lavfiMetadatas = maResult.lavfiMetadatas();
					lavfiMetadatas.getAPhaseMeterReport()
							.forEach(a -> aPhaseMeter.addRow()
									.addCell(a.frame())
									.addCell(a.pts())
									.addCell(a.ptsTime())
									.addCell(a.value()));
				});
	}

	@Override
	public void addToReport(final DataResult result, final ReportDocument document) {
		result.getMediaAnalyserResult()
				.ifPresent(maResult -> {
					final var lavfiMetadatas = maResult.lavfiMetadatas();
					final var aPhaseMeterReport = lavfiMetadatas.getAPhaseMeterReport();
					final var section = new ReportSection(AUDIO, PHASE_CORRELATION);
					if (aPhaseMeterReport.isEmpty() == false) {
						section.add(
								StatisticsUnitValueReportEntry.createFromLong(
										"Phase correlation (L/R)",
										aPhaseMeterReport.stream()
												.map(LavfiMtdValue::value)
												.map(v -> Math.round(v * 100d)), "%", numberUtils));
					}
					document.add(section);
				});
	}

	@Override
	public List<GraphicArtifact> toGraphic(final DataResult result) {
		return result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getAPhaseMeterReport)
				.filter(not(List::isEmpty))
				.map(aPhaseMeterReport -> {
					final var values = aPhaseMeterReport.stream()
							.map(LavfiMtdValue::value)
							.map(d -> d * 100f)
							.toList();

					final var dataGraphic = TimedDataGraphic.create(
							aPhaseMeterReport.stream().map(LavfiMtdValue::ptsTime),
							RangeAxis.createFromRelativesValueSet(
									"Phase (%)", 5,
									values.stream()));
					dataGraphic.addSeries(dataGraphic.new Series(
							PHASE_CORRELATION,
							FULL_PINK,
							THIN_STROKE,
							values.stream()));

					return new GraphicArtifact(
							getSingleUniqGraphicBaseFileName(),
							dataGraphic.makeLinearAxisGraphic(numberUtils),
							appConfig.getGraphicConfig().getImageSizeHalfSize());
				})
				.stream()
				.toList();
	}

	@Override
	public String getSingleUniqGraphicBaseFileName() {
		return appConfig.getGraphicConfig().getAPhaseGraphicFilename();
	}

}
