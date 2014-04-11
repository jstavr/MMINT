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
package edu.toronto.cs.se.modelepedia.kleisli.library;

import edu.toronto.cs.se.mmint.MMINTException;
import edu.toronto.cs.se.mmint.MultiModelHeavyTypeFactory;
import edu.toronto.cs.se.mmint.mid.ExtendibleElementConstraint;
import edu.toronto.cs.se.mmint.mid.MidFactory;
import edu.toronto.cs.se.mmint.mid.MidLevel;
import edu.toronto.cs.se.mmint.mid.Model;
import edu.toronto.cs.se.mmint.mid.ModelOrigin;
import edu.toronto.cs.se.mmint.mid.relationship.ModelEndpointReference;
import edu.toronto.cs.se.mmint.mid.relationship.ModelRel;
import edu.toronto.cs.se.mmint.repository.ExtensionType;
import edu.toronto.cs.se.modelepedia.kleisli.KleisliFactory;
import edu.toronto.cs.se.modelepedia.kleisli.KleisliModel;
import edu.toronto.cs.se.modelepedia.kleisli.KleisliModelEndpoint;
import edu.toronto.cs.se.modelepedia.kleisli.KleisliModelRel;

/**
 * The factory to create/modify/remove Kleisli "heavy" types, i.e. Kleisli types
 * from extensions.
 * 
 * @author Alessio Di Sandro
 * 
 */
public class KleisliMultiModelHeavyTypeFactory extends MultiModelHeavyTypeFactory {

	/**
	 * Creates a Kleisli copy of a "heavy" model type to be used as the extended
	 * target of a Kleisli model type endpoint.
	 * 
	 * @param origModelType
	 *            The model type to be copied
	 * @return The created Kleisli model type.
	 */
	private KleisliModel kleisliCopyHeavyModelType(KleisliModelEndpoint containerModelTypeEndpoint) {

		KleisliModel kModelType = KleisliFactory.eINSTANCE.createKleisliModel();
		Model origModelType = containerModelTypeEndpoint.getTarget();
		kModelType.setUri(origModelType.getUri());
		kModelType.setName(origModelType.getName());
		kModelType.setLevel(MidLevel.TYPES);
		kModelType.setSupertype(origModelType.getSupertype());
		kModelType.setDynamic(false);
		kModelType.setAbstract(origModelType.isAbstract());
		ExtendibleElementConstraint origConstraint = origModelType.getConstraint();
		if (origConstraint != null) {
			ExtendibleElementConstraint kConstraint = MidFactory.eINSTANCE.createExtendibleElementConstraint();
			kConstraint.setLanguage(origConstraint.getLanguage());
			kConstraint.setImplementation(origConstraint.getImplementation());
			kModelType.setConstraint(kConstraint);
		}
		kModelType.setOrigin(ModelOrigin.IMPORTED);
		kModelType.setFileExtension(origModelType.getFileExtension());
		containerModelTypeEndpoint.setExtendedTarget(kModelType);

		return kModelType;
	}

	/**
	 * Kleisli version. {@inheritDoc}
	 */
	@Override
	public ModelRel createHeavyModelRelType(ExtensionType extensionType, boolean isAbstract, boolean isBinary, String constraintLanguage, String constraintImplementation) throws MMINTException {

		KleisliModelRel newModelRelType = (isBinary) ?
			KleisliFactory.eINSTANCE.createKleisliBinaryModelRel() :
			KleisliFactory.eINSTANCE.createKleisliModelRel();
		super.addHeavyModelRelType(newModelRelType, extensionType.getUri(), extensionType.getSupertypeUri(), extensionType.getName(), isAbstract, constraintLanguage, constraintImplementation);

		return newModelRelType;
	}

	/**
	 * Kleisli version. {@inheritDoc}
	 */
	@Override
	public ModelEndpointReference createHeavyModelTypeEndpointAndModelTypeEndpointReference(ExtensionType extensionType, Model targetModelType, boolean isBinarySrc, ModelRel containerModelRelType) throws MMINTException {

		KleisliModelEndpoint newModelTypeEndpoint = KleisliFactory.eINSTANCE.createKleisliModelEndpoint();
		ModelEndpointReference newModelTypeEndpointRef = super.addHeavyModelTypeEndpointAndModelTypeEndpointReference(newModelTypeEndpoint, extensionType.getUri(), extensionType.getSupertypeUri(), extensionType.getName(), targetModelType, isBinarySrc, containerModelRelType);
		kleisliCopyHeavyModelType(newModelTypeEndpoint);

		return newModelTypeEndpointRef;
	}

}
