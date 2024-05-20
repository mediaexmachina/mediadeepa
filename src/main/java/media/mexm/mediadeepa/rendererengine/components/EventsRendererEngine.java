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
import static java.awt.Color.GRAY;
import static java.awt.Color.RED;
import static java.awt.Color.YELLOW;
import static media.mexm.mediadeepa.exportformat.DataGraphic.THICK_STROKE;
import static media.mexm.mediadeepa.exportformat.DataGraphic.THIN_STROKE;
import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.AUDIO;
import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.VIDEO;

import java.awt.Color;
import java.awt.Stroke;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.GraphicArtifact;
import media.mexm.mediadeepa.exportformat.RangeAxis;
import media.mexm.mediadeepa.exportformat.TableDocument;
import media.mexm.mediadeepa.exportformat.TabularDocument;
import media.mexm.mediadeepa.exportformat.TabularExportFormat;
import media.mexm.mediadeepa.exportformat.TimedDataGraphic;
import media.mexm.mediadeepa.exportformat.report.EventReportEntry;
import media.mexm.mediadeepa.exportformat.report.ReportDocument;
import media.mexm.mediadeepa.exportformat.report.ReportSection;
import media.mexm.mediadeepa.rendererengine.GraphicRendererEngine;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.SingleGraphicDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.SingleTabularDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMetadataFilterParser;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdEvent;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;
import tv.hd3g.ffprobejaxb.data.FFProbeFormat;

@Component
public class EventsRendererEngine implements
								  ReportRendererEngine,
								  TableRendererEngine,
								  TabularRendererEngine,
								  GraphicRendererEngine,
								  ConstStrings,
								  SingleTabularDocumentExporterTraits,
								  SingleGraphicDocumentExporterTraits {

	@Autowired
	private AppConfig appConfig;
	@Autowired
	private NumberUtils numberUtils;

	public static final List<String> HEAD_EVENTS = List.of(NAME, SCOPE_CHANNEL, START, END, DURATION);

	@Override
	public String getSingleUniqTabularDocumentBaseFileName() {
		return "events";
	}

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		return result.getMediaAnalyserResult()
				.filter(f -> result.getSourceDuration().isPresent())
				.map(maResult -> {
					final var lavfiMetadatas = maResult.lavfiMetadatas();
					final var sourceDuration = result.getSourceDuration().get();
					final var events = new TabularDocument(tabularExportFormat,
							getSingleUniqTabularDocumentBaseFileName()).head(HEAD_EVENTS);
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
					return events;
				})
				.stream()
				.toList();
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		if (result.getSourceDuration().isEmpty()) {
			return;
		}
		final var sourceDuration = result.getSourceDuration().get();// NOSONAR S3655
		result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.ifPresent(lavfiMetadatas -> {
					final var events = tableDocument.createTable("Events").head(HEAD_EVENTS);
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
				});
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

	@Override
	public List<GraphicArtifact> toGraphic(final DataResult result) {
		final var eventsReportCount = result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getEventCount)
				.orElse(0);

		final var secDurationFloat = result.getFFprobeResult()
				.flatMap(FFprobeJAXB::getFormat)
				.map(FFProbeFormat::duration)
				.orElse(0f);
		if (eventsReportCount == 0 || secDurationFloat < 1f) {
			return List.of();
		}

		final var secDurationRoundedInt = (int) Math.round(Math.floor(secDurationFloat));
		final var dataGraphic = TimedDataGraphic.create(
				IntStream.range(0, secDurationRoundedInt)
						.mapToObj(f -> (float) f),
				new RangeAxis("Events", 0, 1));

		addSeriesFromEvent(result, LavfiMetadataFilterParser::getBlackEvents, secDurationRoundedInt,
				FULL_BLACK_FRAMES, BLUE, THIN_STROKE, dataGraphic);
		addSeriesFromEvent(result, LavfiMetadataFilterParser::getSilenceEvents, secDurationRoundedInt,
				AUDIO_SILENCE, GRAY, THICK_STROKE, dataGraphic);
		addSeriesFromEvent(result, LavfiMetadataFilterParser::getFreezeEvents, secDurationRoundedInt,
				FREEZE_FRAMES, RED, THICK_STROKE, dataGraphic);
		addSeriesFromEvent(result, LavfiMetadataFilterParser::getMonoEvents, secDurationRoundedInt,
				AUDIO_MONO, YELLOW, THIN_STROKE, dataGraphic);

		return List.of(new GraphicArtifact(
				getSingleUniqGraphicBaseFileName(),
				dataGraphic.makeLinearAxisGraphic(numberUtils),
				appConfig.getGraphicConfig().getImageSizeHalfSize()));
	}

	@Override
	public String getSingleUniqGraphicBaseFileName() {
		return appConfig.getGraphicConfig().getEventsGraphicFilename();
	}

	@Override
	public void addToReport(final DataResult result, final ReportDocument document) {
		if (result.getSourceDuration().isEmpty()) {
			return;
		}
		result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.ifPresent(lavfiMetadatas -> {
					final var duration = result.getSourceDuration();
					saveEvents(AUDIO_SILENCE, lavfiMetadatas.getSilenceEvents(), true, document, duration);
					saveEvents(AUDIO_MONO, lavfiMetadatas.getMonoEvents(), true, document, duration);
					saveEvents(BLACK_FRAMES, lavfiMetadatas.getBlackEvents(), false, document, duration);
					saveEvents(FREEZE_STATIC_FRAMES, lavfiMetadatas.getFreezeEvents(), false, document, duration);
				});
	}

	private void saveEvents(final String name,
							final List<LavfiMtdEvent> events,
							final boolean audio,
							final ReportDocument document,
							final Optional<Duration> sourceDuration) {
		if (events.isEmpty()) {
			return;
		}
		final var section = new ReportSection(audio ? AUDIO : VIDEO, name + " events");
		Stream.concat(
				Stream.of(EventReportEntry.createHeader(events, sourceDuration)),
				events.stream()
						.map(event -> new EventReportEntry(event, sourceDuration)))
				.forEach(section::add);
		document.add(section);
	}

}
