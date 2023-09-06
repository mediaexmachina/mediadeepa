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
package media.mexm.mediadeepa.exportformat.tabular;

import static java.util.Locale.FRANCE;

import java.text.DecimalFormatSymbols;

import com.opencsv.CSVWriterBuilder;

public class CSVFrExportFormat extends CSVExportFormat {

	public CSVFrExportFormat() {
		super();
		highFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(FRANCE));
		lowFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(FRANCE));
	}

	@Override
	public String getFormatLongName() {
		return "French flavor CSV files";
	}

	@Override
	protected CSVWriterBuilder prepareCSVWriter(final CSVWriterBuilder csvWriter) {
		return super.prepareCSVWriter(csvWriter)
				.withSeparator(';');
	}

}
