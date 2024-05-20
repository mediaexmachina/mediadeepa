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
import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.AUDIO;

import java.util.List;

import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.TableDocument;
import media.mexm.mediadeepa.exportformat.TabularDocument;
import media.mexm.mediadeepa.exportformat.TabularExportFormat;
import media.mexm.mediadeepa.exportformat.report.NumericUnitValueReportEntry;
import media.mexm.mediadeepa.exportformat.report.ReportDocument;
import media.mexm.mediadeepa.exportformat.report.ReportSection;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.SingleTabularDocumentExporterTraits;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMetadataFilterParser;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdValue;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;

@Component
public class Ebur128SummaryRendererEngine implements
										  ReportRendererEngine,
										  TableRendererEngine,
										  TabularRendererEngine,
										  ConstStrings,
										  SingleTabularDocumentExporterTraits {

	public static final List<String> HEAD_EBUR128_SUMMARY = List.of(
			INTEGRATED,
			LOUDNESS_RANGE,
			LOUDNESS_RANGE_LOW,
			LOUDNESS_RANGE_HIGH,
			SAMPLE_PEAK,
			TRUE_PEAK);

	@Override
	public String getSingleUniqTabularDocumentBaseFileName() {
		return "audio-ebur128-summary";
	}

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		return result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getR128Report)
				.filter(not(List::isEmpty))
				.map(l -> l.get(l.size() - 1))
				.map(LavfiMtdValue::value)
				.map(ebu -> {
					final var t = new TabularDocument(tabularExportFormat, getSingleUniqTabularDocumentBaseFileName())
							.head(HEAD_EBUR128_SUMMARY);
					t.row(
							tabularExportFormat.formatNumberLowPrecision(ebu.integrated()),
							tabularExportFormat.formatNumberLowPrecision(ebu.loudnessRange()),
							tabularExportFormat.formatNumberLowPrecision(ebu.loudnessRangeLow()),
							tabularExportFormat.formatNumberLowPrecision(ebu.loudnessRangeHigh()),
							tabularExportFormat.formatNumberLowPrecision(ebu.samplePeak()),
							tabularExportFormat.formatNumberLowPrecision(ebu.truePeak()));
					return t;
				})
				.stream()
				.toList();
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getR128Report)
				.filter(not(List::isEmpty))
				.map(l -> l.get(l.size() - 1))
				.map(LavfiMtdValue::value)
				.ifPresent(ebu -> {
					final var t = tableDocument.createTable("EBU R 128 Summary").head(HEAD_EBUR128_SUMMARY);
					t.addRow()
							.addCell(ebu.integrated())
							.addCell(ebu.loudnessRange())
							.addCell(ebu.loudnessRangeLow())
							.addCell(ebu.loudnessRangeHigh())
							.addCell(ebu.samplePeak())
							.addCell(ebu.truePeak());
				});
	}

	@Override
	public void addToReport(final DataResult result, final ReportDocument document) {
		result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::lavfiMetadatas)
				.map(LavfiMetadataFilterParser::getR128Report)
				.filter(not(List::isEmpty))
				.map(l -> l.get(l.size() - 1))
				.map(LavfiMtdValue::value)
				.ifPresent(ebu -> {
					final var section = new ReportSection(AUDIO, LOUDNESS_EBU_R128);
					section.add(new NumericUnitValueReportEntry("Integrated", ebu.integrated(), DBFS));
					section.add(new NumericUnitValueReportEntry("Range (LRA)", ebu.loudnessRange(), "dB"));
					section.add(new NumericUnitValueReportEntry("High range", ebu.loudnessRangeHigh(), DBFS));
					section.add(new NumericUnitValueReportEntry("Low range", ebu.loudnessRangeLow(), DBFS));
					section.add(new NumericUnitValueReportEntry("True peak", ebu.truePeak(), "dBTPK"));
					document.add(section);
				});
	}

}
