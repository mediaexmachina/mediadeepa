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

import static j2html.TagCreator.body;
import static j2html.TagCreator.each;
import static j2html.TagCreator.head;
import static j2html.TagCreator.header;
import static j2html.TagCreator.html;
import static j2html.TagCreator.link;
import static j2html.TagCreator.main;
import static j2html.TagCreator.meta;
import static j2html.TagCreator.nav;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.section;
import static j2html.TagCreator.title;
import static j2html.attributes.Attr.HTTP_EQUIV;
import static java.lang.System.lineSeparator;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.joining;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.CustomBlock;
import org.commonmark.node.CustomNode;
import org.commonmark.node.Document;
import org.commonmark.node.Emphasis;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Image;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.LinkReferenceDefinition;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.node.ThematicBreak;
import org.commonmark.node.Visitor;

import j2html.TagCreator;
import j2html.tags.DomContent;
import lombok.Getter;
import media.mexm.mediadeepa.service.DocumentParserService;
import picocli.CommandLine.Model.OptionSpec;

public class ProjectPageGenerator {

	private static final String NEW_LINE = "\r\n";
	private final DocumentParserService documentParserService;
	private final List<String> mdDocumentLines;
	private final MdVisitor mdVisitor;

	public ProjectPageGenerator(final DocumentParserService documentParserService) {
		this.documentParserService = documentParserService;
		mdDocumentLines = new ArrayList<>();
		mdVisitor = new MdVisitor();
	}

	public void addMdContent(final String mdContent) {
		mdDocumentLines.addAll(mdContent.lines().toList());
		mdDocumentLines.add("");
	}

	public void addStaticMdContent(final String fileName) {
		addStaticMdContent(fileName, identity());
	}

	public void addStaticMdContent(final String fileName, final UnaryOperator<String> textTransform) {
		mdDocumentLines.addAll(textTransform.apply(documentParserService.getDocContent(fileName)).lines().toList());
		mdDocumentLines.add("");
	}

	public void addGroupOptions(final Optional<String> groupHeading, final Stream<OptionSpec> options) {
		groupHeading.ifPresent(h -> addMdContent("### " + h));

		options.map(option -> {
			final var sb = new StringBuilder();
			sb.append(Stream.of(option.names())
					.map(o -> "`" + o + "`")
					.collect(joining(", ")));

			final var paramLabel = option.paramLabel();
			if (paramLabel.startsWith("<") == false && paramLabel.endsWith(">") == false) {
				sb.append(" `" + paramLabel + "`");
			}

			sb.append(option.required() ? ", **required**," : "");
			sb.append(option.isMultiValue() ? ", _can be used multiple times_," : "");
			Stream.of(option.description())
					.forEach(d -> sb.append(" " + d));

			return sb.toString();
		}).forEach(o -> mdDocumentLines.add(" - " + o));
		mdDocumentLines.add("");
	}

	public void addExitReturnCodes(final Map<String, String> rCodes) {
		rCodes.keySet()
				.stream()
				.sorted()
				.forEach(code -> mdDocumentLines.add(" - `" + code + "`: " + rCodes.get(code)));
		mdDocumentLines.add("");
	}

	public void writeDoc(final PrintStream ps) {
		final var strMdDocument = mdDocumentLines.stream().collect(joining(lineSeparator()));
		final var mdDocument = documentParserService.markdownParse(strMdDocument);
		mdDocument.accept(mdVisitor);

		final var navContent = nav(each(mdVisitor.getSections()
				.stream()
				.map(s -> TagCreator.a(s.title)
						.withHref("#" + s.getTitleToId()))));

		final var mainContent = main(each(
				mdVisitor.getSections()
						.stream()
						.map(s -> {
							final var content = rawHtml(s.content().stream().collect(joining(NEW_LINE)));
							return section(s.levelToHtag(), content)
									.withId(s.getTitleToId());
						})));

		final var htmlDocument = html(head(
				title("Mediadeepa project documentation page"),
				meta()
						.withName("viewport")
						.withContent("width=device-width, initial-scale=1.0"),
				meta()
						.withData(HTTP_EQUIV, "Content-Type")
						.withContent("text/html; charset=UTF-8"),
				link()
						.withMedia("all")
						.withRel("stylesheet")
						.withHref("style.css")),
				body(
						header(navContent),
						mainContent))
								.withLang("en");

		ps.print(String.join(NEW_LINE, "<!DOCTYPE html>", htmlDocument.render()));
	}

	private record DocSection(String title, List<String> content) {

		String getTitleToId() {
			return title.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
		}

		DomContent levelToHtag() {
			return TagCreator.h1(title);
		}
	}

	private class MdVisitor implements Visitor, MdVisitorTrait {

		private boolean topHeader;
		@Getter
		private final List<DocSection> sections;

		public MdVisitor() {
			sections = new ArrayList<>();
			topHeader = false;
		}

		private DocSection getCurrentSection() {
			if (sections.isEmpty()) {
				throw new IllegalStateException("Not header to start first section");
			}
			return sections.get(sections.size() - 1);
		}

		private void nextSection(final String title) {
			sections.add(new DocSection(title, new ArrayList<>()));
		}

		@Override
		public void visit(final Heading node) {
			if (node.getLevel() == 1) {
				topHeader = true;
				firstChild(node);
				topHeader = false;
				next(node);
			} else {
				render(node);
			}
		}

		@Override
		public void visit(final Text node) {
			if (topHeader) {
				nextSection(node.getLiteral());
			} else {
				render(node);
			}
		}

		private void render(final Node node) {
			getCurrentSection().content().addAll(documentParserService.htmlRender(node).lines().toList());
			next(node);
		}

		@Override
		public void visit(final Document document) {
			firstChild(document);
		}

		@Override
		public void visit(final Paragraph node) {
			render(node);
		}

		@Override
		public void visit(final BlockQuote node) {
			render(node);
		}

		@Override
		public void visit(final BulletList node) {
			render(node);
		}

		@Override
		public void visit(final Code node) {
			render(node);
		}

		@Override
		public void visit(final Emphasis node) {
			render(node);
		}

		@Override
		public void visit(final FencedCodeBlock node) {
			render(node);
		}

		@Override
		public void visit(final HardLineBreak node) {
			render(node);
		}

		@Override
		public void visit(final ThematicBreak node) {
			render(node);
		}

		@Override
		public void visit(final HtmlInline node) {
			render(node);
		}

		@Override
		public void visit(final HtmlBlock node) {
			render(node);
		}

		@Override
		public void visit(final Image node) {
			render(node);
		}

		@Override
		public void visit(final IndentedCodeBlock node) {
			render(node);
		}

		@Override
		public void visit(final Link node) {
			render(node);
		}

		@Override
		public void visit(final ListItem node) {
			render(node);
		}

		@Override
		public void visit(final OrderedList node) {
			render(node);
		}

		@Override
		public void visit(final SoftLineBreak node) {
			render(node);
		}

		@Override
		public void visit(final StrongEmphasis node) {
			render(node);
		}

		@Override
		public void visit(final LinkReferenceDefinition node) {
			render(node);
		}

		@Override
		public void visit(final CustomBlock node) {
			render(node);
		}

		@Override
		public void visit(final CustomNode node) {
			render(node);
		}
	}

}
