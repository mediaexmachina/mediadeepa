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

import java.io.IOException;

import media.mexm.mediadeepa.ImpExArchiveExtractionSession;
import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ExtractTo;
import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ImportFrom;

public interface ImpExArchiveService {

	void saveTo(ExtractTo extractTo, ImpExArchiveExtractionSession session) throws IOException;

	ImpExArchiveExtractionSession loadFrom(ImportFrom importFrom) throws IOException;

}
