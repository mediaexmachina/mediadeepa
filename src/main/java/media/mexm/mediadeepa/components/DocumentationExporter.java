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
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.empty;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableSet;

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
import media.mexm.mediadeepa.ManPage;
import media.mexm.mediadeepa.ReadmeMarkdownDoc;
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
	private static final Comparator<IOrdered> SPEC_COMPARATOR = (o1, o2) -> signum(o1.order() - o2.order());

	@Autowired
	private CommandLine commandLine;
	@Autowired
	private DocumentParserService documentParserService;
	@Autowired
	private EnvironmentVersion environmentVersion;

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
			documentParserService.markdownParse(documentParserService.getDocContent("description.md")).accept(manPage);
			documentParserService.markdownParse(documentParserService.getDocContent("about.md")).accept(manPage);
			documentParserService.markdownParse(documentParserService.getDocContent("features.md")).accept(manPage);

			manPage.startSection("EXAMPLES");
			documentParserService.markdownParse(documentParserService.getDocContent("examples.md")).accept(manPage);

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
			documentParserService.markdownParse(documentParserService.getDocContent("see-also.md")).accept(manPage);

			manPage.startSection("EXIT STATUS");
			manPage.addExitReturnCodes(spec.usageMessage().exitCodeList());

			manPage.startSection("RETURN VALUE");
			documentParserService.markdownParse(documentParserService.getDocContent("return-value.md")).accept(manPage);

			manPage.startSection("ABOUT AND COPYRIGHT");
			documentParserService.markdownParse(documentParserService.getDocContent("project.md")).accept(manPage);
			manPage.addCopyright(List.of(spec.version()),
					appVersion, LocalDate.now().getYear());

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

		doc.addSection("Media Deep Analysis", 1,
				documentParserService.getDocContent("description.md"));
		doc.addDocumentSummary();

		doc.addContent(
				documentParserService.getDocContent("gh-icon-labels.md"));

		doc.addSection("🚩 About", 2,
				documentParserService.getDocContent("about.md"));

		doc.addSection("🏪 Features", 2,
				documentParserService.getDocContent("features.md"));

		doc.addSection("⚡ Getting started", 2,
				documentParserService.getDocContent("getting-started.md")
						.replace("<version>", appVersion));

		doc.addSection("🛫 Examples", 2,
				documentParserService.getDocContent("examples.md"));
		doc.addContent(
				documentParserService.getDocContent("see-also.md"));

		doc.addSection("📕 Documentation, contributing and support", 2,
				documentParserService.getDocContent("project.md"));

		doc.addSection("🌹 Acknowledgments", 2,
				documentParserService.getDocContent("acknowledgments.md"));

		try (final var pw = new PrintStream(readmeFile, UTF_8)) {
			doc.writeDoc(pw);
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
