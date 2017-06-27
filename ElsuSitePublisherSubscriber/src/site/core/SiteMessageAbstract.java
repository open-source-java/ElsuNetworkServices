package site.core;

import elsu.common.*;

/**
 * SiteMessageAbstract class provide support for the BcsMessage class. The
 * purpose of the class is to store common parameters used to format or process
 * the message.
 * <p>
 * The class provides a global message counter to allow receiver to prevent
 * processing of duplicate messages.
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 */
public abstract class SiteMessageAbstract {

    // <editor-fold desc="class private storage">
    // global message number incremented every time the class is created and
    // can only be updated from the instance of the class
    private static long _totalMessageNumber = 0L;

    // message unique id, copied from the global identifier at the time of the
    // class instantiation
    private long _messageNumber = 0L;

    // date format for message date time field
    private String _datetimeFormat = "yyyyMMddHHmmssS";

    // field delimiter for string representation of the message class
    private String _delimiter = "|";

    // message string terminator (record terminator)
    private String _terminator = GlobalStack.LINESEPARATOR;
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    /**
     * BcsMessageBase() constructor is default no argument class constructor
     * used to set the message number by first increasing the global message
     * number and then copying it to the local class variable.
     *
     * @see BcsMessage
     */
    public SiteMessageAbstract() {
        setMessageNumber();
    }

    public SiteMessageAbstract(String datetimeFormat, String delimiter,
            String terminator) {
        setMessageNumber();

        this._datetimeFormat = datetimeFormat;
        this._delimiter = delimiter;
        this._terminator = terminator;
    }
    // </editor-fold>

    // <editor-fold desc="class getter/setters">
    /**
     * getDatetimeFormat() method returns the current datetime format in use for
     * the message.
     *
     * @return <code>String</code> representation of the date format
     */
    public String getDatetimeFormat() {
        return this._datetimeFormat;
    }

    /**
     * setDatetimeFormat(...) sets the date format for the message to use when
     * parsing the message or create a string representation of current message.
     *
     * @param format
     */
    public void setDatetimeFormat(String format) {
        this._datetimeFormat = format;
    }

    /**
     * getDelimiter() method returns the field delimiter currently being used to
     * separate fields when message is formatted as a string or parsed from a
     * string
     *
     * @return <code>String</code> value of the field delimiter
     */
    public String getDelimiter() {
        return this._delimiter;
    }

    /**
     * setDelimiter(...) method sets the value of the field delimiter which will
     * be used for parsing the message from string or to a string.
     *
     * @param delimiter
     */
    public void setDelimiter(String delimiter) {
        this._delimiter = delimiter;
    }

    /**
     * getMessageNumber() method returns the current messages # which is derived
     * from the global static #. The number is reset only if the application is
     * restarted.
     *
     * @return <code>long</code> value of the message #
     */
    public synchronized long getMessageNumber() {
        return this._messageNumber;
    }

    /**
     * setMessageNumber() method increments the global message number and then
     * stores the current value for local message id. The function is private
     * and adjustments to the number can only be done within the class.
     *
     * @return <code>long</code> value of the message #
     */
    private synchronized long setMessageNumber() {
        // retrieve the global message number and store it as local # of message
        this._messageNumber = setTotalMessageNumber();
        return getMessageNumber();
    }

    /**
     * setMessageNumber(...) method allows concrete class implementations to set
     * the message number when a message is decomposed from a string variable
     * back to class object.
     *
     * @param number
     */
    protected synchronized void setMessageNumber(long number) {
        this._messageNumber = number;
    }

    /**
     * getTerminator() method returns the current record terminator of the
     * message
     *
     * @return <code>String</code> value of the record terminator
     */
    public String getTerminator() {
        return this._terminator;
    }

    /**
     * setTerminator(...) method sets the value of the record terminator which
     * will be used for parsing the message from string or to a string.
     *
     * @param terminator
     */
    public void setTerminator(String terminator) {
        this._terminator = terminator;
    }

    /**
     * getTotalMessageNumber() method returns the current number of global
     * message count.
     *
     * @return <code>long</code> value of the message global #
     */
    public synchronized static long getTotalMessageNumber() {
        return SiteMessageAbstract._totalMessageNumber;
    }

    /**
     * setTotalMessageNumber() method increases the global message count and
     * returns the current value. The function is private and adjustments to the
     * number can only be done within the class.
     *
     * @return <code>long</code> value of the message global #
     */
    private synchronized static long setTotalMessageNumber() {
        // increase the global message #
        SiteMessageAbstract._totalMessageNumber++;
        return getTotalMessageNumber();
    }
    // </editor-fold>
}
