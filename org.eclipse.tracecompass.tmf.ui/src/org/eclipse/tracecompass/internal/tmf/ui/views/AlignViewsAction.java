/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Marc-Andre Laperle - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ui.views;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.ITmfImageConstants;
import org.eclipse.tracecompass.internal.tmf.ui.ITmfUIPreferences;
import org.eclipse.tracecompass.internal.tmf.ui.Messages;

public class AlignViewsAction extends Action {
    /**
     * Creates a AlignViewsAction
     */
    public AlignViewsAction() {
        super(Messages.TmfView_AlignViewsActionNameText, IAction.AS_CHECK_BOX);

        setId("org.eclipse.tracecompass.tmf.ui.views.AlignViewsAction"); //$NON-NLS-1$
        setToolTipText(Messages.TmfView_AlignViewsActionToolTipText);
        setImageDescriptor(Activator.getDefault().getImageDescripterFromPath(ITmfImageConstants.IMG_UI_LINK));
        setChecked(isPreferenceEnabled());
    }

    @Override
    public void run() {
        boolean newValue = !isPreferenceEnabled();
        InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).putBoolean(ITmfUIPreferences.PREF_ALIGN_VIEWS, newValue);
        setChecked(newValue);
    }

    private static boolean isPreferenceEnabled() {
        return InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).getBoolean(ITmfUIPreferences.PREF_ALIGN_VIEWS, true);
    }
}
