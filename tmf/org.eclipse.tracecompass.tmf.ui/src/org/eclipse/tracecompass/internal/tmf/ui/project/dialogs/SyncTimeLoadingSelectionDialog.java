/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Marc-Andre Laperle - Initial API and implementation.
 *******************************************************************************/
package org.eclipse.tracecompass.internal.tmf.ui.project.dialogs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.ui.dialogs.SelectionStatusDialog;

/**
 * A dialog for selecting the different parameters synching files in time
 * loading mode.
 */
public class SyncTimeLoadingSelectionDialog extends SelectionStatusDialog {

    private static final String DEFAULT_LOAD_FREQUENCY = "2000"; //$NON-NLS-1$
    private static final Status STATUS_OK = new Status(IStatus.OK, Activator.PLUGIN_ID, null);
    private static final Status STATUS_INVALID_LOAD_FREQUENCY = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.SyncTimeLoadingSelectionDialog_ErrorInvalidLoadFrequency);
    private Text fTimerText;
    private Button fRefreshAllButton;

    private static final String DIALOG_SETTINGS_ID = "org.eclipse.tracecompass.internal.tmf.ui.project.dialogs.SyncTimeLoadingSelectionDialog"; //$NON-NLS-1$
    private static final String DIALOG_SETTINGS_LOAD_FREQUENCY = "loadFrequency"; //$NON-NLS-1$
    private static final String DIALOG_SETTINGS_LOADING_MODE = "loadingMode"; //$NON-NLS-1$
    private Button fLoadIncreasingButton;

    /**
     * Constructor.
     *
     * @param parent
     *            the parent shell
     */
    public SyncTimeLoadingSelectionDialog(Shell parent) {
        super(parent);

        setStatusLineAboveButtons(true);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite dialogAreaComposite = new Composite(parent, SWT.NONE);
        dialogAreaComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
        dialogAreaComposite.setLayout(new GridLayout(2, false));

        Group modeGroup = new Group(dialogAreaComposite, SWT.NONE);
        modeGroup.setText(Messages.SyncTimeLoadingSelectionDialog_LoadingMode);
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        gridData.horizontalSpan = 2;
        modeGroup.setLayoutData(gridData);
        modeGroup.setLayout(new RowLayout(SWT.VERTICAL));

        fRefreshAllButton = new Button(modeGroup, SWT.RADIO);
        fRefreshAllButton.setSelection(true);
        fRefreshAllButton.setText(Messages.SyncTimeLoadingSelectionDialog_RefreshAll);
        fLoadIncreasingButton = new Button(modeGroup, SWT.RADIO);
        fLoadIncreasingButton.setText(Messages.SyncTimeLoadingSelectionDialog_LoadIncreasing);

        Label timerLabel = new Label(dialogAreaComposite, SWT.NONE);
        timerLabel.setText(Messages.SyncTimeLoadingSelectionDialog_LoadFrequency);
        fTimerText = new Text(dialogAreaComposite, SWT.BORDER);
        fTimerText.setText(DEFAULT_LOAD_FREQUENCY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        fTimerText.setLayoutData(gridData);
        fTimerText.addVerifyListener(new VerifyListener() {
            @Override
            public void verifyText(VerifyEvent e) {
                // only numbers and default are allowed.
                e.doit = e.text.matches("[0-9]*"); //$NON-NLS-1$
            }
        });
        fTimerText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent event) {
                validateDialog();
            }
        });

        restoreDialogSettings();

        return super.createDialogArea(parent);
    }

    private void validateDialog() {
        try {
            int parsedInt = Integer.parseInt(fTimerText.getText());
            if (parsedInt <= 0) {
                updateStatus(STATUS_INVALID_LOAD_FREQUENCY);
            } else {
                updateStatus(STATUS_OK);
            }
        } catch (NumberFormatException e) {
            updateStatus(STATUS_INVALID_LOAD_FREQUENCY);
        }
    }

    private void restoreDialogSettings() {
        IDialogSettings settings = Activator.getDefault().getDialogSettings();
        IDialogSettings section = settings.getSection(DIALOG_SETTINGS_ID);
        if (section == null) {
            section = settings.addNewSection(DIALOG_SETTINGS_ID);
        } else {
            try {
                int loadFrequency = section.getInt(DIALOG_SETTINGS_LOAD_FREQUENCY);
                fTimerText.setText(Integer.toString(loadFrequency));
            } catch (NumberFormatException e) {
                // ignore
            }

            boolean loadingMode = section.getBoolean(DIALOG_SETTINGS_LOADING_MODE);
            fRefreshAllButton.setSelection(loadingMode);
            fLoadIncreasingButton.setSelection(!loadingMode);
        }
    }

    @Override
    protected void computeResult() {
    }

    @Override
    protected void okPressed() {
        saveDialogSettings();
        super.okPressed();
    }

    private void saveDialogSettings() {
        IDialogSettings settings = Activator.getDefault().getDialogSettings();
        IDialogSettings section = settings.getSection(DIALOG_SETTINGS_ID);
        section.put(DIALOG_SETTINGS_LOAD_FREQUENCY, Integer.parseInt(fTimerText.getText()));
        section.put(DIALOG_SETTINGS_LOADING_MODE, fRefreshAllButton.getSelection());
    }

    @Override
    public Object[] getResult() {
        return new Object[] { fTimerText.getText(), fRefreshAllButton.getSelection() };
    }

}
