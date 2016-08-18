package org.eclipse.tracecompass.tmf.attributetree.ui.views;

import java.io.File;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.tmf.attributetree.core.model.AttributeTree;
import org.eclipse.tracecompass.tmf.attributetree.ui.Activator;
import org.eclipse.tracecompass.tmf.attributetree.ui.widgets.AttributeTreeEditorComposite;
import org.eclipse.ui.IActionBars;

/**
 * View to edit an attribute tree
 *
 * @author esideli
 *
 */
public class AttributeTreeView extends TmfView {

	private AttributeTreeEditorComposite treeEditorComposite;

	private File openedFile;
	private String LAST_OPENED_KEY = "lastOpenedFile";

	private int GRID_NUM_COLUMNS = 3;

	/**
	 * Constructor
	 */
	public AttributeTreeView() {
		super("org.eclipse.tracecompass.tmf.statemachine.ui.attributeTreeView");
	}

	@Override
	public void createPartControl(Composite parent) {
		String lastOpenedFilePath = Activator.getDefault().getDialogSettings().get(LAST_OPENED_KEY);
        if(lastOpenedFilePath != null) {
            openedFile = new File(lastOpenedFilePath);
        }

        treeEditorComposite = new AttributeTreeEditorComposite(parent, SWT.NONE, openedFile);

        GridDataFactory.fillDefaults().grab(true, true).span(GRID_NUM_COLUMNS, 1).applyTo(treeEditorComposite);

		setViewInformation(openedFile);

        IActionBars bars = getViewSite().getActionBars();
        //bars.getToolBarManager().add(getNewAction());
        bars.getToolBarManager().add(getOpenAction());
        bars.getToolBarManager().add(getSaveAction());
	}

    /**
     * Action to save the attribute tree
     *
     * @return Action
     */
	private Action getSaveAction() {
		Action saveAction = new Action("Save", IAction.AS_PUSH_BUTTON) {
			@Override
            public void run() {
				AttributeTree.getInstance().saveAttributeTree(openedFile);
//				// TODO If openedFile doesn't exist it crash
//				Document xmlFile = null;
//    			try {
//    				DocumentBuilderFactory dbFactory = DocumentBuilderFactory
//    						.newInstance();
//    				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
//    				xmlFile = dBuilder.newDocument();
//    			} catch (ParserConfigurationException exception) {
//    			}
//
//    			Element rootElement = attributeTree.getRoot().createElement(attributeTree.getRoot(), xmlFile);
//    			xmlFile.appendChild(rootElement);
//    			try {
//    				TransformerFactory transformerFactory = TransformerFactory
//    						.newInstance();
//    				Transformer transformer = transformerFactory.newTransformer();
//    				DOMSource source = new DOMSource(xmlFile);
//
//    				StreamResult savedFileResult = new StreamResult(openedFile);
//    				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//    				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
//    				transformer.transform(source, savedFileResult);
//    			} catch (TransformerException exception) {
//    			}
			}
		};
		saveAction.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath("/icons/save_button.gif"));
		//saveAction.setText("Save");
		return saveAction;
	}

    /**
     * Action to open an attribute tree
     *
     * @return Action
     */
	private Action getOpenAction() {
		Action openAction = new Action("Open", IAction.AS_PUSH_BUTTON) {
			@Override
            public void run() {
				FileDialog openDialog = new FileDialog(new Shell(), SWT.OPEN);
				openDialog.setFilterNames(new String[] { "Attribute Tree" + " (*.attributetree)"}); //$NON-NLS-1$
				openDialog.setFilterExtensions(new String[] { "*.attributetree"}); //$NON-NLS-1$

				// Default path for attribute tree selection (the workspace)
				String defaultAttributeTreePath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
				openDialog.setFilterPath(defaultAttributeTreePath);

		        String filePath = openDialog.open();
				if (filePath != null) {
					openedFile = new File(filePath);
					treeEditorComposite.setTreeViewerInput(openedFile);
					IDialogSettings settings = Activator.getDefault().getDialogSettings();
					settings.put(LAST_OPENED_KEY, filePath);
				}
				setViewInformation(openedFile);
			}
		};
		openAction.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath("/icons/open.gif"));
		return openAction;
	}

//	private Action getNewAction() {
//		Action newAction = new Action("New tree", IAction.AS_PUSH_BUTTON) {
//			@Override
//            public void run() {
//				FileDialog saveDialog = new FileDialog(new Shell(), SWT.SAVE);
//				saveDialog.setFilterNames(new String[] { "Attribute Tree" + " (*.attributetree)"}); //$NON-NLS-1$
//				saveDialog.setFilterExtensions(new String[] { "*.attributetree"}); //$NON-NLS-1$
//
//		        String filePath = saveDialog.open();
//		        File treeFile = new File(filePath);
//		        attributeTree.setTreeViewerInput(treeFile);
//			}
//		};
//		newAction.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath("/icons/new.gif"));
//		return newAction;
//	}

    /**
     * Set view information. The name of the file is displayed in the view title
     *
     * @param file
     *            File that is used in the view
     */
	private void setViewInformation(File file) {
		if(file == null) {
			setPartName("Attribute Tree View");
		} else {
			String fileName = file.getName();
			String viewTitle = fileName.substring(0, fileName.indexOf(".")) + " (" + file.getAbsolutePath() + ")";
			setPartName(viewTitle);
		}
	}

	@Override
	public void setFocus() {
	    treeEditorComposite.setFocus();
	}
}
