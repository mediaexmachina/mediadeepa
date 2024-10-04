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
import static media.mexm.mediadeepa.exportformat.DataGraphic.FULL_PINK;
import static media.mexm.mediadeepa.exportformat.DataGraphic.THICK_STROKE;
import static media.mexm.mediadeepa.exportformat.DataGraphic.THIN_STROKE;
import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.VIDEO;

import java.awt.Color;
import java.util.List;
import java.util.stream.Stream;

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
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdSiti;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdValue;
import tv.hd3g.fflauncher.recipes.MediaAnalyserProcessResult;

@Component
public class SITIRendererEngine implements
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

	public static final List<String> HEAD_SITI = List.of(FRAME, PTS, PTS_TIME, SPATIAL_INFO, TEMPORAL_INFO);

	@Override
	public String getSingleUniqTabularDocumentBaseFileName() {
		return "video-siti-ITU-T_P-910";
	}

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		return result.getMediaAnalyserProcessResult()
				.map(maResult -> {
					final var lavfiMetadatas = maResult.lavfiMetadatas();
					final var siti = new TabularDocument(tabularExportFormat,
							getSingleUniqTabularDocumentBaseFileName()).head(
									HEAD_SITI);
					lavfiMetadatas.getSitiReport()
							.forEach(a -> siti.row(a.frame(), a.pts(), a.ptsTime(), a.value().si(), a.value().ti()));
					return siti;
				})
				.stream()
				.toList();
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		result.getMediaAnalyserProcessResult()
				.ifPresent(maResult -> {
					final var siti = tableDocument.createTable("SITI").head(HEAD_SITI);
					final var lavfiMetadatas = maResult.lavfiMetadatas();
					lavfiMetadatas.getSitiReport()
							.forEach(a -> siti.addRow()
									.addCell(a.frame())
									.addCell(a.pts())
									.addCell(a.ptsTime())
									.addCell(a.value().si())
									.addCell(a.value().ti()));
				});
	}

	@Override
	public List<GraphicArtifact> toGraphic(final DataResult result) {
		return result.getMediaAnalyserProcessResult()
				.map(MediaAnalyserProcessResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getSitiReport)
				.filter(not(List::isEmpty))
				.map(sitiReport -> {
					final var dataGraphic = TimedDataGraphic.create(
							sitiReport.stream().map(LavfiMtdValue::ptsTime),
							RangeAxis.createFromRelativesValueSet(
									SPATIAL_TEMPORAL_INFORMATION, 5,
									sitiReport.stream()
											.map(LavfiMtdValue::value)
											.flatMap(s -> Stream.of(s.si(), s.ti()))));

					dataGraphic.addSeries(dataGraphic.new Series(
							SPATIAL_INFORMATION,
							FULL_PINK,
							THIN_STROKE,
							sitiReport.stream()
									.map(LavfiMtdValue::value)
									.map(LavfiMtdSiti::si)));
					dataGraphic.addSeries(dataGraphic.new Series(
							TEMPORAL_INFORMATION,
							Color.YELLOW,
							THICK_STROKE,
							sitiReport.stream()
									.map(LavfiMtdValue::value)
									.map(LavfiMtdSiti::ti)));

					return new GraphicArtifact(
							getSingleUniqGraphicBaseFileName(),
							dataGraphic.addMinMaxValueMarkers().makeLinearAxisGraphic(numberUtils),
							appConfig.getGraphicConfig().getImageSizeFullSize());
				}).stream()
				.toList();
	}

	@Override
	public String getSingleUniqGraphicBaseFileName() {
		return appConfig.getGraphicConfig().getSitiGraphicFilename();
	}

	@Override
	public void addToReport(final DataResult result, final ReportDocument document) {
		final var section = new ReportSection(VIDEO, SPATIAL_TEMPORAL_INFORMATION);
		addAllGraphicsToReport(result, section, appConfig, appCommand);
		document.add(section);
	}

}
