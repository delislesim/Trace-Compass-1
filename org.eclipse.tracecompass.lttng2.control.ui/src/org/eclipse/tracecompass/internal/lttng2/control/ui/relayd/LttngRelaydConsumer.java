/**********************************************************************
 * Copyright (c) 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial implementation
 **********************************************************************/

package org.eclipse.tracecompass.internal.lttng2.control.ui.relayd;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.internal.lttng2.control.core.relayd.ILttngRelaydConnector;
import org.eclipse.tracecompass.internal.lttng2.control.core.relayd.LttngRelaydConnectorFactory;
import org.eclipse.tracecompass.internal.lttng2.control.core.relayd.lttngviewerCommands.AttachReturnCode;
import org.eclipse.tracecompass.internal.lttng2.control.core.relayd.lttngviewerCommands.AttachSessionResponse;
import org.eclipse.tracecompass.internal.lttng2.control.core.relayd.lttngviewerCommands.CreateSessionResponse;
import org.eclipse.tracecompass.internal.lttng2.control.core.relayd.lttngviewerCommands.CreateSessionReturnCode;
import org.eclipse.tracecompass.internal.lttng2.control.core.relayd.lttngviewerCommands.IndexResponse;
import org.eclipse.tracecompass.internal.lttng2.control.core.relayd.lttngviewerCommands.NextIndexReturnCode;
import org.eclipse.tracecompass.internal.lttng2.control.core.relayd.lttngviewerCommands.SessionResponse;
import org.eclipse.tracecompass.internal.lttng2.control.core.relayd.lttngviewerCommands.StreamResponse;
import org.eclipse.tracecompass.internal.lttng2.control.ui.Activator;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ctf.core.CtfTmfTimestamp;
import org.eclipse.tracecompass.tmf.ctf.core.CtfTmfTrace;


/**
 * Consumer of the relay d.
 *
 * @author Matthew Khouzam
 * @since 3.1
 */
public final class LttngRelaydConsumer {

    private static final Pattern PROTOCOL_HOST_PATTERN = Pattern.compile("(\\S+://)*(\\d+\\.\\d+\\.\\d+\\.\\d+)"); //$NON-NLS-1$
    private static final int SIGNAL_THROTTLE_NANOSEC = 10_000_000;
    private static final String ENCODING_UTF_8 = "UTF-8"; //$NON-NLS-1$

    private Map<Long, AttachSessionResponse> fAttachedSessions = new HashMap<>();
    private Map<Long, TraceSession> fTraceSessions = new HashMap<>();
    private Socket fConnection;
    private ILttngRelaydConnector fRelayd;
    private final LttngRelaydConnectionInfo fConnectionInfo;
    private LiveSessionJob fJob;

    public static class TraceSessionInfo {
        String fSessionName;
        long fSessionId;
        //String fPath;

        public String getSessionName() {
            return fSessionName;
        }
        public long getSessionId() {
            return fSessionId;
        }
    }

    public static class TraceSession {
        public TraceSession(CtfTmfTrace tmfTrace, CTFTrace trace, AttachSessionResponse session) {
            super();
            fTmfTrace = tmfTrace;
            fTrace = trace;
            fSession = session;
        }
        CtfTmfTrace fTmfTrace;
        CTFTrace fTrace;
        AttachSessionResponse fSession;

        long fTimestampEnd = 0;
        long fLastSignal = 0;
    }

    /**
     * Start a lttng consumer.
     *
     * @param address
     *            the ip address in string format
     * @param port
     *            the port, an integer
     * @param sessionName
     *            the session name
     * @param project
     *            the default project
     */
    LttngRelaydConsumer(final LttngRelaydConnectionInfo connectionInfo) {
        fConnectionInfo = connectionInfo;
    }

    /**
     * Connects to the relayd at the given address and port then attaches to the
     * given session name.
     *
     * @throws CoreException
     *             If something goes wrong during the connection
     *             <ul>
     *             <li>
     *             Connection could not be established (Socket could not be
     *             opened, etc)</li>
     *             <li>
     *             Connection timeout</li>
     *             <li>
     *             The session was not found</li>
     *             <li>
     *             Could not create viewer session</li>
     *             <li>
     *             Invalid trace (no metadata, no streams)</li>
     *             </ul>
     */
    public void connect() throws CoreException {
        if (fConnection != null) {
            return;
        }

        try {
            Matcher matcher = PROTOCOL_HOST_PATTERN.matcher(fConnectionInfo.getHost());
            if (matcher.matches()){
                String host = matcher.group(2);
                fConnection = new Socket(host, fConnectionInfo.getPort());
                fRelayd = LttngRelaydConnectorFactory.getNewConnector(fConnection);
            }
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.LttngRelaydConsumer_ErrorConnecting + (e.getMessage() != null ? e.getMessage() : ""))); //$NON-NLS-1$
        }
    }

    class LiveSessionJob extends Job {

        public LiveSessionJob(String name) {
            super(name);
        }

        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            try {
                while (!monitor.isCanceled()) {
                    for (Long key : fTraceSessions.keySet()) {
                        TraceSession traceSession = fTraceSessions.get(key);
                        CtfTmfTrace tmfTrace = traceSession.fTmfTrace;
//                        System.out.println("Updating session " + key + " " + tmfTrace.getResource().getName());
                        CTFTrace ctfTrace = tmfTrace.getCTFTrace();
                        if (ctfTrace == null) {
                            continue;
                        }
                        List<StreamResponse> attachedStreams = traceSession.fSession.getStreamList();
                        for (StreamResponse stream : attachedStreams) {
                            if (stream.getMetadataFlag() != 1) {
                                IndexResponse indexReply = fRelayd.getNextIndex(stream);
                                if (indexReply.getStatus() == NextIndexReturnCode.VIEWER_INDEX_OK) {
                                    long nanoTimeStamp = ctfTrace.timestampCyclesToNanos(indexReply.getTimestampEnd());
                                    if (nanoTimeStamp > traceSession.fTimestampEnd) {
                                        CtfTmfTimestamp endTime = new CtfTmfTimestamp(nanoTimeStamp);
                                        TmfTimeRange range = new TmfTimeRange(tmfTrace.getStartTime(), endTime);

                                        long currentTime = System.nanoTime();
                                        if (currentTime - traceSession.fLastSignal > SIGNAL_THROTTLE_NANOSEC) {
                                            TmfTraceRangeUpdatedSignal signal = new TmfTraceRangeUpdatedSignal(LttngRelaydConsumer.this, tmfTrace, range);
                                            tmfTrace.broadcastAsync(signal);
                                            traceSession.fLastSignal = currentTime;
                                        }
                                        traceSession.fTimestampEnd = nanoTimeStamp;
                                    }
                                } else if (indexReply.getStatus() == NextIndexReturnCode.VIEWER_INDEX_HUP) {
                                    // The trace is now complete because the trace session was destroyed
                                    tmfTrace.setComplete(true);
                                    TmfTraceRangeUpdatedSignal signal = new TmfTraceRangeUpdatedSignal(LttngRelaydConsumer.this, tmfTrace, new TmfTimeRange(tmfTrace.getStartTime(), new CtfTmfTimestamp(traceSession.fTimestampEnd)));
                                    tmfTrace.broadcastAsync(signal);
                                    return Status.OK_STATUS;
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Activator.getDefault().logError("Error during live trace reading", e); //$NON-NLS-1$
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.LttngRelaydConsumer_ErrorLiveReading + (e.getMessage() != null ? e.getMessage() : "")); //$NON-NLS-1$
            }

            return Status.OK_STATUS;
        }
    }

    public void attach(long sessionId) throws CoreException {
        if (fAttachedSessions.get(sessionId) != null) {
            return;
        }

        try {
            AttachSessionResponse attachedSession = fRelayd.attachToSession(sessionId);
            if (attachedSession.getStatus() != AttachReturnCode.VIEWER_ATTACH_OK) {
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.LttngRelaydConsumer_AttachSessionError + attachedSession.getStatus().toString()));
            }

            String metadata = fRelayd.getMetadata(attachedSession);
            if (metadata == null) {
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.LttngRelaydConsumer_NoMetadata));
            }

            fAttachedSessions.put(sessionId, attachedSession);
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error attaching to session" + (e.getMessage() != null ? e.getMessage() : "")));
        }
    }

    public String getTracePath(long sessionId) throws CoreException {
        final AttachSessionResponse attachedSession = fAttachedSessions.get(sessionId);
        if (attachedSession == null) {
            return "";
        }

        List<StreamResponse> attachedStreams = attachedSession.getStreamList();
        if (attachedStreams.isEmpty()) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.LttngRelaydConsumer_NoStreams));
        }

        String tracePath = "";
        try {
            tracePath = nullTerminatedByteArrayToString(attachedStreams.get(0).getPathName().getBytes());
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error getting session path" + (e.getMessage() != null ? e.getMessage() : "")));
        }
        return tracePath;
    }

    /**
     * Run the consumer operation for a give trace.
     *
     * @param trace
     *            the trace
     * @param sessionId
     */
    public void run(final CtfTmfTrace trace, long sessionId) {
        final AttachSessionResponse session = fAttachedSessions.get(sessionId);
        if (session == null || fTraceSessions.get(trace) != null) {
            return;
        }

        fJob = new LiveSessionJob("RelayD consumer"); //$NON-NLS-1$
        fTraceSessions.put(sessionId, new TraceSession(trace, trace.getCTFTrace(), session));
        fJob.setSystem(true);
        fJob.schedule();
    }

    /**
     * Dispose the consumer and it's resources (sockets, etc).
     */
    public void dispose(ITmfTrace trace) {
        fTraceSessions.remove(trace);
        if (fTraceSessions.isEmpty()) {
            dispose();
        }
    }

    /**
     * Dispose the consumer and it's resources (sockets, etc).
     */
    public void dispose() {
        try {
            fAttachedSessions.clear();
            fTraceSessions.clear();
            if (fJob != null) {
                fJob.cancel();
                fJob.join();
            }
            if (fConnection != null) {
                fConnection.close();
            }
            if (fRelayd != null) {
                fRelayd.close();
            }

        } catch (IOException e) {
            // Ignore
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    public Iterable<TraceSessionInfo> getTraceSessionInfos() throws CoreException {
        List<TraceSessionInfo> traceSessionsInfos = new ArrayList<>();
        try {
            List<SessionResponse> sessions = fRelayd.getSessions();
            for (SessionResponse session : sessions) {
                String asessionName = nullTerminatedByteArrayToString(session.getSessionName().getBytes());

                if (asessionName.equals(fConnectionInfo.getSessionName())) {
                    TraceSessionInfo traceSessionInfo = new TraceSessionInfo();
                    traceSessionInfo.fSessionId = session.getId();
                    traceSessionInfo.fSessionName = asessionName;

                    CreateSessionResponse createSession = fRelayd.createSession();
                    if (createSession.getStatus() != CreateSessionReturnCode.LTTNG_VIEWER_CREATE_SESSION_OK) {
                        throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.LttngRelaydConsumer_CreateViewerSessionError + createSession.getStatus().toString()));
                    }
                    traceSessionsInfos.add(traceSessionInfo);
                }
            }

        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.LttngRelaydConsumer_ErrorConnecting + (e.getMessage() != null ? e.getMessage() : ""))); //$NON-NLS-1$
        }

        return traceSessionsInfos;
    }

    private static String nullTerminatedByteArrayToString(final byte[] byteArray) throws UnsupportedEncodingException {
        // Find length of null terminated string
        int length = 0;
        while (length < byteArray.length && byteArray[length] != 0) {
            length++;
        }

        String asessionName = new String(byteArray, 0, length, ENCODING_UTF_8);
        return asessionName;
    }

}
