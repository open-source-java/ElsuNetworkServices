package elsu.network.services.core;

import elsu.events.*;
import elsu.common.*;
import elsu.support.*;
import java.util.*;

/**
 * AbstractServiceRuntimeProperties class is used to store runtime properties
 * for the service or service connections.
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 */
public abstract class AbstractServiceRuntimeProperties
        extends AbstractEventManager {

    // <editor-fold desc="class private storage">
    // runtime sync object
    private Object _runtimeSync = new Object();

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
    public int getActiveConnections() {
        int result = 0;

        synchronized (this._runtimeSync) {
            result = this._activeConnections;
        }

        return result;
    }

    public Date getDate() {
        Date result = null;

        synchronized (this._runtimeSync) {
            result = this._date;
        }

        return result;
    }

    protected void setDate() {
        this._date = new Date();
        setLastActionDate();
    }

    public Date getLastActionDate() {
        Date result = null;

        synchronized (this._runtimeSync) {
            result = this._lastActionDate;
        }

        return result;
    }

    private void setLastActionDate() {
        this._lastActionDate = new Date();
    }

    public Date getReceiveDate() {
        Date result = null;

        synchronized (this._runtimeSync) {
            result = this._receiveDate;
        }

        return result;
    }

    protected void setReceiveDate() {
        this._receiveDate = new Date();
        setLastActionDate();
    }

    public boolean isRunning() {
        boolean result = false;

        synchronized (this._runtimeSync) {
            result = this._isRunning;
        }

        return result;
    }

    protected boolean isRunning(boolean running) {
        this._isRunning = running;
        setDate();
        return isRunning();
    }

    public long getSequenceId() {
        long result = 0L;
        
        synchronized(this._runtimeSync) {
            result = this._sequenceId;
        }
        
        return result;
    }

    protected long setSequenceId() {
        synchronized(this._runtimeSync) {
            this._sequenceId++;
        }
        
        return this._sequenceId;
    }

    protected void setSequenceId(long newId) {
        this._sequenceId = newId;
    }

    public Date getSentDate() {
        Date result = null;
        
        synchronized(this._runtimeSync) {
            result = this._sentDate;
        }
        
        return result;
    }

    protected void setSentDate() {
        this._sentDate = new Date();
        setLastActionDate();
    }

    public ThreadGroup getThreadGroup() {
        ThreadGroup result = null;
        
        synchronized(this._runtimeSync) {
            result = this._threadGroup;
        }
        
        return result;
    }

    protected void setThreadGroup(ThreadGroup group) {
        this._threadGroup = group;
    }

    public long getTotalConnections() {
        long result = 0;
        
        synchronized(this._runtimeSync) {
            result = this._totalConnections++;
        }
        
        return result;
    }

    public long getTotalMessagesErrored() {
        long result = 0;
        
        synchronized(this._runtimeSync) {
            result = this._totalMessagesErrored;
        }
        
        return result;
    }

    public long getTotalMessagesReceived() {
        long result = 0;
        
        synchronized(this._runtimeSync) {
            result = this._totalMessgesReceived;
        }
        
        return result;
    }

    public long getTotalMessagesSent() {
        long result = 0;
        
        synchronized(this._runtimeSync) {
            result = this._totalMessgesSent;
        }
        
        return result;
    }
    // </editor-fold>

    // <editor-fold desc="class methods">
    protected void decreaseActiveConnections() {
        this._activeConnections--;
        setLastActionDate();
    }

    protected void increaseActiveConnections() {
        this._activeConnections++;
        setLastActionDate();
    }

    protected void increaseTotalMessagesErrored() {
        this._totalMessagesErrored++;
        setLastActionDate();
    }

    protected void increaseTotalMessagesReceived() {
        this._totalMessgesReceived++;
        setReceiveDate();
    }

    protected void increaseTotalMessagesSent() {
        this._totalMessgesSent++;
        setSentDate();
    }
    // </editor-fold>

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("<object attr='").append(getClass().getName()).append("'>");
        result.append("<activeConnections>").append(getActiveConnections()).append("</activeConnections>");
        result.append("<totalConnections>").append(getTotalConnections()).append("</totalConnections>");
        result.append("<totalMessgesReceived>").append(getTotalMessagesReceived()).append("</totalMessgesReceived>");
        result.append("<totalMessgesSent>").append(getTotalMessagesSent()).append("</totalMessgesSent>");
        result.append("<totalMessagesErrored>").append(getTotalMessagesErrored()).append("</totalMessagesErrored>");
        result.append("<date>").append(DateUtils.convertDate2String(getDate(), ConfigLoader.getDTGFormat())).append("</date>");
        result.append("<lastActionDate>").append(DateUtils.convertDate2String(getLastActionDate(), ConfigLoader.getDTGFormat())).append("</lastActionDate>");
        result.append("<receiveDate>").append(DateUtils.convertDate2String(getReceiveDate(), ConfigLoader.getDTGFormat())).append("</receiveDate>");
        result.append("<sentDate>").append(DateUtils.convertDate2String(getSentDate(), ConfigLoader.getDTGFormat())).append("</sentDate>");
        result.append("<running>").append(isRunning()).append("</running>");
        result.append("<sequenceId>").append(getSequenceId()).append("</sequenceId>");
        result.append("</object>");

        return result.toString();
    }
}
