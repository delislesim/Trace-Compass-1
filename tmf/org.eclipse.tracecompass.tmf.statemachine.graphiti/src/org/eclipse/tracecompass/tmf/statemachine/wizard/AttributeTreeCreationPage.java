package org.eclipse.tracecompass.tmf.statemachine.wizard;

import java.io.File;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tracecompass.tmf.attributetree.ui.widgets.AttributeTreeEditorComposite;

public class AttributeTreeCreationPage extends WizardPage {

    private int NUM_COLUMNS = 2;
    private Composite mainComposite;

    private String currentTree = "";
    private Text currentTreeText;

    private AttributeTreeEditorComposite treeEditorComposite;

    protected AttributeTreeCreationPage(String pageName) {
        super(pageName);
        setTitle("Attribute Tree Creation");
        setDescription("Create or edit an attribute tree");
    }

    @Override
    public void createControl(Composite parent) {
//        Composite composite = new Composite(parent, SWT.NONE);
//        GridLayout layout = new GridLayout(1, true);
//        composite.setLayout(layout);
//        Composite composite = new Composite(parent, SWT.NONE);
//        GridLayout layout = new GridLayout(2, false);
//        composite.setLayout(layout);
//
//        Button testButton = new Button(composite, SWT.NONE);
//        testButton.setText("Test");
//
//        Label label = new Label(composite, SWT.NONE);
//        label.setText("Test Label");

//        Composite composite = new Composite(parent, SWT.BORDER);
//        composite.setLayout(new GridLayout(1, true));
//        AttributeTreeEditorComposite composite = new AttributeTreeEditorComposite(parent, SWT.NONE, null);
        //composite.setLayout(new GridLayout(1, true));

        mainComposite = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(NUM_COLUMNS).equalWidth(false).applyTo(mainComposite);

        Label description = new Label(mainComposite, SWT.WRAP);
        description.setText("Create or edit your attribute tree that will be used with your diagram");
        GridDataFactory.fillDefaults().span(NUM_COLUMNS, 1).grab(true, false).applyTo(description);

        Label currentTreeLabel = new Label(mainComposite, SWT.WRAP);
        currentTreeLabel.setText("Attribute Tree");

        currentTreeText = new Text(mainComposite, SWT.SINGLE);
        currentTreeText.setText(currentTree);
        currentTreeText.setEnabled(false);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(currentTreeText);

//        File file = new File("/home/esideli/Eclipse-workspace/runtime-Tracecompass/test_statemachine/Statemachine/Tree/test_tree.attributetree");
//        AttributeTreeEditorComposite treeComposite = new AttributeTreeEditorComposite(parent, SWT.NONE, file);
//        TestComposite test = new TestComposite(composite, SWT.BORDER);
//        Composite composite = new Composite(parent, SWT.BORDER);
//        composite.setLayout(new GridLayout());
//        Button test = new Button(composite, SWT.PUSH);
//        test.setText("test");

        setControl(mainComposite);
        setPageComplete(false);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if(visible) {
            StatemachineDiagramPage statemachinePage = (StatemachineDiagramPage) getWizard().getPage(StatemachineDiagramWizard.PAGE_NAME_DIAGRAM);
            File currentTreeFile;
            if(statemachinePage.getUseExistingFile()) {
                currentTreeFile = new File(statemachinePage.getExistingTreePath());
                currentTree = statemachinePage.getExistingTreePath();
            } else {
                currentTreeFile = createAttributeTreeFile(statemachinePage.getDiagramName());
                currentTree = statemachinePage.getNewTreeName();
            }
            currentTreeText.setText(currentTree);

            if (treeEditorComposite == null) {
                treeEditorComposite = new AttributeTreeEditorComposite(mainComposite, SWT.NONE, currentTreeFile);
                GridDataFactory.fillDefaults().grab(true, true).span(NUM_COLUMNS, 1).applyTo(treeEditorComposite);
            } else {
                //TODO Set the new tree viewer input here
            }
            setPageComplete(validatePage());
        }
    }

    private static boolean validatePage() {
        return true;
    }

    private File createAttributeTreeFile(String diagramName) {
        if(((StatemachineDiagramWizard) getWizard()).createAttributeTreeFile(diagramName)) {
            return ((StatemachineDiagramWizard) getWizard()).getAttributeTreeFile();
        }
        return null;
    }
}
