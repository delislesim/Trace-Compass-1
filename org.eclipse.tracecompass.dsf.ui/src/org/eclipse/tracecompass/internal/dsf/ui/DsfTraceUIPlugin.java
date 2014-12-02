/*******************************************************************************
 * Copyright (c) 2006, 2010 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tracecompass.internal.dsf.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class DsfTraceUIPlugin extends AbstractUIPlugin {

	/** The plug-in ID */
	public static final String PLUGIN_ID = "org.eclipse.tracecompass.dsf.ui"; //$NON-NLS-1$

	private static DsfTraceUIPlugin plugin;

    private static BundleContext fgBundleContext;


	/**
	 * The constructor
	 */
	public DsfTraceUIPlugin() {
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
    public void start(BundleContext context) throws Exception {
        fgBundleContext = context;
		super.start(context);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
    public void stop(BundleContext context) throws Exception {
		plugin = null;
        fgBundleContext = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static DsfTraceUIPlugin getDefault() {
		return plugin;
	}

    /**
     * @return The Bundle Context for the plugin.
     */
    public static BundleContext getBundleContext() {
        return fgBundleContext;
    }

    /**
     * Logs the specified status with this plug-in's log.
     *
     * @param status status to log
     */
    public static void log(IStatus status) {
        getDefault().getLog().log(status);
    }

}
