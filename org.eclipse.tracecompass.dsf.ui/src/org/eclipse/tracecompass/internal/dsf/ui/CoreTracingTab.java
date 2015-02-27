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

import org.eclipse.cdt.dsf.gdb.launching.LaunchMessages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

@SuppressWarnings({"javadoc"})
public class CoreTracingTab extends AbstractLaunchConfigurationTab {
    public static final String ATTR_TRACING_CORE_TRACES = "tracing.core.traces"; //$NON-NLS-1$

    private Text fTracesText;

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
        label.setText("Traces:"); //$NON-NLS-1$
        fTracesText = new Text(comp, SWT.BORDER);
        fTracesText.setText("(none)"); //$NON-NLS-1$
        gd = GridDataFactory.defaultsFor(fTracesText).grab(true, false).create();
        fTracesText.setLayoutData(gd);
        fTracesText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });


        Button select = new Button(comp, SWT.PUSH);
        select.setText("Browse..."); //$NON-NLS-1$
        select.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog fileDialog = new FileDialog(getShell(), SWT.NONE);
                String result = fileDialog.open();
                if (result != null) {
                    fTracesText.setText(fileDialog.open());
                }
                super.widgetSelected(e);
            }
        });
    }

    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    }

    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        try {
            fTracesText.setText(configuration.getAttribute(ATTR_TRACING_CORE_TRACES, "")); //$NON-NLS-1$
        }
        catch (CoreException e) {
            setErrorMessage(LaunchMessages.getFormattedString("Launch.common.Exception_occurred_reading_configuration_EXCEPTION", e.getStatus().getMessage())); //$NON-NLS-1$
            DsfTraceUIPlugin.logError(e);
        }
    }

    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(ATTR_TRACING_CORE_TRACES, fTracesText.getText());
    }

    @Override
    public Image getImage() {
        return DsfTraceUIPlugin.getDefault().getImageFromImageRegistry("icons/obj16/tracing_tab.gif"); //$NON-NLS-1$
    }

    @Override
    public String getName() {
        return "Tracing"; //$NON-NLS-1$
    }

    @Override
    public boolean isValid(ILaunchConfiguration launchConfig) {
        setErrorMessage(null);
        setMessage(null);

        if (fTracesText != null) {
            String tracesStr = fTracesText.getText().trim();
            // We accept an empty string.
            if (!tracesStr.isEmpty()) {
                IPath tracesPath = new Path(tracesStr);
                if (!tracesPath.toFile().exists()) {
                    setErrorMessage("Trace files location does not exist");
                    return false;
                }
            }
        }

        return super.isValid(launchConfig);
    }
}
