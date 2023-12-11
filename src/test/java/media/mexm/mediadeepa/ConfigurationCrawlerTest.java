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
package media.mexm.mediadeepa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import media.mexm.mediadeepa.components.CLIRunner;
import media.mexm.mediadeepa.config.AppConfig;

@SpringBootTest
class ConfigurationCrawlerTest {

	@Autowired
	AppConfig appConfig;
	@Autowired
	ConfigurationCrawler cc;

	@MockBean
	CLIRunner clirunner;

	@Test
	void test() {
		assertThat(cc.parse()).hasSizeGreaterThan(30);
		assertEquals("mediadeepa", cc.getRootPrefix());
	}

}
