package org.eclipse.tracecompass.internal.dsf.ui;

import java.net.URI;
import java.sql.Date;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.cdt.dsf.debug.service.IRunControl.ISuspendedDMEvent;
import org.eclipse.cdt.dsf.debug.service.IRunControl.StateChangeReason;
import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService.ICommandControlInitializedDMEvent;
import org.eclipse.cdt.dsf.gdb.service.IGDBBackend;
import org.eclipse.cdt.dsf.gdb.service.SessionType;
import org.eclipse.cdt.dsf.service.DsfServiceEventHandler;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.cdt.dsf.service.DsfSession.SessionStartedListener;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.remote.core.IRemoteFileService;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.internal.lttng2.control.ui.Activator;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.dialogs.ImportDialog;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.dialogs.ImportFileInfo;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.messages.Messages;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.model.ITraceControlComponent;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.model.impl.TargetNodeComponent;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.model.impl.TraceSessionComponent;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.remote.IRemoteSystemProxy;
import org.eclipse.tracecompass.tmf.core.project.model.TmfTraceImportException;
import org.eclipse.tracecompass.tmf.core.project.model.TraceTypeHelper;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfOpenTraceHelper;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfProjectElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfProjectRegistry;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceTypeUIUtils;
import org.eclipse.tracecompass.tmf.ui.project.model.TraceUtils;

@SuppressWarnings({ "restriction", "javadoc" })
public class DsfUITraceSessionManager {

    private SessionStartedListener fSessionStartedListener;

    public DsfUITraceSessionManager() {
        super();
        fSessionStartedListener = new SessionStartedListener() {
            @Override
            public void sessionStarted(DsfSession session) {
                session.addServiceEventListener(DsfUITraceSessionManager.this, null);
            }
        };

        DsfSession.addSessionStartedListener(fSessionStartedListener);
    }

    @DsfServiceEventHandler
    public void handleSuspendedEvent(ISuspendedDMEvent event) {

        if (event.getReason() != StateChangeReason.STEP) {
            System.out.println(new Date(System.currentTimeMillis()).toString() + "suspended (session: " + event.getDMContext().getSessionId()); //$NON-NLS-1$
            DsfSession session = DsfSession.getSession(event.getDMContext().getSessionId());
            ILaunch launch = (ILaunch) session.getModelAdapter(ILaunch.class);
            try {
                ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();
                String nodeName = launchConfiguration.getAttribute(TracingTab.ATTR_TRACING_TARGET_NODE, ""); //$NON-NLS-1$
                String sessionName = launchConfiguration.getAttribute(TracingTab.ATTR_TRACING_SESSION_NAME, ""); //$NON-NLS-1$
                if (!nodeName.isEmpty() && !sessionName.isEmpty()) {
                    takeSnapShot(nodeName, sessionName, launchConfiguration.getName());
                }
            } catch (CoreException e) {
                DsfTraceUIPlugin.logError(e);
            }
        }
    }

    private static void takeSnapShot(final String nodeName, final String sessionName, String debugSessionMame) {
        final TraceSessionComponent sessionRet[] = new TraceSessionComponent[1];
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                sessionRet[0] = getSession(nodeName, sessionName);

            }
        });

        final TraceSessionComponent session = sessionRet[0];
        if (session != null) {
            if (!session.isSnapshotSupported()) {
                DsfTraceUIPlugin.logError("Session " + session.getName() + " does not support snapshot mode");
                return;
            }

            try {

                session.recordSnapshot(new NullProgressMonitor());
            } catch (ExecutionException e) {
                DsfTraceUIPlugin.logError(e);
            }

            Job job = new Job(Messages.TraceControl_RecordSnapshotJob) {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        session.recordSnapshot(monitor);
                        if (monitor.isCanceled()) {
                            return Status.CANCEL_STATUS;
                        }
                    } catch (ExecutionException e) {
                        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.TraceControl_RecordSnapshotFailure, e);
                    }
                    return Status.OK_STATUS;
                }
            };
            job.setUser(true);
            job.schedule();
            try {
                job.join();

                IRemoteSystemProxy proxy = session.getTargetNode().getRemoteSystemProxy();

                IRemoteFileService fsss = proxy.getRemoteFileService();

                final String path = session.getSnapshotInfo().getSnapshotPath();
                final IFileStore remoteFolder = fsss.getResource(path);
                IFileStore[] childStores = remoteFolder.childStores(EFS.NONE, new NullProgressMonitor());
                List<IFileStore> asList = Arrays.asList(childStores);
                Collections.sort(asList, new Comparator<IFileStore>() {

                    @Override
                    public int compare(IFileStore o1, IFileStore o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
                IFileStore latestTraceParent = asList.get(asList.size() - 1);
                //TODO: check for multiple traces, make an experiment?
                IFileStore latestTrace = latestTraceParent.childStores(EFS.NONE, new NullProgressMonitor())[0];
                TmfTraceFolder tracesFolder = getTracesFolder(debugSessionMame);

                ImportFileInfo importFileInfo = ImportDialog.getImportFileInfo(session, latestTraceParent, tracesFolder.getResource(), latestTrace, true);
                openTrace(importFileInfo, tracesFolder.getProject().getResource());

            } catch (InterruptedException e) {
                // Cancelled
            } catch (CoreException e) {
                DsfTraceUIPlugin.logError(e);
            } catch (TmfTraceImportException e) {
                DsfTraceUIPlugin.logError(e);
            }
        }

    }

    private static void openTrace(ImportFileInfo importFileInfo, IProject project) throws CoreException, TmfTraceImportException {
        TraceUtils.createFolder(importFileInfo.getDestinationFolder(), new NullProgressMonitor());
        IFolder traceFolder = importFileInfo.getDestinationFolder().getFolder(importFileInfo.getLocalTraceName());
        URI uri = importFileInfo.getImportFile().toURI();
        IPath location = URIUtil.toPath(uri);
        IStatus result = ResourcesPlugin.getWorkspace().validateLinkLocation(traceFolder, location);
        if (result.isOK()) {
            traceFolder.createLink(location, IResource.REPLACE, new NullProgressMonitor());
        } else {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, result.getMessage()));
        }

        TraceTypeHelper selectedTraceType = TmfTraceTypeUIUtils.selectTraceType(location.toOSString(), null, null);
        // No trace type was determined.
        TmfTraceTypeUIUtils.setTraceType(traceFolder, selectedTraceType);

        final TmfProjectElement projectElement = TmfProjectRegistry.getProject(project, true);
        final TmfTraceFolder tracesFolder = projectElement.getTracesFolder();
        final List<TmfTraceElement> traces = tracesFolder.getTraces();
        TmfTraceElement found = null;
        for (TmfTraceElement candidate : traces) {
            if (candidate.getName().equals(importFileInfo.getLocalTraceName())) {
                found = candidate;
            }
        }

        if (found == null) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not find trace element"));
        }

        final TmfTraceElement finalTrace = found;
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                TmfOpenTraceHelper.openTraceFromElement(finalTrace);
            }
        });
    }

    private static TmfTraceFolder getTracesFolder(final String debugSessionMame) {
        final String projectName = debugSessionMame + "_snapshots";
        if (!ResourcesPlugin.getWorkspace().getRoot().getProject(debugSessionMame).exists()) {
            Display.getDefault().syncExec(new Runnable() {

                @Override
                public void run() {
                    TmfProjectRegistry.createProject(projectName, null, null);
                }
            });
        }

        // Streamed trace
        TmfProjectElement projectElement = TmfProjectRegistry.getProject(ResourcesPlugin.getWorkspace().getRoot().getProject(projectName), true);
        projectElement.refresh();
        TmfTraceFolder traceFolder = projectElement.getTracesFolder();
        return traceFolder;

    }

    private static TraceSessionComponent getSession(String nodeName, String sessionName) {
        ITraceControlComponent rootComponent = TracingTab.getRootComponent();
        ITraceControlComponent connectionNode = rootComponent.getChild(nodeName);
        if (connectionNode instanceof TargetNodeComponent) {
            TargetNodeComponent targetNodeComponent = (TargetNodeComponent) connectionNode;
            TraceSessionComponent[] sessions = targetNodeComponent.getSessions();
            for (TraceSessionComponent session : sessions) {
                if (session.getName().equals(sessionName)) {
                    return session;
                }
            }

        }
        return null;
    }

    public void dispose() {
        DsfSession.removeSessionStartedListener(fSessionStartedListener);
    }

    @DsfServiceEventHandler
    public void handleEvent(ICommandControlInitializedDMEvent event) {
        String sessionId = event.getDMContext().getSessionId();
        DsfServicesTracker tracker = new DsfServicesTracker(DsfTraceUIPlugin.getBundleContext(), sessionId);
        IGDBBackend backendService = tracker.getService(IGDBBackend.class);
        if (backendService != null && backendService.getSessionType() == SessionType.CORE){
            DsfSession session = DsfSession.getSession(sessionId);
            ILaunch launch = (ILaunch) session.getModelAdapter(ILaunch.class);
            try {
                ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();
                String tracesPath = launchConfiguration.getAttribute(CoreTracingTab.ATTR_TRACING_CORE_TRACES, ""); //$NON-NLS-1$
                if (tracesPath != null && !tracesPath.isEmpty()) {
//                    Display.getDefault().syncExec(new Runnable() {
//
//                        @Override
//                        public void run() {
//                            try {
//                                //TODO open the specified trace here
//                            } catch (CoreException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    });
                }
            } catch (CoreException e) {
                DsfTraceUIPlugin.logError(e);
            }

        }

        tracker.dispose();
    }
}
