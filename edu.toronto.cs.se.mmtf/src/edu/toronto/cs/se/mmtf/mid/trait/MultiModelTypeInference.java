/*
 * Copyright (c) 2012 Marsha Chechik, Alessio Di Sandro, Michalis Famelis,
 * Rick Salay, Vivien Suen.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Alessio Di Sandro - Implementation.
 */
package edu.toronto.cs.se.mmtf.mid.trait;

import java.util.HashSet;

import org.eclipse.ocl.examples.pivot.ExpressionInOcl;
import org.eclipse.ocl.examples.pivot.OCL;
import org.eclipse.ocl.examples.pivot.helper.OCLHelper;

import edu.toronto.cs.se.mmtf.MMTF.MMTFRegistry;
import edu.toronto.cs.se.mmtf.mid.ExtendibleElement;
import edu.toronto.cs.se.mmtf.mid.MidLevel;
import edu.toronto.cs.se.mmtf.mid.Model;
import edu.toronto.cs.se.mmtf.mid.ModelElement;
import edu.toronto.cs.se.mmtf.mid.ModelElementCategory;
import edu.toronto.cs.se.mmtf.mid.TypedElement;
import edu.toronto.cs.se.mmtf.mid.editor.Editor;
import edu.toronto.cs.se.mmtf.mid.relationship.Link;
import edu.toronto.cs.se.mmtf.mid.relationship.ModelRel;
import edu.toronto.cs.se.mmtf.repository.MMTFExtensionPoints;

/**
 * The type inference engine for multimodels.
 * 
 * @author Alessio Di Sandro
 * 
 */
public class MultiModelTypeInference implements MMTFExtensionPoints {

	private static TypedElement inferType(Model model) {

		String modelTypeUri = model.getRoot().eClass().getEPackage().getNsURI();
		ExtendibleElement inferred = MMTFRegistry.getExtendibleType(modelTypeUri);

		//TODO MMTF: polish, a lot
		for (Model lightModelSubtype : MMTFRegistry.getModelTypes()) {
			if (lightModelSubtype instanceof ModelRel) {
				continue;
			}
			if (lightModelSubtype.getSupertype() == inferred && lightModelSubtype.getConstraint() != null) {
				OCL ocl = OCL.newInstance();
				OCLHelper helper = ocl.createOCLHelper();
				helper.setInstanceContext(model.getRoot());
				try {
					ExpressionInOcl constraint = helper.createInvariant(lightModelSubtype.getConstraint().getBody());
					if (ocl.check(model.getRoot(), constraint)) {
						inferred = lightModelSubtype;
						break;
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		// fallback to root type
		if (inferred == null) {
			inferred = MMTFRegistry.getExtendibleType(ROOT_MODEL_URI);
		}

		return inferred;
	}

	private static TypedElement inferType(ModelRel modelRel) {

		// not specialized yet
		if (modelRel.getModels().size() == 0) {
			return MMTFRegistry.getExtendibleType(ROOT_RELATIONSHIP_URI);
		}

		//TODO MMTF: look for light types and evaluate constraints
		TypedElement inferred = null;

modelTypes:
		for (Model modelType : MMTFRegistry.getModelTypes()) {

			// get only model relationship types
			if (!(modelType instanceof ModelRel)) {
				continue;
			}
			ModelRel modelRelType = (ModelRel) modelType;

			// check cardinality
			if (!(modelRelType.isUnbounded() || modelRelType.getModels().size() == modelRel.getModels().size())) {
				continue;
			}

			// unbounded case
			if (modelRelType.isUnbounded()) {
				HashSet<String> allowedModels = new HashSet<String>();
				for (Model model : modelRelType.getModels()) {
					allowedModels.add(model.getUri());
				}
				for (Model model : modelRel.getModels()) {
					String modelTypeUri = model.getRoot().eClass().getEPackage().getNsURI();
					if (!allowedModels.contains(modelTypeUri)) {
						continue modelTypes;
					}
				}
				inferred = modelRelType;
				break;
			}

			//TODO: MMTF continue with other cases
		}

		// fallback to root type
		if (inferred == null) {
			inferred = MMTFRegistry.getExtendibleType(ROOT_RELATIONSHIP_URI);
		}

		return inferred;
	}

	private static TypedElement inferType(ModelElement modelElem) {

		// fallback to root type
		if (modelElem.getCategory() == ModelElementCategory.ENTITY) {
			return MMTFRegistry.getExtendibleType(ROOT_MODEL_ELEMENT_ENTITY_URI);
		}
		else {
			return MMTFRegistry.getExtendibleType(ROOT_MODEL_ELEMENT_RELATIONSHIP_URI);
		}
	}

	private static TypedElement inferType(Link link) {

		// fallback to root type
		return MMTFRegistry.getExtendibleType(ROOT_RELATIONSHIP_LINK_URI);
	}

	private static TypedElement inferType(Editor editor) {

		return MMTFRegistry.getExtendibleType(editor.getUri());
		//TODO MMTF: fallback to root text editor?
	}

	public static TypedElement inferType(TypedElement element) {

		if (element.getLevel() == MidLevel.TYPES) {
			return null;
		}

		if (element instanceof Link) {
			return inferType((Link) element);
		}
		if (element instanceof ModelElement) {
			return inferType((ModelElement) element);
		}
		if (element instanceof ModelRel) {
			return inferType((ModelRel) element);
		}
		if (element instanceof Model) {
			return inferType((Model) element);
		}
		if (element instanceof Editor) {
			return inferType((Editor) element);
		}
		return null;
	}

}