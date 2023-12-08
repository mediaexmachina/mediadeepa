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

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
		log.info("Start export result files...");
		exportToCmd.getFormat().stream()
				.map(this::getExportFormatByName)
				.forEach(s -> doExportAnalytic(result, exportToCmd, s));
	}

	private void doExportAnalytic(final DataResult result, final ExportToCmd exportToCmd, final ExportFormat s) {
		final var now = System.currentTimeMillis();
		log.info("Export with {} ({})...", s.getFormatLongName(), s.getFormatName());
		final var producedFiles = s.exportResult(result, exportToCmd);
		final var duration = (System.currentTimeMillis() - now) / 1000f;
		if (producedFiles.isEmpty()) {
			log.info("{} has not product files (it take to {} sec)", s.getFormatName(), duration);
		} else if (producedFiles.size() == 1) {
			final var key = producedFiles.entrySet()
					.stream()
					.map(Entry::getKey)
					.findFirst()
					.orElse("");
			final var file = producedFiles.entrySet()
					.stream()
					.map(Entry::getValue)
					.map(File::getPath)
					.findFirst()
					.orElse("");

			log.info("{} has product {} file, in {} sec: {}",
					s.getFormatName(),
					key,
					duration,
					file);
		} else {
			log.info("{} has product {} files, in {} sec:",
					s.getFormatName(), producedFiles.size(), duration);
			producedFiles.entrySet()
					.forEach(entry -> log.info("{} has product {}: {}",
							s.getFormatName(), entry.getKey(), entry.getValue().getPath()));
		}
	}

}
