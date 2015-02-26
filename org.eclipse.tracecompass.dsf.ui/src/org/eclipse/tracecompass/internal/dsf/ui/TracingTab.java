/*******************************************************************************
 * Copyright (c) 2015 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marc-Andre Laperle - initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.dsf.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.dsf.gdb.launching.LaunchMessages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.ControlView;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.model.ITraceControlComponent;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.model.impl.TargetNodeComponent;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.model.impl.TraceSessionComponent;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.model.impl.TraceSessionGroup;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.dialogs.PatternFilter;

@SuppressWarnings({"restriction", "javadoc"})
public class TracingTab extends AbstractLaunchConfigurationTab {
    private Text fLttngSession;

    public static final String ATTR_TRACING_TARGET_NODE = "tracing.node"; //$NON-NLS-1$
    public static final String ATTR_TRACING_SESSION_NAME = "tracing.session"; //$NON-NLS-1$

    private TraceSessionComponent fSession;

    @Override
    public void createControl(Composite parent) {
        Font font = parent.getFont();
        Composite comp = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(3, false);
        comp.setLayout(layout);
        comp.setFont(font);

        GridData gd = new GridData(GridData.FILL_BOTH);
        comp.setLayoutData(gd);
        setControl(comp);

        Label label = new Label(comp, SWT.NONE);
        label.setText("LTTng session:"); //$NON-NLS-1$
        fLttngSession = new Text(comp, SWT.BORDER | SWT.READ_ONLY);
        fLttngSession.setText("(none)"); //$NON-NLS-1$
        gd = GridDataFactory.defaultsFor(fLttngSession).grab(true, false).create();
        fLttngSession.setLayoutData(gd);

        Button select = new Button(comp, SWT.PUSH);
        select.setText("Select..."); //$NON-NLS-1$
        select.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openSessionSelectionDialog();
                super.widgetSelected(e);
            }
        });
    }

    private void openSessionSelectionDialog() {
        final ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getControl().getShell(), new LabelProvider(), new ContentProvider()) {
            private TreeViewer parent(Composite parent, int style) {
                return super.doCreateTreeViewer(parent, style);
            }

            @Override
            protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
                final TreeViewer[] treeViewerRet = new TreeViewer[1];
                new FilteredTree(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER, new PatternFilter(), true) {
                    @Override
                    protected TreeViewer doCreateTreeViewer(Composite aparent, int astyle) {
                        treeViewerRet[0] = parent(aparent, astyle);
                        return treeViewerRet[0];
                    }
                };
                treeViewerRet[0].expandAll();
                return treeViewerRet[0];
            }

            @Override
            protected TreeViewer createTreeViewer(Composite parent) {
                TreeViewer treeViewer = super.createTreeViewer(parent);
                treeViewer.expandAll();
                return treeViewer;
            }
        };

        dialog.setValidator(new ISelectionStatusValidator() {
            @Override
            public IStatus validate(Object[] selection) {
                if (selection.length == 1 && selection[0] instanceof TraceSessionComponent) {
                    return new Status(IStatus.OK, DsfTraceUIPlugin.PLUGIN_ID, null);

                }
                return new Status(IStatus.ERROR, DsfTraceUIPlugin.PLUGIN_ID, null);
            }
        });
        dialog.setTitle("Session selection"); //$NON-NLS-1$
        dialog.setMessage("Select session"); //$NON-NLS-1$

        ITraceControlComponent traceControlRoot = getRootComponent();
        dialog.setInput(traceControlRoot);
        if (dialog.open() == Window.OK) {
            Object[] result = dialog.getResult();
            if (result != null && result.length == 1) {
                fSession = (TraceSessionComponent) result[0];
                fLttngSession.setText(fSession.getTargetNode().getName() + " / " + fSession.getName()); //$NON-NLS-1$
                updateLaunchConfigurationDialog();
            }
        }

    }

    public static ITraceControlComponent getRootComponent() {
        IViewPart view;
        try {
            IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            view = activePage.showView(ControlView.ID);
        } catch (PartInitException e1) {
            throw new RuntimeException(e1);
        }

        ControlView controlView = (ControlView) view;
        ITraceControlComponent traceControlRoot = controlView.getTraceControlRoot();
        return traceControlRoot;
    }

    private final class ContentProvider implements ITreeContentProvider {
        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // Not needed
        }

        @Override
        public void dispose() {
        }

        @Override
        public boolean hasChildren(Object element) {
            return getChildren(element).length > 0;
        }

        private boolean isValidElement(Object element) {
            return element instanceof TargetNodeComponent || element instanceof TraceSessionGroup || element instanceof TraceSessionComponent;
        }

        @Override
        public Object getParent(Object element) {
            return ((ITraceControlComponent) element).getParent();
        }

        @Override
        public Object[] getElements(Object inputElement) {
            return ((ITraceControlComponent) inputElement).getChildren();
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            List<ITraceControlComponent> validChildren = new ArrayList<>();

            ITraceControlComponent[] children = ((ITraceControlComponent) parentElement).getChildren();
            for (ITraceControlComponent child : children) {
                if (isValidElement(child)) {
                    validChildren.add(child);
                }
            }

            return validChildren.toArray(new ITraceControlComponent[validChildren.size()]);
        }
    }

    static private class LabelProvider implements ILabelProvider {

        @Override
        public void addListener(ILabelProviderListener listener) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public boolean isLabelProperty(Object element, String property) {
            return false;
        }

        @Override
        public void removeListener(ILabelProviderListener listener) {
        }

        @Override
        public Image getImage(Object element) {
            return ((ITraceControlComponent) element).getImage();
        }

        @Override
        public String getText(Object element) {
            return ((ITraceControlComponent) element).getName();
        }

    }

    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        configuration.removeAttribute(ATTR_TRACING_TARGET_NODE);
        configuration.removeAttribute(ATTR_TRACING_SESSION_NAME);
    }

    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        try {
            fLttngSession.setText(configuration.getAttribute(ATTR_TRACING_TARGET_NODE, "") + " / " + configuration.getAttribute(ATTR_TRACING_SESSION_NAME, "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        catch (CoreException e) {
            setErrorMessage(LaunchMessages.getFormattedString("Launch.common.Exception_occurred_reading_configuration_EXCEPTION", e.getStatus().getMessage())); //$NON-NLS-1$
            DsfTraceUIPlugin.logError(e);
        }
    }

    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        if (fSession != null) {
            configuration.setAttribute(ATTR_TRACING_TARGET_NODE, fSession.getTargetNode().getName());
            configuration.setAttribute(ATTR_TRACING_SESSION_NAME, fSession.getName());
        }
    }

    @Override
    public Image getImage() {
        return DsfTraceUIPlugin.getDefault().getImageFromImageRegistry("icons/obj16/tracing_tab.gif"); //$NON-NLS-1$
    }

    @Override
    public String getName() {
        return "Tracing"; //$NON-NLS-1$
    }
}
