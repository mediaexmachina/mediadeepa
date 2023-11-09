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

public interface NumericReportEntry {

	String key();

	default String unit() {
		return null;
	}

	default String getKeyWithPlurial(final boolean plurial) {
		return getWithPlurial(key(), plurial);
	}

	default String getUnitWithPlurial(final boolean plurial) {
		return getWithPlurial(unit(), plurial);
	}

	default String getWithPlurial(final String key, final boolean plurial) {
		if (key == null || key.isEmpty()) {
			return "";
		}
		if (plurial) {
			return key.replace("(s)", "s");
		} else {
			return key.replace("(s)", "");
		}
	}

}
