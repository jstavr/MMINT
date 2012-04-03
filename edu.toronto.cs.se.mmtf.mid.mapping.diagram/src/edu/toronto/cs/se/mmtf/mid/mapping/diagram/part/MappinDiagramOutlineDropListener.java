package edu.toronto.cs.se.mmtf.mid.mapping.diagram.part;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramDropTargetListener;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;

public class MappinDiagramOutlineDropListener extends DiagramDropTargetListener {

	public MappinDiagramOutlineDropListener(EditPartViewer viewer, Transfer xfer) {

		super(viewer, xfer);
	}

	@Override
	protected List<EObject> getObjectsBeingDropped() {

		TransferData data = getCurrentEvent().currentDataType;
		ArrayList<EObject> result = new ArrayList<EObject>();
		Object transferedObject = LocalSelectionTransfer.getTransfer().nativeToJava(data);

		if (transferedObject instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) transferedObject;
			for (Iterator<?> it = selection.iterator(); it.hasNext();) {
				Object nextSelectedObject = it.next();
				if (nextSelectedObject instanceof EObject) {
					result.add((EObject) nextSelectedObject);
				}
			}
		}

		return result;
	}

}