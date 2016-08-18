package org.eclipse.tracecompass.tmf.attributetree.ui.widgets;

import java.io.File;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tracecompass.tmf.attributetree.core.model.AbstractAttributeNode;
import org.eclipse.tracecompass.tmf.attributetree.core.model.AttributeTreePath;
import org.eclipse.tracecompass.tmf.attributetree.core.model.AttributeValueNode;
import org.eclipse.tracecompass.tmf.attributetree.core.model.ConstantAttributeNode;
import org.eclipse.tracecompass.tmf.attributetree.core.model.VariableAttributeNode;
import org.eclipse.tracecompass.tmf.attributetree.ui.Activator;

public class AttributeTreeEditorComposite extends Composite {

    private int GRID_NUM_COLUMNS = 3;
    private AttributeTreeComposite attributeTree;
    private File currentAttributeTreeFile;

    private enum NodeType {
        CONSTANT, VARIABLE, VALUE
    }

    public AttributeTreeEditorComposite(Composite parent, int style, File attributeTreeFile) {
        super(parent, style);
        setLayout(new GridLayout(GRID_NUM_COLUMNS, false));

        Image addConstantImage = Activator.getDefault().getImageFromPath("/icons/addconstantAttribute.png");
        Image addVariableImage = Activator.getDefault().getImageFromPath("/icons/addvariableAttribute.png");
        Image addValueImage = Activator.getDefault().getImageFromPath("/icons/addvalue.png");
        Image removeImage = Activator.getDefault().getImageFromPath("/icons/removeAttribute.png");
        Image editAttributeImage = Activator.getDefault().getImageFromPath("/icons/rename.gif");

        Button addConstantAttributeButton = new Button(this, SWT.PUSH);
        //addConstantAttributeButton.setText("Constant");
        addConstantAttributeButton.setImage(addConstantImage);
        addConstantAttributeButton.setToolTipText("Add Constant Attribute");

        Button addVariableAttributeButton = new Button(this, SWT.PUSH);
        //addVariableAttributeButton.setText("Variable");
        addVariableAttributeButton.setImage(addVariableImage);
        addVariableAttributeButton.setToolTipText("Add Variable Attribute");

        Button addAttributeValueButton = new Button(this, SWT.PUSH);
        //addAttributeValueButton.setText("Value");
        addAttributeValueButton.setImage(addValueImage);
        addAttributeValueButton.setToolTipText("Add Possible Attribute Value");

        Button removeAttributeButton = new Button(this, SWT.PUSH);
        //removeAttributeButton.setText("Remove");
        removeAttributeButton.setToolTipText("Remove");
        removeAttributeButton.setImage(removeImage);

        removeAttributeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                IStructuredSelection selection = attributeTree.getSelection();
                if(!selection.isEmpty()) {
                    if(selection.getFirstElement() instanceof AbstractAttributeNode) {
                        removeAttribute((AbstractAttributeNode)selection.getFirstElement());
                    }
                }
            }
        });
        // Will be enabled when selection changes
        removeAttributeButton.setEnabled(false);

        Button editAttributeButton = new Button(this, SWT.PUSH);
        //editAttributeButton.setText("Edit");
        editAttributeButton.setImage(editAttributeImage);
        editAttributeButton.setToolTipText("Edit");

        editAttributeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                editSelection();
            }
        });

          // Will be enabled when selection changes
        editAttributeButton.setEnabled(false);

        // TODO : remove when right click will be implemented
        Button changeQueryVariableAttributeButton = new Button(this, SWT.PUSH);
        changeQueryVariableAttributeButton.setText("Query");
        changeQueryVariableAttributeButton.setToolTipText("Change Query");

        changeQueryVariableAttributeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                IStructuredSelection selection = attributeTree.getSelection();
                if(!selection.isEmpty()) {
                    if(selection.getFirstElement() instanceof VariableAttributeNode) {
                        VariableAttributeNode queryNode = (VariableAttributeNode)selection.getFirstElement();
                        if(queryNode.getIsQuery()) {
                            queryNode.setIsQuery(false);
                            queryNode.setQueryPath(null);
                        } else {
                            queryDialog(getDisplay(), queryNode);
                        }
                        attributeTree.refresh();
                    }
                }
            }
        });
        // Will be enabled when selection changes
        changeQueryVariableAttributeButton.setEnabled(false);

        addConstantAttributeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addAttribute(NodeType.CONSTANT);
            }
        });

        addVariableAttributeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addAttribute(NodeType.VARIABLE);
            }
        });

        addAttributeValueButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addAttribute(NodeType.VALUE);
            }
        });

        currentAttributeTreeFile = attributeTreeFile;
        attributeTree = new AttributeTreeComposite(this, SWT.NONE);
        if(currentAttributeTreeFile != null && currentAttributeTreeFile.exists()) {
            attributeTree.setTreeViewerInput(currentAttributeTreeFile);
        }
//        String lastOpenedFilePath = Activator.getDefault().getDialogSettings().get(LAST_OPENED_KEY);
//        if(lastOpenedFilePath != null) {
//            openedFile = new File(lastOpenedFilePath);
//            if(openedFile.exists()) {
//                attributeTree.setTreeViewerInput(openedFile);
//            }
//        }
        attributeTree.getTreeViewer().addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection selection = attributeTree.getSelection();
                removeAttributeButton.setEnabled(!selection.isEmpty());
                editAttributeButton.setEnabled(!selection.isEmpty());
                changeQueryVariableAttributeButton.setEnabled(!selection.isEmpty() && selection.getFirstElement() instanceof VariableAttributeNode);
            }
        });
        attributeTree.getTreeViewer().getTree().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                editSelection();
            }
        });
    }

    private void removeAttribute(AbstractAttributeNode node) {
        node.getParent().removeChild(node);
        attributeTree.refresh();
    }

    private void addAttribute(NodeType type) {
        IStructuredSelection selection = attributeTree.getSelection();
        AbstractAttributeNode parent;
        if(selection.isEmpty()) {
            parent = attributeTree.getRoot();
        } else {
            parent = (AbstractAttributeNode) selection.getFirstElement();
        }

        switch(type) {
        case CONSTANT:
            new ConstantAttributeNode(parent);
            break;
        case VARIABLE:
            new VariableAttributeNode(parent);
            break;
        case VALUE:
            new AttributeValueNode(parent);
            break;
        default:
            throw new IllegalStateException("Enum value not handled"); //$NON-NLS-1$
        }
        attributeTree.refresh();
    }

    private void editSelection() {
        IStructuredSelection selection = attributeTree.getSelection();
        if (!selection.isEmpty()) {
            if (selection.getFirstElement() instanceof AbstractAttributeNode) {
                editAttributeDialog(getDisplay(), (AbstractAttributeNode) selection.getFirstElement());
            }
        }
    }

    private void editAttributeDialog(Display display, final AbstractAttributeNode attributeNode) {
        final Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        dialog.setLayout (new GridLayout(2, false));
        dialog.setText("Attribtue name");

        GridData gridData;
        Label nameLabel = new Label(dialog, SWT.NONE);
        nameLabel.setText("Name");

        final Text attributeNameText = new Text(dialog, SWT.SINGLE);
        attributeNameText.setText(attributeNode.getName());
        attributeNameText.selectAll();
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        attributeNameText.setLayoutData(gridData);

        Button ok = new Button(dialog, SWT.PUSH);
        ok.setText("Ok");
        ok.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected (SelectionEvent e) {
                performOkInEditDialog(attributeNode, dialog, attributeNameText);
            }

        });

        Button cancel = new Button(dialog, SWT.PUSH);
        cancel.setText("Cancel");
        cancel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected (SelectionEvent e) {
                dialog.close();
            }
        });

        dialog.addTraverseListener(new TraverseListener() {

            @Override
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    performOkInEditDialog(attributeNode, dialog, attributeNameText);
                } else if (e.detail == SWT.TRAVERSE_ESCAPE) {
                    dialog.close();
                }
            }
        });

        dialog.pack();
        dialog.open();
        while (!dialog.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    private void performOkInEditDialog(final AbstractAttributeNode attributeNode, final Shell dialog, final Text attributeNameText) {
        attributeNode.setName(attributeNameText.getText());
        attributeTree.refresh();
        dialog.close();
    }

    private void queryDialog(Display display, final VariableAttributeNode queryNode) {
        final Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        dialog.setLayout (new GridLayout(1, false));
        dialog.setText("Query path");

        final AttributeTreeComposite queryAttributeTree = new AttributeTreeComposite(dialog, SWT.NONE);
        queryAttributeTree.setTreeViewerInput(currentAttributeTreeFile);

        dialog.addTraverseListener(new TraverseListener() {

            @Override
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    selectInQueryDialog(queryNode, dialog, queryAttributeTree);
                } else if (e.detail == SWT.TRAVERSE_ESCAPE) {
                    dialog.close();
                }
            }
        });

        Button selectButton = new Button(dialog, SWT.PUSH);
        selectButton.setText("Select");
        selectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected (SelectionEvent e) {
                selectInQueryDialog(queryNode, dialog, queryAttributeTree);
            }
        });

        dialog.pack();
        dialog.open();
        while (!dialog.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    private static void selectInQueryDialog(final VariableAttributeNode queryNode, final Shell dialog, final AttributeTreeComposite queryAttributeTree) {
        IStructuredSelection selection = queryAttributeTree.getSelection();
        AbstractAttributeNode selectedNode = (AbstractAttributeNode)selection.getFirstElement();
        queryNode.setIsQuery(true);
        queryNode.setQueryPath(new AttributeTreePath(selectedNode));
        dialog.close();
    }

    public void setTreeViewerInput(File inputAttributeTreeFile) {
        attributeTree.setTreeViewerInput(inputAttributeTreeFile);
        currentAttributeTreeFile = inputAttributeTreeFile;
    }

//    public AttributeTreeComposite getAttributeTreeComposite() {
//        return attributeTree;
//    }

}
