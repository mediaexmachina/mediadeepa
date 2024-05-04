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
 * Copyright (C) Media ex Machina 2024
 *
 */
package media.mexm.mediadeepa.e2e;

import static java.io.File.pathSeparatorChar;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.forceMkdirParent;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HexFormat;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import media.mexm.mediadeepa.App;

class E2ESingleExportTest extends E2EUtils {

	File outputFile;

	@Test
	void testSimpleExport() throws IOException {
		rawData = prepareMpgForSimpleE2ETests();
		if (rawData == null) {
			return;
		}

		outputFile = new File("target/e2e-single-export", "testSimpleExportLUFS-notjpg.jpg");
		forceMkdirParent(outputFile);
		if (outputFile.exists()) {
			return;
		}
		runAppSingleExport(defaultAppConfig.getGraphicConfig().getLufsGraphicFilename());
		assertThat(outputFile).exists().size().isNotZero();
		checkFileHeader("89 50 4e 47"); // PNG
		openImage();
	}

	@Test
	void testSimpleExport_multipleSource() throws IOException {
		rawData = prepareMpgForSimpleE2ETests();
		if (rawData == null) {
			return;
		}

		outputFile = new File("target/e2e-single-export", "thisCantExits.jpg");
		forceMkdirParent(outputFile);
		assertThat(outputFile).doesNotExist();

		final var params = new String[] { "--temp", "target/e2e-temp",
										  "-i", rawData.archive().getPath(),
										  "-i", rawData.archive().getPath(),
										  "--single-export", outputFile.getPath() };

		assertThat(SpringApplication.exit(SpringApplication.run(App.class, params))).isEqualTo(2);
		assertThat(outputFile).doesNotExist();
	}

	@Test
	void testJPEGExport() throws IOException {
		rawData = prepareMpgForSimpleE2ETests();
		if (rawData == null) {
			return;
		}

		outputFile = new File("target/e2e-single-export", "testGopSizeGraphic.jpg");
		forceMkdirParent(outputFile);
		if (outputFile.exists()) {
			return;
		}
		runAppJPEGSingleExport(defaultAppConfig.getGraphicConfig().getGopSizeGraphicFilename());
		assertThat(outputFile).exists().size().isNotZero();
		checkFileHeader("ff d8 ff e0 00 10 4a 46 49 46 00 01"); // JPEG
		openImage();
	}

	@Test
	void testCSV() throws IOException {
		rawData = prepareMpgForSimpleE2ETests();
		if (rawData == null) {
			return;
		}

		outputFile = new File("target/e2e-single-export", "container-gop.csv");
		forceMkdirParent(outputFile);
		if (outputFile.exists()) {
			return;
		}
		Stream.of(outputFile.getParentFile()
				.listFiles(f -> getExtension(f.getName()).equalsIgnoreCase("csv")))
				.forEach(FileUtils::deleteQuietly);

		runAppSingleExport("container-video-gop.csv");
		assertThat(outputFile).exists().size().isNotZero();
		assertThat(Stream.of(outputFile.getParentFile()
				.listFiles(f -> getExtension(f.getName()).equalsIgnoreCase("csv"))).count()).isEqualTo(1);
		assertThat(outputFile).usingCharset(UTF_8).content().doesNotContain(";");
	}

	@Test
	void testTXT() throws IOException {
		rawData = prepareMpgForSimpleE2ETests();
		if (rawData == null) {
			return;
		}

		outputFile = new File("target/e2e-single-export", "container-gop.txt");
		forceMkdirParent(outputFile);
		if (outputFile.exists()) {
			return;
		}
		runAppSingleExport("container-video-gop.txt");
		assertThat(outputFile).exists().size().isNotZero();
		assertThat(outputFile).usingCharset(UTF_8).content().contains("\t").doesNotContain(",", ";");
	}

	@Test
	void testReport() throws IOException {
		rawData = prepareMpgForSimpleE2ETests();
		if (rawData == null) {
			return;
		}

		outputFile = new File("target/e2e-single-export", "base-report.html");
		forceMkdirParent(outputFile);
		if (outputFile.exists()) {
			return;
		}

		runAppSingleExport(defaultAppConfig.getReportConfig().getHtmlFilename());
		assertThat(outputFile).exists().size().isNotZero();
		assertThat(outputFile).usingCharset(UTF_8).content().startsWith("<!DOCTYPE html>").endsWith("</html>");
	}

	@Test
	void testXLSX() throws IOException {
		rawData = prepareMpgForSimpleE2ETests();
		if (rawData == null) {
			return;
		}

		outputFile = new File("target/e2e-single-export", "table.xlsx");
		forceMkdirParent(outputFile);
		if (outputFile.exists()) {
			return;
		}

		runAppSingleExport(defaultAppConfig.getXslxtableFileName());
		assertThat(outputFile).exists().size().isNotZero();
	}

	@Test
	void testXML() throws IOException {
		rawData = prepareMpgForSimpleE2ETests();
		if (rawData == null) {
			return;
		}

		outputFile = new File("target/e2e-single-export", "table.xml");
		forceMkdirParent(outputFile);
		if (outputFile.exists()) {
			return;
		}

		runAppSingleExport(defaultAppConfig.getXmltableFileName());
		assertThat(outputFile).exists().size().isNotZero();
		assertThat(outputFile).usingCharset(UTF_8).content()
				.startsWith("<?xml version='1.0' encoding='UTF-8'?><report>").endsWith("</report>");
	}

	@Test
	void testFFProbe() throws IOException {
		rawData = prepareMpgForSimpleE2ETests();
		if (rawData == null) {
			return;
		}

		outputFile = new File("target/e2e-single-export", "ffpr.xml");
		forceMkdirParent(outputFile);
		if (outputFile.exists()) {
			return;
		}

		runAppSingleExport(defaultAppConfig.getFfprobexmlFileName());
		assertThat(outputFile).exists().size().isNotZero();
		assertThat(outputFile).usingCharset(UTF_8).content()
				.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
				.contains("<ffprobe>", "<streams>", "<stream ", "<format ")
				.endsWith("</ffprobe>");
	}

	private void runAppSingleExport(final String what) {
		runApp(
				"--temp", "target/e2e-temp",
				"-i", rawData.archive().getPath(),
				"--single-export", what + pathSeparatorChar + outputFile.getAbsolutePath());
	}

	private void runAppJPEGSingleExport(final String what) {
		runApp(
				"--temp", "target/e2e-temp",
				"-i", rawData.archive().getPath(),
				"--graphic-jpg",
				"--single-export", what + pathSeparatorChar + outputFile.getAbsolutePath());
	}

	private void checkFileHeader(final String hex) throws IOException, FileNotFoundException {
		final var ref = HexFormat.ofDelimiter(" ").parseHex(hex);
		try (var fis = new FileInputStream(outputFile)) {
			final var buffer = new byte[ref.length];
			assertEquals(buffer.length, IOUtils.read(fis, buffer, 0, buffer.length));
			assertThat(buffer).isEqualTo(ref);
		}
	}

	private void openImage() throws IOException {
		final var image = ImageIO.read(outputFile);
		assertThat(image).isNotNull();
	}

}
