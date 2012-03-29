/*
 * Copyright (C) 2012 Marsha Chechik, Alessio Di Sandro, Rick Salay
 * 
 * This file is part of MMTF ver. 0.9.0.
 * 
 * MMTF is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MMTF is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MMTF.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.toronto.cs.se.mmtf.mid.mapping.diagram.part;

import java.util.Collections;
import java.util.List;

import org.eclipse.gef.Tool;
import org.eclipse.gef.palette.PaletteContainer;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gmf.runtime.diagram.ui.tools.UnspecifiedTypeConnectionTool;
import org.eclipse.gmf.runtime.diagram.ui.tools.UnspecifiedTypeCreationTool;
import org.eclipse.gmf.runtime.emf.type.core.IElementType;

import edu.toronto.cs.se.mmtf.mid.mapping.diagram.providers.MIDElementTypes;

/**
 * @generated
 */
public class MIDPaletteFactory {

	/**
	 * @generated
	 */
	public void fillPalette(PaletteRoot paletteRoot) {
		paletteRoot.add(createMid1Group());
	}

	/**
	 * Creates "mid" palette tool group
	 * @generated
	 */
	private PaletteContainer createMid1Group() {
		PaletteGroup paletteContainer = new PaletteGroup(
				Messages.Mid1Group_title);
		paletteContainer.setId("createMid1Group"); //$NON-NLS-1$
		paletteContainer.add(createNewModelElementReference1CreationTool());
		paletteContainer.add(createNewmapping2CreationTool());
		paletteContainer.add(createNewmappingconnection3CreationTool());
		return paletteContainer;
	}

	/**
	 * @generated
	 */
	private ToolEntry createNewModelElementReference1CreationTool() {
		NodeToolEntry entry = new NodeToolEntry(
				Messages.NewModelElementReference1CreationTool_title,
				Messages.NewModelElementReference1CreationTool_desc,
				Collections
						.singletonList(MIDElementTypes.ModelElementReference_3003));
		entry.setId("createNewModelElementReference1CreationTool"); //$NON-NLS-1$
		entry.setSmallIcon(MIDElementTypes
				.getImageDescriptor(MIDElementTypes.ModelElementReference_3003));
		entry.setLargeIcon(entry.getSmallIcon());
		return entry;
	}

	/**
	 * @generated
	 */
	private ToolEntry createNewmapping2CreationTool() {
		NodeToolEntry entry = new NodeToolEntry(
				Messages.Newmapping2CreationTool_title,
				Messages.Newmapping2CreationTool_desc,
				Collections.singletonList(MIDElementTypes.Mapping_2004));
		entry.setId("createNewmapping2CreationTool"); //$NON-NLS-1$
		entry.setSmallIcon(MIDElementTypes
				.getImageDescriptor(MIDElementTypes.Mapping_2004));
		entry.setLargeIcon(entry.getSmallIcon());
		return entry;
	}

	/**
	 * @generated
	 */
	private ToolEntry createNewmappingconnection3CreationTool() {
		LinkToolEntry entry = new LinkToolEntry(
				Messages.Newmappingconnection3CreationTool_title,
				Messages.Newmappingconnection3CreationTool_desc,
				Collections.singletonList(MIDElementTypes.MappingElements_4002));
		entry.setId("createNewmappingconnection3CreationTool"); //$NON-NLS-1$
		entry.setSmallIcon(MIDElementTypes
				.getImageDescriptor(MIDElementTypes.MappingElements_4002));
		entry.setLargeIcon(entry.getSmallIcon());
		return entry;
	}

	/**
	 * @generated
	 */
	private static class NodeToolEntry extends ToolEntry {

		/**
		 * @generated
		 */
		private final List<IElementType> elementTypes;

		/**
		 * @generated
		 */
		private NodeToolEntry(String title, String description,
				List<IElementType> elementTypes) {
			super(title, description, null, null);
			this.elementTypes = elementTypes;
		}

		/**
		 * @generated
		 */
		public Tool createTool() {
			Tool tool = new UnspecifiedTypeCreationTool(elementTypes);
			tool.setProperties(getToolProperties());
			return tool;
		}
	}

	/**
	 * @generated
	 */
	private static class LinkToolEntry extends ToolEntry {

		/**
		 * @generated
		 */
		private final List<IElementType> relationshipTypes;

		/**
		 * @generated
		 */
		private LinkToolEntry(String title, String description,
				List<IElementType> relationshipTypes) {
			super(title, description, null, null);
			this.relationshipTypes = relationshipTypes;
		}

		/**
		 * @generated
		 */
		public Tool createTool() {
			Tool tool = new UnspecifiedTypeConnectionTool(relationshipTypes);
			tool.setProperties(getToolProperties());
			return tool;
		}
	}
}