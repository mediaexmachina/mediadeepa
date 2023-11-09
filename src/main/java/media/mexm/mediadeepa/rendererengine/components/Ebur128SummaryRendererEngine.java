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

import static media.mexm.mediadeepa.exportformat.ReportSectionCategory.AUDIO;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.NumericUnitValueReportEntry;
import media.mexm.mediadeepa.exportformat.ReportDocument;
import media.mexm.mediadeepa.exportformat.ReportSection;
import media.mexm.mediadeepa.exportformat.TableDocument;
import media.mexm.mediadeepa.exportformat.TabularDocument;
import media.mexm.mediadeepa.exportformat.TabularExportFormat;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.TableRendererEngine;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;

@Component
public class Ebur128SummaryRendererEngine implements
										  ReportRendererEngine,
										  TableRendererEngine,
										  TabularRendererEngine,
										  ConstStrings {

	public static final List<String> HEAD_EBUR128_SUMMARY = List.of(
			INTEGRATED,
			INTEGRATED_THRESHOLD,
			LOUDNESS_RANGE,
			LOUDNESS_RANGE_THRESHOLD,
			LOUDNESS_RANGE_LOW,
			LOUDNESS_RANGE_HIGH,
			SAMPLE_PEAK,
			TRUE_PEAK);

	@Override
	public List<TabularDocument> toTabularDocument(final DataResult result,
												   final TabularExportFormat tabularExportFormat) {
		return result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::ebur128Summary)
				.flatMap(Optional::ofNullable)
				.map(ebu -> {
					final var t = new TabularDocument(tabularExportFormat, "audio-ebur128-summary").head(
							HEAD_EBUR128_SUMMARY);
					t.row(
							tabularExportFormat.formatNumberLowPrecision(ebu.getIntegrated()),
							tabularExportFormat.formatNumberLowPrecision(ebu.getIntegratedThreshold()),
							tabularExportFormat.formatNumberLowPrecision(ebu.getLoudnessRange()),
							tabularExportFormat.formatNumberLowPrecision(ebu.getLoudnessRangeThreshold()),
							tabularExportFormat.formatNumberLowPrecision(ebu.getLoudnessRangeLow()),
							tabularExportFormat.formatNumberLowPrecision(ebu.getLoudnessRangeHigh()),
							tabularExportFormat.formatNumberLowPrecision(ebu.getSamplePeak()),
							tabularExportFormat.formatNumberLowPrecision(ebu.getTruePeak()));
					return t;
				})
				.stream()
				.toList();
	}

	@Override
	public void addToTable(final DataResult result, final TableDocument tableDocument) {
		result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::ebur128Summary)
				.flatMap(Optional::ofNullable)
				.ifPresent(ebu -> {
					final var t = tableDocument.createTable("EBU R 128 Summary").head(HEAD_EBUR128_SUMMARY);
					t.addRow()
							.addCell(ebu.getIntegrated())
							.addCell(ebu.getIntegratedThreshold())
							.addCell(ebu.getLoudnessRange())
							.addCell(ebu.getLoudnessRangeThreshold())
							.addCell(ebu.getLoudnessRangeLow())
							.addCell(ebu.getLoudnessRangeHigh())
							.addCell(ebu.getSamplePeak())
							.addCell(ebu.getTruePeak());
				});
	}

	@Override
	public void addToReport(final DataResult result, final ReportDocument document) {
		result.getMediaAnalyserResult()
				.map(MediaAnalyserResult::ebur128Summary)
				.flatMap(Optional::ofNullable)
				.ifPresent(ebu -> {
					final var section = new ReportSection(AUDIO, LOUDNESS_EBU_R128);
					section.add(new NumericUnitValueReportEntry("Integrated", ebu.getIntegrated(), DBFS));
					section.add(new NumericUnitValueReportEntry("Range (LRA)", ebu.getLoudnessRange(), "dB"));
					section.add(new NumericUnitValueReportEntry("High range", ebu.getLoudnessRangeHigh(), DBFS));
					section.add(new NumericUnitValueReportEntry("Low range", ebu.getLoudnessRangeLow(), DBFS));
					section.add(new NumericUnitValueReportEntry("True peak", ebu.getTruePeak(), "dBTPK"));
					document.add(section);
				});
	}

}
