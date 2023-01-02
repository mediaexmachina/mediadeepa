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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {

	public static void main(final String[] args) {
		System.setProperty("spring.main.web-application-type", "NONE");
		System.setProperty("spring.main.banner-mode", "off");
		System.setProperty("spring.main.log-startup-info", "false");

		System.exit(SpringApplication.exit(SpringApplication.run(App.class, args)));

		/*
		 * https://www.innoq.com/en/articles/2022/01/java-cli-libraries/
		 * https://fullstackdeveloper.guru/2020/06/18/how-to-create-a-command-line-tool-using-java/
		 * https://picocli.info/#_spring_boot_example
		 * */

	}

}
