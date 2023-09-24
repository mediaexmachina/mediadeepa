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
import static java.awt.Color.GRAY;
import static java.awt.Color.GREEN;
import static java.awt.Color.ORANGE;
import static java.awt.Color.RED;
import static media.mexm.mediadeepa.components.CLIRunner.makeOutputFileName;
import static media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.COLORS_CHANNEL;
import static media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.FULL_PINK;
import static media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.IMAGE_SIZE_FULL_HEIGHT;
import static media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.IMAGE_SIZE_HALF_HEIGHT;
import static media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.STROKES_CHANNEL;
import static media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.THICK_STROKE;
import static media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.THIN_STROKE;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdetRepeatedFrameType.BOTTOM;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdetRepeatedFrameType.NEITHER;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdetRepeatedFrameType.TOP;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdetSingleFrameType.BFF;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdetSingleFrameType.PROGRESSIVE;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdetSingleFrameType.TFF;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdetSingleFrameType.UNDETERMINED;

import java.awt.Color;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormat;
import media.mexm.mediadeepa.exportformat.graphic.TimedDataGraphic.RangeAxis;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMetadataFilterParser;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdAstats;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdAstatsChannel;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdSiti;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdValue;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.Ebur128Summary;
import tv.hd3g.fflauncher.resultparser.Stereo;

@Slf4j
public class GraphicExportFormat implements ExportFormat {

	public static final String LUFS_SUFFIX_FILE_NAME = "lufs-events.jpg";
	public static final String LUFS_TPK_SUFFIX_FILE_NAME = "lufs-tpk-events.jpg";
	public static final String A_PHASE_SUFFIX_FILE_NAME = "audio-phase.jpg";
	public static final String DC_OFFSET_SUFFIX_FILE_NAME = "audio-dcoffset.jpg";
	public static final String ENTROPY_SUFFIX_FILE_NAME = "audio-entropy.jpg";
	public static final String FLAT_FACTOR_SUFFIX_FILE_NAME = "audio-flat-factor.jpg";
	public static final String NOISE_FLOOR_SUFFIX_FILE_NAME = "audio-noise-floor.jpg";
	public static final String PEAK_LEVEL_SUFFIX_FILE_NAME = "audio-peak-level.jpg";
	public static final String SITI_SUFFIX_FILE_NAME = "video-siti.jpg";
	public static final String BLOCK_SUFFIX_FILE_NAME = "video-block.jpg";
	public static final String BLUR_SUFFIX_FILE_NAME = "video-blur.jpg";
	public static final String IDET_SUFFIX_FILE_NAME = "video-idet.jpg";

	@Override
	public String getFormatLongName() {
		return "Graphical representation of data";
	}

	@Override
	public void exportResult(final DataResult result, final File exportDirectory, final String baseFileName) {
		makeR128(result, exportDirectory, baseFileName);
		makeAPhase(result, exportDirectory, baseFileName);
		makeAStat(result, exportDirectory, baseFileName);
		makeSITI(result, exportDirectory, baseFileName);
		makeBlock(result, exportDirectory, baseFileName);
		makeBlur(result, exportDirectory, baseFileName);
		makeIdet(result, exportDirectory, baseFileName);
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
				new File(exportDirectory, makeOutputFileName(baseFileName, LUFS_SUFFIX_FILE_NAME)),
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
				new File(exportDirectory, makeOutputFileName(baseFileName, LUFS_TPK_SUFFIX_FILE_NAME)),
				IMAGE_SIZE_FULL_HEIGHT);
	}

	private void makeAPhase(final DataResult result, final File exportDirectory, final String baseFileName) {
		final var aPhaseMeterReport = result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getAPhaseMeterReport)
				.orElse(List.of());
		if (aPhaseMeterReport.isEmpty()) {
			return;
		}

		final var values = aPhaseMeterReport.stream()
				.map(LavfiMtdValue::value)
				.map(d -> d * 180f)
				.toList();

		final var dataGraphic = TimedDataGraphic.create(
				aPhaseMeterReport.stream().map(LavfiMtdValue::ptsTime),
				RangeAxis.createFromRelativesValueSet(
						"Phase (°)", 5,
						values.stream())); // new RangeAxis("Phase (°)", -190, 190)
		dataGraphic.addSeries(dataGraphic.new Series(
				"Phase correlation",
				FULL_PINK,
				THIN_STROKE,
				values.stream()));
		dataGraphic.addValueMarker(180);
		dataGraphic.addValueMarker(-180);
		dataGraphic.addValueMarker(90);
		dataGraphic.addValueMarker(-90);

		dataGraphic.makeLinearAxisGraphic(
				new File(exportDirectory, makeOutputFileName(baseFileName, A_PHASE_SUFFIX_FILE_NAME)),
				IMAGE_SIZE_HALF_HEIGHT);
	}

	private TimedDataGraphic prepareAStatGraphic(final RangeAxis rangeAxis,
												 final List<LavfiMtdValue<LavfiMtdAstats>> aStatReport,
												 final Integer chCount,
												 final Function<LavfiMtdAstatsChannel, Number> valuesExtractor) {
		final var dataGraphic = TimedDataGraphic.create(
				aStatReport.stream().map(LavfiMtdValue::ptsTime),
				rangeAxis);
		IntStream.range(0, chCount)
				.forEach(ch -> dataGraphic.addSeries(dataGraphic.new Series(
						"Channel " + (ch + 1),
						COLORS_CHANNEL.get(ch),
						STROKES_CHANNEL.get(ch),
						aStatReport.stream()
								.map(LavfiMtdValue::value)
								.map(LavfiMtdAstats::channels)
								.map(c -> c.get(ch))
								.map(valuesExtractor))));
		dataGraphic.addMinMaxValueMarkers();
		return dataGraphic;
	}

	private TimedDataGraphic prepareAStatGraphic(final List<LavfiMtdValue<LavfiMtdAstats>> aStatReport,
												 final Integer chCount,
												 final Function<LavfiMtdAstatsChannel, Number> valuesExtractor,
												 final String rangeName,
												 final int minRange) {
		final var rangeAxis = RangeAxis.createFromRelativesValueSet(
				rangeName, minRange,
				aStatReport.stream()
						.map(LavfiMtdValue::value)
						.map(LavfiMtdAstats::channels)
						.flatMap(List::stream)
						.map(valuesExtractor));
		return prepareAStatGraphic(rangeAxis, aStatReport, chCount, valuesExtractor);
	}

	private void makeAStat(final DataResult result, final File exportDirectory, final String baseFileName) {
		final var aStatReport = result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getAStatsReport)
				.orElse(List.of());
		if (aStatReport.isEmpty()) {
			return;
		}

		final var chCount = aStatReport.stream()
				.map(LavfiMtdValue::value)
				.map(LavfiMtdAstats::channels)
				.map(List::size)
				.findFirst()
				.orElse(0);
		if (chCount == 0) {
			log.warn("No channel found for export astats");
		} else if (chCount > 2) {
			log.warn("Only the two first channels will be graphed instead of {}", chCount);
		}

		prepareAStatGraphic(
				aStatReport,
				chCount,
				astat -> astat.dcOffset() * 100f,
				"Audio DC offset (%)",
				10).makeLinearAxisGraphic(
						new File(exportDirectory, makeOutputFileName(baseFileName, DC_OFFSET_SUFFIX_FILE_NAME)),
						IMAGE_SIZE_HALF_HEIGHT);
		prepareAStatGraphic(
				aStatReport,
				chCount,
				astat -> astat.entropy() * 100f,
				"Audio entropy (%)",
				10).makeLinearAxisGraphic(
						new File(exportDirectory, makeOutputFileName(baseFileName, ENTROPY_SUFFIX_FILE_NAME)),
						IMAGE_SIZE_HALF_HEIGHT);
		prepareAStatGraphic(
				aStatReport,
				chCount,
				LavfiMtdAstatsChannel::flatFactor,
				"Audio flat factor",
				1).makeLinearAxisGraphic(
						new File(exportDirectory, makeOutputFileName(baseFileName, FLAT_FACTOR_SUFFIX_FILE_NAME)),
						IMAGE_SIZE_HALF_HEIGHT);

		prepareAStatGraphic(
				RangeAxis.createFromValueSet("Audio noise floor (dBFS)",
						-96, 10, 5,
						aStatReport.stream()
								.map(LavfiMtdValue::value)
								.map(LavfiMtdAstats::channels)
								.flatMap(List::stream)
								.map(LavfiMtdAstatsChannel::noiseFloor)),
				aStatReport,
				chCount,
				LavfiMtdAstatsChannel::noiseFloor)
						.makeLogarithmicAxisGraphic(
								new File(exportDirectory, makeOutputFileName(baseFileName,
										NOISE_FLOOR_SUFFIX_FILE_NAME)),
								IMAGE_SIZE_HALF_HEIGHT);

		prepareAStatGraphic(
				RangeAxis.createFromValueSet("Audio audio peak level (dBFS)",
						-96, 10, 5,
						aStatReport.stream()
								.map(LavfiMtdValue::value)
								.map(LavfiMtdAstats::channels)
								.flatMap(List::stream)
								.map(LavfiMtdAstatsChannel::peakLevel)),
				aStatReport,
				chCount,
				LavfiMtdAstatsChannel::peakLevel)
						.makeLogarithmicAxisGraphic(
								new File(exportDirectory, makeOutputFileName(baseFileName,
										PEAK_LEVEL_SUFFIX_FILE_NAME)),
								IMAGE_SIZE_HALF_HEIGHT);
	}

	private void makeSITI(final DataResult result, final File exportDirectory, final String baseFileName) {
		final var sitiReport = result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getSitiReport)
				.orElse(List.of());
		if (sitiReport.isEmpty()) {
			return;
		}

		final var dataGraphic = TimedDataGraphic.create(
				sitiReport.stream().map(LavfiMtdValue::ptsTime),
				RangeAxis.createFromRelativesValueSet(
						"Spatial/Temporal Information", 5,
						sitiReport.stream()
								.map(LavfiMtdValue::value)
								.flatMap(s -> Stream.of(s.si(), s.ti()))));

		dataGraphic.addSeries(dataGraphic.new Series(
				"Spatial Information",
				FULL_PINK,
				THIN_STROKE,
				sitiReport.stream()
						.map(LavfiMtdValue::value)
						.map(LavfiMtdSiti::si)));
		dataGraphic.addSeries(dataGraphic.new Series(
				"Temporal Information",
				Color.YELLOW,
				THICK_STROKE,
				sitiReport.stream()
						.map(LavfiMtdValue::value)
						.map(LavfiMtdSiti::ti)));
		dataGraphic
				.addMinMaxValueMarkers()
				.makeLinearAxisGraphic(
						new File(exportDirectory, makeOutputFileName(baseFileName, SITI_SUFFIX_FILE_NAME)),
						IMAGE_SIZE_FULL_HEIGHT);
	}

	private void makeBlock(final DataResult result, final File exportDirectory, final String baseFileName) {
		final var blockReport = result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getBlockDetectReport)
				.orElse(List.of());
		if (blockReport.isEmpty()) {
			return;
		}

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
		dataGraphic
				.addMinMaxValueMarkers()
				.makeLinearAxisGraphic(
						new File(exportDirectory, makeOutputFileName(baseFileName, BLOCK_SUFFIX_FILE_NAME)),
						IMAGE_SIZE_FULL_HEIGHT);
	}

	private void makeBlur(final DataResult result, final File exportDirectory, final String baseFileName) {
		final var blurReport = result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getBlurDetectReport)
				.orElse(List.of());
		if (blurReport.isEmpty()) {
			return;
		}

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
		dataGraphic
				.addMinMaxValueMarkers()
				.makeLinearAxisGraphic(
						new File(exportDirectory, makeOutputFileName(baseFileName, BLUR_SUFFIX_FILE_NAME)),
						IMAGE_SIZE_FULL_HEIGHT);
	}

	private void makeIdet(final DataResult result, final File exportDirectory, final String baseFileName) {
		final var idetReport = result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getIdetReport)
				.orElse(List.of());
		if (idetReport.isEmpty()) {
			return;
		}

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
				idetReport.stream().map(d -> singleDetectedTypeMap.get(d.value().single().currentFrame()))));
		dataGraphic.addSeries(dataGraphic.new Series(
				"Multiple interlace",
				RED.brighter(),
				THICK_STROKE,
				idetReport.stream().map(d -> singleDetectedTypeMap.get(d.value().multiple().currentFrame()))));
		dataGraphic.addSeries(dataGraphic.new Series(
				"Repeated interlace",
				GRAY.darker(),
				THICK_STROKE,
				idetReport.stream().map(d -> repeatedDetectedTypeMap.get(d.value().repeated().currentFrame()))));

		dataGraphic
				.makeLinearAxisGraphic(
						new File(exportDirectory, makeOutputFileName(baseFileName, IDET_SUFFIX_FILE_NAME)),
						IMAGE_SIZE_HALF_HEIGHT);
	}

	/*
	TODO Crop detect w	h	x	y
	TODO Audio/video events graphical
	TODO Container bitrate graphical Video
	TODO Container bitrate graphical Audio
	TODO GOP size / GOP Width graphical
	*/

}
