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

import static media.mexm.mediadeepa.components.CLIRunner.makeOutputFileName;
import static media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.FULL_PINK;
import static media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.IMAGE_SIZE_HALF_HEIGHT;
import static media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.THIN_STROKE;

import java.io.File;
import java.util.List;

import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormat;
import media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.RangeAxis;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMetadataFilterParser;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdValue;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;

public class APhaseGraphicExportFormat implements ExportFormat {

	public static final String SUFFIX_FILE_NAME = "audio-phase.jpg";

	@Override
	public String getFormatLongName() {
		return "Audio phase graphic";
	}

	@Override
	public void exportResult(final DataResult result, final File exportDirectory, final String baseFileName) {
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

		dataGraphic.makeGraphic(
				new File(exportDirectory, makeOutputFileName(baseFileName, SUFFIX_FILE_NAME)),
				IMAGE_SIZE_HALF_HEIGHT,
				false);
	}
}
