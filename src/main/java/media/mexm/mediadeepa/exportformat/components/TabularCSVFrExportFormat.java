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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.opencsv.CSVWriterBuilder;

import media.mexm.mediadeepa.components.NumberUtils;
import media.mexm.mediadeepa.rendererengine.TabularRendererEngine;

@Component
public class TabularCSVFrExportFormat extends TabularCSVExportFormat {

	public TabularCSVFrExportFormat(@Autowired final List<TabularRendererEngine> engines,
									@Autowired final NumberUtils numberUtils) {
		super(engines, numberUtils);
	}

	@Override
	public String getFormatName() {
		return "csvfr";
	}

	@Override
	public String getFormatLongName() {
		return "French flavor CSV files";
	}

	@Override
	public String getFormatDescription() {
		return "semicolon separated, \"`,`\" decimal separator";
	}

	@Override
	public String formatNumberHighPrecision(final float value) {
		return numberUtils.formatDecimalSimple5Fr(value);
	}

	@Override
	public String formatNumberLowPrecision(final float value) {
		return numberUtils.formatDecimalSimple1Fr(value);
	}

	@Override
	protected CSVWriterBuilder prepareCSVWriter(final CSVWriterBuilder csvWriter) {
		return super.prepareCSVWriter(csvWriter)
				.withSeparator(';');
	}

}
