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

import org.commonmark.node.Node;
import org.commonmark.node.Visitor;

interface MdVisitorTrait extends Visitor {

	default void next(final Node node) {
		if (node == null) {
			return;
		}
		final var next = node.getNext();
		if (next == null) {
			return;
		}
		next.accept(this);
	}

	default void firstChild(final Node node) {
		if (node == null) {
			return;
		}
		final var firstChild = node.getFirstChild();
		if (firstChild == null) {
			return;
		}
		firstChild.accept(this);
	}

}
