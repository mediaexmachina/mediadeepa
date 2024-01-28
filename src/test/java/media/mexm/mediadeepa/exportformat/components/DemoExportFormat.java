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
package media.mexm.mediadeepa.exportformat.components;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.cli.ExportToCmd;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormat;
import net.datafaker.Faker;

@Component
public class DemoExportFormat implements ExportFormat {
	static Faker faker = net.datafaker.Faker.instance();

	String formatName;
	String formatLongName;
	String formatDescr;
	Map<String, File> exportResult;
	List<DataResult> capturedResults;
	String singleExportFileName;
	List<String> singleExport;
	byte[] singleExportData;

	public DemoExportFormat() {
		formatLongName = faker.numerify("formatLongName###");
		formatName = faker.numerify("formatName###");
		formatDescr = faker.numerify("formatDescr###");
		exportResult = new HashMap<>(Map.of(
				faker.numerify("resultKey###"),
				new File(faker.numerify("resultValue###"))));
		capturedResults = new ArrayList<>();
		singleExport = new ArrayList<>();
		singleExportData = "A".getBytes();
		singleExportFileName = faker.numerify("singleExportFileName###");
	}

	@Override
	public Optional<byte[]> makeSingleExport(final DataResult result,
											 final String internalFileName) {
		singleExport.add(internalFileName);
		return Optional.ofNullable(singleExportData);
	}

	@Override
	public Set<String> getInternalProducedFileNames() {
		return Set.of(singleExportFileName);
	}

	@Override
	public Map<String, File> exportResult(final DataResult result, final ExportToCmd exportToCmd) {
		capturedResults.add(result);
		return exportResult;
	}

	@Override
	public String getFormatName() {
		return formatName;
	}

	@Override
	public String getFormatLongName() {
		return formatLongName;
	}

	@Override
	public String getFormatDescription() {
		return formatDescr;
	}

	public List<DataResult> getCapturedResults() {
		return capturedResults;
	}
}
