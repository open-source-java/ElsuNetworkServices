package site.service;

import elsu.common.*;
import java.util.*;
import java.util.regex.*;
import site.core.SiteMessageAbstract;

/**
 * SiteMessage class is used to store the message information. The information is
 * used by BcsMessageService, related subscribers and publishers, and storage
 * services to process user data.
 * <p>
 This class is custom to SiteMessage and related objects, other classes can be
 created to support user needs as required.
 <p>
 * Message conversion to String format is:
 * siteId|date|equipmentId|sequence|payload
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 * @see SiteMessageAbstract
 * @see BcsMessageSubscriberService
 * @see BcsMessagePublisherService
 * @see AlarmStorageService
 * @see CommandForwarderService
 * @see MessageStorageService
 * @see BcsSiteSimulatorService
 */
public class SiteMessage extends SiteMessageAbstract {

    // <editor-fold desc="class private storage">
    // stores the site id related to the message
    private int _siteId = 0;
    // stores the message date
    private Date _messageDate = null;
    // stores the equipment id related to the message
    private int _equipmentId = 0;
    // stores the message
    private String _payload = null;
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    /**
     * BcsMessage() constructor creates an empty message object populating the
     * message date with the current datetime.
     */
    public SiteMessage() {
        // call super class constructor to populate message id
        super();

        // assign current time to the message date field
        setMessageDate(Calendar.getInstance().getTime());
    }

    /**
     * BcsMessage(...) constructor creates a custom message object with the
     * arguments specified.
     *
     * @param siteId
     * @param equipmentId
     * @param payload
     */
    public SiteMessage(int siteId, int equipmentId, String payload) {
        // call super class constructor to populate message id
        super();

        // save the site id passed for the message
        this._siteId = siteId;

        // assign current time to the message date field
        setMessageDate(Calendar.getInstance().getTime());

        // save the equipment id passed related the message
        this._equipmentId = equipmentId;

        // save the message package
        this._payload = payload;
    }

    /**
     * BcsMessage(...) constructor creates a custom message object with the
     * arguments specified to include custom formatting options
     *
     * @param siteId
     * @param equipmentId
     * @param payload
     * @param datetimeFormat
     * @param delimiter
     * @param terminator
     */
    public SiteMessage(int siteId, int equipmentId, String payload,
            String datetimeFormat, String delimiter, String terminator) {
        // call super class constructor to populate message id
        super(datetimeFormat, delimiter, terminator);

        // save the site id passed for the message
        this._siteId = siteId;

        // assign current time to the message date field
        setMessageDate(Calendar.getInstance().getTime());

        // save the equipment id passed related the message
        this._equipmentId = equipmentId;

        // save the message package
        this._payload = payload;
    }
    // </editor-fold>

    // <editor-fold desc="class getter/setters">
    /**
     * getMessageDate() method returns the date assigned for the message
     *
     * @return <code>date</code> object of the message
     */
    public Date getMessageDate() {
        return this._messageDate;
    }

    /**
     * setMessageDate(...) method sets the message date for the message which is
     * passed as an argument.
     *
     * @param date
     */
    private void setMessageDate(Date date) {
        this._messageDate = date;
    }

    /**
     * setMessageDate(...) method sets the message date from the string date
     * using custom date format.
     *
     * @param date
     * @param format
     */
    public void setMessageDate(String date, String format) {
        this._messageDate = DateUtils.convertString2Date(date, format);
    }

    /**
     * getPayload() method returns the message package stored in the object
     *
     * @return <code>String</code> object with the value of the package
     */
    public String getPayload() {
        return this._payload;
    }

    /**
     * getPayload(...) method assigns the message package with the value passed
     * to the method.
     *
     * @param payload
     */
    public void setPayload(String payload) {
        this._payload = payload;
    }

    /**
     * getSiteId() method returns the current site id assigned to the message.
     *
     * @return <code>int</code> value of the site id
     */
    public int getSiteId() {
        return this._siteId;
    }

    /**
     * setSiteId(...) method sets the site id of the message.
     *
     * @param siteId
     */
    public void setSiteId(int siteId) {
        this._siteId = siteId;
    }

    /**
     * getEquipmentId() method returns the current equipment id assigned to the
     * message.
     *
     * @return <code>int</code> value of the equipment id
     */
    public int getEquipmentId() {
        return this._equipmentId;
    }

    /**
     * setEquipmentId(...) method sets the equipment id of the message.
     *
     * @param equipmentId
     */
    public void setEquipmentId(int equipmentId) {
        this._equipmentId = equipmentId;
    }
    // </editor-fold>

    // <editor-fold desc="class methods">
    /**
     * getBcsMessage(...) method is static implementation allowing the program
     * to request a message object using provided individual parts of the
     * message vice creating one and then populating it.
     * <p>
     * The message parsing properties are updated with the custom properties.
     * <p>
     * Do not specify terminator in the payload, this method is to decompose one
     * record; terminator is stored for later use when string representation is
     * requested.
     *
     * @param siteId
     * @param equipmentId
     * @param payload
     * @param datetimeFormat
     * @param delimiter
     * @param terminator
     * @return <code>SiteMessage</code> object with all properties instantiated
     * from the parameters passed using custom parsing values (field delimiter,
     * date format, and record delimiter)
     */
    public static SiteMessage getBcsMessage(int siteId, int equipmentId,
            String payload,
            String datetimeFormat, String delimiter, String terminator) {
        SiteMessage bcsMessage = new SiteMessage(siteId, equipmentId, payload,
                datetimeFormat, delimiter, terminator);

        return bcsMessage;
    }

    /**
     * getBcsMessage(...) method is static implementation allowing the program
     * to request a message object using provided composite message comprising
     * of site id, date format, equipment id, and message payload.
     * <p>
     * Do not specify terminator in the payload.
     *
     * @param message
     * @return <code>SiteMessage</code> object with all properties instantiated
     * from the parameters passed using default parsing values (field delimiter,
     * date format, and record delimiter)
     */
    public static SiteMessage getBcsMessage(String message) {
        SiteMessage bcsMessage = new SiteMessage();
        bcsMessage.setBcsMessage(message);
        return bcsMessage;
    }

    /**
     * getBcsMessage(...) method is static implementation allowing the program
     * to request a message object using provided individual parts of the
     * message vice creating one and then populating it.
     * <p>
     * Input data is parsed using custom date format, field delimiter, and
     * record terminator. The message parsing properties are updated with the
     * custom properties.
     * <p>
     * Do not specify terminator in the payload, this method is to decompose one
     * record; terminator is stored for later use when string representation is
     * requested.
     *
     * @param message
     * @param datetimeFormat
     * @param delimiter
     * @param terminator
     * @return <code>SiteMessage</code> object with all properties instantiated
     * from the parameters passed using custom parsing values (field delimiter,
     * date format, and record delimiter)
     */
    public static SiteMessage getBcsMessage(String message,
            String datetimeFormat, String delimiter, String terminator) {
        SiteMessage bcsMessage = new SiteMessage();
        bcsMessage.setBcsMessage(message, datetimeFormat, delimiter, terminator);
        return bcsMessage;
    }

    /**
     * getBcsMessage() method returns this object as a string representation
     * using default delimiter, date format, and record terminators.
     * 
     * 20141129 SSD updated to consolidate two methods and calling custom
     * getBcsMessage()
     *
     * @return <code>String</code> object with message
     */
    public String getBcsMessage() {
        return getBcsMessage(getDatetimeFormat(), getDelimiter(), getTerminator());
    }

    /**
     * getBcsMessage(...) method returns this object as a string representation
     * using custom delimiter, date format, and record terminators.
     *
     * @param datetimeFormat
     * @param delimiter
     * @param terminator
     * @return <code>String</code> object with message
     */
    public String getBcsMessage(String datetimeFormat, String delimiter,
            String terminator) {
        StringBuilder result = new StringBuilder();

        result.append(getSiteId())
                .append(delimiter);
        result.append(DateUtils.convertDate2String(getMessageDate(),
                datetimeFormat))
                .append(delimiter);
        result.append(getEquipmentId())
                .append(delimiter);
        result.append(getMessageNumber())
                .append(delimiter);
        result.append(getPayload())
                .append(terminator);

        return result.toString();
    }

    /**
     * setBcsMessage(...) method parses the message parameter using the default
     * delimiter, date format, and record terminator. Upon completion the
     * current object site id, equipment id, date, and message contain the
     * passed values.
     * <p>
     * Do not specify terminator in the payload.  20141129 SSD updated to remove
     * terminator
     *
     * @param message
     * @return <code>String</code> object with the message
     */
    public String setBcsMessage(String message) {
        String[] lineData = message.split(Pattern.quote(getDelimiter()));

        if (lineData.length >= 5) {
            setSiteId(Integer.parseInt(lineData[0]));
            setMessageDate(lineData[1], getDatetimeFormat());
            setEquipmentId(Integer.parseInt(lineData[2]));
            setMessageNumber(Long.parseLong(lineData[3]));
            setPayload(lineData[4].replace(getTerminator(), ""));
        }

        return getBcsMessage();
    }

    /**
     * setBcsMessage(...) method parses the message parameter using the custom
     * delimiter, date format, and record terminator. Upon completion the
     * current object site id, equipment id, date, and message contain the
     * passed values and parsing properties are updated.
     * <p>
     * Do not specify terminator in the payload, this method is to decompose one
     * record; terminator is stored for later use when string representation is
     * requested.
     * 
     * 20141129 SSD updated to consolidate two methods and calling root
     * getMessage() after setters
     * 
     * @param message
     * @param datetimeFormat
     * @param delimiter
     * @param terminator
     * @return <code>String</code> object with the message
     */
    public String setBcsMessage(String message,
            String datetimeFormat, String delimiter, String terminator) {
        setDatetimeFormat(datetimeFormat);
        setDelimiter(delimiter);
        setTerminator(terminator);

        setBcsMessage(message);
        return getBcsMessage();
    }
    // </editor-fold>

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("<object attr='").append(getClass().getName()).append("'>");
        result.append("<siteId>").append(getSiteId()).append("</sideId>");
        result.append("<messageDate>")
                .append(DateUtils.convertDate2String(getMessageDate(), getDatetimeFormat()))
                .append("</messageDate>");
        result.append("<equipmentId>")
                .append(getEquipmentId()).append("</equipmentId>");
        result.append("<messageNumber>")
                .append(getMessageNumber()).append("</messageNumber>");
        result.append("<totalMessageNumber>")
                .append(getTotalMessageNumber()).append("</totalMessageNumber>");
        result.append("<payload>").append(getPayload()).append("</payload>");
        result.append("</object>");

        return result.toString();
    }
}
