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
package media.mexm.mediadeepa.exportformat;

import static media.mexm.mediadeepa.exportformat.ReportSectionCategory.SUMMARY;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import lombok.Getter;

public class ReportDocument {

	private final List<ReportSection> sections;
	@Getter
	private final ReportSection summarySection;

	public ReportDocument() {
		sections = new ArrayList<>();
		summarySection = new ReportSection(SUMMARY, "Media summary");
	}

	public void add(final ReportSection section) {
		sections.add(Objects.requireNonNull(section, "\"section\" can't to be null"));
	}

	public void addSummarySection(final ReportEntry reportEntry) {
		summarySection.add(reportEntry);
	}

	public Stream<ReportSection> getSections() {
		return sections.stream()
				.filter(ReportSection::isNotEmpty)
				.sorted((l, r) -> l.getCategory().compareTo(r.getCategory()));
	}

}
