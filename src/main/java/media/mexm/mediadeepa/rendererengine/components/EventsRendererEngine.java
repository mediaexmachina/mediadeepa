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
import static media.mexm.mediadeepa.exportformat.report.EventReportEntry.haveEnd;

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
import media.mexm.mediadeepa.exportformat.report.EventReportEntry;
import media.mexm.mediadeepa.exportformat.report.EventReportEntry.EventReportEntryHeader;
import media.mexm.mediadeepa.exportformat.report.ReportDocument;
import media.mexm.mediadeepa.exportformat.report.ReportSection;
import media.mexm.mediadeepa.exportformat.report.ReportSectionCategory;
import media.mexm.mediadeepa.rendererengine.GraphicRendererEngine;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.SingleGraphicDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.SingleTabularDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMetadataFilterParser;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdEvent;
import tv.hd3g.fflauncher.recipes.MediaAnalyserProcessResult;
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

	private static final String EVENTS2 = "Events";
	@Autowired
	private AppConfig appConfig;
	@Autowired
	private AppCommand appCommand;
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
		return result.getMediaAnalyserProcessResult()
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
		result.getMediaAnalyserProcessResult()
				.map(MediaAnalyserProcessResult::lavfiMetadatas)
				.ifPresent(lavfiMetadatas -> {
					final var events = tableDocument.createTable(EVENTS2).head(HEAD_EVENTS);
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
		final var events = result.getMediaAnalyserProcessResult()
				.map(MediaAnalyserProcessResult::lavfiMetadatas)
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
		final var eventsReportCount = result.getMediaAnalyserProcessResult()
				.map(MediaAnalyserProcessResult::lavfiMetadatas)
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
				new RangeAxis(EVENTS2, 0, 1));

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
		result.getMediaAnalyserProcessResult()
				.map(MediaAnalyserProcessResult::lavfiMetadatas)
				.ifPresent(lavfiMetadatas -> {
					final var duration = result.getSourceDuration();
					final var section = new ReportSection(ReportSectionCategory.EVENTS, EVENTS2);

					final var tEvents = Stream.of(
							Event.getEvents(AUDIO_SILENCE, lavfiMetadatas.getSilenceEvents()),
							Event.getEvents(AUDIO_MONO, lavfiMetadatas.getMonoEvents()),
							Event.getEvents(BLACK_FRAMES, lavfiMetadatas.getBlackEvents()),
							Event.getEvents(FREEZE_STATIC_FRAMES, lavfiMetadatas.getFreezeEvents()))
							.flatMap(Function.identity())
							.sorted()
							.toList();

					final var someHaveEnd = tEvents.stream()
							.map(Event::mtdEvent)
							.anyMatch(event -> haveEnd(event, duration));
					final var someHaveScope = tEvents.stream()
							.map(Event::mtdEvent)
							.anyMatch(EventReportEntry::haveScope);

					final var allEvents = tEvents.stream()
							.map(event -> event.toEventReportEntry(duration, someHaveEnd, someHaveScope))
							.toList();

					Stream.concat(
							Stream.of(new EventReportEntryHeader(someHaveScope, someHaveEnd)),
							allEvents.stream())
							.forEach(section::add);

					addAllGraphicsToReport(this, result, section, appConfig, appCommand);
					document.add(section);
				});
	}

	private record Event(String type, LavfiMtdEvent mtdEvent) implements Comparable<Event> {

		@Override
		public int compareTo(final Event o) {
			return mtdEvent.compareTo(o.mtdEvent);
		}

		static Stream<Event> getEvents(final String type, final List<LavfiMtdEvent> mtdEvent) {
			return mtdEvent.stream().map(event -> new Event(type, event));
		}

		EventReportEntry toEventReportEntry(final Optional<Duration> duration,
											final boolean someHaveEnd,
											final boolean someHaveScope) {
			return new EventReportEntry(type, mtdEvent, duration, someHaveEnd, someHaveScope);
		}

	}

}
