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
package media.mexm.mediadeepa.exportformat.tables;

import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.HEAD_ABOUT;
import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.HEAD_ACONSTS;
import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.HEAD_AFRAMES;
import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.HEAD_APHASE;
import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.HEAD_ASTATS;
import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.HEAD_BLOCK;
import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.HEAD_BLUR;
import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.HEAD_CONTAINER_PACKETS;
import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.HEAD_CROP;
import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.HEAD_EBUR128;
import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.HEAD_EBUR128_SUMMARY;
import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.HEAD_EVENTS;
import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.HEAD_GOPSTATS;
import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.HEAD_IDET;
import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.HEAD_MEDIA_SUMMARY;
import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.HEAD_SITI;
import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.HEAD_SITI_REPORT;
import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.HEAD_STDERRFILTERS;
import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.HEAD_VCONSTS;
import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.HEAD_VFRAMES;
import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.SPATIAL_INFO;
import static media.mexm.mediadeepa.exportformat.tabular.TabularExportFormat.TEMPORAL_INFO;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormat;
import media.mexm.mediadeepa.exportformat.tables.TableDocument.Table;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMetadataFilterParser;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserResult;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.Ebur128Summary;
import tv.hd3g.fflauncher.resultparser.RawStdErrFilterEvent;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;
import tv.hd3g.ffprobejaxb.MediaSummary;

public abstract class TableExportFormat implements ExportFormat {

	@Override
	public void exportResult(final DataResult result, final File exportDirectory, final String baseFileName) {
		final var tableDocument = new TableDocument();

		result.getFFprobeResult()
				.map(FFprobeJAXB::getMediaSummary)
				.ifPresent(ms -> saveMediaSummary(tableDocument, ms));

		result.getMediaAnalyserResult()
				.ifPresent(maResult -> {
					Optional.ofNullable(maResult.ebur128Summary())
							.ifPresent(ebu -> saveEbur128Summary(tableDocument, ebu));

					final var lavfiMetadatas = maResult.lavfiMetadatas();

					saveAPhaseMeter(tableDocument, lavfiMetadatas);
					saveAStats(tableDocument, lavfiMetadatas);
					saveSITI(tableDocument, lavfiMetadatas);
					saveSITIReport(tableDocument, lavfiMetadatas);
					saveBlock(tableDocument, lavfiMetadatas);
					saveBlur(tableDocument, lavfiMetadatas);
					saveCrop(tableDocument, lavfiMetadatas);
					saveIdet(tableDocument, lavfiMetadatas);
					saveAboutMeasure(maResult, tableDocument);
					result.getSourceDuration()
							.ifPresent(sourceDuration -> saveEvents(
									sourceDuration, tableDocument, lavfiMetadatas));
				});

		saveEbur128(result.getEbur128events(), tableDocument);
		saveRawstderrfilters(result.getRawStdErrEvents(), tableDocument);

		result.getContainerAnalyserResult()
				.ifPresent(caResult -> {
					savePackets(caResult, tableDocument);
					saveVFrames(caResult, tableDocument);
					saveAFrames(caResult, tableDocument);
					saveVConsts(caResult, tableDocument);
					saveAConsts(caResult, tableDocument);
					saveGopStats(caResult, tableDocument);
				});

		save(result, tableDocument.getTables(), exportDirectory, baseFileName);
	}

	public abstract void save(DataResult result, List<Table> tables, File exportDirectory, String baseFileName);

	private void saveMediaSummary(final TableDocument doc, final MediaSummary mediaSummary) {
		final var t = doc.createTable("Media summary").head(HEAD_MEDIA_SUMMARY);
		mediaSummary.streams().forEach(s -> t.addRow().addCell("Stream").addCell(s));
		t.addRow().addCell("Format").addCell(mediaSummary.format());
	}

	private void saveEbur128Summary(final TableDocument doc, final Ebur128Summary ebu) {
		final var t = doc.createTable("EBU R 128 Summary").head(HEAD_EBUR128_SUMMARY);
		t.addRow()
				.addCell(ebu.getIntegrated())
				.addCell(ebu.getIntegratedThreshold())
				.addCell(ebu.getLoudnessRange())
				.addCell(ebu.getLoudnessRangeThreshold())
				.addCell(ebu.getLoudnessRangeLow())
				.addCell(ebu.getLoudnessRangeHigh())
				.addCell(ebu.getSamplePeak())
				.addCell(ebu.getTruePeak());
	}

	private void saveSITIReport(final TableDocument doc,
								final LavfiMetadataFilterParser lavfiMetadatas) {
		if (lavfiMetadatas.getSitiReport().isEmpty() == false) {
			final var sitiStats = doc.createTable("SITI Stats").head(HEAD_SITI_REPORT);
			final var stats = lavfiMetadatas.computeSitiStats();
			sitiStats
					.addRow()
					.addCell(SPATIAL_INFO)
					.addCell(stats.si().getAverage())
					.addCell(stats.si().getMax())
					.addCell(stats.si().getMin());
			sitiStats.addRow()
					.addCell(TEMPORAL_INFO)
					.addCell(stats.ti().getAverage())
					.addCell(stats.ti().getMax())
					.addCell(stats.ti().getMin());
		}
	}

	private void saveSITI(final TableDocument doc,
						  final LavfiMetadataFilterParser lavfiMetadatas) {
		final var siti = doc.createTable("SITI").head(HEAD_SITI);
		lavfiMetadatas.getSitiReport()
				.forEach(a -> siti.addRow()
						.addCell(a.frame())
						.addCell(a.pts())
						.addCell(a.ptsTime())
						.addCell(a.value().si())
						.addCell(a.value().ti()));
	}

	private void saveAStats(final TableDocument doc,
							final LavfiMetadataFilterParser lavfiMetadatas) {
		final var aStats = doc.createTable("Audio Stats").head(HEAD_ASTATS);
		lavfiMetadatas.getAStatsReport()
				.forEach(a -> {
					final var channels = a.value().channels();
					for (var pos = 0; pos < channels.size(); pos++) {
						final var channel = channels.get(pos);
						var other = channel.other().toString();
						if (other.equals("{}")) {
							other = "";
						}
						aStats.addRow()
								.addCell(a.frame())
								.addCell(a.pts())
								.addCell(a.ptsTime())
								.addCell(pos + 1)
								.addCell(channel.dcOffset())
								.addCell(channel.entropy())
								.addCell(channel.flatFactor())
								.addCell(channel.noiseFloor())
								.addCell(channel.noiseFloorCount())
								.addCell(channel.peakLevel())
								.addCell(channel.peakCount())
								.addCell(other);
					}
				});
	}

	private void saveBlock(final TableDocument doc,
						   final LavfiMetadataFilterParser lavfiMetadatas) {
		final var block = doc.createTable("Block detect").head(HEAD_BLOCK);
		lavfiMetadatas.getBlockDetectReport()
				.forEach(a -> block.addRow()
						.addCell(a.frame())
						.addCell(a.pts())
						.addCell(a.ptsTime())
						.addCell(a.value()));
	}

	private void saveBlur(final TableDocument doc,
						  final LavfiMetadataFilterParser lavfiMetadatas) {
		final var blur = doc.createTable("Blur detect").head(HEAD_BLUR);
		lavfiMetadatas.getBlurDetectReport()
				.forEach(a -> blur.addRow()
						.addCell(a.frame())
						.addCell(a.pts())
						.addCell(a.ptsTime())
						.addCell(a.value()));
	}

	private void saveCrop(final TableDocument doc,
						  final LavfiMetadataFilterParser lavfiMetadatas) {
		final var crop = doc.createTable("Crop detect").head(HEAD_CROP);
		lavfiMetadatas.getCropDetectReport().forEach(a -> {
			final var value = a.value();
			crop.addRow()
					.addCell(a.frame())
					.addCell(a.pts())
					.addCell(a.ptsTime())
					.addCell(value.x1()).addCell(value.x2()).addCell(value.y1()).addCell(value.y2())
					.addCell(value.w()).addCell(value.h()).addCell(value.x()).addCell(value.y());
		});
	}

	private void saveIdet(final TableDocument doc,
						  final LavfiMetadataFilterParser lavfiMetadatas) {
		final var idet = doc.createTable("Interlace detect").head(HEAD_IDET);
		lavfiMetadatas.getIdetReport()
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
	}

	private void saveEvents(final Duration sourceDuration,
							final TableDocument doc,
							final LavfiMetadataFilterParser lavfiMetadatas) {
		final var events = doc.createTable("Events").head(HEAD_EVENTS);
		Stream.of(
				lavfiMetadatas.getMonoEvents(),
				lavfiMetadatas.getSilenceEvents(),
				lavfiMetadatas.getBlackEvents(),
				lavfiMetadatas.getFreezeEvents())
				.flatMap(List::stream)
				.sorted()
				.forEach(ev -> events.addRow()
						.addCell(ev.name())
						.addCell(ev.scope())
						.addCell(ev.start())
						.addCell(ev.getEndOr(sourceDuration))
						.addCell(ev.getEndOr(sourceDuration).minus(ev.start())));
	}

	private void saveAboutMeasure(final MediaAnalyserResult maResult,
								  final TableDocument doc) {
		final var aboutMeasure = doc.createTable("Filters").head(HEAD_ABOUT);
		maResult.filters()
				.forEach(f -> aboutMeasure.addRow()
						.addCell(f.type())
						.addCell(f.name())
						.addCell(f.setup())
						.addCell(f.className()));
	}

	private void saveAPhaseMeter(final TableDocument doc,
								 final LavfiMetadataFilterParser lavfiMetadatas) {
		final var aPhaseMeter = doc.createTable("Audio phase").head(HEAD_APHASE);
		lavfiMetadatas.getAPhaseMeterReport()
				.forEach(a -> aPhaseMeter.addRow()
						.addCell(a.frame())
						.addCell(a.pts())
						.addCell(a.ptsTime())
						.addCell(a.value()));
	}

	private void saveEbur128(final List<Ebur128StrErrFilterEvent> ebur128events,
							 final TableDocument doc) {
		final var t = doc.createTable("EBU R 128").head(HEAD_EBUR128);
		ebur128events.forEach(ebu -> t.addRow()
				.addCell(ebu.getT())
				.addCell(ebu.getI())
				.addCell(ebu.getM())
				.addCell(ebu.getS())
				.addCell(ebu.getLra())
				.addCell(ebu.getSpk().left())
				.addCell(ebu.getSpk().right())
				.addCell(ebu.getFtpk().left())
				.addCell(ebu.getFtpk().right())
				.addCell(ebu.getTpk().left())
				.addCell(ebu.getTpk().right()));
	}

	private void saveRawstderrfilters(final List<RawStdErrFilterEvent> rawStdErrEvents,
									  final TableDocument doc) {
		final var t = doc.createTable("rawstderrfilters").head(HEAD_STDERRFILTERS);
		rawStdErrEvents.stream()
				.filter(r -> r.getFilterName().equals("cropdetect") == false)
				.forEach(r -> t.addRow()
						.addCell(r.getFilterName())
						.addCell(r.getFilterChainPos())
						.addCell(r.getLineValue()));
	}

	private void saveGopStats(final ContainerAnalyserResult caResult,
							  final TableDocument doc) {
		final var gopStats = doc.createTable("Container GOP").head(HEAD_GOPSTATS);
		caResult.extractGOPStats()
				.forEach(f -> gopStats.addRow()
						.addCell(f.gopFrameCount())
						.addCell(f.pFramesCount())
						.addCell(f.bFramesCount())
						.addCell(f.gopDataSize())
						.addCell(f.iFrameDataSize())
						.addCell(f.pFramesDataSize())
						.addCell(f.bFramesDataSize()));
	}

	private void saveAConsts(final ContainerAnalyserResult caResult,
							 final TableDocument doc) {
		final var aConsts = doc.createTable("Container audio consts").head(HEAD_ACONSTS);
		Stream.concat(
				caResult.olderAudioConsts().stream(),
				Stream.of(caResult.audioConst()))
				.forEach(c -> {
					final var frame = c.updatedWith().frame();
					aConsts.addRow()
							.addOptionalToString(c.channelLayout())
							.addCell(c.channels())
							.addCell(c.sampleFmt())
							.addCell(frame.pts())
							.addCell(frame.ptsTime())
							.addCell(frame.pktDts())
							.addCell(frame.pktDtsTime())
							.addCell(frame.bestEffortTimestamp())
							.addCell(frame.bestEffortTimestampTime())
							.addCell(frame.pktDuration())
							.addCell(frame.pktDurationTime())
							.addCell(frame.pktPos());
				});
	}

	private void saveVConsts(final ContainerAnalyserResult caResult,
							 final TableDocument doc) {
		final var vConsts = doc.createTable("Container video consts").head(HEAD_VCONSTS);
		Stream.concat(
				caResult.olderVideoConsts().stream(),
				Stream.of(caResult.videoConst()))
				.filter(Objects::nonNull)
				.forEach(c -> {
					final var frame = c.updatedWith().frame();
					vConsts.addRow()
							.addCell(c.width())
							.addCell(c.height())
							.addCell(c.sampleAspectRatio())
							.addCell(c.topFieldFirst() ? 1 : 0)
							.addCell(c.interlacedFrame() ? 1 : 0)
							.addCell(c.pixFmt())
							.addCell(c.colorRange())
							.addCell(c.colorPrimaries())
							.addCell(c.colorTransfer())
							.addCell(c.colorSpace())
							.addCell(c.codedPictureNumber())
							.addCell(c.displayPictureNumber())
							.addCell(frame.pts())
							.addCell(frame.ptsTime())
							.addCell(frame.pktDts())
							.addCell(frame.pktDtsTime())
							.addCell(frame.bestEffortTimestamp())
							.addCell(frame.bestEffortTimestampTime())
							.addCell(frame.pktDuration())
							.addCell(frame.pktDurationTime())
							.addCell(frame.pktPos());
				});
	}

	private void saveAFrames(final ContainerAnalyserResult caResult,
							 final TableDocument doc) {
		final var aFrames = doc.createTable("Container audio frames").head(HEAD_AFRAMES);

		caResult.audioFrames().forEach(r -> {
			final var frame = r.frame();
			aFrames.addRow()
					.addOptionalToString(frame.mediaType())
					.addCell(frame.streamIndex())
					.addCell(r.nbSamples())
					.addCell(frame.pts())
					.addCell(frame.ptsTime())
					.addCell(frame.pktDts())
					.addCell(frame.pktDtsTime())
					.addCell(frame.bestEffortTimestamp())
					.addCell(frame.bestEffortTimestampTime())
					.addCell(frame.pktDuration())
					.addCell(frame.pktDurationTime())
					.addCell(frame.pktPos())
					.addCell(frame.pktSize());
		});
	}

	private void saveVFrames(final ContainerAnalyserResult caResult,
							 final TableDocument doc) {
		final var vFrames = doc.createTable("Container video frames").head(HEAD_VFRAMES);

		caResult.videoFrames().forEach(r -> {
			final var frame = r.frame();
			vFrames.addRow()
					.addOptionalToString(frame.mediaType())
					.addCell(frame.streamIndex())
					.addCell(frame.keyFrame() ? 1 : 0)
					.addOptionalToString(r.pictType())
					.addCell(r.repeatPict() ? 1 : 0)
					.addCell(frame.pts())
					.addCell(frame.ptsTime())
					.addCell(frame.pktDts())
					.addCell(frame.pktDtsTime())
					.addCell(frame.bestEffortTimestamp())
					.addCell(frame.bestEffortTimestampTime())
					.addCell(frame.pktDuration())
					.addCell(frame.pktDurationTime())
					.addCell(frame.pktPos())
					.addCell(frame.pktSize());
		});
	}

	private void savePackets(final ContainerAnalyserResult caResult,
							 final TableDocument doc) {
		final var packets = doc.createTable("Container packets").head(HEAD_CONTAINER_PACKETS);
		caResult.packets().forEach(
				r -> packets.addRow()
						.addOptionalToString(r.codecType())
						.addCell(r.streamIndex())
						.addCell(r.pts())
						.addCell(r.ptsTime())
						.addCell(r.dts())
						.addCell(r.dtsTime())
						.addCell(r.duration())
						.addCell(r.durationTime())
						.addCell(r.size())
						.addCell(r.pos())
						.addCell(r.flags()));
	}

}
