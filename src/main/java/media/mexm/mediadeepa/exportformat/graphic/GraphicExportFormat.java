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
import static java.awt.Color.CYAN;
import static java.awt.Color.GRAY;
import static java.awt.Color.GREEN;
import static java.awt.Color.ORANGE;
import static java.awt.Color.RED;
import static java.awt.Color.YELLOW;
import static java.lang.Float.NaN;
import static media.mexm.mediadeepa.components.CLIRunner.makeOutputFileName;
import static media.mexm.mediadeepa.exportformat.graphic.DataGraphic.COLORS_CHANNEL;
import static media.mexm.mediadeepa.exportformat.graphic.DataGraphic.FULL_PINK;
import static media.mexm.mediadeepa.exportformat.graphic.DataGraphic.IMAGE_SIZE_FULL_HEIGHT;
import static media.mexm.mediadeepa.exportformat.graphic.DataGraphic.IMAGE_SIZE_HALF_HEIGHT;
import static media.mexm.mediadeepa.exportformat.graphic.DataGraphic.STROKES_CHANNEL;
import static media.mexm.mediadeepa.exportformat.graphic.DataGraphic.THICK_STROKE;
import static media.mexm.mediadeepa.exportformat.graphic.DataGraphic.THIN_STROKE;
import static tv.hd3g.fflauncher.ffprobecontainer.FFprobeCodecType.VIDEO;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdetRepeatedFrameType.BOTTOM;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdetRepeatedFrameType.NEITHER;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdetRepeatedFrameType.TOP;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdetSingleFrameType.BFF;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdetSingleFrameType.PROGRESSIVE;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdetSingleFrameType.TFF;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdIdetSingleFrameType.UNDETERMINED;

import java.awt.Color;
import java.awt.Stroke;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.ffmpeg.ffprobe.FormatType;
import org.ffmpeg.ffprobe.StreamType;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormat;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeAudioFrame;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeBaseFrame;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeVideoFrame;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeVideoFrameConst;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMetadataFilterParser;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdAstats;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdAstatsChannel;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdEvent;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdSiti;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdValue;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserResult;
import tv.hd3g.fflauncher.recipes.GOPStatItem;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.Ebur128Summary;
import tv.hd3g.fflauncher.resultparser.Stereo;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;

@Slf4j
public class GraphicExportFormat implements ExportFormat {

	public static final String LUFS_SUFFIX_FILE_NAME = "audio-loudness.jpg";
	public static final String LUFS_TPK_SUFFIX_FILE_NAME = "audio-loundness-truepeak.jpg";
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
	public static final String CROP_SUFFIX_FILE_NAME = "video-crop.jpg";
	public static final String EVENTS_SUFFIX_FILE_NAME = "events.jpg";
	public static final String VBITRATE_SUFFIX_FILE_NAME = "video-bitrate.jpg";
	public static final String ABITRATE_SUFFIX_FILE_NAME = "audio-bitrate.jpg";
	public static final String VFRAMEDURATION_SUFFIX_FILE_NAME = "video-frame-duration.jpg";
	public static final String GOP_COUNT_SUFFIX_FILE_NAME = "video-gop-count.jpg";
	public static final String GOP_SIZES_SUFFIX_FILE_NAME = "video-gop-size.jpg";

	@Override
	public String getFormatLongName() {
		return "Graphical representation of data";
	}

	public Optional<File> getGraphicFileBySuffixName(final File exportDirectory,
													 final String baseFileName,
													 final String suffixName) {
		final var outFile = new File(exportDirectory, makeOutputFileName(baseFileName, suffixName));
		if (outFile.exists()) {
			return Optional.ofNullable(outFile);
		}
		return Optional.empty();
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
		makeCrop(result, exportDirectory, baseFileName);
		makeEvents(result, exportDirectory, baseFileName);
		makeVideoBitrate(result, exportDirectory, baseFileName);
		makeAudioBitrate(result, exportDirectory, baseFileName);
		makeVideoFrameDuration(result, exportDirectory, baseFileName);
		makeVideoGOPCount(result, exportDirectory, baseFileName);
		makeVideoGOPSize(result, exportDirectory, baseFileName);
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
				"True peak left (per frame)", BLUE, THIN_STROKE,
				r128events.stream().map(Ebur128StrErrFilterEvent::getFtpk).map(Stereo::left)));
		dataGraphicTPK.addSeries(dataGraphicLUFS.new Series(
				"True peak right (per frame)", RED, THICK_STROKE,
				r128events.stream().map(Ebur128StrErrFilterEvent::getFtpk).map(Stereo::right)));
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
				.map(d -> d * 100f)
				.toList();

		final var dataGraphic = TimedDataGraphic.create(
				aPhaseMeterReport.stream().map(LavfiMtdValue::ptsTime),
				RangeAxis.createFromRelativesValueSet(
						"Phase (%)", 5,
						values.stream()));
		dataGraphic.addSeries(dataGraphic.new Series(
				"Phase correlation",
				FULL_PINK,
				THIN_STROKE,
				values.stream()));

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
						-144, 20, 0,
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

	private void makeCrop(final DataResult result, final File exportDirectory, final String baseFileName) {
		final var cropReport = result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getCropDetectReport)
				.orElse(List.of());
		if (cropReport.isEmpty()) {
			return;
		}

		final var rangeAxis = RangeAxis.createFromRelativesValueSet("Pixels", 0,
				cropReport.stream()
						.map(LavfiMtdValue::value)
						.flatMap(d -> Stream.of(d.x(), d.y(), d.w(), d.h())));

		final var dataGraphic = TimedDataGraphic.create(
				cropReport.stream().map(LavfiMtdValue::ptsTime),
				rangeAxis);

		dataGraphic.addSeries(dataGraphic.new Series(
				"Crop X offset",
				BLUE,
				THIN_STROKE,
				cropReport.stream().map(d -> d.value().x())));
		dataGraphic.addSeries(dataGraphic.new Series(
				"Crop Y offset",
				RED,
				THICK_STROKE,
				cropReport.stream().map(d -> d.value().y())));
		dataGraphic.addSeries(dataGraphic.new Series(
				"Crop width",
				CYAN,
				THIN_STROKE,
				cropReport.stream().map(d -> d.value().w())));
		dataGraphic.addSeries(dataGraphic.new Series(
				"Crop height",
				ORANGE,
				THICK_STROKE,
				cropReport.stream().map(d -> d.value().h())));

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

		dataGraphic
				.makeLinearAxisGraphic(
						new File(exportDirectory, makeOutputFileName(baseFileName, CROP_SUFFIX_FILE_NAME)),
						IMAGE_SIZE_FULL_HEIGHT);
	}

	private void addSeriesFromEvent(final DataResult result,
									final Function<LavfiMetadataFilterParser, List<LavfiMtdEvent>> dataSelector,
									final int secDurationRoundedInt,
									final String seriesName,
									final Color color,
									final Stroke stroke,
									final TimedDataGraphic dataGraphic) {
		final var events = result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.stream()
				.map(dataSelector)
				.flatMap(List::stream)
				.toList();
		dataGraphic.addSeries(dataGraphic.new Series(
				seriesName,
				color,
				stroke,
				IntStream.range(0, secDurationRoundedInt)
						.mapToObj(posSec -> events.stream()
								.anyMatch(event -> posSec >= event.start().toSeconds()
												   && posSec < event.end().toSeconds()) ? 1 : 0)));
	}

	private void makeEvents(final DataResult result, final File exportDirectory, final String baseFileName) {
		final var eventsReportCount = result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getEventCount)
				.orElse(0);
		final var secDurationFloat = result.getFFprobeResult()
				.map(FFprobeJAXB::getFormat)
				.map(FormatType::getDuration)
				.orElse(0f);
		if (eventsReportCount == 0 || secDurationFloat < 1f) {
			return;
		}

		final var secDurationRoundedInt = (int) Math.round(Math.floor(secDurationFloat));
		final var dataGraphic = TimedDataGraphic.create(
				IntStream.range(0, secDurationRoundedInt)
						.mapToObj(f -> (float) f),
				new RangeAxis("Events", 0, 1));

		addSeriesFromEvent(result, LavfiMetadataFilterParser::getBlackEvents, secDurationRoundedInt,
				"Full black frames", BLUE, THIN_STROKE, dataGraphic);
		addSeriesFromEvent(result, LavfiMetadataFilterParser::getSilenceEvents, secDurationRoundedInt,
				"Audio silence", GRAY, THICK_STROKE, dataGraphic);
		addSeriesFromEvent(result, LavfiMetadataFilterParser::getFreezeEvents, secDurationRoundedInt,
				"Freeze frames", RED, THICK_STROKE, dataGraphic);
		addSeriesFromEvent(result, LavfiMetadataFilterParser::getMonoEvents, secDurationRoundedInt,
				"Audio mono", YELLOW, THIN_STROKE, dataGraphic);
		dataGraphic
				.makeLinearAxisGraphic(
						new File(exportDirectory, makeOutputFileName(baseFileName, EVENTS_SUFFIX_FILE_NAME)),
						IMAGE_SIZE_HALF_HEIGHT);
	}

	private void makeVideoBitrate(final DataResult result, final File exportDirectory, final String baseFileName) {
		final var videoFramesReport = result.getContainerAnalyserResult()
				.map(ContainerAnalyserResult::videoFrames)
				.stream()
				.flatMap(List::stream)
				.filter(d -> d.repeatPict() == false)
				.map(FFprobeVideoFrame::frame)
				.filter(f -> f.mediaType() == VIDEO)
				.toList();
		if (videoFramesReport.isEmpty()) {
			return;
		}
		final var firstStreamIndex = videoFramesReport.stream()
				.map(FFprobeBaseFrame::streamIndex)
				.skip(1)
				.findFirst().orElseThrow(() -> new IllegalArgumentException("Can't found video stream index"));

		final var values = videoFramesReport.stream()
				.filter(f -> f.streamIndex() == firstStreamIndex)
				.map(FFprobeBaseFrame::pktSize)
				.map(f -> (float) f / 1024f)
				.toList();

		final var dataGraphic = TimedDataGraphic.create(
				videoFramesReport.stream()
						.filter(f -> f.streamIndex() == firstStreamIndex)
						.map(FFprobeBaseFrame::pktDtsTime),
				RangeAxis.createFromRelativesValueSet(
						"Frame size (kbytes)", 0,
						values.stream()));

		dataGraphic.addSeries(dataGraphic.new Series(
				"Video packet/frame size",
				BLUE.brighter(),
				THIN_STROKE,
				values.stream()));
		dataGraphic
				.addMinMaxValueMarkers()
				.makeLinearAxisGraphic(
						new File(exportDirectory, makeOutputFileName(baseFileName, VBITRATE_SUFFIX_FILE_NAME)),
						IMAGE_SIZE_FULL_HEIGHT);
	}

	private void makeAudioBitrate(final DataResult result, final File exportDirectory, final String baseFileName) {
		final var audioBReport = result.getContainerAnalyserResult()
				.map(ContainerAnalyserResult::audioFrames)
				.stream()
				.flatMap(List::stream)
				.toList();
		if (audioBReport.isEmpty()) {
			return;
		}

		final var firstStreamIndex = audioBReport.stream()
				.map(FFprobeAudioFrame::frame)
				.map(FFprobeBaseFrame::streamIndex)
				.findFirst().orElseThrow(() -> new IllegalArgumentException("Can't found audio stream index"));

		final var dataGraphic = TimedDataGraphic.create(
				audioBReport.stream()
						.map(FFprobeAudioFrame::frame)
						.filter(f -> f.streamIndex() == firstStreamIndex)
						.map(FFprobeBaseFrame::ptsTime),
				RangeAxis.createFromRelativesValueSet(
						"Audio packet/frame size (bytes)", 0,
						audioBReport.stream()
								.map(FFprobeAudioFrame::frame)
								.map(FFprobeBaseFrame::pktSize)));

		final var allStreamIndexes = audioBReport.stream()
				.map(FFprobeAudioFrame::frame)
				.map(FFprobeBaseFrame::streamIndex)
				.distinct()
				.sorted()
				.toList();
		final var colorSpliter = 1f / allStreamIndexes.size();

		allStreamIndexes.forEach(streamIndex -> dataGraphic
				.addSeries(dataGraphic.new Series(
						"Stream #" + streamIndex + " (audio)",
						Color.getHSBColor(allStreamIndexes.indexOf(streamIndex) * colorSpliter, 1, 1),
						THIN_STROKE,
						audioBReport.stream()
								.map(FFprobeAudioFrame::frame)
								.filter(f -> f.streamIndex() == streamIndex)
								.map(FFprobeBaseFrame::pktSize))));
		dataGraphic
				.addMinMaxValueMarkers()
				.makeLinearAxisGraphic(
						new File(exportDirectory, makeOutputFileName(baseFileName, ABITRATE_SUFFIX_FILE_NAME)),
						IMAGE_SIZE_HALF_HEIGHT);
	}

	public static final UnaryOperator<Float> secToMs = s -> s * 1000f;

	public static double[] getTimeDerivative(final Stream<Float> timeValues, final int itemCount) {
		final var result = new double[itemCount];

		final var interator = timeValues.iterator();
		Float previous;
		var actual = NaN;
		var pos = 0;
		while (interator.hasNext()) {
			previous = actual;
			actual = interator.next();
			if (previous != NaN) {
				result[pos++] = actual - previous;
			}
		}
		return result;
	}

	private void makeVideoFrameDuration(final DataResult result,
										final File exportDirectory,
										final String baseFileName) {
		final var videoFramesReport = result.getContainerAnalyserResult()
				.map(ContainerAnalyserResult::videoFrames)
				.stream()
				.flatMap(List::stream)
				.toList();
		if (videoFramesReport.isEmpty()) {
			return;
		}
		final var firstStreamIndex = videoFramesReport.stream()
				.map(FFprobeVideoFrame::frame)
				.map(FFprobeBaseFrame::streamIndex)
				.findFirst().orElseThrow(() -> new IllegalArgumentException("Can't found video stream index"));
		final var allFrames = videoFramesReport.stream()
				.map(FFprobeVideoFrame::frame)
				.filter(f -> f.streamIndex() == firstStreamIndex)
				.toList();

		final var pktDtsTimeDerivative = getTimeDerivative(allFrames.stream()
				.map(FFprobeBaseFrame::pktDtsTime)
				.map(ms -> ms > -1f ? ms : Float.NaN)
				.map(secToMs),
				allFrames.size());
		final var bestEffortTimestampTimeDerivative = getTimeDerivative(allFrames.stream()
				.map(FFprobeBaseFrame::bestEffortTimestampTime)
				.map(ms -> ms > -1f ? ms : Float.NaN)
				.map(secToMs),
				allFrames.size());

		final var dataGraphic = new XYLineChartDataGraphic(RangeAxis.createAutomaticRangeAxis(
				"Frame duration (milliseconds)"), allFrames.size());

		dataGraphic.addSeries(new SeriesStyle("DTS video frame duration", BLUE, THIN_STROKE),
				pktDtsTimeDerivative);
		dataGraphic.addSeries(new SeriesStyle("Best effort video frame duration", RED, THICK_STROKE),
				bestEffortTimestampTimeDerivative);

		dataGraphic.makeLinearAxisGraphic(new File(exportDirectory, makeOutputFileName(baseFileName,
				VFRAMEDURATION_SUFFIX_FILE_NAME)), IMAGE_SIZE_HALF_HEIGHT);
	}

	private void makeVideoGOPCount(final DataResult result, final File exportDirectory, final String baseFileName) {
		final var gopStatsReport = result.getContainerAnalyserResult()
				.map(ContainerAnalyserResult::extractGOPStats)
				.stream()
				.flatMap(List::stream)
				.toList();
		if (gopStatsReport.isEmpty()) {
			return;
		}

		final var dataGraphic = new StackedXYAreaChartDataGraphic(RangeAxis.createAutomaticRangeAxis(
				"Number of frames"));
		dataGraphic.addValueMarker(gopStatsReport.stream().mapToInt(GOPStatItem::gopFrameCount).max().orElse(0));

		dataGraphic.addSeriesByCounter(new SeriesStyle("P frame count by GOP", BLUE, THIN_STROKE),
				gopStatsReport.stream().map(GOPStatItem::pFramesCount));
		dataGraphic.addSeriesByCounter(new SeriesStyle("B frame count by GOP", RED.darker(), THIN_STROKE),
				gopStatsReport.stream().map(GOPStatItem::bFramesCount));
		dataGraphic.addSeriesByCounter(new SeriesStyle("Video frame count by GOP", GRAY, THIN_STROKE),
				gopStatsReport.stream().map(g -> g.gopFrameCount() - (g.bFramesCount() + g.pFramesCount())));
		dataGraphic.makeLinearAxisGraphic(new File(exportDirectory, makeOutputFileName(baseFileName,
				GOP_COUNT_SUFFIX_FILE_NAME)), IMAGE_SIZE_HALF_HEIGHT);
	}

	private void makeVideoGOPSize(final DataResult result, final File exportDirectory, final String baseFileName) {
		final var gopStatsReport = result.getContainerAnalyserResult()
				.map(ContainerAnalyserResult::extractGOPStats)
				.stream()
				.flatMap(List::stream)
				.toList();
		if (gopStatsReport.isEmpty()) {
			return;
		}

		final var dataGraphic = new StackedXYAreaChartDataGraphic(RangeAxis.createAutomaticRangeAxis(
				"GOP frame size (kbytes)"));
		dataGraphic.addValueMarker(gopStatsReport.stream()
				.mapToDouble(GOPStatItem::gopDataSize)
				.max()
				.stream()
				.map(d -> d / 1024d)
				.findFirst().orElse(0d));

		dataGraphic.addSeriesByCounter(new SeriesStyle("P frames size in GOP", BLUE, THIN_STROKE),
				gopStatsReport.stream().flatMap(g -> g.videoFrames().stream()
						.map(f -> g.pFramesDataSize() / 1024d)));
		dataGraphic.addSeriesByCounter(new SeriesStyle("B frames size in GOP", RED.darker(), THIN_STROKE),
				gopStatsReport.stream().flatMap(g -> g.videoFrames().stream()
						.map(f -> g.bFramesDataSize() / 1024d)));
		dataGraphic.addSeriesByCounter(new SeriesStyle("I frames size in GOP", GRAY, THIN_STROKE),
				gopStatsReport.stream().flatMap(g -> g.videoFrames().stream()
						.map(f -> (g.gopDataSize() - (g.bFramesDataSize() + g.pFramesDataSize())) / 1024d)));

		dataGraphic.makeLinearAxisGraphic(new File(exportDirectory, makeOutputFileName(baseFileName,
				GOP_SIZES_SUFFIX_FILE_NAME)), IMAGE_SIZE_FULL_HEIGHT);
	}

}
