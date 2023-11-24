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

import static java.util.Locale.US;
import static java.util.stream.Collectors.joining;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
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

import picocli.CommandLine.Model.OptionSpec;

public class ManPage implements Visitor {
	private static final String START_UNDERLINE = "\\fI";
	private static final String ENDS_UNDERLINE = "\\fR";
	private static final String START_INCR = ".RS 4";
	private static final String ENDS_INCR = ".RE";
	private static final String QUOTESPACEQUOTE = "\" \"";

	private final PrintStream ps;
	private final SimpleDateFormat dateFormat;
	private boolean inBlockQuote;
	private boolean inListItem;

	public ManPage(final PrintStream ps) {
		this.ps = ps;
		dateFormat = new SimpleDateFormat("MM/dd/yyyy", US);
		inListItem = false;
		inBlockQuote = false;
	}

	private String escape(final String text) {
		return text
				.replace(".", "\\&.")
				.replace("-", "\\-");
	}

	private void addEscaped(final String text) {
		ps.print(escape(text));
	}

	public void startSection(final String name) {
		ps.print(".SH");
		ps.print(" \"");
		addEscaped(name.toUpperCase());
		ps.print("\"");
		ps.println();
	}

	public void text(final String... texts) {
		addEscaped(Stream.of(texts)
				.filter(Objects::nonNull)
				.map(String::trim)
				.collect(joining(" "))
				.trim());
		ps.println();
	}

	private void next(final Node node) {
		if (node == null) {
			return;
		}
		final var next = node.getNext();
		if (next == null) {
			return;
		}
		next.accept(this);
	}

	private void firstChild(final Node node) {
		if (node == null) {
			return;
		}
		final var firstChild = node.getFirstChild();
		if (firstChild == null) {
			return;
		}
		firstChild.accept(this);
	}

	@Override
	public void visit(final Document document) {
		firstChild(document);
		ps.println();
	}

	@Override
	public void visit(final Paragraph paragraph) {
		if (inListItem) {
			ps.print("\\- ");
		}
		firstChild(paragraph);
		if (inBlockQuote == false) {
			ps.println();
			ps.println(".PP");
		}
		next(paragraph);
	}

	@Override
	public void visit(final StrongEmphasis strongEmphasis) {
		ps.print(START_UNDERLINE);
		firstChild(strongEmphasis);
		ps.print(ENDS_UNDERLINE);
		next(strongEmphasis);
	}

	@Override
	public void visit(final Text text) {
		addEscaped(text.getLiteral());
		next(text);
	}

	@Override
	public void visit(final Link link) {
		firstChild(link);
		ps.print(" " + START_UNDERLINE);
		addEscaped(link.getDestination());
		ps.print(ENDS_UNDERLINE);
		next(link);
	}

	@Override
	public void visit(final BlockQuote blockQuote) {
		inBlockQuote = true;
		ps.println(START_INCR);
		firstChild(blockQuote);
		ps.println();
		ps.println(ENDS_INCR);
		inBlockQuote = false;
		next(blockQuote);
	}

	@Override
	public void visit(final BulletList bulletList) {
		ps.println(START_INCR);
		firstChild(bulletList);
		ps.println(ENDS_INCR);
		next(bulletList);
	}

	@Override
	public void visit(final Code code) {
		ps.print("\\fB");
		addEscaped(code.getLiteral());
		ps.print(ENDS_UNDERLINE);
		firstChild(code);
		next(code);
	}

	@Override
	public void visit(final ListItem listItem) {
		inListItem = true;
		firstChild(listItem);
		inListItem = false;
		next(listItem);
	}

	@Override
	public void visit(final Emphasis node) {
		firstChild(node);
		next(node);
	}

	@Override
	public void visit(final FencedCodeBlock fencedCodeBlock) {
		ps.println(START_INCR);
		fencedCodeBlock.getLiteral()
				.lines()
				.forEach(l -> {
					addEscaped(l);
					ps.println();
				});
		ps.println(ENDS_INCR);
		ps.println(".PP");
		firstChild(fencedCodeBlock);
		next(fencedCodeBlock);
	}

	@Override
	public void visit(final HardLineBreak node) {
		firstChild(node);
		ps.println();
		next(node);
	}

	@Override
	public void visit(final Heading node) {
		firstChild(node);
		ps.println();
		next(node);
	}

	@Override
	public void visit(final ThematicBreak node) {
		firstChild(node);
		ps.println();
		next(node);
	}

	@Override
	public void visit(final HtmlInline node) {
		firstChild(node);
		next(node);
	}

	@Override
	public void visit(final HtmlBlock node) {
		firstChild(node);
		ps.println();
		next(node);
	}

	@Override
	public void visit(final Image image) {
		/**
		 * Images are not managed for man pages...
		 */
	}

	@Override
	public void visit(final IndentedCodeBlock node) {
		firstChild(node);
		ps.println();
		next(node);
	}

	@Override
	public void visit(final OrderedList node) {
		firstChild(node);
		ps.println();
		next(node);
	}

	@Override
	public void visit(final SoftLineBreak node) {
		firstChild(node);
		ps.println();
		next(node);
	}

	@Override
	public void visit(final LinkReferenceDefinition node) {
		firstChild(node);
		ps.println();
		next(node);
	}

	@Override
	public void visit(final CustomBlock node) {
		firstChild(node);
		ps.println();
		next(node);
	}

	@Override
	public void visit(final CustomNode node) {
		firstChild(node);
		ps.println();
		next(node);
	}

	public void printGroupOptions(final Optional<String> groupHeading, final Stream<OptionSpec> options) {
		groupHeading.ifPresent(h -> {
			ps.print("==== ");
			addEscaped(h);
			ps.println(" ====");
			ps.println(".PP");
		});

		options.forEach(option -> {
			ps.print(Stream.of(option.names())
					.map(this::escape)
					.map(n -> "\\fB" + n + "\\fR")
					.collect(joining(", ")));

			final var paramLabel = option.paramLabel();
			if (paramLabel.startsWith("<") == false && paramLabel.endsWith(">") == false) {
				ps.print(" ");
				ps.print(START_UNDERLINE);
				addEscaped(paramLabel);
				ps.print(ENDS_UNDERLINE);
			}

			addEscaped(option.required() ? " (required)" : "");
			addEscaped(option.isMultiValue() ? " [can be used multiple times]" : "");

			ps.println();
			ps.println(".PP");
			ps.println(START_INCR);
			Stream.of(option.description())
					.forEach(d -> {
						addEscaped(d);
						ps.println();
						ps.println(".PP");
					});
			ps.println(ENDS_INCR);
			ps.println();
		});
	}

	public void addExitReturnCodes(final Map<String, String> rCodes) {
		rCodes.keySet()
				.stream()
				.sorted()
				.forEach(code -> {
					addEscaped(code);
					ps.println();
					ps.println(START_INCR);
					addEscaped(rCodes.get(code));
					ps.println();
					ps.println(ENDS_INCR);
				});
	}

	public void addCopyright(final List<String> version, final Object... values) {
		addPrintfEscaped(version, values);
	}

	public void addPrintfEscaped(final List<String> lines, final Object... values) {
		lines.forEach(v -> {
			final var f = new Formatter();
			f.format(v, values);
			addEscaped(f.toString());
			ps.println();
			ps.println(".PP");
			f.close();
		});
	}

	/**
	 * Like .TH "MEDIADEEPA" "1" "11/24/2022" "mediadeepa 4\&.8\&.1" ""
	 */
	public void addDocumentHeader(final String appName,
								  final String manType,
								  final Date date,
								  final String packageName,
								  final String packageVersion) {
		final var formatedDate = dateFormat.format(date);
		ps.println(".\\\" t");
		ps.println(".\\\"     Title: " + appName);
		ps.println(".\\\"    Author: Media ex Machina / hdsdi3g ");
		ps.println(".\\\" Generator: " + appName);
		ps.println(".\\\"      Date: " + formatedDate);
		ps.println(".\\\"    Source: " + packageName + " " + packageVersion);
		ps.println(".\\\"  Language: English");
		ps.print(".TH");
		ps.print(" \"");
		ps.print(appName.toUpperCase());
		ps.print(QUOTESPACEQUOTE);
		ps.print(manType);
		ps.print(QUOTESPACEQUOTE);
		ps.print(formatedDate);
		ps.print(QUOTESPACEQUOTE);
		addEscaped(packageName);
		ps.print(" ");
		addEscaped(packageVersion);
		ps.print("\" \"\"");
		ps.println();

		final var dashLine = ".\\\" " + StringUtils.repeat("-", 65);
		ps.println(dashLine);
		ps.println(".ie \\n(.g .ds Aq \\(aq");
		ps.println(".el       .ds Aq '");
		ps.println(dashLine);
		ps.println(".\\\"Autogenerated document ; based on man login.");
		ps.println(dashLine);
		ps.println(".\\\"Set default formatting");
		ps.println(dashLine);
		ps.println(".\\\"Disable hyphenation");
		ps.println(".nh");
		ps.println(".\\\"Disable justification (adjust text to left margin only)");
		ps.println(".ad l");
		ps.println(dashLine);
		ps.println(".\\\"MAIN CONTENT STARTS HERE");
		ps.println(dashLine);
	}

}
