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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.datafaker.Faker;

class ReadmeMarkdownDocTest {
	static Faker faker = net.datafaker.Faker.instance();

	ReadmeMarkdownDoc doc;
	PrintStream ps;
	ByteArrayOutputStream baosPs;

	@BeforeEach
	void init() throws Exception {
		doc = new ReadmeMarkdownDoc();
		baosPs = new ByteArrayOutputStream();
		ps = new PrintStream(baosPs);
	}

	@AfterEach
	void ends() {
	}

	List<String> getPrintText() {
		return baosPs.toString().lines().toList();
	}

	@Test
	void testAddComments() {
		final var comment = faker.numerify("comment###");
		doc.addComments(comment);
		doc.writeDoc(ps);

		final var t = getPrintText();
		assertEquals(2, t.size());
		assertThat(t.get(0)).contains(comment, "<!--", "-->");
		assertEquals("", t.get(1));
	}

	@Test
	void testAddContent() {
		final var content = faker.numerify("content###");
		doc.addContent(content);
		doc.writeDoc(ps);

		final var t = getPrintText();
		assertEquals(2, t.size());
		assertThat(t.get(0)).isEqualTo(content);
		assertEquals("", t.get(1));
	}

	@Test
	void testAddSection() {
		final int level = faker.random().nextInt(0, 1000);
		doc.addSection("Title Name", level, "mdContent");
		doc.writeDoc(ps);

		final var t = getPrintText();
		assertEquals(4, t.size());
		assertThat(t.get(0)).isEqualTo("<h" + level + " id=\"titlename\">Title Name</h" + level + ">");
		assertEquals("", t.get(1));
		assertThat(t.get(2)).isEqualTo("mdContent");
		assertEquals("", t.get(3));
	}

	@Test
	void testAddDocumentSummary() {
		doc.addSection("Title Name0", 2, "mdContent");
		doc.addDocumentSummary();
		doc.addSection("Title Name1", 2, "mdContent");
		doc.addSection("Title Name2", 2, "mdContent");
		doc.writeDoc(ps);

		final var t = getPrintText();
		assertEquals(4 * 4, t.size());
		assertEquals("<h2 id=\"titlename0\">Title Name0</h2>", t.get(0));
		assertEquals("> [Title Name0](#titlename0)\\", t.get(4));
		assertEquals("> [Title Name1](#titlename1)\\", t.get(5));
		assertEquals("> [Title Name2](#titlename2)", t.get(6));
		assertEquals("<h2 id=\"titlename1\">Title Name1</h2>", t.get(8));
		assertEquals("<h2 id=\"titlename2\">Title Name2</h2>", t.get(12));
	}

	@Test
	void testWriteDoc() {
		doc.writeDoc(ps);
		assertEquals(0, getPrintText().size());
	}

}
