/*******************************************************************************
 * Copyright (c) 2009, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *   Bernd Hufmann - Added possibility to pin view
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.views;

import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.ITmfUIPreferences;
import org.eclipse.tracecompass.internal.tmf.ui.views.AlignViewsAction;
import org.eclipse.tracecompass.internal.tmf.ui.views.TmfAlignmentSynchronizer;
import org.eclipse.tracecompass.tmf.core.component.ITmfComponent;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.ViewPart;

/**
 * Basic abstract TMF view class implementation.
 *
 * It registers any sub class to the signal manager for receiving and sending
 * TMF signals.
 *
 * @author Francois Chouinard
 */
public abstract class TmfView extends ViewPart implements ITmfComponent {

    private final String fName;
    /** This allows us to keep track of the view sizes */
    private Composite fParentComposite;
    private static final TmfAlignmentSynchronizer fTimeAlignmentSynchronizer = new TmfAlignmentSynchronizer();

    /**
     * Action class for pinning of TmfView.
     */
    protected PinTmfViewAction fPinAction;
    private static AlignViewsAction fAlignViewsAction;
    private IPreferenceChangeListener fAlignViewPrefListener;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Constructor. Creates a TMF view and registers to the signal manager.
     *
     * @param viewName
     *            A view name
     */
    public TmfView(String viewName) {
        super();
        fName = viewName;
        TmfSignalManager.register(this);
    }

    /**
     * Disposes this view and de-registers itself from the signal manager
     */
    @Override
    public void dispose() {
        TmfSignalManager.deregister(this);

        if (fAlignViewPrefListener != null) {
            InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).removePreferenceChangeListener(fAlignViewPrefListener);
            fAlignViewPrefListener = null;
        }

        super.dispose();
    }

    // ------------------------------------------------------------------------
    // ITmfComponent
    // ------------------------------------------------------------------------

    @Override
    public String getName() {
        return fName;
    }

    @Override
    public void broadcast(TmfSignal signal) {
        TmfSignalManager.dispatchSignal(signal);
    }

    @Override
    public void broadcastAsync(TmfSignal signal) {
        TmfSignalManager.dispatchSignalAsync(signal);
    }

    // ------------------------------------------------------------------------
    // View pinning support
    // ------------------------------------------------------------------------

    /**
     * Returns whether the pin flag is set.
     * For example, this flag can be used to ignore time synchronization signals from other TmfViews.
     *
     * @return pin flag
     */
    public boolean isPinned() {
        return ((fPinAction != null) && (fPinAction.isChecked()));
    }

    /**
     * Method adds a pin action to the TmfView. The pin action allows to toggle the <code>fIsPinned</code> flag.
     * For example, this flag can be used to ignore time synchronization signals from other TmfViews.
     */
    protected void contributePinActionToToolBar() {
        if (fPinAction == null) {
            fPinAction = new PinTmfViewAction();

            IToolBarManager toolBarManager = getViewSite().getActionBars()
                    .getToolBarManager();
            toolBarManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
            toolBarManager.add(fPinAction);
        }
    }

    @Override
    public void createPartControl(Composite parent) {
        fParentComposite = parent;
        if (this instanceof ITmfTimeAligned) {
            if (fAlignViewsAction == null) {
                fAlignViewsAction = new AlignViewsAction();
            }

            fAlignViewPrefListener = new IPreferenceChangeListener() {

                @Override
                public void preferenceChange(PreferenceChangeEvent event) {
                    if (event.getKey().equals(ITmfUIPreferences.PREF_ALIGN_VIEWS)) {
                        Object oldValue = event.getOldValue();
                        Object newValue = event.getNewValue();
                        if (Boolean.toString(false).equals(oldValue) && Boolean.toString(true).equals(newValue)) {
                            fTimeAlignmentSynchronizer.realignViews(TmfView.this);
                        } else if (Boolean.toString(true).equals(oldValue) && Boolean.toString(false).equals(newValue)) {
                            fTimeAlignmentSynchronizer.restoreViews();
                        }
                    }
                }
            };
            InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).addPreferenceChangeListener(fAlignViewPrefListener);

            IToolBarManager toolBarManager = getViewSite().getActionBars()
                    .getToolBarManager();
            toolBarManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
            toolBarManager.add(fAlignViewsAction);

            parent.addControlListener(new ControlListener() {
                @Override
                public void controlResized(ControlEvent e) {
                    fTimeAlignmentSynchronizer.realignViews(TmfView.this);
                }

                @Override
                public void controlMoved(ControlEvent e) {
                }
            });
            fTimeAlignmentSynchronizer.realignViews(TmfView.this);
        }
    }


    /**
     * @since 1.0
     */
    public Composite getParentComposite() {
        return fParentComposite;
    }
}
