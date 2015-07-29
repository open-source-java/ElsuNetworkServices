package elsu.network.services;

import elsu.network.service.factory.*;
import elsu.common.*;
import java.util.*;

/**
 * AbstractServiceRuntimeProperties class is used to store runtime properties
 for the service or service connections.
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 */
public abstract class AbstractServiceRuntimeProperties {

    // <editor-fold desc="class private storage">
    // count of active connections currently connected to the service
    private volatile int _activeConnections = 0;

    // total # of connections allowed by the service
    private volatile long _totalConnections = 0L;

    // service connections thread group
    private volatile ThreadGroup _threadGroup;

    // total # of messages received for the life of the service
    private volatile long _totalMessgesReceived = 0L;

    // total # of messages sent for the life of the service
    private volatile long _totalMessgesSent = 0L;

    // total # of messages errored for the life of the service
    private volatile long _totalMessagesErrored = 0L;

    // datetime the service status (isrunning) changed
    private volatile Date _date = new Date();

    // datetime of the last action by the service connections
    private volatile Date _lastActionDate = null;

    // datetime of the last message received
    private volatile Date _receiveDate = null;

    // datetime of the last sent message
    private volatile Date _sentDate = null;

    // indicator if the service is running, allows connections to monitor this
    // and if false, connections gracefully shutdown
    private volatile boolean _isRunning = false;

    // general shared id by all connections to track messages # if required
    private volatile long _sequenceId = 0;
    // </editor-fold>

    // <editor-fold desc="class getter/setters">
    public synchronized int getActiveConnections() {
        return this._activeConnections;
    }

    public synchronized Date getDate() {
        return this._date;
    }

    protected synchronized void setDate() {
        this._date = new Date();
        setLastActionDate();
    }

    public synchronized Date getLastActionDate() {
        return this._lastActionDate;
    }

    private synchronized void setLastActionDate() {
        this._lastActionDate = new Date();
    }

    public synchronized Date getReceiveDate() {
        return this._receiveDate;
    }

    protected synchronized void setReceiveDate() {
        this._receiveDate = new Date();
        setLastActionDate();
    }

    public synchronized boolean isRunning() {
        return this._isRunning;
    }

    public synchronized boolean isRunning(boolean running) {
        this._isRunning = running;
        setDate();
        return isRunning();
    }

    public synchronized long getSequenceId() {
        return this._sequenceId;
    }

    protected synchronized long setSequenceId() {
        this._sequenceId++;
        return this._sequenceId;
    }

    public synchronized void setSequenceId(long newId) {
        this._sequenceId = newId;
    }

    public synchronized Date getSentDate() {
        return this._sentDate;
    }

    protected synchronized void setSentDate() {
        this._sentDate = new Date();
        setLastActionDate();
    }

    public synchronized ThreadGroup getThreadGroup() {
        return this._threadGroup;
    }

    protected synchronized void setThreadGroup(ThreadGroup group) {
        this._threadGroup = group;
    }

    public synchronized long getTotalConnections() {
        return this._totalConnections++;
    }

    public synchronized long getTotalMessagesErrored() {
        return this._totalMessagesErrored;
    }

    public synchronized long getTotalMessagesReceived() {
        return this._totalMessgesReceived;
    }

    public synchronized long getTotalMessagesSent() {
        return this._totalMessgesSent;
    }
    // </editor-fold>

    // <editor-fold desc="class methods">
    public synchronized void decreaseActiveConnections() {
        this._activeConnections--;
        setLastActionDate();
    }

    public synchronized void increaseActiveConnections() {
        this._activeConnections++;
        setLastActionDate();
    }

    public synchronized void increaseTotalMessagesErrored() {
        this._totalMessagesErrored++;
        setLastActionDate();
    }

    public synchronized void increaseTotalMessagesReceived() {
        this._totalMessgesReceived++;
        setReceiveDate();
    }

    public synchronized void increaseTotalMessagesSent() {
        this._totalMessgesSent++;
        setSentDate();
    }
    // </editor-fold>

    @Override
    public synchronized String toString() {
        StringBuilder result = new StringBuilder();

        result.append("<object attr='").append(getClass().getName()).append("'>");
        result.append("<activeConnections>").append(getActiveConnections()).append("</activeConnections>");
        result.append("<totalConnections>").append(getTotalConnections()).append("</totalConnections>");
        result.append("<totalMessgesReceived>").append(getTotalMessagesReceived()).append("</totalMessgesReceived>");
        result.append("<totalMessgesSent>").append(getTotalMessagesSent()).append("</totalMessgesSent>");
        result.append("<totalMessagesErrored>").append(getTotalMessagesErrored()).append("</totalMessagesErrored>");
        result.append("<date>").append(DateStack.convertDate2String(getDate(), ConfigLoader.DTGFORMAT)).append("</date>");
        result.append("<lastActionDate>").append(DateStack.convertDate2String(getLastActionDate(), ConfigLoader.DTGFORMAT)).append("</lastActionDate>");
        result.append("<receiveDate>").append(DateStack.convertDate2String(getReceiveDate(), ConfigLoader.DTGFORMAT)).append("</receiveDate>");
        result.append("<sentDate>").append(DateStack.convertDate2String(getSentDate(), ConfigLoader.DTGFORMAT)).append("</sentDate>");
        result.append("<running>").append(isRunning()).append("</running>");
        result.append("<sequenceId>").append(getSequenceId()).append("</sequenceId>");
        result.append("</object>");
        
        return result.toString();
    }
}
