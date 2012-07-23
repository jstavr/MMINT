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
package edu.toronto.cs.se.modelepedia.randommodel.operator;

import java.net.URL;
import java.util.Date;
import java.util.Properties;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;

import edu.toronto.cs.se.mmtf.mid.Model;
import edu.toronto.cs.se.mmtf.mid.ModelOrigin;
import edu.toronto.cs.se.mmtf.mid.MultiModel;
import edu.toronto.cs.se.mmtf.mid.editor.Editor;
import edu.toronto.cs.se.mmtf.mid.operator.impl.OperatorExecutableImpl;
import edu.toronto.cs.se.mmtf.mid.trait.MultiModelFactoryUtils;
import edu.toronto.cs.se.mmtf.mid.trait.OperatorUtils;
import edu.toronto.cs.se.modelepedia.randommodel.RandomModel;
import edu.toronto.cs.se.modelepedia.randommodel.RandommodelPackage;

public class RandomModelGenerate extends OperatorExecutableImpl {

	private static final String RANDOM_SUFFIX = "_random_";
	private static final String PYTHON_SCRIPT = "/python/graph_gen.py";
	/** % of annotated elements in the random model. */
	private static final String PROPERTY_ANNOTATIONS = "annotations";
	/** % of may elements among the annotated elements. */
	private static final String PROPERTY_MAY = "may";
	/** % of set elements among the annotated elements. */
	private static final String PROPERTY_SET = "set";
	/** % of var elements among the annotated elements. */
	private static final String PROPERTY_VAR = "var";

	private double annotations;
	private double may;
	private double set;
	private double var;

	private void readProperties(Properties properties) throws Exception {

		annotations = OperatorUtils.getDoubleProperty(properties, PROPERTY_ANNOTATIONS);
		may = OperatorUtils.getDoubleProperty(properties, PROPERTY_MAY);
		set = OperatorUtils.getDoubleProperty(properties, PROPERTY_SET);
		var = OperatorUtils.getDoubleProperty(properties, PROPERTY_VAR);
	}

	@Override
	public EList<Model> execute(EList<Model> actualParameters) throws Exception {

		// create random instance
		Model model = actualParameters.get(0);
		Properties inputProperties = OperatorUtils.getInputPropertiesFile(this, model, null, false);
		readProperties(inputProperties);
		String baseUri = model.getUri().substring(0, model.getUri().lastIndexOf(IPath.SEPARATOR)+1);
		String modelType = ((RandomModel) model.getRoot()).getName();
		String randomUri =
			baseUri +
			modelType +
			RANDOM_SUFFIX +
			(new Date()).getTime() +
			"." + RandommodelPackage.eNAME;
		String workspaceUri = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
		URL url = RandomModelOperatorActivator.getDefault().getBundle().getEntry(PYTHON_SCRIPT);
		String pythonPath = FileLocator.toFileURL(url).toString().substring(5); // cuts "file:/"
		String[] cmd = new String[] {
			"python",
			pythonPath,
			"-input",
			workspaceUri + model.getUri(),
			"-output",
			workspaceUri + randomUri,
			"-instname",
			randomUri,
			"-annotated",
			String.valueOf(annotations),
			"-may",
			String.valueOf(may),
			"-set",
			String.valueOf(set),
			"-var",
			String.valueOf(var)
		};
		Runtime rt = Runtime.getRuntime();
		Process p = rt.exec(cmd);
		p.waitFor();

		EList<Model> result = new BasicEList<Model>();
		if (!OperatorUtils.isUpdatingMid(inputProperties)) {
			return result;
		}

		// create model
		URI modelUri = URI.createPlatformResourceURI(randomUri, true);
		MultiModel owner = (MultiModel) model.eContainer();
		MultiModelFactoryUtils.assertModelUnique(owner, modelUri);
		Model newElement = MultiModelFactoryUtils.createModel(null, ModelOrigin.CREATED, owner, modelUri);
		Editor editor = MultiModelFactoryUtils.createEditor(newElement);
		if (editor != null) {
			MultiModelFactoryUtils.addModelEditor(editor, owner);
		}
		result.add(newElement);

		return result;
	}

}
