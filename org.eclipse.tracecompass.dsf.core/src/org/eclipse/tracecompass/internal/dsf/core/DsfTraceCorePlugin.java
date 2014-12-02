/*******************************************************************************
 * Copyright (c) 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Marc Khouzam (Ericsson) - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.dsf.core;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/** */
public class DsfTraceCorePlugin extends Plugin {

    /** The plug-in ID */
    public static final String PLUGIN_ID = "org.eclipse.tracecompass.dsf.core"; //$NON-NLS-1$

    private static DsfTraceCorePlugin plugin;

    private static BundleContext fBundleContext;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        fBundleContext = context;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * @return The instance of the plugin.
     */
    public static DsfTraceCorePlugin getDefault() {
        return plugin;
    }

    /**
     * @return The Bundle Context for the plugin.
     */
    public static BundleContext getBundleContext() {
        return fBundleContext;
    }
}
