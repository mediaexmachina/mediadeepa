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

import static java.awt.Color.ORANGE;
import static java.util.function.Predicate.not;
import static media.mexm.mediadeepa.exportformat.DataGraphic.THIN_STROKE;
import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.VIDEO;
import static media.mexm.mediadeepa.exportformat.report.StatisticsUnitValueReportEntry.createFromFloat;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.cli.AppCommand;
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
public class BlurRendererEngine implements
								TableRendererEngine,
								TabularRendererEngine,
								GraphicRendererEngine,
								ReportRendererEngine,
								ConstStrings,
								SingleTabularDocumentExporterTraits,
								SingleGraphicDocumentExporterTraits {

	@Autowired
	private AppConfig appConfig;
	@Autowired
	private AppCommand appCommand;
	@Autowired
	private NumberUtils numberUtils;

	public static final List<String> HEAD_BLUR = List.of(FRAME, PTS, PTS_TIME, VALUE);

	@Override
	public String getSingleUniqTabularDocumentBaseFileName() {
		return "video-blur-detect";
	}

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		return result.getMediaAnalyserResult()
				.map(maResult -> {
					final var lavfiMetadatas = maResult.lavfiMetadatas();
					final var blur = new TabularDocument(tabularExportFormat,
							getSingleUniqTabularDocumentBaseFileName()).head(HEAD_BLUR);
					lavfiMetadatas.getBlurDetectReport()
							.forEach(a -> blur.row(a.frame(), a.pts(), a.ptsTime(), a.value()));
					return blur;
				})
				.stream()
				.toList();
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		result.getMediaAnalyserResult()
				.ifPresent(maResult -> {
					final var blur = tableDocument.createTable("Blur detect").head(HEAD_BLUR);
					maResult.lavfiMetadatas().getBlurDetectReport()
							.forEach(a -> blur.addRow()
									.addCell(a.frame())
									.addCell(a.pts())
									.addCell(a.ptsTime())
									.addCell(a.value()));
				});
	}

	@Override
	public List<GraphicArtifact> toGraphic(final DataResult result) {
		return result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getBlurDetectReport)
				.filter(not(List::isEmpty))
				.stream()
				.map(blurReport -> {
					final var dataGraphic = TimedDataGraphic.create(
							blurReport.stream().map(LavfiMtdValue::ptsTime),
							RangeAxis.createFromRelativesValueSet(
									"Blur", 10,
									blurReport.stream().map(LavfiMtdValue::value)));

					dataGraphic.addSeries(dataGraphic.new Series(
							"Blur detection",
							ORANGE,
							THIN_STROKE,
							blurReport.stream().map(LavfiMtdValue::value)));

					return new GraphicArtifact(
							getSingleUniqGraphicBaseFileName(),
							dataGraphic.addMinMaxValueMarkers()
									.makeLinearAxisGraphic(numberUtils),
							appConfig.getGraphicConfig().getImageSizeFullSize());
				})
				.toList();
	}

	@Override
	public String getSingleUniqGraphicBaseFileName() {
		return appConfig.getGraphicConfig().getBlurGraphicFilename();
	}

	@Override
	public void addToReport(final DataResult result, final ReportDocument document) {
		result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.ifPresent(lavfiMetadatas -> {
					final var blurDetectReport = lavfiMetadatas.getBlurDetectReport();
					final var section = new ReportSection(VIDEO, IMAGE_BLUR_DETECTION);
					section.add(
							createFromFloat(
									"Blurriness detection",
									blurDetectReport.stream()
											.map(LavfiMtdValue::value),
									"", numberUtils::formatDecimalFull1En));

					addAllGraphicsToReport(this, result, section, appConfig, appCommand);
					document.add(section);
				});
	}

}
