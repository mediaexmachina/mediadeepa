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
package media.mexm.mediadeepa.exportformat.graphic;

import static java.awt.Color.BLUE;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static media.mexm.mediadeepa.components.CLIRunner.makeOutputFileName;
import static media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.FULL_PINK;
import static media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.IMAGE_SIZE_FULL_HEIGHT;
import static media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.IMAGE_SIZE_HALF_HEIGHT;
import static media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.THICK_STROKE;
import static media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.THIN_STROKE;

import java.awt.Color;
import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormat;
import media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.RangeAxis;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMetadataFilterParser;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdValue;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.Ebur128Summary;
import tv.hd3g.fflauncher.resultparser.Stereo;

public class GraphicExportFormat implements ExportFormat {

	public static final String SUFFIX_LUFS_FILE_NAME = "lufs-events.jpg";// TODO RENAME
	public static final String SUFFIX_TPK_FILE_NAME = "lufs-tpk-events.jpg";// TODO RENAME
	public static final String A_PHASE_SUFFIX_FILE_NAME = "audio-phase.jpg";

	@Override
	public String getFormatLongName() {
		return "Graphical representation of data";
	}

	@Override
	public void exportResult(final DataResult result, final File exportDirectory, final String baseFileName) {
		makeR128(result, exportDirectory, baseFileName);
		makeAPhase(result, exportDirectory, baseFileName);
	}

	private void makeR128(final DataResult result, final File exportDirectory, final String baseFileName) {
		final var r128events = result.getEbur128events();
		if (r128events.isEmpty()) {
			return;
		}

		final var dataGraphicLUFS = new TimedDataGraphic(
				r128events.stream()
						.map(Ebur128StrErrFilterEvent::getT)
						.map(t -> t * 1000f)
						.map(Math::ceil)
						.map(Math::round),
				RangeAxis.createFromValueSet("dB LU", -40, 10, 1,
						r128events.stream()
								.flatMap(event -> Stream.of(
										event.getI(),
										event.getM(),
										event.getS(),
										event.getTarget()))));

		dataGraphicLUFS.addSeries(dataGraphicLUFS.new Series(
				"Integrated",
				BLUE,
				THICK_STROKE,
				r128events.stream().map(Ebur128StrErrFilterEvent::getI)));
		dataGraphicLUFS.addSeries(dataGraphicLUFS.new Series(
				"Short term",
				GREEN.darker(),
				THIN_STROKE,
				r128events.stream().map(Ebur128StrErrFilterEvent::getS)));
		dataGraphicLUFS.addSeries(dataGraphicLUFS.new Series(
				"Momentary",
				Color.getHSBColor(0.5f, 1f, 0.3f),
				THIN_STROKE,
				r128events.stream().map(Ebur128StrErrFilterEvent::getM)));

		final var oEbur128Sum = result.getMediaAnalyserResult().map(MediaAnalyserResult::ebur128Summary);
		dataGraphicLUFS
				.addValueMarker(oEbur128Sum.map(Ebur128Summary::getIntegrated))
				.addValueMarker(oEbur128Sum.map(Ebur128Summary::getLoudnessRangeHigh))
				.addValueMarker(oEbur128Sum.map(Ebur128Summary::getLoudnessRangeLow))
				.addValueMarker(r128events.get(r128events.size() - 1).getI());

		dataGraphicLUFS.makeLogarithmicAxisGraphic(
				new File(exportDirectory, makeOutputFileName(baseFileName, SUFFIX_LUFS_FILE_NAME)),
				IMAGE_SIZE_FULL_HEIGHT);

		final var ra = RangeAxis.createFromValueSet("dB LU",
				oEbur128Sum.map(Ebur128Summary::getIntegrated).orElse(-23f), 10, 1,
				r128events.stream()
						.flatMap(event -> Stream.of(
								event.getFtpk().left(),
								event.getFtpk().right(),
								event.getSpk().left(),
								event.getSpk().right())));
		final var dataGraphicTPK = dataGraphicLUFS.cloneWithSamePositions(ra);

		dataGraphicTPK.addSeries(dataGraphicLUFS.new Series(
				"True peak right (per frame)", RED, THIN_STROKE,
				r128events.stream().map(Ebur128StrErrFilterEvent::getFtpk).map(Stereo::right)));
		dataGraphicTPK.addSeries(dataGraphicLUFS.new Series(
				"True peak left (per frame)", BLUE, THICK_STROKE,
				r128events.stream().map(Ebur128StrErrFilterEvent::getFtpk).map(Stereo::left)));
		dataGraphicTPK
				.addValueMarker(oEbur128Sum.map(Ebur128Summary::getTruePeak))
				.addValueMarker(oEbur128Sum.map(Ebur128Summary::getSamplePeak))
				.addValueMarker(-3)
				.addValueMarker(r128events.stream()
						.flatMap(event -> Stream.of(
								event.getFtpk().left(),
								event.getFtpk().right(),
								event.getSpk().left(),
								event.getSpk().right()))
						.mapToDouble(Float::doubleValue)
						.summaryStatistics().getAverage());

		dataGraphicTPK.makeLogarithmicAxisGraphic(
				new File(exportDirectory, makeOutputFileName(baseFileName, SUFFIX_TPK_FILE_NAME)),
				IMAGE_SIZE_FULL_HEIGHT);
	}

	private void makeAPhase(final DataResult result, final File exportDirectory, final String baseFileName) {
		final var aPhaseMeterReport = result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getAPhaseMeterReport)
				.orElse(List.of());
		if (aPhaseMeterReport.isEmpty()
			|| aPhaseMeterReport.stream().allMatch(r -> r.value() == 0f)) {
			return;
		}

		final var dataGraphic = TimedDataGraphic.create(
				aPhaseMeterReport.stream().map(LavfiMtdValue::ptsTime),
				new RangeAxis("Phase (°)", -190, 190));

		dataGraphic.addSeries(dataGraphic.new Series(
				"Audio stereo relative phase left/right",
				FULL_PINK,
				THIN_STROKE,
				aPhaseMeterReport.stream().map(d -> d.value() * 180f)));
		dataGraphic.addValueMarker(180);
		dataGraphic.addValueMarker(-180);
		dataGraphic.addValueMarker(90);
		dataGraphic.addValueMarker(-90);

		dataGraphic.makeLinearAxisGraphic(
				new File(exportDirectory, makeOutputFileName(baseFileName, A_PHASE_SUFFIX_FILE_NAME)),
				IMAGE_SIZE_HALF_HEIGHT);
	}
}
