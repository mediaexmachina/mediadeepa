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
package media.mexm.mediadeepa;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReadmeMarkdownDoc {

	private final List<String> sections;
	private final List<String> sectionTitles;

	private int summaryPosition;

	public ReadmeMarkdownDoc() {
		sections = new ArrayList<>();
		sectionTitles = new ArrayList<>();
		summaryPosition = -1;
	}

	public void addComments(final String comments) {
		comments.lines()
				.map(c -> "<!-- " + c + "-->")
				.forEach(sections::add);
		sections.add("");
	}

	public void addContent(final String mdContent) {
		mdContent.lines().forEach(sections::add);
		sections.add("");
	}

	public void addSection(final String title, final int levelTitle, final String mdContent) {
		if (levelTitle == 2) {
			sectionTitles.add(title);
		}
		sections.add("<h" + levelTitle + " id=\"" + getIdFromTitleName(title) + "\">"
					 + title
					 + "</h" + levelTitle + ">");
		sections.add("");
		addContent(mdContent);
	}

	public void addDocumentSummary() {
		summaryPosition = sections.size();
	}

	private static String getIdFromTitleName(final String titleName) {
		return titleName.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
	}

	public void writeDoc(final PrintStream ps) {
		if (summaryPosition > -1) {
			final var fullSummary = new ArrayList<String>();
			sectionTitles.stream()
					.map(t -> ("> [" + t + "](#" + getIdFromTitleName(t) + ")"))
					.collect(Collectors.joining("\\\n"))
					.lines()
					.forEach(fullSummary::add);
			fullSummary.add("");
			sections.addAll(summaryPosition, fullSummary);
		}
		sections.forEach(ps::println);
	}

}
