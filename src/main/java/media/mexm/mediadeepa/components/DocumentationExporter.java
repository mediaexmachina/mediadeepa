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
package media.mexm.mediadeepa.components;

import static java.lang.Integer.signum;
import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.empty;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static media.mexm.mediadeepa.App.NAME;
import static org.apache.commons.lang3.StringUtils.capitalize;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.ConfigurationCrawler;
import media.mexm.mediadeepa.ManPage;
import media.mexm.mediadeepa.ProjectPageGenerator;
import media.mexm.mediadeepa.ReadmeMarkdownDoc;
import media.mexm.mediadeepa.exportformat.ExportFormat;
import media.mexm.mediadeepa.service.DocumentParserService;
import picocli.CommandLine;
import picocli.CommandLine.Model.ArgGroupSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.IOrdered;
import picocli.CommandLine.Model.OptionSpec;
import tv.hd3g.commons.version.EnvironmentVersion;

@Slf4j
@Component
public class DocumentationExporter {
	private static final String YEAR = String.valueOf(LocalDate.now().getYear());
	private static final String SINGLE_EXPORT_MD = "single-export.md";
	private static final String RELEASES_MD = "releases.md";
	private static final String E2E_TESTS_MD = "e2e-tests.md";
	private static final String LOGS_MD = "logs.md";
	private static final String INTERNAL_OPTIONS_BEFORE_LIST_MD = "internal-options-before-list.md";
	private static final String INTERNAL_OPTIONS_SEE_ALSO_MD = "internal-options-see-also.md";
	private static final String AUTO_GENERATED_DOCS_MD = "auto-generated-docs.md";
	private static final String ROAD_MAP_MD = "road-map.md";
	private static final String OPTIONS_AFTER_LIST_MD = "options-after-list.md";
	private static final String OPTIONS_BEFORE_LIST_MD = "options-before-list.md";
	private static final String EXPORT_FORMATS_AFTERLIST_MD = "export-formats-afterlist.md";
	private static final String PROJECTPAGE_VERSION_MD = "projectpage-version.md";
	private static final String EXPORT_FORMATS_BEFORELIST_MD = "export-formats-beforelist.md";
	private static final String ACKNOWLEDGMENTS_MD = "acknowledgments.md";
	private static final String GETTING_STARTED_MD = "getting-started.md";
	private static final String GH_ICON_LABELS_MD = "gh-icon-labels.md";
	private static final String PROJECT_MD = "project.md";
	private static final String RETURN_VALUE_MD = "return-value.md";
	private static final String SEE_ALSO_MD = "see-also.md";
	private static final String EXAMPLES_MD = "examples.md";
	private static final String FEATURES_MD = "features.md";
	private static final String ABOUT_MD = "about.md";
	private static final String DESCRIPTION_MD = "description.md";
	private static final String BIN_SEARCH_MD = "bin-search.md";
	private static final String VERSION_TAG = "<version>";
	private static final String FOOTER_WEBPAGE_MD = "footer-webpage.md";
	private static final String YEAR_TAG = "<year>";

	private static final Comparator<IOrdered> SPEC_COMPARATOR = (o1, o2) -> signum(o1.order() - o2.order());

	@Autowired
	private CommandLine commandLine;
	@Autowired
	private DocumentParserService documentParserService;
	@Autowired
	private EnvironmentVersion environmentVersion;
	@Autowired
	private List<ExportFormat> exportFormatList;
	@Autowired
	private ExportFormatComparator exportFormatComparator;
	@Autowired
	private ConfigurationCrawler configurationCrawler;

	private CommandSpec spec;
	private String appVersion;

	@PostConstruct
	public void init() {
		spec = commandLine.getCommandSpec().root();
		appVersion = environmentVersion.appVersion();
	}

	public void exportManPage(final File manFile) throws IOException {
		log.info("Save man page to {}", manFile);
		FileUtils.forceMkdirParent(manFile);
		try (final var pw = new PrintStream(manFile, UTF_8)) {
			final var manPage = new ManPage(pw);
			manPage.addDocumentHeader(spec.name(), "1", new Date(), spec.name(), appVersion);

			manPage.startSection("NAME");
			manPage.text(spec.name(), "-",
					Stream.of(spec.usageMessage().description()).collect(joining(" ")));

			manPage.startSection("SYNOPSIS");
			manPage.text(spec.commandLine().getHelp().synopsis(0));

			manPage.startSection("DESCRIPTION");
			documentParserService.markdownParse(documentParserService.getDocContent(DESCRIPTION_MD)).accept(manPage);
			documentParserService.markdownParse(documentParserService.getDocContent(ABOUT_MD)).accept(manPage);
			documentParserService.markdownParse(documentParserService.getDocContent(FEATURES_MD)).accept(manPage);

			manPage.startSection("EXAMPLES");
			documentParserService.markdownParse(documentParserService.getDocContent(EXAMPLES_MD)).accept(manPage);

			manPage.startSection("OPTIONS");
			final var groups = optionListGroups(spec);
			final var allOptionsNested = getAllOptionsNested(groups);

			manPage.printGroupOptions(empty(), spec.options().stream()
					.filter(not(allOptionsNested::contains))
					.sorted(SPEC_COMPARATOR));

			groups.forEach(group -> manPage.printGroupOptions(
					Optional.ofNullable(group.heading())
							.map(h -> h.replace("%n", "")),
					group.options().stream()
							.sorted(SPEC_COMPARATOR)));

			manPage.startSection("SEE ALSO");
			documentParserService.markdownParse(documentParserService.getDocContent(SEE_ALSO_MD)).accept(manPage);
			documentParserService.markdownParse(documentParserService.getDocContent(INTERNAL_OPTIONS_SEE_ALSO_MD))
					.accept(manPage);
			documentParserService.markdownParse(documentParserService.getDocContent(LOGS_MD)).accept(manPage);
			documentParserService.markdownParse(documentParserService.getDocContent(BIN_SEARCH_MD)).accept(manPage);

			manPage.startSection("EXIT STATUS");
			manPage.addExitReturnCodes(spec.usageMessage().exitCodeList());

			manPage.startSection("RETURN VALUE");
			documentParserService.markdownParse(documentParserService.getDocContent(RETURN_VALUE_MD)).accept(manPage);

			manPage.startSection("ABOUT AND COPYRIGHT");
			documentParserService.markdownParse(documentParserService.getDocContent(PROJECT_MD)).accept(manPage);
			manPage.addCopyright(List.of(spec.version()),
					appVersion, YEAR);

			/**
			 * END
			 */
			if (spec.subcommands().isEmpty() == false) {
				log.warn("Subcomand is not managed for man ({} subcommand(s))", spec.subcommands().size());
			}
		}
	}

	public void exportReadmeProjectMarkdown(final File readmeFile) throws IOException {
		log.info("Export project README markdown file to {}", readmeFile);

		final var doc = new ReadmeMarkdownDoc();

		doc.addComments("""
				AUTOGENERATED DOCUMENT BY MEDIADEEPA
				DO NOT EDIT, SOURCE ARE LOCATED ON src/main/resources/doc/en
				""");
		doc.addComments("GENERATED BY " + getClass().getName());

		doc.addSection(capitalize(NAME), 1,
				documentParserService.getDocContent(DESCRIPTION_MD));
		doc.addDocumentSummary();

		doc.addContent(
				documentParserService.getDocContent(GH_ICON_LABELS_MD));

		doc.addSection("🚩 About", 2,
				documentParserService.getDocContent(ABOUT_MD));

		doc.addSection("🏪 Features", 2,
				documentParserService.getDocContent(FEATURES_MD));

		doc.addSection("⚡ Getting started", 2,
				documentParserService.getDocContent(GETTING_STARTED_MD)
						.replace(VERSION_TAG, appVersion));

		doc.addSection("🛫 Examples", 2,
				documentParserService.getDocContent(EXAMPLES_MD));
		doc.addContent(
				documentParserService.getDocContent(SEE_ALSO_MD));
		doc.addContent(
				documentParserService.getDocContent(INTERNAL_OPTIONS_SEE_ALSO_MD));

		doc.addSection("📕 Documentation, contributing and support", 2,
				documentParserService.getDocContent(PROJECT_MD));
		doc.addContent(
				documentParserService.getDocContent(E2E_TESTS_MD));
		doc.addContent(
				documentParserService.getDocContent(RELEASES_MD));

		doc.addSection("🌹 Acknowledgments", 2,
				documentParserService.getDocContent(ACKNOWLEDGMENTS_MD));
		doc.addSection("", 2, documentParserService.getDocContent(FOOTER_WEBPAGE_MD)
				.replace(YEAR_TAG, YEAR));

		try (final var pw = new PrintStream(readmeFile, UTF_8)) {
			doc.writeDoc(pw);
		}
	}

	public void exportProjectPage(final File projectPageFile) throws IOException {
		log.info("Export project web page to html file {}", projectPageFile);
		final var ppg = new ProjectPageGenerator(documentParserService);

		ppg.addMdContent("# About Mediadeepa");
		ppg.addStaticMdContent(DESCRIPTION_MD);
		ppg.addStaticMdContent(PROJECTPAGE_VERSION_MD,
				t -> t.replace(VERSION_TAG, appVersion));
		ppg.addStaticMdContent(ABOUT_MD);

		ppg.addMdContent("# Features");
		ppg.addStaticMdContent(FEATURES_MD);

		ppg.addStaticMdContent(EXPORT_FORMATS_BEFORELIST_MD);
		ppg.addMdContent(exportFormatList.stream()
				.sorted(exportFormatComparator)
				.map(exportFormat -> " - `"
									 + exportFormat.getFormatName() + "`: "
									 + exportFormat.getFormatLongName() + " " +
									 exportFormat.getFormatDescription())
				.collect(joining("\n")));
		ppg.addStaticMdContent(EXPORT_FORMATS_AFTERLIST_MD);

		ppg.addMdContent("# Getting started");
		ppg.addStaticMdContent(GETTING_STARTED_MD,
				t -> t.replace(VERSION_TAG, appVersion));

		ppg.addMdContent("# Examples");
		ppg.addStaticMdContent(EXAMPLES_MD);
		ppg.addStaticMdContent(SEE_ALSO_MD);

		ppg.addStaticMdContent(OPTIONS_BEFORE_LIST_MD);

		final var groups = optionListGroups(spec);
		final var allOptionsNested = getAllOptionsNested(groups);

		ppg.addMdContent("## General options");
		ppg.addGroupOptions(empty(), spec.options().stream()
				.filter(not(allOptionsNested::contains))
				.sorted(SPEC_COMPARATOR));

		ppg.addMdContent("## Specific options");
		groups.forEach(group -> ppg.addGroupOptions(
				Optional.ofNullable(group.heading())
						.map(h -> h.replace("%n", "")),
				group.options().stream()
						.sorted(SPEC_COMPARATOR)));

		ppg.addStaticMdContent(OPTIONS_AFTER_LIST_MD);

		ppg.addStaticMdContent(SINGLE_EXPORT_MD);
		ppg.addMdContent(exportFormatList.stream()
				.sorted(exportFormatComparator)
				.map(exportFormat -> {
					final var fileNames = exportFormat.getInternalProducedFileNames()
							.stream()
							.sorted()
							.map(f -> " - `" + f + "`")
							.collect(joining("\n"));
					return "### " + exportFormat.getFormatLongName() + " (" + exportFormat.getFormatName() + ")\n\n"
						   + fileNames;
				})
				.collect(joining("\n")));

		ppg.addStaticMdContent(INTERNAL_OPTIONS_BEFORE_LIST_MD);

		final var rootPrefix = configurationCrawler.getRootPrefix();
		final var internalOptions = configurationCrawler.parse().stream()
				.map(ce -> ce.getFullKey(rootPrefix) + "=" + ce.getDefaultValue() + "    # " + ce.getType())
				.collect(joining(
						lineSeparator(),
						"```" + lineSeparator(),
						lineSeparator() + "```"));
		ppg.addMdContent(internalOptions);

		ppg.addStaticMdContent(LOGS_MD);
		ppg.addStaticMdContent(BIN_SEARCH_MD);

		ppg.addMdContent("## Application return");
		ppg.addStaticMdContent(RETURN_VALUE_MD);
		ppg.addMdContent("### Return codes");
		ppg.addExitReturnCodes(spec.usageMessage().exitCodeList());

		ppg.addMdContent("# Documentation, contributing and support");
		ppg.addStaticMdContent(PROJECT_MD);
		ppg.addStaticMdContent(GH_ICON_LABELS_MD);
		ppg.addStaticMdContent(E2E_TESTS_MD);
		ppg.addStaticMdContent(RELEASES_MD);

		ppg.addStaticMdContent(ROAD_MAP_MD);
		ppg.addStaticMdContent(AUTO_GENERATED_DOCS_MD,
				t -> t.replace(VERSION_TAG, appVersion));

		ppg.addMdContent("# Acknowledgments");
		ppg.addStaticMdContent(ACKNOWLEDGMENTS_MD);

		ppg.setFooter(FOOTER_WEBPAGE_MD, t -> t.replace(YEAR_TAG, YEAR));

		try (final var pw = new PrintStream(projectPageFile, UTF_8)) {
			pw.println("""
					<!--
					--------------------------------------------------------------------------
					This file is part of mediadeepa.

					This program is free software; you can redistribute it and/or modify
					it under the terms of the GNU Lesser General Public License as published by
					the Free Software Foundation; either version 3 of the License, or
					any later version.

					This program is distributed in the hope that it will be useful,
					but WITHOUT ANY WARRANTY; without even the implied warranty of
					MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
					GNU Lesser General Public License for more details.

					Copyright (C) Media ex Machina 2023
					--------------------------------------------------------------------------
					AUTOGENERATED DOCUMENT BY MEDIADEEPA
					DO NOT EDIT, SOURCE ARE LOCATED ON src/main/resources/doc/en
					""");
			pw.println("GENERATED BY " + getClass().getName());
			pw.println("-->");
			ppg.writeDoc(pw);
		}
	}

	private static Set<OptionSpec> getAllOptionsNested(final List<ArgGroupSpec> groups) {
		return groups.stream()
				.map(ArgGroupSpec::allOptionsNested)
				.flatMap(List::stream)
				.distinct()
				.collect(toUnmodifiableSet());
	}

	private static List<ArgGroupSpec> optionListGroups(final CommandSpec commandSpec) {
		final List<ArgGroupSpec> result = new ArrayList<>();
		optionListGroups(commandSpec.argGroups(), result);
		return result.stream()
				.sorted(SPEC_COMPARATOR)
				.toList();
	}

	private static void optionListGroups(final List<ArgGroupSpec> groups, final List<ArgGroupSpec> result) {
		groups.forEach(group -> {
			if (group.heading() != null) {
				result.add(group);
			}
			optionListGroups(group.subgroups(), result);
		});
	}

}
