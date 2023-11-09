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
package media.mexm.mediadeepa.service;

import static java.util.Collections.unmodifiableMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.cli.ExportToCmd;
import media.mexm.mediadeepa.components.ExportFormatComparator;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ExportFormat;

@Service
@Slf4j
public class MediaAnalyticsTransformerServiceImpl implements MediaAnalyticsTransformerService {

	@Autowired
	private List<ExportFormat> exportFormatList;
	@Autowired
	private ExportFormatComparator exportFormatComparator;

	private ExportFormat getExportFormatByName(final String name) {
		return exportFormatList.stream()
				.filter(f -> name.equalsIgnoreCase(f.getFormatName()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Can't found " + name + " format"));
	}

	@Override
	public boolean isExportFormatExists(final String name) {
		return exportFormatList.stream()
				.anyMatch(f -> name.equalsIgnoreCase(f.getFormatName()));
	}

	@Override
	public Map<String, String> getExportFormatInformation() {
		final var result = new LinkedHashMap<String, String>();
		exportFormatList.stream()
				.sorted(exportFormatComparator)
				.forEach(eF -> result.put(eF.getFormatName(), eF.getFormatLongName()));
		return unmodifiableMap(result);
	}

	@Override
	public void exportAnalytics(final DataResult result,
								final ExportToCmd exportToCmd) {
		exportToCmd.getFormat().stream()
				.map(this::getExportFormatByName)
				.forEach(s -> {
					log.debug("Start export with {}/{}", s.getFormatName(), s.getFormatLongName());
					final var producedFiles = s.exportResult(result, exportToCmd);
					log.debug("Produced files: {}", producedFiles);
				});
	}

}
