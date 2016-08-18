package org.eclipse.tracecompass.tmf.statemachine.wizard;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.command.CommandStack;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.ui.editor.DiagramEditor;
import org.eclipse.graphiti.ui.editor.DiagramEditorInput;
import org.eclipse.graphiti.ui.services.GraphitiUi;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tracecompass.tmf.attributetree.core.model.AttributeTree;
import org.eclipse.tracecompass.tmf.attributetree.core.utils.AttributeTreeUtils;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.Transaction;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.impl.TransactionalEditingDomainImpl;

/**
 * Wizard to create a statemachine and an attribute tree. Create resources in a
 * tracing project under a folder called Statemachine
 *
 * @author esideli
 *
 */
public class StatemachineDiagramWizard extends BasicNewResourceWizard {

	/**
	 * Wizard page name for statemachine diagram creation
	 */
	public static final String PAGE_NAME_DIAGRAM = "New Diagram";

	/**
	 * Wizard page name for attribute tree editor
	 */
	public static final String PAGE_NAME_ATTRIBUTE_TREE = "Edit Attribute Tree";

	/**
	 * Wizard window title
	 */
	public static final String WIZARD_WINDOW_TITLE = "New Diagram";

	private String diagramTypeId = "State Machine";

	private AttributeTreeCreationPage attributeTreeEditorPage;
	private StatemachineDiagramPage newDiagramPage;
	private IStructuredSelection fSelection;

	private File attributeTreeFile;

	@Override
	public void addPages() {
		super.addPages();
		newDiagramPage = new StatemachineDiagramPage(PAGE_NAME_DIAGRAM, fSelection);
		attributeTreeEditorPage = new AttributeTreeCreationPage(PAGE_NAME_ATTRIBUTE_TREE);
		addPage(newDiagramPage);
		addPage(attributeTreeEditorPage);
	}

	@Override
	public boolean canFinish() {
	    if(!newDiagramPage.getUseExistingFile() && !attributeTreeEditorPage.isPageComplete()) {
	        return false;
	    }
		return super.canFinish();
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		super.init(workbench, currentSelection);
		fSelection = currentSelection;
		setWindowTitle(WIZARD_WINDOW_TITLE);
	}

	@Override
	public boolean performFinish() {
		String diagramName = newDiagramPage.getDiagramName();

		IProject project = null;
		IFolder diagramFolder = null;

		Object element = fSelection.getFirstElement();
		if (element instanceof IProject) {
			project = (IProject) element;
		} else if (element instanceof IFolder) {
			diagramFolder = (IFolder) element;
			project = diagramFolder.getProject();
		}

		if (project == null || !project.isAccessible()) {
			return false;
		}

		Diagram diagram = Graphiti.getPeCreateService().createDiagram(diagramTypeId, diagramName, true);

		if (diagramFolder == null) {
			IFolder statemachineFolder = null;
			IFolder statemachineDiagramFolder = null;

			statemachineFolder = project.getFolder("Statemachine");
			if (!statemachineFolder.exists()) {
				try {
					statemachineFolder.create(IResource.NONE, true, null);
				} catch (CoreException e) {
					return false;
				}
			}

			statemachineDiagramFolder = statemachineFolder.getFolder("Diagrams");
			if (!statemachineDiagramFolder.exists()) {
				try {
					statemachineDiagramFolder.create(IResource.NONE, true, null);
				} catch (CoreException e) {
					return false;
				}
			}

			diagramFolder = project.getFolder("Statemachine/Diagrams");
		}

		String statemachineDiagramExtension = "diagram";
		IFile diagramFile = diagramFolder.getFile(diagramName + "." + statemachineDiagramExtension);
		URI uri = URI.createPlatformResourceURI(diagramFile.getFullPath().toString(), true);
		boolean isCreated = createDiagramFile(uri, diagram);
		if(!isCreated) {
			return false;
		}

		String editorID = DiagramEditor.DIAGRAM_EDITOR_ID;
		String providerId = GraphitiUi.getExtensionManager().getDiagramTypeProviderId(diagram.getDiagramTypeId());
		DiagramEditorInput editorInput = new DiagramEditorInput(EcoreUtil.getURI(diagram), providerId);
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(editorInput, editorID);
		} catch (PartInitException e) {
			return false;
		}

		// Save or use the attribute tree
		if(newDiagramPage.getUseExistingFile()) {
			String treeFilePath = newDiagramPage.getExistingTreePath();
			AttributeTree.getInstance().saveAttributeTree(new File(treeFilePath));
			AttributeTreeUtils.addAttributeTreeFile(diagramName, treeFilePath);
		} else {
		    AttributeTree.getInstance().saveAttributeTree(attributeTreeFile);
		}
		// TODO If attribute cannot be create the diagram will be create and it's wrong !
		return true;
	}

	@Override
	public boolean performCancel() {
	    // Delete the attribute tree that was created previously if any
	    if(attributeTreeFile.exists()) {
	        attributeTreeFile.delete();
	    }
	    return super.performCancel();
	}

    /**
     * Create an attribute tree file under Statemachine/Tree folder in your
     * tracing project.
     *
     * @param diagramName
     *            Name of the new diagram to associate the new tree to this
     *            diagram.
     * @return True if the file was correctly created, False if not.
     */
	public boolean createAttributeTreeFile(String diagramName) {
		IFolder attributeTreeFolder = null;
		IProject attributeTreeProject = null;

		Object selectedElement = fSelection.getFirstElement();
		if (selectedElement instanceof IProject) {
			attributeTreeProject = (IProject) selectedElement;
		} else if (selectedElement instanceof IFolder) {
			attributeTreeFolder = (IFolder) selectedElement;
			attributeTreeProject = attributeTreeFolder.getProject();
		}

		if (attributeTreeProject == null || !attributeTreeProject.isAccessible()) {
			return false;
		}

		if (attributeTreeFolder == null) {
			IFolder statemachineFolder = null;
			IFolder treeFolder = null;

			statemachineFolder = attributeTreeProject.getFolder("Statemachine");
			if (!statemachineFolder.exists()) {
				try {
					statemachineFolder.create(IResource.NONE, true, null);
				} catch (CoreException e) {
					return false;
				}
			}

			treeFolder = statemachineFolder.getFolder("Tree");
			if (!treeFolder.exists()) {
				try {
					treeFolder.create(IResource.NONE, true, null);
				} catch (CoreException e) {
					return false;
				}
			}
			attributeTreeFolder = attributeTreeProject.getFolder("Statemachine/Tree");
		}

		attributeTreeFile = attributeTreeFolder.getFile(newDiagramPage.getNewTreeName() + ".attributetree").getLocation().toFile();
		if (!attributeTreeFile.exists()) {
			if(!AttributeTree.getInstance().createNewAttributeTree(attributeTreeFile)) {
				return false;
			}
		}

		AttributeTreeUtils.addAttributeTreeFile(diagramName, attributeTreeFile.getAbsolutePath());
		return true;
	}

    /**
     * Create the resources and the Graphiti editing domain, than call
     * save(TransactionalEditingDomain) to create the diagram file
     *
     * @param uri
     *            Diagram file location
     * @param diagram
     *            The diagram previously created with Graphiti services
     * @return True if everything was correctly created, false if not
     */
	private static boolean createDiagramFile(URI uri, final Diagram diagram) {
		TransactionalEditingDomain editingDomain = GraphitiUi.getEmfService().createResourceSetAndEditingDomain();
		ResourceSet resourceSet = editingDomain.getResourceSet();

		final Resource resource = resourceSet.createResource(uri);

		CommandStack commandStack = editingDomain.getCommandStack();
		commandStack.execute(new RecordingCommand(editingDomain) {

			@Override
			protected void doExecute() {
				resource.setTrackingModification(true);
				resource.getContents().add(diagram);
			}
		});

		boolean saveStatus = save(editingDomain);
		editingDomain.dispose();

		return saveStatus;
	}

    /**
     * Create the Graphiti diagram file with EMF transaction and editing domain
     *
     * @param editingDomain
     * @return True if there is no error during the creation
     */
	private static boolean save(final TransactionalEditingDomain editingDomain) {
		boolean saveStatus = true;
		final Map<URI, String> failedSaves = new HashMap<>();

		IWorkspaceRunnable wsRunnable = new IWorkspaceRunnable() {

			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						Transaction parentTransaction;
						if(editingDomain != null) {
                            if ((parentTransaction = ((TransactionalEditingDomainImpl) editingDomain).getActiveTransaction()) != null) {
                            	do {
                            		if(!parentTransaction.isReadOnly()) {
                            			throw new IllegalStateException("deadlock"); //TODO Change this message
                            		}
                            	} while ((parentTransaction = ((TransactionalEditingDomainImpl) editingDomain).getActiveTransaction().getParent()) != null);
                            }
                            Resource[] resourcesArray = (Resource[]) editingDomain.getResourceSet().getResources().toArray();
                            for(int i = 0; i < resourcesArray.length; i++) {
                                Resource resource = resourcesArray[i];
                                if(resource.isModified()) {
                                    try {
                                        resource.save((Map<?, ?>) Collections.emptyMap().get(resource));
                                    } catch (IOException e) {
                                        failedSaves.put(resource.getURI(), "Fail");
                                    }
                                }
                            }
                        }

					}
				};

				try {
					editingDomain.runExclusive(runnable);
				} catch (final InterruptedException e) {
					throw new RuntimeException(e);
				}
				editingDomain.getCommandStack().flush();
			}
		};

		try {
			ResourcesPlugin.getWorkspace().run(wsRunnable, null);
			if (!failedSaves.isEmpty()) {
				saveStatus = false;
			}
		} catch (final CoreException e) {
			saveStatus = false;
		}

		return saveStatus;
	}

	/**
	 * @return The attribute tree file that will be used in the attribute tree editor.
	 */
	public File getAttributeTreeFile() {
	    return attributeTreeFile;
	}

}
