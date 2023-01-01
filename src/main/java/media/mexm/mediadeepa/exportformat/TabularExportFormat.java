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
package media.mexm.mediadeepa.exportformat;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;
import static media.mexm.mediadeepa.components.CLIRunner.makeOutputFileName;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMetadataFilterParser;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserResult;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.Ebur128Summary;
import tv.hd3g.fflauncher.resultparser.RawStdErrFilterEvent;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;

public abstract class TabularExportFormat implements ExportFormat, TabularDocumentExporter {
	private static final String PTS_TIME = "Pts time";
	private static final String STREAM_INDEX = "Stream index";
	private static final String VALUE = "Value";
	private static final String PTS = "PTS";
	private static final String FRAME = "Frame";

	@Override
	public void exportResult(final DataResult result, final File exportDirectory, final String baseFileName) {
		result.getMediaAnalyserResult()
				.ifPresent(maResult -> {
					Optional.ofNullable(maResult.ebur128Summary())
							.ifPresent(ebu -> saveEbur128Summary(exportDirectory, baseFileName, ebu));

					final var lavfiMetadatas = maResult.lavfiMetadatas();

					saveAPhaseMeter(exportDirectory, baseFileName, lavfiMetadatas);
					saveAStats(exportDirectory, baseFileName, lavfiMetadatas);
					saveSITI(exportDirectory, baseFileName, lavfiMetadatas);
					saveSITIReport(exportDirectory, baseFileName, lavfiMetadatas);
					saveBlock(exportDirectory, baseFileName, lavfiMetadatas);
					saveBlur(exportDirectory, baseFileName, lavfiMetadatas);
					saveCrop(exportDirectory, baseFileName, lavfiMetadatas);
					saveIdet(exportDirectory, baseFileName, lavfiMetadatas);
					saveAboutMeasure(maResult, exportDirectory, baseFileName);
					result.getSourceDuration()
							.ifPresent(sourceDuration -> saveEvents(
									sourceDuration, exportDirectory, baseFileName, lavfiMetadatas));
				});

		saveEbur128(result.getEbur128events(), exportDirectory, baseFileName);
		saveRawstderrfilters(result.getRawStdErrEvents(), exportDirectory, baseFileName);

		result.getContainerAnalyserResult()
				.ifPresent(caResult -> {
					savePackets(caResult, exportDirectory, baseFileName);
					saveVFrames(caResult, exportDirectory, baseFileName);
					saveAFrames(caResult, exportDirectory, baseFileName);
					saveVConsts(caResult, exportDirectory, baseFileName);
					saveAConsts(caResult, exportDirectory, baseFileName);
					saveGopStats(caResult, exportDirectory, baseFileName);
				});

		result.getFFprobeResult()
				.ifPresent(ffprobeResult -> saveFFprobeJAXB(ffprobeResult, exportDirectory, baseFileName));
	}

	private void saveEbur128Summary(final File exportDirectory, final String baseFileName, final Ebur128Summary ebu) {
		final var t = new TabularDocument(this).head("Type", VALUE);
		t.row("Integrated", ebu.getIntegrated());
		t.row("Integrated Threshold", ebu.getIntegratedThreshold());
		t.row("Loudness Range", ebu.getLoudnessRange());
		t.row("Loudness Range Threshold", ebu.getLoudnessRangeThreshold());
		t.row("Loudness Range Low", ebu.getLoudnessRangeLow());
		t.row("Loudness Range High", ebu.getLoudnessRangeHigh());
		t.row("Sample Peak", ebu.getSamplePeak());
		t.row("True Peak", ebu.getTruePeak());
		t.exportToFile(makeOutputFileName(baseFileName, "audio-ebur128-summary.txt"), exportDirectory);
	}

	private void saveSITIReport(final File exportDirectory,
								final String baseFileName,
								final LavfiMetadataFilterParser lavfiMetadatas) {
		if (lavfiMetadatas.getSitiReport().isEmpty() == false) {
			final var sitiStats = new TabularDocument(this).head("Type", "Average", "Max", "Min");
			final var stats = lavfiMetadatas.computeSitiStats();
			sitiStats
					.row("Spatial Info",
							stats.si().getAverage(),
							stats.si().getMax(),
							stats.si().getMin())
					.row("Temporal Info",
							stats.ti().getAverage(),
							stats.ti().getMax(),
							stats.ti().getMin())
					.exportToFile(makeOutputFileName(baseFileName, "video-siti-stats-ITU-T_P-910.txt"), exportDirectory);
		}
	}

	private void saveSITI(final File exportDirectory,
						  final String baseFileName,
						  final LavfiMetadataFilterParser lavfiMetadatas) {
		final var siti = new TabularDocument(this).head(FRAME, PTS, PTS_TIME, "Spatial Info",
				"Temporal Info");
		lavfiMetadatas.getSitiReport()
				.forEach(a -> siti.row(a.frame(), a.pts(), a.ptsTime(), a.value().si(), a.value().ti()));
		siti.exportToFile(makeOutputFileName(baseFileName, "video-siti-ITU-T_P-910.txt"), exportDirectory);
	}

	private void saveAStats(final File exportDirectory,
							final String baseFileName,
							final LavfiMetadataFilterParser lavfiMetadatas) {
		final var aStats = new TabularDocument(this).head(FRAME, PTS, PTS_TIME,
				"Channel",
				"DC Offset",
				"Entropy",
				"Flat factor",
				"Noise floor",
				"Noise floor count",
				"Peak level",
				"Peak count",
				"Other");
		lavfiMetadatas.getAStatsReport()
				.forEach(a -> {
					final var channels = a.value().channels();
					for (var pos = 0; pos < channels.size(); pos++) {
						final var channel = channels.get(pos);
						var other = channel.other().toString();
						if (other.equals("{}")) {
							other = "";
						}
						aStats.row(a.frame(), a.pts(), a.ptsTime(),
								pos + 1,
								channel.dcOffset(),
								channel.entropy(),
								channel.flatFactor(),
								channel.noiseFloor(),
								channel.noiseFloorCount(),
								channel.peakLevel(),
								channel.peakCount(),
								other);
					}
				});
		aStats.exportToFile(makeOutputFileName(baseFileName, "audio-stats.txt"), exportDirectory);
	}

	private void saveBlock(final File exportDirectory,
						   final String baseFileName,
						   final LavfiMetadataFilterParser lavfiMetadatas) {
		final var block = new TabularDocument(this).head(FRAME, PTS, PTS_TIME, VALUE);
		lavfiMetadatas.getBlockDetectReport()
				.forEach(a -> block.row(a.frame(), a.pts(), a.ptsTime(), a.value()));
		block.exportToFile(makeOutputFileName(baseFileName, "video-block-detect.txt"), exportDirectory);
	}

	private void saveBlur(final File exportDirectory,
						  final String baseFileName,
						  final LavfiMetadataFilterParser lavfiMetadatas) {
		final var blur = new TabularDocument(this).head(FRAME, PTS, PTS_TIME, VALUE);
		lavfiMetadatas.getBlurDetectReport()
				.forEach(a -> blur.row(a.frame(), a.pts(), a.ptsTime(), a.value()));
		blur.exportToFile(makeOutputFileName(baseFileName, "video-blur-detect.txt"), exportDirectory);
	}

	private void saveCrop(final File exportDirectory,
						  final String baseFileName,
						  final LavfiMetadataFilterParser lavfiMetadatas) {
		final var crop = new TabularDocument(this).head(FRAME, PTS, PTS_TIME,
				"x1", "x2", "y1", "y2", "w", "h", "x", "y");
		lavfiMetadatas.getCropDetectReport().forEach(a -> {
			final var value = a.value();
			crop.row(a.frame(), a.pts(), a.ptsTime(),
					value.x1(), value.x2(), value.y1(), value.y2(),
					value.w(), value.h(), value.x(), value.y());
		});
		crop.exportToFile(makeOutputFileName(baseFileName, "video-crop-detect.txt"), exportDirectory);
	}

	private void saveIdet(final File exportDirectory,
						  final String baseFileName,
						  final LavfiMetadataFilterParser lavfiMetadatas) {
		final var idet = new TabularDocument(this).head(FRAME, PTS, PTS_TIME,
				"Single top field first",
				"Single bottom field first",
				"Single current frame",
				"Single progressive",
				"Single undetermined",
				"Multiple top field first",
				"Multiple bottom field first",
				"Multiple current frame",
				"Multiple progressive",
				"Multiple undetermined",
				"Repeated current frame",
				"Repeated top",
				"Repeated bottom",
				"Repeated neither");
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
		idet.exportToFile(makeOutputFileName(baseFileName, "video-interlace-detect.txt"), exportDirectory);
	}

	private void saveEvents(final Duration sourceDuration,
							final File exportDirectory,
							final String baseFileName,
							final LavfiMetadataFilterParser lavfiMetadatas) {
		final var events = new TabularDocument(this).head(
				"Name", "Scope/Channel", "Start", "End", "Duration");
		Stream.of(
				lavfiMetadatas.getMonoEvents(),
				lavfiMetadatas.getSilenceEvents(),
				lavfiMetadatas.getBlackEvents(),
				lavfiMetadatas.getFreezeEvents())
				.flatMap(List::stream)
				.sorted()
				.forEach(ev -> events.row(
						ev.name(),
						ev.scope(),
						ev.start(),
						ev.getEndOr(sourceDuration),
						ev.getEndOr(sourceDuration).minus(ev.start())));
		events.exportToFile(makeOutputFileName(baseFileName, "events.txt"), exportDirectory);
	}

	private void saveAboutMeasure(final MediaAnalyserResult maResult,
								  final File exportDirectory,
								  final String baseFileName) {
		final var aboutMeasure = new TabularDocument(this).head(
				"Type", "Name", "Setup", "Java class");
		maResult.filters()
				.forEach(f -> aboutMeasure.row(
						f.type(),
						f.name(),
						f.setup(),
						f.className()));
		aboutMeasure.exportToFile(makeOutputFileName(baseFileName, "filters.txt"), exportDirectory);
	}

	private void saveAPhaseMeter(final File exportDirectory,
								 final String baseFileName,
								 final LavfiMetadataFilterParser lavfiMetadatas) {
		final var aPhaseMeter = new TabularDocument(this).head(FRAME, PTS, PTS_TIME, VALUE);
		lavfiMetadatas.getAPhaseMeterReport()
				.forEach(a -> aPhaseMeter.row(a.frame(), a.pts(), a.ptsTime(), a.value()));
		aPhaseMeter.exportToFile(makeOutputFileName(baseFileName, "audio-phase-meter.txt"), exportDirectory);
	}

	private void saveEbur128(final List<Ebur128StrErrFilterEvent> ebur128events,
							 final File exportDirectory,
							 final String baseFileName) {
		final var dfMs = new DecimalFormat("#.#");
		dfMs.setRoundingMode(RoundingMode.CEILING);
		dfMs.setDecimalFormatSymbols(new DecimalFormatSymbols(ENGLISH));

		final var t = new TabularDocument(this).head(
				"Position",
				"Integrated",
				"Momentary",
				"Short-term",
				"Loudness Range",
				"Sample-peak L", "Sample-peak R",
				"True-peak per frame L", "True-peak per frame R",
				"True-peak L", "True-peak R");
		ebur128events.forEach(ebu -> t.row(
				dfMs.format(ebu.getT()),
				dfMs.format(ebu.getI()),
				dfMs.format(ebu.getM()),
				dfMs.format(ebu.getS()),
				dfMs.format(ebu.getLra()),
				dfMs.format(ebu.getSpk().left()),
				dfMs.format(ebu.getSpk().right()),
				dfMs.format(ebu.getFtpk().left()),
				dfMs.format(ebu.getFtpk().right()),
				dfMs.format(ebu.getTpk().left()),
				dfMs.format(ebu.getTpk().right())));
		t.exportToFile(makeOutputFileName(baseFileName, "audio-ebur128.txt"), exportDirectory);
	}

	private void saveRawstderrfilters(final List<RawStdErrFilterEvent> rawStdErrEvents,
									  final File exportDirectory,
									  final String baseFileName) {
		final var t = new TabularDocument(this).head("Filter name", "Chain pos", "Line");
		rawStdErrEvents.stream()
				.filter(r -> r.getFilterName().equals("cropdetect") == false)
				.forEach(r -> t.row(r.getFilterName(), r.getFilterChainPos(), r.getLineValue()));
		t.exportToFile(makeOutputFileName(baseFileName, "rawstderrfilters.txt"), exportDirectory);
	}

	private void saveGopStats(final ContainerAnalyserResult caResult,
							  final File exportDirectory,
							  final String baseFileName) {
		final var gopStats = new TabularDocument(this).head(
				"GOP frame count",
				"P frames count",
				"B frames count",
				"GOP data size",
				"I frame data size",
				"P frames data size",
				"B frames data size");
		caResult.extractGOPStats()
				.forEach(f -> gopStats.row(
						f.gopFrameCount(),
						f.pFramesCount(),
						f.bFramesCount(),
						f.gopDataSize(),
						f.iFrameDataSize(),
						f.pFramesDataSize(),
						f.bFramesDataSize()));
		gopStats.exportToFile(makeOutputFileName(baseFileName, "container-video-gop.txt"), exportDirectory);
	}

	private void saveAConsts(final ContainerAnalyserResult caResult,
							 final File exportDirectory,
							 final String baseFileName) {
		final var aConsts = new TabularDocument(this).head(
				"Channel layout",
				"Channels",
				"Sample format",
				"Ref pts",
				"Ref pts time",
				"Ref pkt dts",
				"Ref pkt dts time",
				"Ref best effort timestamp",
				"Ref best effort timestamp time",
				"Ref pkt duration",
				"Ref pkt duration time",
				"Ref pkt pos");
		Stream.concat(
				caResult.olderAudioConsts().stream(),
				Stream.of(caResult.audioConst()))
				.forEach(c -> {
					final var frame = c.updatedWith().frame();
					aConsts.row(
							c.channelLayout(),
							c.channels(),
							c.sampleFmt(),
							frame.pts(),
							frame.ptsTime(),
							frame.pktDts(),
							frame.pktDtsTime(),
							frame.bestEffortTimestamp(),
							frame.bestEffortTimestampTime(),
							frame.pktDuration(),
							frame.pktDurationTime(),
							frame.pktPos());
				});
		aConsts.exportToFile(makeOutputFileName(baseFileName, "container-audio-consts.txt"), exportDirectory);
	}

	private void saveVConsts(final ContainerAnalyserResult caResult,
							 final File exportDirectory,
							 final String baseFileName) {
		final var vConsts = new TabularDocument(this).head(
				"Width",
				"Height",
				"Sample aspect ratio",
				"Top field first",
				"Interlaced frame",
				"Pix fmt",
				"Color range",
				"Color primaries",
				"Color transfer",
				"Color space",
				"Coded picture number",
				"Display picture number",
				"Ref pts",
				"Ref pts time",
				"Ref pkt dts",
				"Ref pkt dts time",
				"Ref best effort timestamp",
				"Ref best effort timestamp time",
				"Ref pkt duration",
				"Ref pkt duration time",
				"Ref pkt pos");
		Stream.concat(
				caResult.olderVideoConsts().stream(),
				Stream.of(caResult.videoConst()))
				.filter(Objects::nonNull)
				.forEach(c -> {
					final var frame = c.updatedWith().frame();
					vConsts.row(
							c.width(),
							c.height(),
							c.sampleAspectRatio(),
							c.topFieldFirst() ? "1" : "0",
							c.interlacedFrame() ? "1" : "0",
							c.pixFmt(),
							c.colorRange(),
							c.colorPrimaries(),
							c.colorTransfer(),
							c.colorSpace(),
							c.codedPictureNumber(),
							c.displayPictureNumber(),
							frame.pts(),
							frame.ptsTime(),
							frame.pktDts(),
							frame.pktDtsTime(),
							frame.bestEffortTimestamp(),
							frame.bestEffortTimestampTime(),
							frame.pktDuration(),
							frame.pktDurationTime(),
							frame.pktPos());
				});
		vConsts.exportToFile(makeOutputFileName(baseFileName, "container-video-consts.txt"), exportDirectory);
	}

	private void saveAFrames(final ContainerAnalyserResult caResult,
							 final File exportDirectory,
							 final String baseFileName) {
		final var aFrames = new TabularDocument(this).head(
				"Media type",
				STREAM_INDEX,
				"Nb samples",
				"Pts",
				PTS_TIME,
				"Pkt dts",
				"Pkt dts time",
				"Best effort timestamp",
				"Best effort timestamp time",
				"Pkt duration",
				"Pkt duration time",
				"Pkt pos",
				"Pkt size");

		caResult.audioFrames().forEach(r -> {
			final var frame = r.frame();
			aFrames.row(
					frame.mediaType(),
					frame.streamIndex(),
					r.nbSamples(),
					frame.pts(),
					frame.ptsTime(),
					frame.pktDts(),
					frame.pktDtsTime(),
					frame.bestEffortTimestamp(),
					frame.bestEffortTimestampTime(),
					frame.pktDuration(),
					frame.pktDurationTime(),
					frame.pktPos(),
					frame.pktSize());
		});
		aFrames.exportToFile(makeOutputFileName(baseFileName, "container-audio-frames.txt"), exportDirectory);
	}

	private void saveVFrames(final ContainerAnalyserResult caResult,
							 final File exportDirectory,
							 final String baseFileName) {
		final var vFrames = new TabularDocument(this).head(
				"Media type",
				STREAM_INDEX,
				"Key frame",
				"Pict type",
				"Repeat pict",
				"Pts",
				PTS_TIME,
				"Pkt dts",
				"Pkt dts time",
				"Best effort timestamp",
				"Best effort timestamp time",
				"Pkt duration",
				"Pkt duration time",
				"Pkt pos",
				"Pkt size");

		caResult.videoFrames().forEach(r -> {
			final var frame = r.frame();
			vFrames.row(
					frame.mediaType(),
					frame.streamIndex(),
					frame.keyFrame() ? "1" : "0",
					r.pictType(),
					r.repeatPict() ? "1" : "0",
					frame.pts(),
					frame.ptsTime(),
					frame.pktDts(),
					frame.pktDtsTime(),
					frame.bestEffortTimestamp(),
					frame.bestEffortTimestampTime(),
					frame.pktDuration(),
					frame.pktDurationTime(),
					frame.pktPos(),
					frame.pktSize());
		});
		vFrames.exportToFile(makeOutputFileName(baseFileName, "container-video-frames.txt"), exportDirectory);
	}

	private void savePackets(final ContainerAnalyserResult caResult,
							 final File exportDirectory,
							 final String baseFileName) {
		final var packets = new TabularDocument(this).head(
				"Codec type",
				STREAM_INDEX,
				"Pts",
				PTS_TIME,
				"Dts",
				"Dts time",
				"Duration",
				"Duration time",
				"Size",
				"Pos",
				"Flags");
		caResult.packets().forEach(
				r -> packets.row(
						r.codecType(),
						r.streamIndex(),
						r.pts(),
						r.ptsTime(),
						r.dts(),
						r.dtsTime(),
						r.duration(),
						r.durationTime(),
						r.size(),
						r.pos(),
						r.flags()));
		packets.exportToFile(makeOutputFileName(baseFileName, "container-packets.txt"), exportDirectory);
	}

	private void saveFFprobeJAXB(final FFprobeJAXB ffprobeResult,
								 final File exportDirectory,
								 final String baseFileName) {
		try {
			FileUtils.write(
					new File(exportDirectory, makeOutputFileName(baseFileName, "ffprobe.xml")),
					ffprobeResult.getXmlContent(),
					UTF_8,
					false);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write file", e);
		}
	}
}
