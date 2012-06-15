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

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.ocl.examples.pivot.ExpressionInOCL;
import org.eclipse.ocl.examples.pivot.OCL;
import org.eclipse.ocl.examples.pivot.Type;
import org.eclipse.ocl.examples.pivot.ecore.Ecore2Pivot;
import org.eclipse.ocl.examples.pivot.helper.OCLHelper;
import org.eclipse.ocl.examples.pivot.manager.MetaModelManager;

import edu.toronto.cs.se.mmtf.MMTF.MMTFRegistry;
import edu.toronto.cs.se.mmtf.MMTFException;
import edu.toronto.cs.se.mmtf.mid.MidLevel;
import edu.toronto.cs.se.mmtf.mid.Model;
import edu.toronto.cs.se.mmtf.mid.ModelElement;
import edu.toronto.cs.se.mmtf.mid.ModelElementCategory;
import edu.toronto.cs.se.mmtf.mid.MultiModel;
import edu.toronto.cs.se.mmtf.mid.relationship.Link;
import edu.toronto.cs.se.mmtf.mid.relationship.ModelElementReference;
import edu.toronto.cs.se.mmtf.mid.relationship.ModelReference;
import edu.toronto.cs.se.mmtf.mid.relationship.ModelRel;

/**
 * The constraint checker for multimodels.
 * 
 * @author alessio
 *
 */
public class MultiModelConstraintChecker {

	private final static String ENTITY_WILDCARD_CLASSIFIER_NAME = "ModelElementEntityWildcard";
	private final static String RELATIONSHIP_WILDCARD_FEATURE_NAME = "modelElementRelationshipWildcard";

	/**
	 * Checks if a multimodel (the root of a MID diagram) is a TYPES multimodel
	 * or an INSTANCES multimodel.
	 * 
	 * @param multiModel
	 *            The multimodel to be checked.
	 * @return True for INSTANCES level, false for TYPES level.
	 */
	public static boolean isInstanceLevel(MultiModel multiModel) {

		if (multiModel.getLevel() == MidLevel.TYPES) {
			return false;
		}
		return true;
	}

	/**
	 * Checks if a model relationship (the root of a Relationship diagram) is a
	 * TYPES model relationship or an INSTANCES model relationship.
	 * 
	 * @param modelRel
	 *            The model relationship to be checked.
	 * @return True for INSTANCES level, false for TYPES level.
	 */
	public static boolean isInstanceLevel(ModelRel modelRel) {

		if (modelRel.getLevel() == MidLevel.TYPES) {
			return false;
		}
		return true;
	}

	public static boolean isAllowedModelType(ModelRel modelRelType) {

		for (ModelRel subModelRelType : MMTFRegistry.getModelRelTypes()) {
			if (MMTFRegistry.isSubtypeOf(subModelRelType.getUri(), modelRelType.getUri())) {
				return false;
			}
		}

		return true;
	}	

	public static boolean isAllowedModel(ModelRel modelRel, Model model) {

		if (model == null) { // model not added yet
			return true;
		}

		String modelTypeUri = model.getMetatypeUri();
		boolean okModel = false;
		for (Model modelType : ((ModelRel) modelRel.getMetatype()).getModels()) {
			if (modelType.getUri().equals(modelTypeUri) || MMTFRegistry.isSubtypeOf(modelTypeUri, modelType.getUri())) {
				okModel = true;
				break;
			}
		}

		return okModel;
	}

	public static boolean isAllowedModelElementTypeReference(Link linkType) {

		return true;
	}

	public static boolean isAllowedModelElementReference(Link link, ModelElementReference modelElemRef) {

		if (modelElemRef == null) { // model element reference not added yet
			return true;
		}

		String modelElemTypeUri = modelElemRef.getObject().getMetatypeUri();
		boolean okElement = false;
		for (ModelElementReference modelElemTypeRef : ((Link) link.getMetatype()).getElementRefs()) {
			ModelElement modelElemType = (ModelElement) modelElemTypeRef.getObject();
			// MMTFRegistry.isSubtypeOf(modelElemTypeUri, modelElemType.getUri()) check is not needed here
			if (modelElemType.getUri().equals(modelElemTypeUri)) {
				okElement = true;
				break;
			}
		}

		return okElement;
	}

	private static boolean isAllowedModelElement(ModelElement elementType, EObject droppedElement) {

		// entity-relationship differences
		String metaName;
		if (droppedElement instanceof EReference) {
			if (elementType.getCategory() != ModelElementCategory.RELATIONSHIP) {
				return false;
			}
			EStructuralFeature feature = (EStructuralFeature) elementType.getPointer();
			metaName = feature.getName();
			if (metaName == RELATIONSHIP_WILDCARD_FEATURE_NAME) {
				return true;
			}
		}
		else {
			if (elementType.getCategory() != ModelElementCategory.ENTITY) {
				return false;
			}
			EClassifier classifier = (EClassifier) elementType.getPointer();
			metaName = classifier.getName();
			if (metaName == ENTITY_WILDCARD_CLASSIFIER_NAME) {
				return true;
			}
		}

		// check dropped element compliance
		String droppedClassLiteral = MMTFRegistry.getDroppedElementClassLiteral(MidLevel.INSTANCES, droppedElement);
		if (elementType.getClassLiteral().equals(droppedClassLiteral)) {
			return true;
		}

		return false;
	}

	public static ModelElement getAllowedModelElementType(ModelReference modelRef, EObject droppedElement) {

		ModelRel modelRelType = (ModelRel) ((ModelRel) modelRef.eContainer()).getMetatype();
		for (Model modelType : modelRelType.getModels()) {
			for (ModelElement modelElemType : modelType.getElements()) {
				if (isAllowedModelElement(modelElemType, droppedElement)) {
					return modelElemType;
				}
			}
		}

		return null;
	}

	/**
	 * Checks if an OCL constraint is satisfied on a model.
	 * 
	 * @param model
	 *            The model.
	 * @param oclConstraint
	 *            The OCL constraint.
	 * @return True if the OCL constraint is satisfied, false otherwise.
	 */
	public static boolean checkOCLConstraint(Model model, String oclConstraint) {

		if (oclConstraint.equals("")) { // empty constraint
			return true;
		}

		EObject root = model.getRoot();
		OCL ocl = OCL.newInstance();
		OCLHelper helper = ocl.createOCLHelper();
		//TODO MMTF: workaround for bug #375485
		//helper.setInstanceContext(root);
		MetaModelManager metaModelManager = helper.getOCL().getMetaModelManager();
		EClass eClass = root.eClass();
		Type pivotType = metaModelManager.getPivotType(eClass.getName());
		if (pivotType == null) {
			Resource resource = eClass.eResource();
			Ecore2Pivot ecore2Pivot = Ecore2Pivot.getAdapter(resource, metaModelManager);
			pivotType = ecore2Pivot.getCreated(Type.class, eClass);
		}
		helper.setContext(pivotType);

		try {
			ExpressionInOCL constraint = helper.createInvariant(oclConstraint);
			return ocl.check(root, constraint);
		}
		catch (Exception e) {
			MMTFException.print(MMTFException.Type.WARNING, "Constraint error: " + oclConstraint, e);
			return false;
		}
	}

}
