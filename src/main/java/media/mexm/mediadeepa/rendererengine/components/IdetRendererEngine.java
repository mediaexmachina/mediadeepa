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
import static java.awt.Color.GRAY;
import static java.awt.Color.RED;
import static java.util.function.Predicate.not;
import static media.mexm.mediadeepa.exportformat.DataGraphic.THICK_STROKE;
import static media.mexm.mediadeepa.exportformat.DataGraphic.THIN_STROKE;
import static media.mexm.mediadeepa.exportformat.ReportSectionCategory.VIDEO;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdetRepeatedFrameType.BOTTOM;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdetRepeatedFrameType.NEITHER;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdetRepeatedFrameType.TOP;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdetSingleFrameType.BFF;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdetSingleFrameType.PROGRESSIVE;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdetSingleFrameType.TFF;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdetSingleFrameType.UNDETERMINED;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import media.mexm.mediadeepa.exportformat.TableDocument;
import media.mexm.mediadeepa.exportformat.TabularDocument;
import media.mexm.mediadeepa.exportformat.TabularExportFormat;
import media.mexm.mediadeepa.exportformat.TimedDataGraphic;
import media.mexm.mediadeepa.rendererengine.GraphicRendererEngine;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMetadataFilterParser;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdValue;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;

@Component
public class IdetRendererEngine implements
								ReportRendererEngine,
								TableRendererEngine,
								TabularRendererEngine,
								GraphicRendererEngine,
								ConstStrings {

	@Autowired
	private AppConfig appConfig;
	@Autowired
	private NumberUtils numberUtils;

	public static final List<String> HEAD_IDET = List.of(FRAME, PTS, PTS_TIME,
			SINGLE_TOP_FIELD_FIRST,
			SINGLE_BOTTOM_FIELD_FIRST,
			SINGLE_CURRENT_FRAME,
			SINGLE_PROGRESSIVE,
			SINGLE_UNDETERMINED,
			MULTIPLE_TOP_FIELD_FIRST,
			MULTIPLE_BOTTOM_FIELD_FIRST,
			MULTIPLE_CURRENT_FRAME,
			MULTIPLE_PROGRESSIVE,
			MULTIPLE_UNDETERMINED,
			REPEATED_CURRENT_FRAME,
			REPEATED_TOP,
			REPEATED_BOTTOM,
			REPEATED_NEITHER);

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		return result.getMediaAnalyserResult()
				.map(maResult -> {
					final var lavfiMetadatas = maResult.lavfiMetadatas();
					final var idet = new TabularDocument(tabularExportFormat, "video-interlace-detect").head(HEAD_IDET);
					lavfiMetadatas.getIdetReport()
							.forEach(a -> {
								final var value = a.value();
								final var single = value.single();
								final var multiple = value.multiple();
								final var repeated = value.repeated();

								idet.row(a.frame(), a.pts(), a.ptsTime(),
										single.tff(),
										single.bff(),
										single.currentFrame(),
										single.progressive(),
										single.undetermined(),

										multiple.tff(),
										multiple.bff(),
										multiple.currentFrame(),
										multiple.progressive(),
										multiple.undetermined(),

										repeated.currentFrame(),
										repeated.top(),
										repeated.bottom(),
										repeated.neither());
							});
					return idet;
				})
				.stream()
				.toList();
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		result.getMediaAnalyserResult()
				.ifPresent(maResult -> {
					final var idet = tableDocument.createTable("Interlace detect").head(HEAD_IDET);
					maResult.lavfiMetadatas().getIdetReport()
							.forEach(a -> {
								final var value = a.value();
								final var single = value.single();
								final var multiple = value.multiple();
								final var repeated = value.repeated();

								idet.addRow()
										.addCell(a.frame())
										.addCell(a.pts())
										.addCell(a.ptsTime())
										.addCell(single.tff())
										.addCell(single.bff())
										.addOptionalToString(single.currentFrame())
										.addCell(single.progressive())
										.addCell(single.undetermined())

										.addCell(multiple.tff())
										.addCell(multiple.bff())
										.addOptionalToString(multiple.currentFrame())
										.addCell(multiple.progressive())
										.addCell(multiple.undetermined())

										.addOptionalToString(repeated.currentFrame())
										.addCell(repeated.top())
										.addCell(repeated.bottom())
										.addCell(repeated.neither());
							});
				});
	}

	@Override
	public List<GraphicArtifact> toGraphic(final DataResult result) {
		return result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getIdetReport)
				.filter(not(List::isEmpty))
				.stream()
				.map(idetReport -> {
					final var singleDetectedTypeMap = Map.of(
							UNDETERMINED, -1,
							PROGRESSIVE, 0,
							TFF, 1,
							BFF, 2);
					final var repeatedDetectedTypeMap = Map.of(
							NEITHER, 0,
							TOP, 1,
							BOTTOM, 2);

					final var dataGraphic = TimedDataGraphic.create(
							idetReport.stream().map(LavfiMtdValue::ptsTime),
							new RangeAxis("Interlace detection", -1, 4));

					dataGraphic.addSeries(dataGraphic.new Series(
							"Single interlace",
							BLUE.brighter(),
							THIN_STROKE,
							idetReport.stream().map(d -> singleDetectedTypeMap.get(d.value().single()
									.currentFrame()))));
					dataGraphic.addSeries(dataGraphic.new Series(
							"Multiple interlace",
							RED.brighter(),
							THICK_STROKE,
							idetReport.stream().map(d -> singleDetectedTypeMap.get(d.value().multiple()
									.currentFrame()))));
					dataGraphic.addSeries(dataGraphic.new Series(
							"Repeated interlace",
							GRAY.darker(),
							THICK_STROKE,
							idetReport.stream().map(d -> repeatedDetectedTypeMap.get(d.value().repeated()
									.currentFrame()))));

					return new GraphicArtifact(
							appConfig.getGraphicConfig().getItetGraphicFilename(),
							dataGraphic.makeLinearAxisGraphic(numberUtils),
							appConfig.getGraphicConfig().getImageSizeHalfSize());
				})
				.toList();
	}

	@Override
	public void addToReport(final DataResult result, final ReportDocument document) {
		result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.flatMap(Optional::ofNullable)
				.map(LavfiMetadataFilterParser::getIdetReport)
				.filter(not(List::isEmpty))
				.ifPresent(idetReport -> {
					final var section = new ReportSection(VIDEO, INTERLACING_DETECTION);

					final var frameCount = idetReport.size();
					final var lastIdet = idetReport.get(frameCount - 1).value();

					addIdetStatus(section, "Detected as progressive",
							lastIdet.single().progressive(), frameCount);
					addIdetStatus(section, "Detected as progressive, using multiple-frame detection",
							lastIdet.multiple().progressive(), frameCount);

					addIdetStatus(section, "Detected as top field first",
							lastIdet.single().tff(), frameCount);
					addIdetStatus(section, "Detected as top field first, using multiple-frame detection",
							lastIdet.multiple().tff(), frameCount);
					if (lastIdet.repeated().neither() != frameCount) {
						addIdetStatus(section, "With the top field repeated from the previous frame’s top field",
								lastIdet.repeated().top(), frameCount);
					}

					addIdetStatus(section, "Detected as bottom field first",
							lastIdet.single().bff(), frameCount);
					addIdetStatus(section, "Detected as bottom field first, using multiple-frame detection",
							lastIdet.multiple().bff(), frameCount);
					if (lastIdet.repeated().neither() != frameCount) {
						addIdetStatus(section, "With the bottom field repeated from the previous frame’s bottom field",
								lastIdet.repeated().bottom(), frameCount);
					}

					addIdetStatus(section, "Could not be classified using single-frame detection",
							lastIdet.single().undetermined(), frameCount);
					addIdetStatus(section, "Could not be classified using multiple-frame detection",
							lastIdet.multiple().undetermined(), frameCount);
					if (lastIdet.repeated().neither() != frameCount) {
						addIdetStatus(section, "No repeated field",
								lastIdet.repeated().neither(), frameCount);
					}
					document.add(section);
				});
	}

	private void addIdetStatus(final ReportSection section, final String name, final int value, final int frameCount) {
		if (value == 0) {
			return;
		}
		section.add(new NumericUnitValueReportEntry(
				name,
				Math.round(value * 100f / frameCount),
				"%"));
	}

}
