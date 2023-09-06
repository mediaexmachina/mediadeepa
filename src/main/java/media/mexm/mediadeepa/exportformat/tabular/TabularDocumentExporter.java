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
 * Copyright (C) Media ex Machina 2023
 *
 */
package media.mexm.mediadeepa.exportformat.tabular;

import static java.util.Locale.ENGLISH;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;

public interface TabularDocumentExporter {

	String HIGH_DECIMAL_FORMAT = "#.#####";
	String LOW_DECIMAL_FORMAT = "#.#";

	byte[] getDocument(List<String> header, List<List<String>> lines);

	String getDocumentFileExtension();

	String formatToString(float value, boolean lowPrecison);

	static DecimalFormat getENHighDecimalFormat() {
		final var format = new DecimalFormat(HIGH_DECIMAL_FORMAT);
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(ENGLISH));
		return format;
	}

	static DecimalFormat getENLowDecimalFormat() {
		final var format = new DecimalFormat(LOW_DECIMAL_FORMAT);
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(ENGLISH));
		return format;
	}

}
