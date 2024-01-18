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
import static java.awt.Color.RED;
import static java.util.function.Predicate.not;
import static media.mexm.mediadeepa.exportformat.DataGraphic.THICK_STROKE;
import static media.mexm.mediadeepa.exportformat.DataGraphic.THIN_STROKE;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.GraphicArtifact;
import media.mexm.mediadeepa.exportformat.RangeAxis;
import media.mexm.mediadeepa.exportformat.SeriesStyle;
import media.mexm.mediadeepa.exportformat.XYLineChartDataGraphic;
import media.mexm.mediadeepa.rendererengine.GraphicRendererEngine;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeBaseFrame;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeVideoFrame;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserResult;

@Component
public class FramesDurationRendererEngine implements
										  GraphicRendererEngine,
										  ConstStrings {
	@Autowired
	private AppConfig appConfig;
	@Autowired
	private NumberUtils numberUtils;

	@Override
	public List<GraphicArtifact> toGraphic(final DataResult result) {
		final var videoFramesReport = result.getContainerAnalyserResult()
				.map(ContainerAnalyserResult::videoFrames)
				.stream()
				.flatMap(List::stream)
				.toList();
		if (videoFramesReport.isEmpty()) {
			return List.of();
		}
		final var firstStreamIndex = videoFramesReport.stream()
				.map(FFprobeVideoFrame::frame)
				.map(FFprobeBaseFrame::streamIndex)
				.findFirst().orElseThrow(() -> new IllegalArgumentException("Can't found video stream index"));
		final var allFrames = videoFramesReport.stream()
				.map(FFprobeVideoFrame::frame)
				.filter(f -> f.streamIndex() == firstStreamIndex)
				.toList();

		final var pktDtsTimeDerivative = numberUtils.getTimeDerivative(allFrames.stream()
				.map(FFprobeBaseFrame::pktDtsTime)
				.map(ms -> ms > -1f ? ms : Float.NaN)
				.map(numberUtils::secToMs),
				allFrames.size());
		final var bestEffortTimestampTimeDerivative = numberUtils.getTimeDerivative(allFrames.stream()
				.map(FFprobeBaseFrame::bestEffortTimestampTime)
				.map(ms -> ms > -1f ? ms : Float.NaN)
				.map(numberUtils::secToMs),
				allFrames.size());

		final var stats = Stream.concat(
				Arrays.stream(pktDtsTimeDerivative).boxed(),
				Arrays.stream(bestEffortTimestampTimeDerivative).boxed())
				.filter(not(d -> Double.isNaN(d)))
				.filter(v -> v > 0d)
				.mapToDouble(v -> v)
				.summaryStatistics();
		final var rangeValue = stats.getMax() - stats.getMin();
		var minRange = 0d;
		var maxRange = stats.getMax() + stats.getMax() / 2d;
		if (rangeValue < 0.1d) {
			minRange = stats.getMin() - rangeValue * 0.1d;
			maxRange = stats.getMax() + rangeValue * 0.1d;
		}

		final var dataGraphic = new XYLineChartDataGraphic(
				new RangeAxis("Frame duration (milliseconds)", minRange, maxRange),
				allFrames.size());

		dataGraphic.addSeries(new SeriesStyle("DTS video frame duration", BLUE, THIN_STROKE),
				pktDtsTimeDerivative);
		dataGraphic.addSeries(new SeriesStyle("Best effort video frame duration", RED, THICK_STROKE),
				bestEffortTimestampTimeDerivative);

		return List.of(
				new GraphicArtifact(
						appConfig.getGraphicConfig().getVFrameDurationGraphicFilename(),
						dataGraphic.makeLinearAxisGraphic(numberUtils),
						appConfig.getGraphicConfig().getImageSizeHalfSize()));
	}

}
