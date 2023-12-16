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
package media.mexm.mediadeepa.exportformat.components;

import static j2html.TagCreator.a;
import static j2html.TagCreator.article;
import static j2html.TagCreator.attrs;
import static j2html.TagCreator.body;
import static j2html.TagCreator.each;
import static j2html.TagCreator.footer;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.head;
import static j2html.TagCreator.header;
import static j2html.TagCreator.html;
import static j2html.TagCreator.li;
import static j2html.TagCreator.link;
import static j2html.TagCreator.main;
import static j2html.TagCreator.meta;
import static j2html.TagCreator.nav;
import static j2html.TagCreator.style;
import static j2html.TagCreator.text;
import static j2html.TagCreator.title;
import static j2html.TagCreator.ul;
import static j2html.attributes.Attr.HTTP_EQUIV;
import static java.nio.charset.StandardCharsets.UTF_8;
import static media.mexm.mediadeepa.exportformat.ReportSectionCategory.ABOUT;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.cli.ExportToCmd;
import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.components.RendererEngineComparator;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormat;
import media.mexm.mediadeepa.exportformat.ReportDocument;
import media.mexm.mediadeepa.exportformat.ReportSection;
import media.mexm.mediadeepa.exportformat.SimpleKeyValueReportEntry;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;

@Component
public class ReportExportFormat implements ExportFormat, ConstStrings {

	@Autowired
	private List<ReportRendererEngine> engines;
	@Autowired
	private RendererEngineComparator rendererEngineComparator;
	@Autowired
	private AppConfig appConfig;
	@Autowired
	private NumberUtils numberUtils;

	@Value("classpath:html-report-style.css")
	private Resource cssHTMLReportResource;

	@Override
	public String getFormatName() {
		return "report";
	}

	@Override
	public String getFormatLongName() {
		return "HTML document report";
	}

	@Override
	public String getFormatDescription() {
		return "with file and signal stats, event detection, codecs, GOP stats...";
	}

	private String getDocumentCSS() {
		try {
			return cssHTMLReportResource.getContentAsString(UTF_8);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't get CSS file from resources", e);
		}
	}

	@Override
	public Map<String, File> exportResult(final DataResult result, final ExportToCmd exportToCmd) {
		final var document = new ReportDocument();

		engines.stream()
				.sorted(rendererEngineComparator)
				.forEach(engine -> engine.addToReport(result, document));

		final var summarySection = header(
				h1(MEDIADEEPA_REPORT_DOCUMENT),
				new SimpleKeyValueReportEntry(TARGET_SOURCE, result.getSource())
						.toDomContent(numberUtils),
				new SimpleKeyValueReportEntry(DOCUMENT_CREATION_DATE, new Date().toString())
						.toDomContent(numberUtils),
				document.getSummarySection()
						.toDomContent(numberUtils));

		final var version = result.getVersions();
		final var items = version.entrySet().stream()
				.map(entry -> new SimpleKeyValueReportEntry(entry.getKey(), entry.getValue()))
				.toList();

		final var aboutDocument = new ReportSection(ABOUT, ABOUT_THIS_DOCUMENT).add(items);

		final var tocSection = nav(
				a().withId("top"),
				h2("🗃 Table of content"),
				ul(each(
						each(document.getSections()
								.map(section -> li(
										a(section.getCategory().getEmoji() + " " + section.getTitle())
												.withHref("#" + section.getSectionAnchorName())))),
						li(a(aboutDocument.getCategory().getEmoji() + " " + aboutDocument.getTitle())
								.withHref("#" + aboutDocument.getSectionAnchorName())))));

		final var docSections = main(each(document.getSections()
				.map(section -> article(
						a().withId(section.getSectionAnchorName()),
						h2(text(section.getCategory().getEmoji()
								+ " "
								+ section.getTitle()),
								a(attrs(".backtotop"), TO_TOP_ICON).withHref("#top")),
						section.toDomContent(numberUtils)))));

		final var footerSections = footer(article(
				a().withId(aboutDocument.getSectionAnchorName()),
				h2(text(aboutDocument.getCategory().getEmoji()
						+ " "
						+ aboutDocument.getTitle()),
						a(attrs(".backtotop"), TO_TOP_ICON).withHref("#top")),
				aboutDocument.toDomContent(numberUtils)));

		final var htmlDocument = String.join("\r\n",
				"<!DOCTYPE html>",
				html(
						head(
								title(result.getSource() + " :: Mediadeepa report"),
								meta()
										.withName("viewport")
										.withContent("width=device-width, initial-scale=1.0"),
								meta()
										.withData(HTTP_EQUIV, "Content-Type")
										.withContent("text/html; charset=UTF-8"),
								style(getDocumentCSS()).withType("text/css"),
								link()
										.withMedia("all")
										.withRel("stylesheet")
										.withHref("style.css")),
						body(each(summarySection, tocSection, docSections, footerSections)))
								.withLang("en")
								.render())
				.trim();
		try {
			final var outFile = exportToCmd.makeOutputFile(appConfig.getReportConfig().getHtmlFilename());
			FileUtils.write(
					outFile,
					htmlDocument,
					UTF_8,
					false);

			return Map.of("html_report", outFile);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write file", e);
		}
	}

}
