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
package media.mexm.mediadeepa.exportformat;

import static java.util.Collections.unmodifiableMap;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExportFormatManager {

	private final Map<String, ExportFormat> formats;

	public ExportFormatManager() {
		formats = new LinkedHashMap<>();
	}

	public synchronized void register(final String name, final ExportFormat exportFormat) {
		if (isFormatExists(name)) {
			throw new IllegalArgumentException(name + " was previously registed");
		}
		log.debug("Add {} to internal list", name);
		formats.put(name, exportFormat);
	}

	public synchronized boolean isFormatExists(final String name) {
		return formats.containsKey(name);
	}

	public ExportFormat getExportFormat(final String name) {
		if (formats.containsKey(name) == false) {
			throw new IllegalArgumentException("Can't found " + name + " format");
		}
		return formats.get(name);
	}

	public Map<String, String> getRegisted() {
		final var result = new LinkedHashMap<String, String>();
		formats.forEach((k, v) -> result.put(k, v.getFormatLongName()));
		return unmodifiableMap(result);
	}

}
