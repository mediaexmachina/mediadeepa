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
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static media.mexm.mediadeepa.exportformat.DataGraphic.THICK_STROKE;
import static media.mexm.mediadeepa.exportformat.DataGraphic.THIN_STROKE;

import java.awt.Color;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jfree.data.time.FixedMillisecond;
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
import media.mexm.mediadeepa.rendererengine.GraphicRendererEngine;
import media.mexm.mediadeepa.rendererengine.MultipleGraphicDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.SingleGraphicMaker;
import media.mexm.mediadeepa.rendererengine.SingleTabularDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.Ebur128RendererEngine.EBUR128ReportItem;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.Ebur128Summary;
import tv.hd3g.fflauncher.resultparser.Stereo;

@Component
public class Ebur128RendererEngine implements
								   TableRendererEngine,
								   TabularRendererEngine,
								   GraphicRendererEngine,
								   ConstStrings,
								   SingleTabularDocumentExporterTraits,
								   MultipleGraphicDocumentExporterTraits<EBUR128ReportItem> {

	@Autowired
	private AppConfig appConfig;
	@Autowired
	private NumberUtils numberUtils;

	private List<SingleGraphicMaker<EBUR128ReportItem>> graphicMakerList;

	public static final List<String> HEAD_EBUR128 = List.of(
			POSITION,
			INTEGRATED,
			MOMENTARY,
			SHORT_TERM,
			LOUDNESS_RANGE,
			SAMPLE_PEAK_L, SAMPLE_PEAK_R,
			TRUE_PEAK_PER_FRAME_L, TRUE_PEAK_PER_FRAME_R,
			TRUE_PEAK_L, TRUE_PEAK_R);

	@Override
	public String getSingleUniqTabularDocumentBaseFileName() {
		return "audio-ebur128";
	}

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		final var t = new TabularDocument(tabularExportFormat, getSingleUniqTabularDocumentBaseFileName()).head(
				HEAD_EBUR128);
		result.getEbur128events()
				.forEach(ebu -> t.row(
						tabularExportFormat.formatNumberLowPrecision(ebu.getT()),
						tabularExportFormat.formatNumberLowPrecision(ebu.getI()),
						tabularExportFormat.formatNumberLowPrecision(ebu.getM()),
						tabularExportFormat.formatNumberLowPrecision(ebu.getS()),
						tabularExportFormat.formatNumberLowPrecision(ebu.getLra()),
						tabularExportFormat.formatNumberLowPrecision(ebu.getSpk().left()),
						tabularExportFormat.formatNumberLowPrecision(ebu.getSpk().right()),
						tabularExportFormat.formatNumberLowPrecision(ebu.getFtpk().left()),
						tabularExportFormat.formatNumberLowPrecision(ebu.getFtpk().right()),
						tabularExportFormat.formatNumberLowPrecision(ebu.getTpk().left()),
						tabularExportFormat.formatNumberLowPrecision(ebu.getTpk().right())));
		return List.of(t);
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		final var t = tableDocument.createTable("EBU R 128").head(HEAD_EBUR128);
		result.getEbur128events().forEach(ebu -> t.addRow()
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

	static record EBUR128ReportItem(
									List<Ebur128StrErrFilterEvent> r128events,
									Optional<Ebur128Summary> oEbur128Sum,
									List<FixedMillisecond> positions) {

		static EBUR128ReportItem make(final List<Ebur128StrErrFilterEvent> r128events,
									  final Optional<Ebur128Summary> oEbur128Sum) {
			final var positions = r128events.stream()
					.map(Ebur128StrErrFilterEvent::getT)
					.map(t -> t * 1000f)
					.map(Math::ceil)
					.map(Math::round)
					.map(FixedMillisecond::new)
					.toList();

			return new EBUR128ReportItem(r128events, oEbur128Sum, positions);
		}

	}

	@Override
	public void afterPropertiesSet() throws Exception {
		graphicMakerList = List.of(new LUFSGraphicMaker(), new TPKGraphicMaker());
	}

	@Override
	public List<SingleGraphicMaker<EBUR128ReportItem>> getGraphicMakerList() {
		return graphicMakerList;
	}

	@Override
	public Optional<EBUR128ReportItem> makeGraphicReportItem(final DataResult result) {
		final var r128events = result.getEbur128events();
		if (r128events.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(EBUR128ReportItem.make(
				r128events,
				result.getMediaAnalyserResult().map(MediaAnalyserResult::ebur128Summary)));
	}

	class LUFSGraphicMaker implements SingleGraphicMaker<EBUR128ReportItem> {

		@Override
		public String getBaseFileName() {
			return appConfig.getGraphicConfig().getLufsGraphicFilename();
		}

		@Override
		public GraphicArtifact makeGraphic(final EBUR128ReportItem item) {
			final var dataGraphicLUFS = new TimedDataGraphic(
					item.positions,
					RangeAxis.createFromValueSet("dB LU", -40, 10, 1,
							item.r128events.stream()
									.flatMap(event -> Stream.of(
											event.getI(),
											event.getM(),
											event.getS(),
											event.getTarget()))));

			dataGraphicLUFS.addSeries(dataGraphicLUFS.new Series(
					"Integrated",
					BLUE,
					THICK_STROKE,
					item.r128events.stream().map(Ebur128StrErrFilterEvent::getI)));
			dataGraphicLUFS.addSeries(dataGraphicLUFS.new Series(
					"Short term",
					GREEN.darker(),
					THIN_STROKE,
					item.r128events.stream().map(Ebur128StrErrFilterEvent::getS)));
			dataGraphicLUFS.addSeries(dataGraphicLUFS.new Series(
					"Momentary",
					Color.getHSBColor(0.5f, 1f, 0.3f),
					THIN_STROKE,
					item.r128events.stream().map(Ebur128StrErrFilterEvent::getM)));
			dataGraphicLUFS
					.addValueMarker(item.oEbur128Sum.map(Ebur128Summary::getIntegrated))
					.addValueMarker(item.oEbur128Sum.map(Ebur128Summary::getLoudnessRangeHigh))
					.addValueMarker(item.oEbur128Sum.map(Ebur128Summary::getLoudnessRangeLow))
					.addValueMarker(item.r128events.get(item.r128events.size() - 1).getI());
			return new GraphicArtifact(
					getBaseFileName(),
					dataGraphicLUFS.makeLogarithmicAxisGraphic(numberUtils),
					appConfig.getGraphicConfig().getImageSizeFullSize());
		}

	}

	class TPKGraphicMaker implements SingleGraphicMaker<EBUR128ReportItem> {

		@Override
		public String getBaseFileName() {
			return appConfig.getGraphicConfig().getLufsTPKGraphicFilename();
		}

		@Override
		public GraphicArtifact makeGraphic(final EBUR128ReportItem item) {
			final var dataGraphicTPK = new TimedDataGraphic(
					item.positions,
					RangeAxis.createFromValueSet("dB LU",
							item.oEbur128Sum.map(Ebur128Summary::getIntegrated).orElse(-23f), 10, 1,
							item.r128events.stream()
									.flatMap(event -> Stream.of(
											event.getFtpk().left(),
											event.getFtpk().right(),
											event.getSpk().left(),
											event.getSpk().right()))));

			dataGraphicTPK.addSeries(dataGraphicTPK.new Series(
					"True peak left (per frame)", BLUE, THIN_STROKE,
					item.r128events.stream().map(Ebur128StrErrFilterEvent::getFtpk).map(Stereo::left)));
			dataGraphicTPK.addSeries(dataGraphicTPK.new Series(
					"True peak right (per frame)", RED, THICK_STROKE,
					item.r128events.stream().map(Ebur128StrErrFilterEvent::getFtpk).map(Stereo::right)));
			dataGraphicTPK
					.addValueMarker(item.oEbur128Sum.map(Ebur128Summary::getTruePeak))
					.addValueMarker(item.oEbur128Sum.map(Ebur128Summary::getSamplePeak))
					.addValueMarker(-3)
					.addValueMarker(item.r128events.stream()
							.flatMap(event -> Stream.of(
									event.getFtpk().left(),
									event.getFtpk().right(),
									event.getSpk().left(),
									event.getSpk().right()))
							.mapToDouble(Float::doubleValue)
							.summaryStatistics().getAverage());
			return new GraphicArtifact(
					appConfig.getGraphicConfig().getLufsTPKGraphicFilename(),
					dataGraphicTPK.makeLogarithmicAxisGraphic(numberUtils),
					appConfig.getGraphicConfig().getImageSizeFullSize());
		}

	}

}
