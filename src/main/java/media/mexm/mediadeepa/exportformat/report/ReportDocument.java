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
package media.mexm.mediadeepa.exportformat.report;

import static j2html.TagCreator.a;
import static j2html.TagCreator.article;
import static j2html.TagCreator.attrs;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.footer;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.head;
import static j2html.TagCreator.header;
import static j2html.TagCreator.html;
import static j2html.TagCreator.img;
import static j2html.TagCreator.li;
import static j2html.TagCreator.link;
import static j2html.TagCreator.main;
import static j2html.TagCreator.meta;
import static j2html.TagCreator.nav;
import static j2html.TagCreator.script;
import static j2html.TagCreator.style;
import static j2html.TagCreator.text;
import static j2html.TagCreator.title;
import static j2html.TagCreator.ul;
import static j2html.attributes.Attr.HTTP_EQUIV;
import static java.nio.charset.StandardCharsets.UTF_8;
import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.ABOUT;
import static media.mexm.mediadeepa.exportformat.report.ReportSectionCategory.SUMMARY;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.components.RendererEngineComparator;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;

public final class ReportDocument implements ConstStrings, JsonContentProvider {

	private final String source;

	private final List<ReportSection> sections;
	private final ReportSection summarySection;
	private final ReportSection aboutSection;

	public ReportDocument(final DataResult result,
						  final List<ReportRendererEngine> engines,
						  final RendererEngineComparator rendererEngineComparator) {
		source = result.getSource();
		sections = new ArrayList<>();
		summarySection = new ReportSection(SUMMARY, "Media summary");
		aboutSection = new ReportSection(ABOUT, ABOUT_THIS_DOCUMENT);

		engines.stream()
				.sorted(rendererEngineComparator)
				.forEach(engine -> engine.addToReport(result, this));
		result.resultMetadatasToReportDocument(this);
	}

	public void add(final ReportSection section) {
		sections.add(Objects.requireNonNull(section, "\"section\" can't to be null"));
	}

	public void addSummarySection(final ReportEntry reportEntry) {
		summarySection.add(reportEntry);
	}

	public void addAboutSection(final ReportEntry reportEntry) {
		aboutSection.add(reportEntry);
	}

	private Stream<ReportSection> getActiveSectionStream() {
		return sections.stream()
				.filter(ReportSection::isNotEmpty)
				.sorted((l, r) -> l.getCategory().compareTo(r.getCategory()));
	}

	public String toHTML(final Resource cssHTMLReportResource,
						 final Resource jsHTMLReportResource,
						 final NumberUtils numberUtils) {
		var documentCSS = "";
		var documentJS = "";
		try {
			documentCSS = cssHTMLReportResource.getContentAsString(UTF_8);
			documentJS = jsHTMLReportResource.getContentAsString(UTF_8);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't get CSS file from resources", e);
		}

		final var modalPopupWapper = div(attrs(".image-modal-popup"),
				div(attrs(".wrapper"), img()));

		final var htmlDocument = html(
				head(
						title(source + " :: Mediadeepa report"),
						meta()
								.withName("viewport")
								.withContent("width=device-width, initial-scale=1.0"),
						meta()
								.withData(HTTP_EQUIV, "Content-Type")
								.withContent("text/html; charset=UTF-8"),
						style(documentCSS).withType("text/css"),
						script(documentJS),
						link()
								.withMedia("all")
								.withRel("stylesheet")
								.withHref("style.css")),
				body(each(
						header(
								h1(MEDIADEEPA_REPORT_DOCUMENT),
								new SimpleKeyValueReportEntry(TARGET_SOURCE, source)
										.toDomContent(numberUtils),
								new SimpleKeyValueReportEntry(DOCUMENT_CREATION_DATE, new Date()
										.toString())
												.toDomContent(numberUtils),
								summarySection
										.toDomContent(numberUtils)),
						nav(
								a().withId("top"),
								h2("🗃 Table of content"),
								ul(each(
										each(getActiveSectionStream()
												.map(section -> li(
														a(section.getCategory().getEmoji() + " "
														  + section
																  .getTitle())
																		  .withHref("#" + section
																				  .getSectionAnchorName())))),
										li(a(aboutSection.getCategory().getEmoji() + " " + aboutSection
												.getTitle())
														.withHref("#" + aboutSection
																.getSectionAnchorName()))))),
						main(
								each(getActiveSectionStream()
										.map(section -> article(
												a().withId(section.getSectionAnchorName()),
												h2(text(section.getCategory().getEmoji()
														+ " "
														+ section.getTitle()),
														a(attrs(".backtotop"), TO_TOP_ICON).withHref("#top")),
												section.toDomContent(numberUtils)))),
								modalPopupWapper),
						footer(
								article(
										a().withId(aboutSection.getSectionAnchorName()),
										h2(text(aboutSection.getCategory().getEmoji()
												+ " "
												+ aboutSection.getTitle()),
												a(attrs(".backtotop"), TO_TOP_ICON).withHref("#top")),
										aboutSection.toDomContent(numberUtils))))));

		return String.join("\r\n",
				"<!DOCTYPE html>",
				htmlDocument
						.withLang("en")
						.render()
						.trim());
	}

	@Override
	public void toJson(final JsonGenerator gen,
					   final SerializerProvider provider) throws IOException {
		gen.writeStartObject();
		gen.writeStringField("title", MEDIADEEPA_REPORT_DOCUMENT);
		gen.writeStringField(jsonHeader(TARGET_SOURCE), source);
		gen.writeObjectField(jsonHeader(DOCUMENT_CREATION_DATE), ZonedDateTime.now());

		final var allSections = Stream.of(
				Stream.of(summarySection),
				getActiveSectionStream(),
				Stream.of(aboutSection))
				.flatMap(Function.identity());

		final var itemMap = allSections.reduce(new LinkedHashMap<ReportSectionCategory, List<ReportSection>>(),
				(map, section) -> {
					final var category = section.getCategory();
					if (map.containsKey(category) == false) {
						map.put(section.getCategory(), new ArrayList<>(List.of(section)));
					} else {
						map.get(section.getCategory()).add(section);
					}
					return map;
				}, (l, r) -> {
					l.putAll(r);
					return l;
				});

		for (final var mapEntry : itemMap.entrySet()) {
			final var category = jsonHeader(mapEntry.getKey().toString());
			final var sectionList = mapEntry.getValue();

			gen.writeObjectFieldStart(category);
			for (final var section : sectionList) {
				gen.writeObject(section);
			}
			gen.writeEndObject();
		}

		gen.writeEndObject();
	}

}
