/**
 * Copyright (c) 2013 Marsha Chechik, Alessio Di Sandro, Michalis Famelis,
 * Rick Salay, Vivien Suen.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Alessio Di Sandro - Implementation.
 */
package edu.toronto.cs.se.mmtf.repository;

import edu.toronto.cs.se.mmtf.mid.Model;

public abstract class JavaModelConstraint {

	protected Model model;

	public JavaModelConstraint(Model model) {

		this.model = model;
	}

	public abstract boolean validate();

}
