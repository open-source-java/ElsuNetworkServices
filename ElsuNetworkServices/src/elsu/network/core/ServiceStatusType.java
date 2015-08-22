/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package elsu.network.core;

import elsu.events.EventStatusType;

/**
 *
 * @author ss.dhaliwal
 */
public class ServiceStatusType {
    public ServiceStatusType() {
        initializeLocalProperties();
    }
    

    /**
     * initializeProperties() is a generic method to consolidate all initial
     * variable instantiation outside of class constructor. It allows the
     * variables to be reset from another method within a class if required.
     *
     */
    private void initializeLocalProperties() {
        try {
            EventStatusType.addStatusType("GETMAXIMUMCONNECTIONS", 7001);
            EventStatusType.addStatusType("SETMAXIMUMCONNECTIONS", 7002);
            EventStatusType.addStatusType("GETSERVICECONNECTIONS", 7003);
            EventStatusType.addStatusType("GETPROPERTY", 7004);
            EventStatusType.addStatusType("GETPROPERTIES", 7005);
            EventStatusType.addStatusType("ADDSERVICE", 7006);
            EventStatusType.addStatusType("DECREASESERVICECONNECTIONS", 7007);
            EventStatusType.addStatusType("INCREASESERVICECONNECTIONS", 7008);
            EventStatusType.addStatusType("LOGERROR", 7009);
            EventStatusType.addStatusType("LOGDEBUG", 7010);
            EventStatusType.addStatusType("LOGINFO", 7011);
            EventStatusType.addStatusType("GETCONFIG", 7012);
            EventStatusType.addStatusType("REMOVESERVICE", 7013);
            EventStatusType.addStatusType("STARTSERVICE", 7014);
            EventStatusType.addStatusType("TOSTRING", 7015);
            EventStatusType.addStatusType("GETSERVICE", 7016);
            EventStatusType.addStatusType("VALIDATESERVICE", 7017);
            EventStatusType.addStatusType("ISSERVICERUNNING", 7018);
        } catch (Exception ex) {
            //logError(getClass().toString()
            //        + ", initializeLocalProperties(), invalid connection.maximum, "
            //        + ex.getMessage());
        }
    }
}
