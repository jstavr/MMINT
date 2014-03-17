/**
 * Copyright (c) 2012-2014 Marsha Chechik, Alessio Di Sandro, Michalis Famelis,
 * Rick Salay.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Alessio Di Sandro - Implementation.
 */
package edu.toronto.cs.se.modelepedia.tests;

import static org.junit.Assert.*;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.junit.Test;
import org.osgi.framework.Bundle;

import edu.toronto.cs.se.mmtf.MMTF;
import edu.toronto.cs.se.mmtf.MMTFException;
import edu.toronto.cs.se.mmtf.MultiModelTypeFactory;
import edu.toronto.cs.se.mmtf.MultiModelTypeRegistry;
import edu.toronto.cs.se.mmtf.mid.EMFInfo;
import edu.toronto.cs.se.mmtf.mid.ExtendibleElementConstraint;
import edu.toronto.cs.se.mmtf.mid.ExtendibleElementConstraintLanguage;
import edu.toronto.cs.se.mmtf.mid.MidFactory;
import edu.toronto.cs.se.mmtf.mid.MidLevel;
import edu.toronto.cs.se.mmtf.mid.Model;
import edu.toronto.cs.se.mmtf.mid.ModelElement;
import edu.toronto.cs.se.mmtf.mid.ModelOrigin;
import edu.toronto.cs.se.mmtf.mid.MultiModel;
import edu.toronto.cs.se.mmtf.mid.library.MultiModelRegistry;
import edu.toronto.cs.se.mmtf.mid.library.MultiModelUtils;
import edu.toronto.cs.se.mmtf.mid.operator.Operator;
import edu.toronto.cs.se.mmtf.mid.relationship.Link;
import edu.toronto.cs.se.mmtf.mid.relationship.LinkReference;
import edu.toronto.cs.se.mmtf.mid.relationship.ModelElementEndpoint;
import edu.toronto.cs.se.mmtf.mid.relationship.ModelElementReference;
import edu.toronto.cs.se.mmtf.mid.relationship.ModelEndpointReference;
import edu.toronto.cs.se.modelepedia.kleisli.KleisliModelEndpoint;
import edu.toronto.cs.se.modelepedia.kleisli.KleisliModelEndpointReference;
import edu.toronto.cs.se.modelepedia.kleisli.KleisliModelRel;

public class KleisliTest {

	private final static String TESTS_BUNDLE_NAME = "edu.toronto.cs.se.modelepedia.tests";
	private final static String TESTS_BUNDLE_MODEL_DIR= "model";
	private final static String TESTS_BUNDLE_TEST_DIR= "test";
	private final static String KLEISLI_MODELRELTYPE_URI = "http://se.cs.toronto.edu/modelepedia/KleisliModelRel";
	private final static String KLEISLI_TRANSFORMATIONOPERATORTYPE_URI = "http://se.cs.toronto.edu/modelepedia/KleisliModelRel";
	private final static String SRC_MODELTYPE_NAME = "Company";
	private final static String SRC_MODELTYPEENDPOINT_NAME = SRC_MODELTYPE_NAME;
	private final static String TGT_MODELTYPE_NAME = "Bank";
	private final static String TGT_MODELTYPEENDPOINT_NAME = TGT_MODELTYPE_NAME;
	private final static String MODELRELTYPE_NAME = SRC_MODELTYPE_NAME + MMTF.BINARY_MODELREL_LINK_SEPARATOR + TGT_MODELTYPE_NAME;
	private final static String[] SRC_METAMODELOBJ_NAMES = {"Company", "Student", "Businessman", "Company/clients", "Client/number"};
	private final static String[] TGT_METAMODELOBJ_NAMES = {"Bank", "_StudentAccount", "_BusinessAccount", "Bank/accounts", "Account/_number"};
	private final static String[] TGT_MODELELEM_OCLQUERIES = {null, "id.startsWith('S')", "id.startsWith('B')", null, "id.substring(2,2)"};
	private final static String TGT_MODELOBJ_METAMODELROOTCLASS = "Bank";
	private final static String TGT_MODELOBJ_METAMODELCLASSTOCREATE = "Account";
	private final static String TGT_MODELOBJ_METAMODELCONTAININGFEATURE = "accounts";
	private final static String TGT_MODELOBJ_METAMODELATTRIBUTETOCREATE = "id";
	private final static String[] TGT_MODELOBJ_ATTRIBUTEVALUES = {"S1", "B2", "A3"};

	private ModelElementReference dropMetamodelObject(EPackage metamodelRootObj, String metamodelObjName, ModelEndpointReference containerModelTypeEndpointRef, ModelElement rootModelElemType) throws MMTFException {

		String[] names = metamodelObjName.split("/");
		EObject metamodelObj;
		if (names.length == 1) {
			metamodelObj = metamodelRootObj.getEClassifier(names[0]);
		}
		else {
			EClass metamodelObjContainer = (EClass) metamodelRootObj.getEClassifier(names[0]);
			metamodelObj = metamodelObjContainer.getEStructuralFeature(names[1]);
		}
		assertTrue(containerModelTypeEndpointRef.acceptModelElementType(metamodelObj));
		String modelElemTypeUri = MultiModelRegistry.getModelAndModelElementUris(metamodelObj, MidLevel.TYPES)[1];
		EMFInfo eInfo = MultiModelRegistry.getModelElementEMFInfo(metamodelObj, MidLevel.TYPES);
		String newModelElemTypeName = MultiModelRegistry.getModelElementName(eInfo, metamodelObj, MidLevel.TYPES);
		ModelElementReference newModelElemTypeRef = rootModelElemType.createSubtypeAndReference(null, modelElemTypeUri, newModelElemTypeName, eInfo, containerModelTypeEndpointRef);

		return newModelElemTypeRef;
	}

	@Test
	public void test() throws Exception {

		Bundle bundle = Platform.getBundle(TESTS_BUNDLE_NAME);
		//TODO MMTF[TESTS] MMTF has to run before this, with dynamic types from previous run discarded
		// model types
		Model rootModelType = MultiModelTypeRegistry.getType(MMTF.ROOT_MODEL_URI);
		Model srcModelType = rootModelType.createSubtype(SRC_MODELTYPE_NAME, null, null, true);
		String srcMetamodelName = SRC_MODELTYPE_NAME + MMTF.MODEL_FILEEXTENSION_SEPARATOR + EcorePackage.eNAME;
		URL srcMetamodelUrl = bundle.findEntries(TESTS_BUNDLE_MODEL_DIR, srcMetamodelName, false).nextElement();
		Files.copy(Paths.get(FileLocator.toFileURL(srcMetamodelUrl).toURI()), Paths.get(MultiModelUtils.prependStateToUri(srcMetamodelName)));
		Model tgtModelType = rootModelType.createSubtype(TGT_MODELTYPE_NAME, null, null, true);
		String tgtMetamodelName = TGT_MODELTYPE_NAME + MMTF.MODEL_FILEEXTENSION_SEPARATOR + EcorePackage.eNAME;
		URL tgtMetamodelUrl = bundle.findEntries(TESTS_BUNDLE_MODEL_DIR, tgtMetamodelName, false).nextElement();
		Files.copy(Paths.get(FileLocator.toFileURL(tgtMetamodelUrl).toURI()), Paths.get(MultiModelUtils.prependStateToUri(tgtMetamodelName)));

		// model rel type
		KleisliModelRel kRootModelRelType = MultiModelTypeRegistry.getType(KLEISLI_MODELRELTYPE_URI);
		KleisliModelRel kModelRelType = (KleisliModelRel) kRootModelRelType.createSubtype(MODELRELTYPE_NAME, false, null, null);
		KleisliModelEndpoint kRootModelTypeEndpoint = (KleisliModelEndpoint) kRootModelRelType.getModelEndpoints().get(0);
		//TODO MMTF[TESTS] here we need MMTF.isInitialized() to return false
		kRootModelTypeEndpoint.createSubtypeAndReference(null, SRC_MODELTYPEENDPOINT_NAME, srcModelType, false, kModelRelType);
		String kTgtMetamodelName = TGT_MODELTYPEENDPOINT_NAME + MMTF.ENDPOINT_SEPARATOR + TGT_MODELTYPE_NAME + MMTF.MODEL_FILEEXTENSION_SEPARATOR + EcorePackage.eNAME;
		URL kTgtMetamodelUrl = bundle.findEntries(TESTS_BUNDLE_MODEL_DIR, kTgtMetamodelName, false).nextElement();
		Files.copy(Paths.get(FileLocator.toFileURL(kTgtMetamodelUrl).toURI()), Paths.get(MultiModelUtils.prependStateToUri(MODELRELTYPE_NAME + MMTF.URI_SEPARATOR + kTgtMetamodelName)));
		kRootModelTypeEndpoint.createSubtypeAndReference(null, TGT_MODELTYPEENDPOINT_NAME, tgtModelType, false, kModelRelType);
		// model element types and link types
		ModelEndpointReference srcModelTypeEndpointRef = kModelRelType.getModelEndpointRefs().get(0);
		EPackage srcMetamodelRootObj = (EPackage) MultiModelUtils.getModelFileInState(srcMetamodelName);
		KleisliModelEndpointReference kTgtModelTypeEndpointRef = (KleisliModelEndpointReference) kModelRelType.getModelEndpointRefs().get(1);
		EPackage kTgtMetamodelRootObj = (EPackage) MultiModelUtils.getModelFileInState(kTgtMetamodelName);
		ModelElement rootModelElemType = MultiModelTypeRegistry.getType(MMTF.ROOT_MODELELEM_URI);
		Link rootLinkType = MultiModelTypeRegistry.getType(MMTF.ROOT_LINK_URI);
		ModelElementEndpoint rootModelElemTypeEndpoint = MultiModelTypeRegistry.getType(MMTF.ROOT_MODELELEMENDPOINT_URI);
		for (int i = 0; i < SRC_METAMODELOBJ_NAMES.length; i++) {
			ModelElementReference srcModelElemTypeRef = dropMetamodelObject(srcMetamodelRootObj, SRC_METAMODELOBJ_NAMES[i], srcModelTypeEndpointRef, rootModelElemType);
			ModelElementReference tgtModelElemTypeRef = dropMetamodelObject(kTgtMetamodelRootObj, TGT_METAMODELOBJ_NAMES[i], kTgtModelTypeEndpointRef, rootModelElemType);
			if (TGT_MODELELEM_OCLQUERIES[i] != null) {
				ExtendibleElementConstraint constraint = MidFactory.eINSTANCE.createExtendibleElementConstraint();
				constraint.setLanguage(ExtendibleElementConstraintLanguage.OCL);
				constraint.setImplementation(TGT_MODELELEM_OCLQUERIES[i]);
				tgtModelElemTypeRef.getObject().setConstraint(constraint);
			}
			String newLinkTypeName = srcModelElemTypeRef.getObject().getName() + MMTF.BINARY_MODELREL_LINK_SEPARATOR + tgtModelElemTypeRef.getObject().getName();
			LinkReference linkTypeRef = rootLinkType.createSubtypeAndReference(null, newLinkTypeName, true, kModelRelType);
			rootModelElemTypeEndpoint.createSubtypeAndReference(null, srcModelElemTypeRef.getObject().getName(), srcModelElemTypeRef, false, linkTypeRef);
			rootModelElemTypeEndpoint.createSubtypeAndReference(null, tgtModelElemTypeRef.getObject().getName(), tgtModelElemTypeRef, false, linkTypeRef);
		}

		// instances
		MultiModel instanceMID = MidFactory.eINSTANCE.createMultiModel();
		String newModelUri = TESTS_BUNDLE_TEST_DIR + IPath.SEPARATOR + TGT_MODELTYPE_NAME.toLowerCase() + MMTF.MODEL_FILEEXTENSION_SEPARATOR + MultiModelTypeFactory.ECORE_REFLECTIVE_FILE_EXTENSION;
		Model bankModel = tgtModelType.createInstanceAndEditor(newModelUri, ModelOrigin.CREATED, instanceMID);
		EObject rootModelObj = bankModel.getEMFInstanceRoot();
		EPackage tgtMetamodelRootObj = (EPackage) MultiModelUtils.getModelFileInState(tgtMetamodelName);
		EFactory tgtMetamodelFactory = tgtMetamodelRootObj.getEFactoryInstance();
		EStructuralFeature tgtMetamodelContainingFeature = ((EClass) tgtMetamodelRootObj.getEClassifier(TGT_MODELOBJ_METAMODELROOTCLASS)).getEStructuralFeature(TGT_MODELOBJ_METAMODELCONTAININGFEATURE);
		EClass tgtMetamodelClassToCreate = (EClass) tgtMetamodelRootObj.getEClassifier(TGT_MODELOBJ_METAMODELCLASSTOCREATE);
		EStructuralFeature tgtMetamodelAttributeToCreate = tgtMetamodelClassToCreate.getEStructuralFeature(TGT_MODELOBJ_METAMODELATTRIBUTETOCREATE);
		@SuppressWarnings("unchecked")
		EList<EObject> y = (EList<EObject>) rootModelObj.eGet(tgtMetamodelContainingFeature);
		for (String attributeValue : TGT_MODELOBJ_ATTRIBUTEVALUES) {
			EObject classValue = tgtMetamodelFactory.create(tgtMetamodelClassToCreate);
			classValue.eSet(tgtMetamodelAttributeToCreate, attributeValue);
			y.add(classValue);
		}
		MultiModelUtils.createModelFile(rootModelObj, newModelUri, true);
		EList<Model> transformationParameters = new BasicEList<Model>();
		transformationParameters.add(kModelRelType);
		transformationParameters.add(bankModel);
		MultiModelTypeRegistry.<Operator>getType(KLEISLI_TRANSFORMATIONOPERATORTYPE_URI).execute(transformationParameters);
		//TODO MMTF[TESTS] test that result model + model rel are fine
		fail("TODO");
	}

}