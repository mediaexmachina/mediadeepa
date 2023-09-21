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

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;
import static java.awt.Color.BLUE;
import static java.awt.Color.ORANGE;
import static java.awt.Color.RED;
import static media.mexm.mediadeepa.components.CLIRunner.makeOutputFileName;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.util.stream.Stream;

import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormat;
import media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.RangeAxis;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.Ebur128Summary;
import tv.hd3g.fflauncher.resultparser.Stereo;

public class LoudnessGraphicExportFormat implements ExportFormat {

	public static final String SUFFIX_LUFS_FILE_NAME = "lufs-events.jpg";
	public static final String SUFFIX_TPK_FILE_NAME = "lufs-tpk-events.jpg";

	@Override
	public String getFormatLongName() {
		return "Loudness graphic";
	}

	@Override
	public void exportResult(final DataResult result, final File exportDirectory, final String baseFileName) {
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

		final var thinStroke = new BasicStroke(3, CAP_BUTT, JOIN_MITER);
		final var thickStroke = new BasicStroke(6, CAP_BUTT, JOIN_MITER);

		dataGraphicLUFS.addSeries(dataGraphicLUFS.new Series(
				"Integrated",
				RED,
				thickStroke,
				r128events.stream().map(Ebur128StrErrFilterEvent::getI)));
		dataGraphicLUFS.addSeries(dataGraphicLUFS.new Series(
				"Short term",
				BLUE,
				thinStroke,
				r128events.stream().map(Ebur128StrErrFilterEvent::getS)));
		dataGraphicLUFS.addSeries(dataGraphicLUFS.new Series(
				"Momentary",
				Color.getHSBColor(0.5f, 1f, 0.3f),
				thinStroke,
				r128events.stream().map(Ebur128StrErrFilterEvent::getM)));

		final var oEbur128Sum = result.getMediaAnalyserResult().map(MediaAnalyserResult::ebur128Summary);
		dataGraphicLUFS
				.addValueMarker(oEbur128Sum.map(Ebur128Summary::getIntegrated))
				.addValueMarker(oEbur128Sum.map(Ebur128Summary::getLoudnessRangeHigh))
				.addValueMarker(oEbur128Sum.map(Ebur128Summary::getLoudnessRangeLow))
				.addValueMarker(r128events.get(r128events.size() - 1).getI());

		dataGraphicLUFS.makeGraphic(
				new File(exportDirectory, makeOutputFileName(baseFileName, SUFFIX_LUFS_FILE_NAME)),
				new Point(2000, 1200));

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
				"True peak left (per frame)", BLUE, thinStroke,
				r128events.stream().map(Ebur128StrErrFilterEvent::getFtpk).map(Stereo::left)));
		dataGraphicTPK.addSeries(dataGraphicLUFS.new Series(
				"True peak right (per frame)", RED, thinStroke,
				r128events.stream().map(Ebur128StrErrFilterEvent::getFtpk).map(Stereo::right)));
		dataGraphicTPK.addSeries(dataGraphicLUFS.new Series(
				"Sample peak left", new Color(0, 128, 255), thickStroke,
				r128events.stream().map(Ebur128StrErrFilterEvent::getSpk).map(Stereo::left)));
		dataGraphicTPK.addSeries(dataGraphicLUFS.new Series(
				"Sample peak right", ORANGE, thickStroke,
				r128events.stream().map(Ebur128StrErrFilterEvent::getSpk).map(Stereo::right)));
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

		dataGraphicTPK.makeGraphic(
				new File(exportDirectory, makeOutputFileName(baseFileName, SUFFIX_TPK_FILE_NAME)),
				new Point(2000, 1200));

	}

}
