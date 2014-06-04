/**
 * Copyright (c) 2012-2014 Marsha Chechik, Alessio Di Sandro, Michalis Famelis,
 * Rick Salay.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Naama Ben-David - Implementation.
 */
package edu.toronto.cs.se.modelepedia.z3.reasoning;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.gmf.runtime.notation.Connector;
import org.eclipse.gmf.runtime.notation.Diagram;
import org.eclipse.gmf.runtime.notation.Shape;
import org.eclipse.gmf.runtime.notation.View;

import edu.toronto.cs.se.mmint.MMINTException;
import edu.toronto.cs.se.mmint.MultiModelTypeRegistry;
import edu.toronto.cs.se.mmint.mavo.MAVOElement;
import edu.toronto.cs.se.mmint.mid.Model;
import edu.toronto.cs.se.mmint.mid.constraint.MultiModelConstraintChecker.MAVOTruthValue;
import edu.toronto.cs.se.mmint.mid.library.MultiModelUtils;
import edu.toronto.cs.se.mmint.mid.operator.Operator;
import edu.toronto.cs.se.mmint.mid.ui.GMFDiagramUtils;
import edu.toronto.cs.se.mmint.mid.ui.MultiModelDiagramUtils;
import edu.toronto.cs.se.modelepedia.z3.Z3SMTIncrementalSolver;
import edu.toronto.cs.se.modelepedia.z3.Z3SMTModel;
import edu.toronto.cs.se.modelepedia.z3.Z3SMTUtils;
import edu.toronto.cs.se.modelepedia.z3.Z3SMTIncrementalSolver.Z3IncrementalBehavior;
import edu.toronto.cs.se.modelepedia.z3.mavo.EcoreMAVOToSMTLIB;
import edu.toronto.cs.se.modelepedia.z3.mavo.EcoreMAVOToSMTLIBListener;

public class MAVOConcretizationHighlighter{
	private static final int GREY_OUT_COLOR = 0xD9D9D9;
	private static final String DIAGRAM_ID = "edu.toronto.cs.se.modelepedia.graph_mavo.diagram.part.Graph_MAVODiagramEditorID";
	private static final String PREVIOUS_OPERATOR_URI = "http://se.cs.toronto.edu/modelepedia/Operator_EcoreMAVOToSMTLIB";
	
	private MAVOTruthValue resultMAVO;
	private String smtEncoding;
	private Map<Integer, String> smtEncodingEdges;
	private Map<Integer, String> smtEncodingNodes;
	private String newDiagramURI;
	private String smtProperty;
	private Map<String, View> diagramElements;

	public MAVOConcretizationHighlighter() throws MMINTException {

		EcoreMAVOToSMTLIB previousOperator = 
			(EcoreMAVOToSMTLIB) MultiModelTypeRegistry.<Operator>getType(PREVIOUS_OPERATOR_URI);
		
		resultMAVO = MAVOTruthValue.ERROR;
		EcoreMAVOToSMTLIBListener smtListener = previousOperator.getListener();
		smtEncoding = smtListener.getSMTLIBEncoding();
		
		smtEncodingEdges = smtListener.getSMTLIBEncodingEdges();
		smtEncodingNodes = smtListener.getSMTLIBEncodingNodes();
		
		diagramElements = new HashMap<String, View>();

	}

	@SuppressWarnings("unchecked")
	public EList<Model> execute(EList<Model> actualParameters) throws Exception {

		Model model = actualParameters.get(0);
		smtProperty = model.getConstraint().getImplementation();
		resultMAVO = Z3SMTReasoningEngine.checkMAVOProperty(smtEncoding, smtProperty);
		
		if (resultMAVO != MAVOTruthValue.MAYBE){
			return null;
		}
		
		boolean showExample = askUser();
		
		if (showExample == false){
			return null;
		}
		
		//Load diagram
		Diagram d = copyAndLoadDiagram(model);
		
		//Get view elements from diagram
		Map<String, View> modelNodes = collectFormulaIDs((EList<View>) d.getChildren());
		Map<String, View> modelEdges = collectFormulaIDs((EList<View>) d.getEdges());
		diagramElements.putAll(modelNodes);
		diagramElements.putAll(modelEdges);

		Z3SMTModel resultModel = runZ3SMTSolver();
		
		//get FormulaIDs of elements to be grayed out
		Set<String> notInExample = findUnusedElements(resultModel);
		
		//grey out anything that's not in the example
		for (String FID : notInExample){
			View element = diagramElements.get(FID);
			color(element, GREY_OUT_COLOR);
		}
		
		//Write diagram to file
		MultiModelUtils.createModelFile(d, newDiagramURI, false);
		GMFDiagramUtils.openGMFDiagram(newDiagramURI, DIAGRAM_ID);

		return null;
	}
	
	/**
	 * Create a pop-up asking user whether or not to show the example
	 * @return the user's answer
	 */
	private boolean askUser(){
		String dialogTitle = "Example Viewer";
		String dialogMessage = "Would you like to view an example?";
		return MultiModelDiagramUtils.getBooleanInput(dialogTitle, dialogMessage);
	}
	
	/**
	 * Find and return the set of Formula IDs of the elements of the diagram
	 * that are not in the resultModel.
	 * @param resultModel - a concretization of the model
	 * @return the set of Formula IDs of elements not included in the concretization
	 */
	private Set<String> findUnusedElements(Z3SMTModel resultModel){
		Map<String, Integer> resultModelEdges = resultModel.getZ3ModelEdges(smtEncodingEdges);
		Map<String, Integer> resultModelNodes = resultModel.getZ3ModelNodes(smtEncodingNodes);
		
		Set<String> remainingElements = diagramElements.keySet();
		//remove FormulaIDs of elements in example
		for (Integer intID: resultModelEdges.values()){
			String FID = smtEncodingEdges.get(intID);
			remainingElements.remove(FID);
		} 
		for (Integer intID: resultModelNodes.values()){
			String FID = smtEncodingNodes.get(intID);
			remainingElements.remove(FID);
		}
		
		return remainingElements;
	}
	
	private void color(View element, int color) {
		if (element instanceof Shape) {
			((Shape) element).setFillColor(color);
		}
		else if (element instanceof Connector){
			Connector line = (Connector) element;
			line.setLineColor(color);
			line.setLineWidth(0);
		}
		else{
			System.err.println("View object not an instance of Shape or Connector");
		}
	}

	/**
	 * Create a copy of the original diagram and return the copy
	 * @param model
	 * @return
	 * @throws Exception 
	 */
	private Diagram copyAndLoadDiagram(Model model) throws Exception{

		String diagramURI = MultiModelUtils.replaceFileExtensionInUri(
			model.getUri(),
			"graphdiag_mavo"
		);
		diagramURI = MultiModelUtils.prependWorkspaceToUri(diagramURI);
		newDiagramURI = newFileName(diagramURI);

		try {
			MultiModelUtils.copyTextFileAndReplaceText(diagramURI, newDiagramURI, "", "", false);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		Diagram d = (Diagram) MultiModelUtils.getModelFile(newDiagramURI, false);
		return d;
	}
	
	private String newFileName(String oldFileName){
		int lastPathSeparator = oldFileName.lastIndexOf(System.getProperty("file.separator"));
		String path = oldFileName.substring(0, lastPathSeparator+1);
		String name = oldFileName.substring(lastPathSeparator+1);
		String newName = "copy_".concat(name);
		return path.concat(newName);
		
	}
	
	private Z3SMTModel runZ3SMTSolver(){
		Z3SMTIncrementalSolver z3Solver = new Z3SMTIncrementalSolver();
		z3Solver.firstCheckSatAndGetModel(smtEncoding);
		Z3SMTModel resultModel = z3Solver.checkSatAndGetModel(Z3SMTUtils.assertion(smtProperty), Z3IncrementalBehavior.POP);
		return resultModel;
	}
	
	/**
	 * Map diagram view elements to the formula IDs of the model elements they represent.
	 * @param elementList - List of View elements from the diagram.
	 * @return a Map whose keys are formula IDs, and the values are the view elements representing those elements of the model.
	 */
	private Map<String, View> collectFormulaIDs(EList<View> elementList){
		Map<String, View> modelElements = new HashMap<String, View>();
		for (View element: elementList){
			MAVOElement modelElement = (MAVOElement) element.getElement();
			modelElements.put(modelElement.getFormulaId(), element);
		}
		return modelElements;
	}
	
}
