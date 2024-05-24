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

import static java.util.function.Predicate.not;
import static media.mexm.mediadeepa.exportformat.DataGraphic.COLORS_CHANNEL;
import static media.mexm.mediadeepa.exportformat.DataGraphic.STROKES_CHANNEL;
import static media.mexm.mediadeepa.exportformat.report.ReportEntrySubset.toEntrySubset;
import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.AUDIO;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.cli.AppCommand;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.GraphicArtifact;
import media.mexm.mediadeepa.exportformat.RangeAxis;
import media.mexm.mediadeepa.exportformat.TableDocument;
import media.mexm.mediadeepa.exportformat.TabularDocument;
import media.mexm.mediadeepa.exportformat.TabularExportFormat;
import media.mexm.mediadeepa.exportformat.TimedDataGraphic;
import media.mexm.mediadeepa.exportformat.report.NumericUnitValueReportEntry;
import media.mexm.mediadeepa.exportformat.report.ReportDocument;
import media.mexm.mediadeepa.exportformat.report.ReportSection;
import media.mexm.mediadeepa.exportformat.report.StatisticsUnitValueReportEntry;
import media.mexm.mediadeepa.rendererengine.GraphicRendererEngine;
import media.mexm.mediadeepa.rendererengine.MultipleGraphicDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.SingleGraphicMaker;
import media.mexm.mediadeepa.rendererengine.SingleTabularDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.AStatsRendererEngine.AStatReportItem;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMetadataFilterParser;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdAstats;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdAstatsChannel;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdValue;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;

@Component
@Slf4j
public class AStatsRendererEngine implements
								  ReportRendererEngine,
								  TableRendererEngine,
								  TabularRendererEngine,
								  GraphicRendererEngine,
								  ConstStrings,
								  SingleTabularDocumentExporterTraits,
								  MultipleGraphicDocumentExporterTraits<AStatReportItem> {

	@Autowired
	private AppConfig appConfig;
	@Autowired
	private AppCommand appCommand;
	@Autowired
	private NumberUtils numberUtils;

	private List<SingleGraphicMaker<AStatReportItem>> graphicMakerList;

	public static final List<String> HEAD_ASTATS = List.of(
			FRAME, PTS, PTS_TIME,
			CHANNEL,
			DC_OFFSET,
			ENTROPY,
			FLATNESS,
			NOISE_FLOOR,
			NOISE_FLOOR_COUNT,
			PEAK_LEVEL,
			PEAK_COUNT,
			OTHER);

	@Override
	public String getSingleUniqTabularDocumentBaseFileName() {
		return "audio-stats";
	}

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		return result.getMediaAnalyserResult()
				.map(maResult -> {
					final var lavfiMetadatas = maResult.lavfiMetadatas();
					final var aStats = new TabularDocument(tabularExportFormat,
							getSingleUniqTabularDocumentBaseFileName()).head(HEAD_ASTATS);
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
											channel.flatness(),
											channel.noiseFloor(),
											channel.noiseFloorCount(),
											channel.peakLevel(),
											channel.peakCount(),
											other);
								}
							});
					return aStats;
				})
				.stream()
				.toList();
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		result.getMediaAnalyserResult()
				.ifPresent(maResult -> {
					final var aStats = tableDocument.createTable("Audio Stats").head(HEAD_ASTATS);
					maResult.lavfiMetadatas().getAStatsReport()
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
											.addCell(channel.flatness())
											.addCell(channel.noiseFloor())
											.addCell(channel.noiseFloorCount())
											.addCell(channel.peakLevel())
											.addCell(channel.peakCount())
											.addCell(other);
								}
							});
				});
	}

	@Override
	public List<SingleGraphicMaker<AStatReportItem>> getGraphicMakerList() {
		return graphicMakerList;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		graphicMakerList = List.of(
				new DcOffsetGraphicMaker(),
				new EntropyGraphicMaker(),
				new FlatnessGraphicMaker(),
				new NoiseFloorGraphicMaker(),
				new PeakLevelGraphicMaker());
	}

	static record AStatReportItem(List<LavfiMtdValue<LavfiMtdAstats>> aStatsReport, int chCount) {
	}

	@Override
	public Optional<AStatReportItem> makeGraphicReportItem(final DataResult result) {
		return result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getAStatsReport)
				.filter(not(List::isEmpty))
				.map(aStatReport -> {
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

					return new AStatReportItem(aStatReport, chCount);
				});
	}

	class DcOffsetGraphicMaker implements SingleGraphicMaker<AStatReportItem> {

		@Override
		public String getBaseFileName() {
			return appConfig.getGraphicConfig().getDcOffsetGraphicFilename();
		}

		@Override
		public GraphicArtifact makeGraphic(final AStatReportItem item) {
			return new GraphicArtifact(
					getBaseFileName(),
					prepareAStatGraphic(
							item.aStatsReport,
							item.chCount,
							astat -> astat.dcOffset() * 100f,
							"Audio DC offset (%)",
							10).makeLinearAxisGraphic(numberUtils),
					appConfig.getGraphicConfig().getImageSizeHalfSize());
		}
	}

	class EntropyGraphicMaker implements SingleGraphicMaker<AStatReportItem> {

		@Override
		public String getBaseFileName() {
			return appConfig.getGraphicConfig().getEntropyGraphicFilename();
		}

		@Override
		public GraphicArtifact makeGraphic(final AStatReportItem item) {
			return new GraphicArtifact(
					getBaseFileName(),
					prepareAStatGraphic(
							item.aStatsReport,
							item.chCount,
							astat -> astat.entropy() * 100f,
							"Audio entropy (%)",
							10).makeLinearAxisGraphic(numberUtils),
					appConfig.getGraphicConfig().getImageSizeHalfSize());
		}
	}

	class FlatnessGraphicMaker implements SingleGraphicMaker<AStatReportItem> {

		@Override
		public String getBaseFileName() {
			return appConfig.getGraphicConfig().getFlatnessGraphicFilename();
		}

		@Override
		public GraphicArtifact makeGraphic(final AStatReportItem item) {
			return new GraphicArtifact(
					getBaseFileName(),
					prepareAStatGraphic(
							item.aStatsReport,
							item.chCount,
							LavfiMtdAstatsChannel::flatness,
							"Audio flatness",
							1).makeLinearAxisGraphic(numberUtils),
					appConfig.getGraphicConfig().getImageSizeHalfSize());
		}

	}

	class NoiseFloorGraphicMaker implements SingleGraphicMaker<AStatReportItem> {

		@Override
		public String getBaseFileName() {
			return appConfig.getGraphicConfig().getNoiseFloorGraphicFilename();
		}

		@Override
		public GraphicArtifact makeGraphic(final AStatReportItem item) {
			return new GraphicArtifact(
					getBaseFileName(),
					prepareAStatGraphic(
							RangeAxis.createFromValueSet("Audio noise floor (dBFS)",
									-144, 20, 0,
									item.aStatsReport.stream()
											.map(LavfiMtdValue::value)
											.map(LavfiMtdAstats::channels)
											.flatMap(List::stream)
											.map(LavfiMtdAstatsChannel::noiseFloor)),
							item.aStatsReport,
							item.chCount,
							LavfiMtdAstatsChannel::noiseFloor)
									.makeLogarithmicAxisGraphic(numberUtils),
					appConfig.getGraphicConfig().getImageSizeHalfSize());
		}

	}

	class PeakLevelGraphicMaker implements SingleGraphicMaker<AStatReportItem> {

		@Override
		public String getBaseFileName() {
			return appConfig.getGraphicConfig().getPeakLevelGraphicFilename();
		}

		@Override
		public GraphicArtifact makeGraphic(final AStatReportItem item) {
			return new GraphicArtifact(
					getBaseFileName(),
					prepareAStatGraphic(
							RangeAxis.createFromValueSet("Audio audio peak level (dBFS)",
									-96, 10, 5,
									item.aStatsReport.stream()
											.map(LavfiMtdValue::value)
											.map(LavfiMtdAstats::channels)
											.flatMap(List::stream)
											.map(LavfiMtdAstatsChannel::peakLevel)),
							item.aStatsReport,
							item.chCount,
							LavfiMtdAstatsChannel::peakLevel)
									.makeLogarithmicAxisGraphic(numberUtils),
					appConfig.getGraphicConfig().getImageSizeHalfSize());
		}

	}

	private static TimedDataGraphic prepareAStatGraphic(final RangeAxis rangeAxis,
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

	private static TimedDataGraphic prepareAStatGraphic(final List<LavfiMtdValue<LavfiMtdAstats>> aStatReport,
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

	@Override
	public void addToReport(final DataResult result, final ReportDocument document) {
		result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.flatMap(Optional::ofNullable)
				.map(LavfiMetadataFilterParser::getAStatsReport)
				.filter(not(List::isEmpty))
				.ifPresent(aStatsReport -> {
					final var section = new ReportSection(AUDIO, SIGNAL_STATS);
					final var channelCount = aStatsReport.stream()
							.map(LavfiMtdValue::value)
							.map(LavfiMtdAstats::channels)
							.mapToInt(List::size)
							.max()
							.orElse(0);
					if (channelCount == 0) {
						return;
					}

					final var aStatsChannelsValuesList = aStatsReport.stream()
							.map(LavfiMtdValue::value)
							.map(LavfiMtdAstats::channels)
							.toList();

					toEntrySubset(IntStream.range(0, channelCount)
							.mapToObj(chIndex -> StatisticsUnitValueReportEntry.createFromFloat(
									"DC offset channel " + (chIndex + 1),
									aStatsChannelsValuesList.stream()
											.map(f -> f.get(chIndex))
											.map(LavfiMtdAstatsChannel::dcOffset)
											.map(v -> v * 100f),
									"%", numberUtils::formatDecimalFull1En)), section);

					toEntrySubset(IntStream.range(0, channelCount)
							.mapToObj(chIndex -> StatisticsUnitValueReportEntry.createFromFloat(
									"Peak level channel " + (chIndex + 1),
									aStatsChannelsValuesList.stream()
											.map(f -> f.get(chIndex))
											.map(LavfiMtdAstatsChannel::peakLevel),
									DBFS, numberUtils::formatDecimalFull1En)), section);

					toEntrySubset(IntStream.range(0, channelCount)
							.mapToObj(chIndex -> StatisticsUnitValueReportEntry.createFromFloat(
									"Noise floor channel " + (chIndex + 1),
									aStatsChannelsValuesList.stream()
											.map(f -> f.get(chIndex))
											.map(LavfiMtdAstatsChannel::noiseFloor),
									DBFS, numberUtils::formatDecimalFull1En)), section);

					toEntrySubset(IntStream.range(0, channelCount)
							.mapToObj(chIndex -> StatisticsUnitValueReportEntry.createFromFloat(
									"Entropy (complexity) channel " + (chIndex + 1),
									aStatsChannelsValuesList.stream()
											.map(f -> f.get(chIndex))
											.map(LavfiMtdAstatsChannel::entropy)
											.map(v -> v * 100f),
									"%", numberUtils::formatDecimalFull1En)), section);

					toEntrySubset(IntStream.range(0, channelCount)
							.mapToObj(chIndex -> {
								final var list = aStatsChannelsValuesList.stream()
										.map(f -> f.get(chIndex))
										.map(p -> (long) p.peakCount()) /** Workaround for maven compiler bug */
										.toList();
								return new NumericUnitValueReportEntry("Peak count channel " + (chIndex + 1),
										list.get(list.size() - 1),
										SAMPLE_S);
							}), section);

					toEntrySubset(IntStream.range(0, channelCount)
							.mapToObj(chIndex -> {
								final var list = aStatsChannelsValuesList.stream()
										.map(f -> f.get(chIndex))
										.map(p -> (long) p.flatness()) /** Workaround for maven compiler bug */
										.toList();
								return new NumericUnitValueReportEntry("Flatness count channel " + (chIndex + 1),
										list.get(list.size() - 1),
										SAMPLE_S);
							}), section);

					toEntrySubset(IntStream.range(0, channelCount)
							.mapToObj(chIndex -> {
								final var list = aStatsChannelsValuesList.stream()
										.map(f -> f.get(chIndex))
										.map(p -> (long) p.noiseFloorCount()) /** Workaround for maven compiler bug */
										.toList();
								return new NumericUnitValueReportEntry("Noise floor count channel " + (chIndex + 1),
										list.get(list.size() - 1),
										SAMPLE_S);
							}), section);

					addAllGraphicsToReport(this, result, section, appConfig, appCommand);
					document.add(section);
				});
	}

}
