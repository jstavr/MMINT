/*
 * Copyright (c) 2012 Marsha Chechik, Alessio Di Sandro, Michalis Famelis,
 * Rick Salay, Vivien Suen.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Alessio Di Sandro, Vivien Suen - Implementation.
 */
package edu.toronto.cs.se.modelepedia.operator.propagate;

import java.util.HashSet;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;

import edu.toronto.cs.se.mmtf.MultiModelTypeRegistry;
import edu.toronto.cs.se.mmtf.mavo.MAVOElement;
import edu.toronto.cs.se.mmtf.mid.Model;
import edu.toronto.cs.se.mmtf.mid.ModelElement;
import edu.toronto.cs.se.mmtf.mid.ModelOrigin;
import edu.toronto.cs.se.mmtf.mid.MultiModel;
import edu.toronto.cs.se.mmtf.mid.operator.impl.OperatorExecutableImpl;
import edu.toronto.cs.se.mmtf.mid.relationship.BinaryLink;
import edu.toronto.cs.se.mmtf.mid.relationship.BinaryLinkReference;
import edu.toronto.cs.se.mmtf.mid.relationship.BinaryModelRel;
import edu.toronto.cs.se.mmtf.mid.relationship.Link;
import edu.toronto.cs.se.mmtf.mid.relationship.LinkReference;
import edu.toronto.cs.se.mmtf.mid.relationship.ModelElementEndpointReference;
import edu.toronto.cs.se.mmtf.mid.relationship.ModelElementReference;
import edu.toronto.cs.se.mmtf.mid.relationship.ModelEndpointReference;
import edu.toronto.cs.se.mmtf.mid.relationship.RelationshipPackage;
import edu.toronto.cs.se.mmtf.mid.trait.MultiModelConstraintChecker;
import edu.toronto.cs.se.mmtf.mid.trait.MultiModelHierarchyUtils;
import edu.toronto.cs.se.mmtf.mid.trait.MultiModelInstanceFactory;
import edu.toronto.cs.se.mmtf.mid.trait.MultiModelRegistry;
import edu.toronto.cs.se.mmtf.mid.trait.MultiModelTypeIntrospection;

public class ChangePropagation extends OperatorExecutableImpl {

	private final static String PROP_MODEL_SUFFIX = "_propagated";
	private final static String PROPREFINEMENT_MODELREL_NAME = "propRefinement";
	private final static String PROPTRACE_MODELREL_NAME = "propTrace";
	private final static String PROPTRACE_RULE4_LINK_NAME = "rule4Trace";
	private final static String NAME_FEATURE = "name";

	//TODO MMTF: make this a library function, accessible with ctrl+c/ctrl+v
	private Model createRelatedModelCopy(Model relatedModel) throws Exception {

		// copy and serialize new model
		EObject relatedRoot = relatedModel.getRoot();
		EObject copyRoot = EcoreUtil.copy(relatedRoot);
		String fileExtension = relatedModel.getUri().substring(
			relatedModel.getUri().lastIndexOf('.'),
			relatedModel.getUri().length()
		);
		String uri = relatedModel.getUri().replace(fileExtension, PROP_MODEL_SUFFIX + fileExtension);
		URI newCopyUri = MultiModelTypeIntrospection.writeRoot(copyRoot, uri, true);

		// create model in multimodel
		MultiModel multiModel = (MultiModel) relatedModel.eContainer();
		Model newCopyModel = MultiModelInstanceFactory.createModel(null, ModelOrigin.IMPORTED, multiModel, newCopyUri, true);

		return newCopyModel;
	}

	private EList<BinaryLinkReference> propagateTraceLinksFromRefinements(LinkReference refinementLinkRef, BinaryModelRel traceRel, Model newPropModel, BinaryModelRel newPropTraceRel) throws Exception {

		ModelEndpointReference origModelEndpointRef_traceRel = traceRel.getModelEndpointRefs().get(0);
		String origModelUri = origModelEndpointRef_traceRel.getTargetUri();

		// get model element refs in trace rel that got refined
		EList<ModelElementReference> refinedModelElemRefs_refinementRel = new BasicEList<ModelElementReference>();
		EList<ModelElementReference> origModelElemRefs_traceRel = new BasicEList<ModelElementReference>();
		for (ModelElementEndpointReference refinementModelElemEndpointRef : refinementLinkRef.getModelElemEndpointRefs()) {
			ModelElementReference refinementModelElemRef = refinementModelElemEndpointRef.getModelElemRef();
			if (((ModelEndpointReference) refinementModelElemRef.eContainer()).getTargetUri().equals(origModelUri)) { // orig model element ref in trace rel
				ModelElementReference origModelElemRef_traceRel = MultiModelHierarchyUtils.getReference(refinementModelElemRef.getUri(), origModelEndpointRef_traceRel.getModelElemRefs());
				if (origModelElemRef_traceRel == null) {
					continue;
				}
				origModelElemRefs_traceRel.add(origModelElemRef_traceRel);
			}
			else { // refined model element ref in refinement rel
				refinedModelElemRefs_refinementRel.add(refinementModelElemRef);
			}
		}

		// no propagation or rule 4 propagation
		EList<BinaryLinkReference> newPropTraceLinkRefs = new BasicEList<BinaryLinkReference>();
		if (origModelElemRefs_traceRel.isEmpty()) {
			for (ModelElementReference refinedModelElemRef_refinementRel : refinedModelElemRefs_refinementRel) {
				BinaryLinkReference newPropTraceLinkRef = createDanglingTraceLink(refinedModelElemRef_refinementRel, newPropTraceRel, 0, 1);
				if (newPropTraceLinkRef != null) {
					newPropTraceLinkRefs.add(newPropTraceLinkRef);
				}
			}
			return newPropTraceLinkRefs;
		}

		// propagate trace links
		EList<ModelElementReference> newRefinedModelElemRefs_propTraceRel = new BasicEList<ModelElementReference>();
		ModelEndpointReference refinedModelEndpointRef_propTraceRel = newPropTraceRel.getModelEndpointRefs().get(0);
		for (ModelElementReference refinedModelElemRef_refinementRel : refinedModelElemRefs_refinementRel) {
			// create refined model elem ref in propagated trace rel
			ModelElementReference newRefinedModelElemRef_propTraceRel = MultiModelInstanceFactory.createModelElementAndModelElementReference(
				refinedModelEndpointRef_propTraceRel,
				refinedModelElemRef_refinementRel.getObject().getName(),
				refinedModelElemRef_refinementRel.getObject().getPointer()
			);
			newRefinedModelElemRefs_propTraceRel.add(newRefinedModelElemRef_propTraceRel);
		}
		ModelEndpointReference propModelEndpointRef_propTraceRel = newPropTraceRel.getModelEndpointRefs().get(1);
		for (ModelElementReference origModelElemRef_traceRel : origModelElemRefs_traceRel) {
			for (ModelElementEndpointReference traceModelElemEndpointRef : origModelElemRef_traceRel.getModelElemEndpointRefs()) {
				BinaryLinkReference traceLinkRef = (BinaryLinkReference) traceModelElemEndpointRef.eContainer();
				BinaryLink traceLink = traceLinkRef.getObject();
				ModelElementReference relatedModelElemRef_traceRel = traceLinkRef.getTargetModelElemRef();
				String propModelEObjectUri =
					newPropModel.getUri() +
					relatedModelElemRef_traceRel.getUri().substring(relatedModelElemRef_traceRel.getUri().lastIndexOf(MultiModelRegistry.ECORE_METAMODEL_URI_SEPARATOR));
				EObject propModelEObject = MultiModelTypeIntrospection.getPointer(propModelEObjectUri);
				// create propagated model elem ref in propagated trace rel
				ModelElementReference newPropModelElemRef_propTraceRel = MultiModelInstanceFactory.createModelElementAndModelElementReference(
					propModelEndpointRef_propTraceRel,
					relatedModelElemRef_traceRel.getObject().getName(),
					propModelEObject
				);
				// create new propagated trace links
				for (ModelElementReference newRefinedModelElemRef_propTraceRel : newRefinedModelElemRefs_propTraceRel) {
					BinaryLinkReference newPropTraceLinkRef = (BinaryLinkReference) MultiModelInstanceFactory.createLinkAndLinkReference(
						traceLink.getMetatype(),
						newPropTraceRel,
						RelationshipPackage.eINSTANCE.getBinaryLink(),
						RelationshipPackage.eINSTANCE.getBinaryLinkReference()
					);
					MultiModelInstanceFactory.createModelElementEndpointAndModelElementEndpointReference(
						traceLink.getModelElemEndpoints().get(0).getMetatype(),
						newPropTraceLinkRef,
						newRefinedModelElemRef_propTraceRel,
						false
					);
					MultiModelInstanceFactory.createModelElementEndpointAndModelElementEndpointReference(
						traceLink.getModelElemEndpoints().get(1).getMetatype(),
						newPropTraceLinkRef,
						newPropModelElemRef_propTraceRel,
						false
					);
					Link newPropTraceLink = newPropTraceLinkRef.getObject();
					newPropTraceLink.setName(traceLink.getName());
					newPropTraceLink.setMay(traceLink.isMay());
					newPropTraceLink.setSet(traceLink.isSet());
					newPropTraceLink.setVar(traceLink.isVar());
					newPropTraceLinkRefs.add((BinaryLinkReference) newPropTraceLinkRef);
				}
			}
		}

		return newPropTraceLinkRefs;
	}

	private BinaryLinkReference createDanglingTraceLink(ModelElementReference modelElemRefA, BinaryModelRel traceRel, int indexA, int indexB) {

		// rule 4, 1st half
		if (!traceRel.getModelEndpoints().get(indexB).getTarget().isInc()) {
			return null;
		}
		ModelElement modelElemTypeA = MultiModelConstraintChecker.getAllowedModelElementType(traceRel.getModelEndpointRefs().get(indexA), modelElemRefA.getObject().getPointer());
		if (modelElemTypeA == null) {
			return null;
		}

		BinaryLinkReference newTraceLinkRef = null;
		for (LinkReference linkTypeRef : MultiModelTypeRegistry.getLinkTypeReferences(traceRel.getMetatype())) {
			if (!(linkTypeRef instanceof BinaryLinkReference)) { // a trace rel type is meant to have two model types
				continue;
			}
			ModelElementReference modelElemTypeRefA = linkTypeRef.getModelElemEndpointRefs().get(indexA).getModelElemRef();
			if (!modelElemTypeRefA.getUri().equals(modelElemTypeA.getUri())) {
				continue;
			}
			ModelElementEndpointReference modelElemTypeEndpointRefB = linkTypeRef.getModelElemEndpointRefs().get(indexB);
			if (modelElemTypeEndpointRefB.getObject().getLowerBound() < 1) {
				continue;
			}
			// create new dangling trace link
			ModelElementReference newModelElemRefA = MultiModelInstanceFactory.createModelElementAndModelElementReference(
				traceRel.getModelEndpointRefs().get(indexA),
				modelElemRefA.getObject().getName(),
				modelElemRefA.getObject().getPointer()
			);
			newTraceLinkRef = (BinaryLinkReference) MultiModelInstanceFactory.createLinkAndLinkReferenceAndModelElementEndpointsAndModelElementEndpointReferences(
				linkTypeRef.getObject(),
				RelationshipPackage.eINSTANCE.getBinaryLink(),
				RelationshipPackage.eINSTANCE.getBinaryLinkReference(),
				newModelElemRefA
			);
			newTraceLinkRef.getObject().setVar(true);
			newTraceLinkRef.getObject().setName(PROPTRACE_RULE4_LINK_NAME);
			break;
		}

		return newTraceLinkRef;
	}

	private EList<BinaryLinkReference> reduceTraceLinkUncertainty(EObject modelRootB, EList<BinaryLinkReference> traceLinkRefs, int indexA, int indexB) throws Exception {

		int n = 0;
		HashSet<String> uniqueModelElemUrisB = new HashSet<String>();
		for (BinaryLinkReference traceLinkRef : traceLinkRefs) {
			if (traceLinkRef.getModelElemEndpointRefs().size() == 1) { // dangling link
				continue;
			}
			ModelElementReference traceModelElemRefB = traceLinkRef.getModelElemEndpointRefs().get(indexB).getModelElemRef();
			if (!uniqueModelElemUrisB.contains(traceModelElemRefB.getUri())) {
				n++;
				uniqueModelElemUrisB.add(traceModelElemRefB.getUri());
			}
		}

		boolean again = false;
		LinkReference unifiedLinkRef = null;
traceLinks:
		for (BinaryLinkReference traceLinkRef : traceLinkRefs) {
			ModelElementEndpointReference traceModelElemEndpointRefA = traceLinkRef.getModelElemEndpointRefs().get(indexA);
			ModelElementReference modelElemRefA = traceModelElemEndpointRefA.getModelElemRef();
			ModelElement modelElemA = modelElemRefA.getObject();

			// rule 4, 2nd half
			if (n == 0) {
				completeDanglingTraceLink(modelRootB, traceLinkRef, modelElemA.getName(), indexB);
				again = true;
				continue;
			}

			ModelElementEndpointReference traceModelElemEndpointRefB = traceLinkRef.getModelElemEndpointRefs().get(indexB);
			ModelElementReference modelElemRefB = traceModelElemEndpointRefB.getModelElemRef();
			ModelElement modelElemB = modelElemRefB.getObject();
			BinaryLink traceLink = traceLinkRef.getObject();
			boolean Ma = modelElemA.isMay(), Sa = modelElemA.isSet(), Va = modelElemA.isVar();
			boolean Mab = traceLink.isMay(), Sab = traceLink.isSet(), Vab = traceLink.isVar();
			boolean Mb = modelElemB.isMay(), Sb = modelElemB.isSet(), Vb = modelElemB.isVar();
			int Ua = traceModelElemEndpointRefA.getObject().getMetatype().getUpperBound();
			int Lb = traceModelElemEndpointRefB.getObject().getMetatype().getLowerBound(), Ub = traceModelElemEndpointRefB.getObject().getMetatype().getUpperBound();
			MAVOElement modelEObjectB = (MAVOElement) MultiModelTypeIntrospection.getPointer(modelRootB.eResource(), modelElemB.getUri());

			// rule 1
			if (Mb && !Mab) {
				Mb = false;
				modelElemB.setMay(Mb);
				modelEObjectB.setMay(Mb);
				again = true;
			}
			// rule 2
			if (Sab && !Sa && !Sb) {
				Sab = false;
				traceLink.setSet(Sab);
				again = true;
			}
			// rule 3
			if (Vab && !Va && !Vb) {
				Vab = false;
				traceLink.setVar(Vab);
				again = true;
			}
			if (Lb == 1 && n == 1) {
				// rule 5
				if (Mab && !Ma) {
					Mab = false;
					traceLink.setMay(Mab);
					again = true;
				}
			}
			if (Ub == 1) {
				if (Ua == -1) {
					// rule 6
					if (Sb && !Sa && !Mab) {
						Sb = false;
						modelElemB.setSet(Sb);
						modelEObjectB.setSet(Sb);
						again = true;
					}
				}
				// rule 7
				if (Vab && Vb && !Mab) {
					for (BinaryLinkReference traceLinkRef2 : traceLinkRefs) {
						if (traceLinkRef2 == traceLinkRef) {
							continue;
						}
						boolean Mac = traceLinkRef2.getObject().isMay();
						if (!Mac) {
							unifyVarTraceLink(traceLinkRef, traceLinkRef2, indexA, indexB);
							unifiedLinkRef = traceLinkRef;
							again = true;
							break traceLinks;
						}
					}
				}
			}
		}
		// rule 7
		if (unifiedLinkRef != null) {
			traceLinkRefs.remove(unifiedLinkRef);
			unifiedLinkRef = null;
		}

		// keep reducing uncertainty
		if (again) {
			reduceTraceLinkUncertainty(modelRootB, traceLinkRefs, indexA, indexB);
		}

		return traceLinkRefs;
	}

	private void unifyModelElementUris(ModelElementReference unifiedModelElemRef, ModelElementReference modelElemRef) {

		String unifiedModelElemUri = unifiedModelElemRef.getUri();
		String modelElemUri = modelElemRef.getUri();
		ModelEndpointReference modelEndpointRef = (ModelEndpointReference) modelElemRef.eContainer();
		String unifiedModelElemUriBase = unifiedModelElemUri.substring(0, unifiedModelElemUri.lastIndexOf('.')+1);
		int unifiedModelElemUriIndex = Integer.parseInt(unifiedModelElemUri.substring(unifiedModelElemUri.lastIndexOf('.')+1));

		for (ModelElement modelElem : modelEndpointRef.getObject().getTarget().getElements()) {
			String otherModelElemUri = modelElem.getUri();
			// other model element to be affected by unification of model elements
			if (otherModelElemUri.contains(unifiedModelElemUriBase)) {
				String otherModelElemUriExtra = otherModelElemUri.substring(otherModelElemUri.lastIndexOf(unifiedModelElemUriBase) + unifiedModelElemUriBase.length());
				int otherModelElemUriIndex = (otherModelElemUriExtra.indexOf('/') == -1) ?
					Integer.parseInt(otherModelElemUriExtra) :
					Integer.parseInt(otherModelElemUriExtra.substring(0, otherModelElemUriExtra.indexOf('/')));
				// uri to be fully replaced
				if (otherModelElemUriIndex == unifiedModelElemUriIndex) {
					modelElem.setUri(otherModelElemUri.replace(unifiedModelElemUri, modelElemUri));
				}
				// uri to be shifted
				else if (otherModelElemUriIndex > unifiedModelElemUriIndex) {
					modelElem.setUri(otherModelElemUri.replace(unifiedModelElemUriBase + otherModelElemUriIndex, unifiedModelElemUriBase + --otherModelElemUriIndex));
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void unifyVarTraceLink(BinaryLinkReference varTraceLinkRef, BinaryLinkReference traceLinkRef, int indexA, int indexB) throws Exception {

		ModelElementReference varModelElemRef = varTraceLinkRef.getModelElemEndpointRefs().get(indexB).getModelElemRef();
		ModelElementReference modelElemRef = traceLinkRef.getModelElemEndpointRefs().get(indexB).getModelElemRef();
		ModelEndpointReference modelEndpointRef = (ModelEndpointReference) modelElemRef.eContainer();
		// get var object and other object from same resource
		EObject varModelEObject = MultiModelTypeIntrospection.getPointer(varModelElemRef.getUri());
		EObject modelEObject = MultiModelTypeIntrospection.getPointer(varModelEObject.eResource(), modelElemRef.getUri());
		// unify contents
		for (EObject varModelEObjectContent : varModelEObject.eContents()) {
			EStructuralFeature varModelEObjectContainingFeature = varModelEObjectContent.eContainingFeature();
			Object value = modelEObject.eGet(varModelEObjectContainingFeature);
			if (value instanceof EList) {
				((EList<EObject>) value).add(varModelEObjectContent);
			}
		}

		// remove var object
		 EcoreUtil.delete(varModelEObject);
		//TODO MMTF: should we try to preserve references to it, maybe using EcoreUtil.CrossReferencer?
		// serialize modified model
		MultiModelTypeIntrospection.writeRoot(EcoreUtil.getRootContainer(modelEObject), modelEndpointRef.getTargetUri(), true);
		// remove unified links and model elements
		MultiModelInstanceFactory.removeModelElementAndModelElementReference(varModelElemRef);
		// update uris due to model element unification
		unifyModelElementUris(varModelElemRef, modelElemRef);
	}

	@SuppressWarnings("unchecked")
	private EObject[] navigateIncompleteModel(EObject currentContainer, EFactory modelTypeFactory, String modelElemTypeUri, String modelElemName) {

		for (EReference containment : currentContainer.eClass().getEAllContainments()) {
			EObject currentContainerCopy = EcoreUtil.copy(currentContainer);
			EClass containedEClass = (EClass) containment.getEType();
			EObject contained = modelTypeFactory.create(containedEClass);
			if (contained instanceof MAVOElement) {
				((MAVOElement) contained).setVar(true);
			}
			EStructuralFeature feature = containedEClass.getEStructuralFeature(NAME_FEATURE);
			if (feature != null && feature instanceof EAttribute && ((EAttribute) feature).getEType().getName().equals("EString")) {
				contained.eSet(feature, modelElemName);
			}
			Object value = currentContainerCopy.eGet(containment);
			if (value instanceof EList) {
				((EList<EObject>) value).add(contained);
			}
			else {
				currentContainerCopy.eSet(containment, contained);
			}

			// model element created
			if (modelElemTypeUri.equals(containedEClass.getName())) {
				EObject rootContainer = EcoreUtil.getRootContainer(currentContainerCopy);
				if (rootContainer == null) {
					rootContainer = currentContainerCopy;
				}
				return new EObject[] {rootContainer, contained};
			}

			// continue recursion
			EObject[] result = navigateIncompleteModel(contained, modelTypeFactory, modelElemTypeUri, modelElemName);
			if (result != null) {
				return result;
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private void completeDanglingTraceLink(EObject modelRootB, LinkReference traceLinkRef, String modelElemName, int indexB) throws Exception {

		ModelEndpointReference modelEndpointRef = ((BinaryModelRel) traceLinkRef.eContainer()).getModelEndpointRefs().get(indexB);
		Model model = modelEndpointRef.getObject().getTarget();
		EFactory modelTypeFactory = ((EPackage) model.getMetatype().getRoot()).getEFactoryInstance();
		ModelElementEndpointReference modelElemTypeEndpointRef = traceLinkRef.getObject().getMetatype().getModelElemEndpointRefs().get(indexB);
		ModelElement modelElemType = modelElemTypeEndpointRef.getModelElemRef().getObject();
		String modelElemTypeUri = MultiModelRegistry.getModelAndModelElementUris(modelElemType.getPointer(), false)[1];

		EObject[] result = null;
		for (EReference containment : modelRootB.eClass().getEAllContainments()) {
			EClass containedEClass = (EClass) containment.getEType();
			EObject contained = modelTypeFactory.create(containedEClass);
			if (contained instanceof MAVOElement) {
				((MAVOElement) contained).setVar(true);
			}
			EStructuralFeature feature = containedEClass.getEStructuralFeature(NAME_FEATURE);
			if (feature != null && feature instanceof EAttribute && ((EAttribute) feature).getEType().getName().equals("EString")) {
				contained.eSet(feature, modelElemName);
			}
			result = navigateIncompleteModel(contained, modelTypeFactory, modelElemTypeUri, modelElemName);
			if (result != null) {
				Object value = modelRootB.eGet(containment);
				if (value instanceof EList) {
					((EList<EObject>) value).add(result[0]);
				}
				else {
					modelRootB.eSet(containment, result[0]);
				}
				break;
			}
		}

		ModelElementReference newModelElemRef = MultiModelInstanceFactory.createModelElementAndModelElementReference(
			modelEndpointRef,
			null,
			result[1]
		);
		MultiModelInstanceFactory.createModelElementEndpointAndModelElementEndpointReference(
			modelElemTypeEndpointRef.getObject(),
			traceLinkRef,
			newModelElemRef,
			false
		);
	}

	private void propagateRefinementLinks(BinaryLinkReference propTraceLinkRef, BinaryModelRel refinementRel, Model relatedModel, BinaryModelRel traceRel, BinaryModelRel newPropRefinementRel) {

		ModelElementReference propModelElemRef_propTraceRel = propTraceLinkRef.getTargetModelElemRef();
		ModelEndpointReference propModelEndpointRef_propRefinementRel = newPropRefinementRel.getModelEndpointRefs().get(1);
		// create new propagated model element refs in propagated refinement rel
		boolean duplicateRel = true;
		ModelElementReference newPropModelElemRef = MultiModelHierarchyUtils.getReference(
			MultiModelRegistry.getModelAndModelElementUris(propModelElemRef_propTraceRel.getObject().getPointer(), true)[1],
			propModelEndpointRef_propRefinementRel.getModelElemRefs()
		);
		if (newPropModelElemRef == null) {
			duplicateRel = false;
			newPropModelElemRef = MultiModelInstanceFactory.createModelElementAndModelElementReference(
				propModelEndpointRef_propRefinementRel,
				propModelElemRef_propTraceRel.getObject().getName(),
				propModelElemRef_propTraceRel.getObject().getPointer()
			);
		}

		ModelElementReference refinedModelElemRef_propTraceRel = propTraceLinkRef.getSourceModelElemRef();
		ModelEndpointReference relatedModelEndpointRef_propRefinementRel = newPropRefinementRel.getModelEndpointRefs().get(0);
		ModelEndpointReference refinedModelEndpointRef_refinementRel = refinementRel.getModelEndpointRefs().get(1);
		String refinedModelUri = refinedModelEndpointRef_refinementRel.getTargetUri();
		ModelElementReference refinedModelElemRef_refinementRel = MultiModelHierarchyUtils.getReference(refinedModelElemRef_propTraceRel, refinedModelEndpointRef_refinementRel.getModelElemRefs());
		ModelElementEndpointReference refinementModelElemEndpointRef = refinedModelElemRef_refinementRel.getModelElemEndpointRefs().get(0); // many to one here has to be mapped through an nary link
		LinkReference refinementLinkRef = (LinkReference) refinementModelElemEndpointRef.eContainer();
		// create new propagated refinement link
		LinkReference newPropRefinementLinkRef = MultiModelInstanceFactory.createLinkAndLinkReferenceAndModelElementEndpointsAndModelElementEndpointReferences(
			null,
			RelationshipPackage.eINSTANCE.getLink(),
			RelationshipPackage.eINSTANCE.getLinkReference(),
			newPropModelElemRef
		);
		newPropRefinementLinkRef.getObject().setName(refinementLinkRef.getObject().getName());

		boolean duplicateProp = true;
		for (ModelElementEndpointReference refinementModelElemEndpointRef2 : refinementLinkRef.getModelElemEndpointRefs()) {
			ModelElementReference origModelElemRef_refinementRel = refinementModelElemEndpointRef2.getModelElemRef();
			if (((ModelEndpointReference) origModelElemRef_refinementRel.eContainer()).getTargetUri().equals(refinedModelUri)) {
				continue;
			}
			ModelEndpointReference origModelEndpointRef_traceRel = traceRel.getModelEndpointRefs().get(0);
			ModelElementReference origModelElemRef_traceRel = MultiModelHierarchyUtils.getReference(origModelElemRef_refinementRel, origModelEndpointRef_traceRel.getModelElemRefs());
			for (ModelElementEndpointReference traceModelElementEndpoint : origModelElemRef_traceRel.getModelElemEndpointRefs()) {
				BinaryLinkReference traceLinkRef = (BinaryLinkReference) traceModelElementEndpoint.eContainer();
				ModelElementReference relatedModelElemRef_traceRel = traceLinkRef.getTargetModelElemRef();
				// create new related model element refs in propagated refinement rel
				ModelElementReference newRelatedModelElemRef = MultiModelHierarchyUtils.getReference(
					MultiModelRegistry.getModelAndModelElementUris(relatedModelElemRef_traceRel.getObject().getPointer(), true)[1],
					relatedModelEndpointRef_propRefinementRel.getModelElemRefs()
				);
				if (newRelatedModelElemRef == null) {
					duplicateProp = false;
					newRelatedModelElemRef = MultiModelInstanceFactory.createModelElementAndModelElementReference(
						relatedModelEndpointRef_propRefinementRel,
						relatedModelElemRef_traceRel.getObject().getName(),
						relatedModelElemRef_traceRel.getObject().getPointer()
					);
				}
				MultiModelInstanceFactory.createModelElementEndpointAndModelElementEndpointReference(
					null,
					newPropRefinementLinkRef,
					newRelatedModelElemRef,
					false
				);
			}
		}

		// remove duplicate
		if (duplicateRel && duplicateProp) {
			MultiModelInstanceFactory.removeLinkAndLinkReference(newPropRefinementLinkRef);
		}
	}

	@Override
	public EList<Model> execute(EList<Model> actualParameters) throws Exception {

		Model origModel = actualParameters.get(0);
		BinaryModelRel refinementRel = (BinaryModelRel) actualParameters.get(1);
		Model refinedModel = actualParameters.get(2);
		BinaryModelRel traceRel = (BinaryModelRel) actualParameters.get(3);
		Model relatedModel = actualParameters.get(4);
		MultiModel multiModel = (MultiModel) origModel.eContainer();

		// create output model and model relationships
		Model newPropModel = createRelatedModelCopy(relatedModel);
		BinaryModelRel newPropRefinementRel = (BinaryModelRel) MultiModelInstanceFactory.createModelRelAndModelEndpointsAndModelEndpointReferences(
			null,
			ModelOrigin.CREATED,
			null,
			RelationshipPackage.eINSTANCE.getBinaryModelRel(),
			relatedModel,
			newPropModel
		);
		newPropRefinementRel.setName(PROPREFINEMENT_MODELREL_NAME);
		BinaryModelRel newPropTraceRel = (BinaryModelRel) MultiModelInstanceFactory.createModelRel(
			traceRel.getMetatype(),
			multiModel,
			ModelOrigin.CREATED,
			null,
			RelationshipPackage.eINSTANCE.getBinaryModelRel()
		);
		newPropTraceRel.setName(PROPTRACE_MODELREL_NAME);
		MultiModelInstanceFactory.createModelEndpointAndModelEndpointReference(
			traceRel.getModelEndpoints().get(0).getMetatype(),
			newPropTraceRel,
			refinedModel,
			false
		);
		MultiModelInstanceFactory.createModelEndpointAndModelEndpointReference(
			traceRel.getModelEndpoints().get(1).getMetatype(),
			newPropTraceRel,
			newPropModel,
			false
		);

		// change propagation algorithm
		EList<EList<BinaryLinkReference>> propTraceLinkRefsList = new BasicEList<EList<BinaryLinkReference>>();
		for (LinkReference refinementLinkRef : refinementRel.getLinkRefs()) {
			EList<BinaryLinkReference> propTraceLinkRefs = propagateTraceLinksFromRefinements(refinementLinkRef, traceRel, newPropModel, newPropTraceRel);
			propTraceLinkRefsList.add(propTraceLinkRefs);
		}
		EObject newPropModelRoot = newPropModel.getRoot();
		for (EList<BinaryLinkReference> propTraceLinkRefs : propTraceLinkRefsList) {
			reduceTraceLinkUncertainty(newPropModelRoot, propTraceLinkRefs, 0, 1);
		}
		MultiModelTypeIntrospection.writeRoot(newPropModelRoot, newPropModel.getUri(), true);
		for (EList<BinaryLinkReference> propTraceLinkRefs : propTraceLinkRefsList) {
			for (BinaryLinkReference propTraceLinkRef : propTraceLinkRefs) {
				propagateRefinementLinks(propTraceLinkRef, refinementRel, relatedModel, traceRel, newPropRefinementRel);
			}
		}
		//TODO MMTF: reason about how to concretely use indexA and indexB, when the refineUncertainty becomes an independent operator

		EList<Model> result = new BasicEList<Model>();
		result.add(newPropModel);
		result.add(newPropRefinementRel);
		result.add(newPropTraceRel);
		return result;
	}

}
