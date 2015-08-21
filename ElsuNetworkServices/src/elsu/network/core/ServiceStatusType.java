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
            EventStatusType.addStatusType("GETMAXIMUMCONNECTIONS", 7000);
            EventStatusType.addStatusType("GETSERVICECONNECTIONS", 7001);
            EventStatusType.addStatusType("GETPROPERTY", 7002);
            EventStatusType.addStatusType("GETPROPERTIES", 7003);
            EventStatusType.addStatusType("ADDSERVICE", 7004);
            EventStatusType.addStatusType("DECREASESERVICECONNECTIONS", 7005);
            EventStatusType.addStatusType("INCREASESERVICECONNECTIONS", 7006);
            EventStatusType.addStatusType("LOGERROR", 7007);
            EventStatusType.addStatusType("LOGDEBUG", 7008);
            EventStatusType.addStatusType("LOGINFO", 7009);
        } catch (Exception ex) {
            //logError(getClass().toString()
            //        + ", initializeLocalProperties(), invalid connection.maximum, "
            //        + ex.getMessage());
        }
    }
}
