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

import static java.awt.Color.GREEN;
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
import tv.hd3g.fflauncher.recipes.MediaAnalyserProcessResult;

@Component
public class BlockRendererEngine implements
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

	public static final List<String> HEAD_BLOCK = List.of(FRAME, PTS, PTS_TIME, VALUE);

	@Override
	public String getSingleUniqTabularDocumentBaseFileName() {
		return "video-block-detect";
	}

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		return result.getMediaAnalyserProcessResult()
				.map(maResult -> {
					final var lavfiMetadatas = maResult.lavfiMetadatas();
					final var block = new TabularDocument(tabularExportFormat,
							getSingleUniqTabularDocumentBaseFileName()).head(HEAD_BLOCK);
					lavfiMetadatas.getBlockDetectReport()
							.forEach(a -> block.row(a.frame(), a.pts(), a.ptsTime(), a.value()));
					return block;
				})
				.stream()
				.toList();
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		result.getMediaAnalyserProcessResult()
				.ifPresent(maResult -> {
					final var block = tableDocument.createTable("Block detect").head(HEAD_BLOCK);
					maResult.lavfiMetadatas().getBlockDetectReport()
							.forEach(a -> block.addRow()
									.addCell(a.frame())
									.addCell(a.pts())
									.addCell(a.ptsTime())
									.addCell(a.value()));
				});
	}

	@Override
	public List<GraphicArtifact> toGraphic(final DataResult result) {
		return result.getMediaAnalyserProcessResult()
				.map(MediaAnalyserProcessResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getBlockDetectReport)
				.filter(not(List::isEmpty))
				.stream()
				.map(blockReport -> {
					final var dataGraphic = TimedDataGraphic.create(
							blockReport.stream().map(LavfiMtdValue::ptsTime),
							RangeAxis.createFromRelativesValueSet(
									"Block", 10,
									blockReport.stream().map(LavfiMtdValue::value)));

					dataGraphic.addSeries(dataGraphic.new Series(
							"Block detection",
							GREEN,
							THIN_STROKE,
							blockReport.stream().map(LavfiMtdValue::value)));

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
		return appConfig.getGraphicConfig().getBlockGraphicFilename();
	}

	@Override
	public void addToReport(final DataResult result, final ReportDocument document) {
		result.getMediaAnalyserProcessResult()
				.map(MediaAnalyserProcessResult::lavfiMetadatas)
				.ifPresent(lavfiMetadatas -> {
					final var blockDetectReport = lavfiMetadatas.getBlockDetectReport();
					final var section = new ReportSection(VIDEO, IMAGE_COMPRESSION_ARTIFACT_DETECTION);
					section.add(
							createFromFloat(
									"Blockiness detection",
									blockDetectReport.stream()
											.map(LavfiMtdValue::value),
									"", numberUtils::formatDecimalFull1En));

					addAllGraphicsToReport(result, section, appConfig, appCommand);
					document.add(section);
				});
	}

}
