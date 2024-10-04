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
import static java.util.function.UnaryOperator.identity;
import static media.mexm.mediadeepa.exportformat.DataGraphic.COLORS_CHANNEL;
import static media.mexm.mediadeepa.exportformat.DataGraphic.STROKES_CHANNEL;
import static media.mexm.mediadeepa.exportformat.report.ReportEntrySubset.toEntrySubset;
import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.AUDIO;
import static tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdAstatsChannel.getFieldNames;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
import tv.hd3g.fflauncher.recipes.MediaAnalyserProcessResult;

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

	@Override
	public String getSingleUniqTabularDocumentBaseFileName() {
		return "audio-stats";
	}

	private static String makeFieldName(final String field) {
		final var sb = new StringBuilder();
		for (var pos = 0; pos < field.length(); pos++) {
			final var chr = field.substring(pos, pos + 1);
			if (pos == 0) {
				sb.append(chr.toUpperCase());
			} else if (pos + 1 == field.length() || chr.equals(chr.toUpperCase()) == false) {
				sb.append(chr);
			} else {
				sb.append(" ");
				sb.append(chr);
			}
		}
		return sb.toString();
	}

	private static final List<String> HEAD_ASTATS = Stream.concat(
			Stream.of(FRAME, PTS, PTS_TIME, CHANNEL),
			getFieldNames(AStatsRendererEngine::makeFieldName))
			.toList();

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		return result.getMediaAnalyserProcessResult()
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
									final var items = Stream.concat(
											Stream.concat(
													Stream.of(a.frame(), a.pts(), a.ptsTime(), pos + 1)
															.map(f -> (Object) f),
													channel.getValues().map(f -> (Object) f)),
											Stream.of(other).map(f -> (Object) f))
											.toList()
											.toArray();

									aStats.row(items);
								}
							});
					return aStats;
				})
				.stream()
				.toList();
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		result.getMediaAnalyserProcessResult()
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

									final var row = aStats.addRow()
											.addCell(a.frame())
											.addCell(a.pts())
											.addCell(a.ptsTime())
											.addCell(pos + 1);

									channel.getValues().forEach(row::addCell);
									row.addCell(other);
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
				new RMSLevelGraphicMaker(),
				new DcOffsetGraphicMaker(),
				new EntropyGraphicMaker(),
				new FlatnessGraphicMaker(),
				new NoiseFloorGraphicMaker(),
				new PeakLevelGraphicMaker(),
				new DynamicRangeGraphicMaker());
	}

	static record AStatReportItem(List<LavfiMtdValue<LavfiMtdAstats>> aStatsReport, int chCount) {
	}

	@Override
	public Optional<AStatReportItem> makeGraphicReportItem(final DataResult result) {
		return result.getMediaAnalyserProcessResult()
				.map(MediaAnalyserProcessResult::lavfiMetadatas)
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

	class DynamicRangeGraphicMaker implements SingleGraphicMaker<AStatReportItem> {

		@Override
		public String getBaseFileName() {
			return appConfig.getGraphicConfig().getDynamicRangeGraphicFilename();
		}

		@Override
		public GraphicArtifact makeGraphic(final AStatReportItem item) {
			return new GraphicArtifact(
					getBaseFileName(),
					prepareAStatGraphic(
							RangeAxis.createFromValueSet("Dynamic range (dBFS)",
									-96, 10, 5,
									item.aStatsReport.stream()
											.map(LavfiMtdValue::value)
											.map(LavfiMtdAstats::channels)
											.flatMap(List::stream)
											.map(LavfiMtdAstatsChannel::dynamicRange)),
							item.aStatsReport,
							item.chCount,
							LavfiMtdAstatsChannel::dynamicRange)
									.makeLogarithmicAxisGraphic(numberUtils),
					appConfig.getGraphicConfig().getImageSizeHalfSize());
		}

	}

	class RMSLevelGraphicMaker implements SingleGraphicMaker<AStatReportItem> {

		@Override
		public String getBaseFileName() {
			return appConfig.getGraphicConfig().getRmsLevelGraphicFilename();
		}

		@Override
		public GraphicArtifact makeGraphic(final AStatReportItem item) {
			return new GraphicArtifact(
					getBaseFileName(),
					prepareAStatGraphic(
							RangeAxis.createFromValueSet("RMS Level (dBFS)",
									-96, 10, 5,
									item.aStatsReport.stream()
											.map(LavfiMtdValue::value)
											.map(LavfiMtdAstats::channels)
											.flatMap(List::stream)
											.map(LavfiMtdAstatsChannel::rmsLevel)),
							item.aStatsReport,
							item.chCount,
							LavfiMtdAstatsChannel::rmsLevel)
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
		result.getMediaAnalyserProcessResult()
				.map(MediaAnalyserProcessResult::lavfiMetadatas)
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

					final var c = new ReportConsts(
							channelCount,
							aStatsReport.stream()
									.map(LavfiMtdValue::value)
									.map(LavfiMtdAstats::channels)
									.toList(),
							section);

					addStatValuesToReport(
							LavfiMtdAstatsChannel::rmsLevel, identity(),
							"RMS Level channel", DBFS, c);

					addStatValuesToReport(
							LavfiMtdAstatsChannel::peakLevel, identity(),
							"Peak level channel", DBFS, c);

					addStatValuesToReport(
							LavfiMtdAstatsChannel::noiseFloor, identity(),
							"Noise floor channel", DBFS, c);

					addStatValuesToReport(
							LavfiMtdAstatsChannel::dynamicRange, identity(),
							"Dynamic Range channel", DBFS, c);

					addStatValuesToReport(
							LavfiMtdAstatsChannel::dcOffset, v -> v * 100f,
							"DC offset channel", "%", c);

					addStatValuesToReport(
							LavfiMtdAstatsChannel::entropy, v -> v * 100f,
							"Entropy (complexity) channel", "%", c);

					addStatValuesToReport(
							LavfiMtdAstatsChannel::crestFactor, identity(),
							"Crest Factor channel", "", c);

					addStatCountToReport(
							LavfiMtdAstatsChannel::peakCount,
							"Peak count channel", SAMPLE_S, c);

					addStatCountToReport(
							LavfiMtdAstatsChannel::flatness,
							"Flatness count channel", SAMPLE_S, c);

					addStatCountToReport(
							u -> (long) u.bitDepth(),
							"Bit depth channel", "bits", c);

					addStatCountToReport(
							LavfiMtdAstatsChannel::noiseFloorCount,
							"Noise floor count channel", SAMPLE_S, c);

					addStatCountToReport(
							LavfiMtdAstatsChannel::numberOfInfs,
							"Number Of Infs channel", SAMPLE_S, c);

					addStatCountToReport(
							LavfiMtdAstatsChannel::numberOfNaNs,
							"Number Of Na Ns", SAMPLE_S, c);

					addStatCountToReport(
							LavfiMtdAstatsChannel::numberOfDenormals,
							"Number Of Denormals", SAMPLE_S, c);

					addStatCountToReport(
							LavfiMtdAstatsChannel::noiseFloorCount,
							"Noise floor count channel", SAMPLE_S, c);

					addAllGraphicsToReport(result, section, appConfig, appCommand);
					document.add(section);
				});
	}

	private record ReportConsts(int channelCount,
								List<List<LavfiMtdAstatsChannel>> aStatsChannelsValuesList,
								ReportSection section) {
	}

	private void addStatValuesToReport(final Function<LavfiMtdAstatsChannel, Float> valueSelector,
									   final UnaryOperator<Float> transformator,
									   final String label,
									   final String unit,
									   final ReportConsts consts) {
		toEntrySubset(IntStream.range(0, consts.channelCount)
				.mapToObj(chIndex -> StatisticsUnitValueReportEntry.createFromFloat(
						label + " " + (chIndex + 1),
						consts.aStatsChannelsValuesList.stream()
								.map(f -> f.get(chIndex))
								.map(valueSelector)
								.map(transformator),
						unit, numberUtils::formatDecimalFull1En)), consts.section);
	}

	private void addStatCountToReport(final Function<LavfiMtdAstatsChannel, Long> valueSelector,
									  final String label,
									  final String unit,
									  final ReportConsts consts) {
		toEntrySubset(IntStream.range(0, consts.channelCount)
				.mapToObj(chIndex -> {
					final var list = consts.aStatsChannelsValuesList.stream()
							.map(f -> f.get(chIndex))
							.map(valueSelector)
							.map(p -> (long) p) /** Workaround for maven compiler bug */
							.toList();
					final var result = list.get(list.size() - 1);
					if (result == 0l) {
						return new NumericUnitValueReportEntry("", null, "");
					}
					return new NumericUnitValueReportEntry(
							label + " " + (chIndex + 1),
							result,
							unit);
				}), consts.section);
	}

}
