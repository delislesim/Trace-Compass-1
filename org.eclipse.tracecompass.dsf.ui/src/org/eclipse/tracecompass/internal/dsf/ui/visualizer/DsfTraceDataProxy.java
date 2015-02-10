/*******************************************************************************
 * Copyright (c) 2012, 2015 Tilera Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     William R. Swanson (Tilera Corporation) - initial API and implementation
 *     Marc Dumais (Ericsson) - Add CPU/core load information to the multicore visualizer (Bug 396268)
 *     Xavier Raynaud (Kalray) - Bug 431935
 *     Alvaro Sanchez-Leon (Ericsson) - Bug 459114 - override construction of the data model
 *******************************************************************************/

package org.eclipse.tracecompass.internal.dsf.ui.visualizer;

import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.utils.DSFDebugModel;


/** Debugger state information accessors.
 *
 *  NOTE: The methods on this class perform asynchronous operations,
 *  and call back to a method on a provided DSFDebugModelListener instance
 *  when the operation is completed.
 *
 */
public class DsfTraceDataProxy extends DSFDebugModel {

}
