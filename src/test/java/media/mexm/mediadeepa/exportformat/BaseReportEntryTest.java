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

import org.junit.jupiter.api.BeforeEach;

import net.datafaker.Faker;

public abstract class BaseReportEntryTest {

	static Faker faker = Faker.instance();

	String key;
	ReportEntry entry;

	@BeforeEach
	void initKey() {
		key = faker.numerify("key###");
	}

}
