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

import static java.awt.Color.BLUE;
import static java.awt.Color.CYAN;
import static java.awt.Color.ORANGE;
import static java.awt.Color.RED;
import static java.util.function.Predicate.not;
import static media.mexm.mediadeepa.exportformat.DataGraphic.THICK_STROKE;
import static media.mexm.mediadeepa.exportformat.DataGraphic.THIN_STROKE;
import static media.mexm.mediadeepa.exportformat.ReportSectionCategory.VIDEO;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ffmpeg.ffprobe.StreamType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.GraphicArtifact;
import media.mexm.mediadeepa.exportformat.NumericUnitValueReportEntry;
import media.mexm.mediadeepa.exportformat.RangeAxis;
import media.mexm.mediadeepa.exportformat.ReportDocument;
import media.mexm.mediadeepa.exportformat.ReportSection;
import media.mexm.mediadeepa.exportformat.SimpleKeyValueListReportEntry;
import media.mexm.mediadeepa.exportformat.TableDocument;
import media.mexm.mediadeepa.exportformat.TabularDocument;
import media.mexm.mediadeepa.exportformat.TabularExportFormat;
import media.mexm.mediadeepa.exportformat.TimedDataGraphic;
import media.mexm.mediadeepa.rendererengine.GraphicRendererEngine;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeVideoFrameConst;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMetadataFilterParser;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdValue;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserResult;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;

@Component
public class CropRendererEngine implements
								ReportRendererEngine,
								TableRendererEngine,
								TabularRendererEngine,
								GraphicRendererEngine,
								ConstStrings {

	@Autowired
	private AppConfig appConfig;
	@Autowired
	private NumberUtils numberUtils;

	public static final List<String> HEAD_CROP = List.of(FRAME, PTS, PTS_TIME,
			"x1", "x2", "y1", "y2", "w", "h", "x", "y");

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		return result.getMediaAnalyserResult()
				.map(maResult -> {
					final var lavfiMetadatas = maResult.lavfiMetadatas();
					final var crop = new TabularDocument(tabularExportFormat, "video-crop-detect").head(HEAD_CROP);
					lavfiMetadatas.getCropDetectReport().forEach(a -> {
						final var value = a.value();
						crop.row(a.frame(), a.pts(), a.ptsTime(),
								value.x1(), value.x2(), value.y1(), value.y2(),
								value.w(), value.h(), value.x(), value.y());
					});
					return crop;
				})
				.stream()
				.toList();
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		result.getMediaAnalyserResult()
				.ifPresent(maResult -> {
					final var crop = tableDocument.createTable("Crop detect").head(HEAD_CROP);
					maResult.lavfiMetadatas().getCropDetectReport().forEach(a -> {
						final var value = a.value();
						crop.addRow()
								.addCell(a.frame())
								.addCell(a.pts())
								.addCell(a.ptsTime())
								.addCell(value.x1()).addCell(value.x2()).addCell(value.y1()).addCell(value.y2())
								.addCell(value.w()).addCell(value.h()).addCell(value.x()).addCell(value.y());
					});
				});
	}

	@Override
	public List<GraphicArtifact> toGraphic(final DataResult result) {
		return result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getCropDetectReport)
				.filter(not(List::isEmpty))
				.stream()
				.map(cropReport -> {
					final var rangeAxis = RangeAxis.createFromRelativesValueSet("Pixels", 0,
							cropReport.stream()
									.map(LavfiMtdValue::value)
									.flatMap(d -> Stream.of(d.x1(), d.y1(), d.x2(), d.y2())));

					final var dataGraphic = TimedDataGraphic.create(
							cropReport.stream().map(LavfiMtdValue::ptsTime),
							rangeAxis);

					dataGraphic.addSeries(dataGraphic.new Series(
							"Crop X1",
							BLUE,
							THIN_STROKE,
							cropReport.stream().map(d -> d.value().x1())));
					dataGraphic.addSeries(dataGraphic.new Series(
							"Crop Y1",
							RED,
							THICK_STROKE,
							cropReport.stream().map(d -> d.value().y1())));
					dataGraphic.addSeries(dataGraphic.new Series(
							"Crop X2",
							CYAN,
							THIN_STROKE,
							cropReport.stream().map(d -> d.value().x2())));
					dataGraphic.addSeries(dataGraphic.new Series(
							"Crop Y2",
							ORANGE,
							THICK_STROKE,
							cropReport.stream().map(d -> d.value().y2())));

					result.getFFprobeResult()
							.flatMap(FFprobeJAXB::getFirstVideoStream)
							.map(StreamType::getHeight)
							.or(() -> result.getContainerAnalyserResult()
									.map(ContainerAnalyserResult::videoConst)
									.map(FFprobeVideoFrameConst::height))
							.ifPresent(dataGraphic::addValueMarker);
					result.getFFprobeResult()
							.flatMap(FFprobeJAXB::getFirstVideoStream)
							.map(StreamType::getWidth)
							.or(() -> result.getContainerAnalyserResult()
									.map(ContainerAnalyserResult::videoConst)
									.map(FFprobeVideoFrameConst::width))
							.ifPresent(dataGraphic::addValueMarker);

					return new GraphicArtifact(
							appConfig.getCropGraphicFilename(),
							dataGraphic.makeLinearAxisGraphic(numberUtils),
							appConfig.getImageSizeFullSize());
				})
				.toList();
	}

	@Override
	public void addToReport(final DataResult result, final ReportDocument document) {
		result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.flatMap(Optional::ofNullable)
				.map(LavfiMetadataFilterParser::getCropDetectReport)
				.filter(not(List::isEmpty))
				.ifPresent(cropDetectReport -> {
					final var section = new ReportSection(VIDEO, BLACK_BORDERS_CROP_DETECTION);
					final var cropEvents = cropDetectReport.stream()
							.map(LavfiMtdValue::value)
							.distinct()
							.toList();
					section.add(new NumericUnitValueReportEntry("Crop detection activity", cropEvents.size(), EVENT_S));
					section.add(new SimpleKeyValueListReportEntry("First crop values",
							cropEvents.stream()
									.limit(10)
									.map(l -> Stream.of(
											"x1: " + l.x1(),
											"x2: " + l.x2(),
											"y1: " + l.y1(),
											"y2: " + l.y2(),
											"X: " + l.x(),
											"Y: " + l.y(),
											"Width: " + l.w(),
											"Height: " + l.h()))
									.map(l -> l.collect(Collectors.joining(", ")))
									.toList()));
					document.add(section);
				});
	}

}
