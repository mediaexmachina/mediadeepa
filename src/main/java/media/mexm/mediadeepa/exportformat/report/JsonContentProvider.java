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
 * Copyright (C) Media ex Machina 2024
 *
 */
package media.mexm.mediadeepa.exportformat.report;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import media.mexm.mediadeepa.exportformat.report.EventReportEntry.EventReportEntryHeader;

public sealed interface JsonContentProvider
											permits
											ReportDocument,
											ReportSection,
											SimpleKeyValueReportEntry,
											SimpleKeyValueListReportEntry,
											RatioReportEntry,
											ResolutionReportEntry,
											NumericUnitValueReportEntry,
											KeyPreValueReportEntry,
											EventReportEntry,
											EventReportEntryHeader,
											CropEventTableReportEntry,
											CropEventReportEntry,
											ReportEntryStream,
											ReportEntryStreamList,
											ReportEntrySubset,
											StatisticsUnitValueReportEntry {
	Logger internalLog = getLogger(JsonContentProvider.class);

	void toJson(JsonGenerator gen,
				SerializerProvider provider) throws IOException;

	default String jsonHeader(final String text) {
		var result = text
				.trim()
				.replace(" ", "_")
				.replace(",", "_")
				.replace("/", "_")
				.replace("-", "_")
				.replace("(s)", "s")
				.replace("(", "_")
				.replace(")", "_")
				.replace("_____", "_")
				.replace("____", "_")
				.replace("___", "_")
				.replace("__", "_")
				.toLowerCase();

		if (result.startsWith("_")) {
			result = result.substring(1);
		}
		if (result.endsWith("_")) {
			result = result.substring(0, result.length() - 1);
		}

		return result;
	}

	default void writeObject(final Object value, final JsonGenerator gen) {
		if (value == null) {
			return;
		}
		try {
			gen.writeObject(value);
		} catch (final JsonGenerationException e) {
			internalLog.error("Can't serialize Json on {}: {}", value.getClass(), e);
		} catch (final IOException e) {
			throw new UncheckedIOException("Json", e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static SimpleModule createModule() {
		final var module = new SimpleModule();

		Stream.of(JsonContentProvider.class.getPermittedSubclasses())
				.forEach(c -> module.addSerializer(c, new StdSerializer(c) {

					@Override
					public void serialize(final Object value,
										  final JsonGenerator gen,
										  final SerializerProvider provider) throws IOException {
						try {
							c.getMethod("toJson", JsonGenerator.class, SerializerProvider.class)
									.invoke(value, gen, provider);
						} catch (NoSuchMethodException | SecurityException | IllegalAccessException
								 | IllegalArgumentException | InvocationTargetException e) {
							throw new IllegalCallerException(e);
						}
					}

				}));

		return module;
	}

}
