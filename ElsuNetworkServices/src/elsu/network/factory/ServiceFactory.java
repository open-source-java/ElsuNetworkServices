package elsu.network.factory;

import elsu.events.*;
import elsu.network.application.*;
import elsu.network.core.*;
import elsu.network.services.core.*;
import elsu.network.services.system.*;
import elsu.support.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * ServiceFactory processes the config file or config params provided as
 * arguments and returns the service.
 * <p>
 * There are two main types of main services: Server or Client and two child
 * services: Subscriber or Publisher. Server service creates a listener and
 * after accepting the connection and uses the connection input/output streams
 * to perform service actions. Client service is used to support child services
 * and does not create a listener by default. Child services are used to group
 * services under one parent service when multiple actions have common
 * configuration properties.
 *
 * 20141128 SSD updated for reflection generics warning on
 * getDeclaredConstructor
 *
 * @see ConfigLoader
 * @see AbstractService
 * @see IService
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 * @version .51
 */
public abstract class ServiceFactory {

    /**
     * initializeServiceAbstracts() method is used to iterate through the list
     * of services and start activating them one by one. Only services which are
     * not marked as Disabled are created through reflection using the service
     * class and constructor parameters.
     * <p>
     * ServiceAbstracts marked Automatic are started once they have their
     * instance was successfully created. ServiceAbstracts marked DelayedStart
     * are started after the Automatic services have been started.
     * <p>
     * DelayedStart services are always started last because they may have
     * dependencies on other services.
     *
     * @throws Exception
     */
    public static void initializeServices(ServiceManager serviceManager) throws Exception {
        String serviceName = "";
        IService service = null;

        try {
            // collect the list of all services into array list for processing
            // do not use iterator since control service can change the scope
            // of the iterate and will result in exceptions.
            ArrayList<String> spIterator;
            spIterator = new ArrayList<>(serviceManager.getConfig().getClassSet());

            // loop through all the services in the service list
            for (String spObject : spIterator) {
                serviceName = spObject.replace(".class", "");

                initializeServices(serviceManager, serviceName);
            }

            // since all the services which were not disabled were already 
            // loaded and services which were Automatic were started previously
            // we need to now start the services marked DelayedStart.
            // collect the list of all services into array list for processing
            // do not use iterator since control service can change the scope
            // of the iterate and will result in exceptions.
            List<IService> serviceList;
            serviceList = new ArrayList<>(serviceManager.getServices().values());

            for (Iterator<IService> it = serviceList.iterator(); it.hasNext();) {
                service = it.next();
                if (service.getServiceConfig().getStartupType()
                        == ServiceStartupType.DELAYEDSTART) {
                    // temporarily update the service startup type to Automatic
                    // so we can use the common start method
                    service.getServiceConfig().setStartupType(
                            ServiceStartupType.AUTOMATIC);

                    // start the service.  we do not need to add the service,
                    // just start it.
                    boolean status = serviceManager.startService(service.getServiceConfig().getConnectionPort());

                    // reset the service startup type back to DelayedStart
                    service.getServiceConfig().setStartupType(
                            ServiceStartupType.DELAYEDSTART);

                    // check status of service, if it did not start, generate exception
                    if (!status) {
                        throw new Exception("ServiceFactory.class, initializeServices(), "
                                + "service load error, (" + service.getServiceConfig().getServiceName() + ")");
                    }
                }

                Thread.yield();
            }

            // clear the service list to allow garbage collection to recover
            // the memory
            serviceList = null;
        } catch (Exception ex) {
            // log error if there was any exception in processing during
            // reflection or parameter discovery and throw it to allow calling
            // function to handle it
            throw new Exception("ServiceFactory.class, initializeServices(), "
                    + ex.getMessage());
        }
    }

    public static void initializeServices(ServiceManager serviceManager, String serviceName)
            throws Exception {
        ServiceConfig serviceConfig = null;
        IService service = null;

        try {
            // make sure this is not a child service type: PUBLISHER or SUBSCRIBER
            if (serviceManager.getConfig().getProperty(serviceName + ".serviceType") == null) {
            } else
            if (serviceManager.getConfig().getProperty(serviceName + ".serviceType").toString().equals("SUBSCRIBER")
                    || serviceManager.getConfig().getProperty(serviceName + ".serviceType").toString().equals("PUBLISHER")) {
                // ignore this class type
            } else {
                // extract the service properties for parsing
                serviceConfig = ServiceConfig.LoadConfig(serviceManager.getConfig(), serviceName);

                // control service is a custom service and there does not use
                // reflection but direct instantiation.
                if (serviceName.equals("application.services.service.controlService")) {
                    // is the service disabled, if not create an instance of
                    // the service
                    if (serviceConfig.getStartupType() != ServiceStartupType.DISABLED) {
                        // log the action
                        serviceManager.logInfo(".. service loaded (" + serviceName + ")");

                        // create the service instance
                        service = new ControlService(serviceManager, serviceConfig);

                        // connect the factory event listeners
                        ((IEventPublisher) service).addEventListener(serviceManager);
                        serviceManager.addEventListener((IEventSubscriber) service);

                        // add the service to the service list in the factory
                        serviceManager.addService(service);
                    }
                } else if (serviceConfig.getStartupType() != ServiceStartupType.DISABLED) {
                    // service is not control service, so if it is not 
                    // disabled process the service properties

                    // log the action
                    serviceManager.logInfo(".. service loaded (" + serviceName + ")");

                    // using reflection, load the class for the service
                    Class<?> serviceClass = Class.forName(serviceConfig.getServiceClass());

                    // create service constructor discovery type parameter array
                    // populate it with the required class types
                    Class<?>[] argTypes = {String.class, 
                        ServiceManager.class, ServiceConfig.class};

                    // retrieve the matching constructor for the service using
                    // reflection
                    Constructor<?> cons = serviceClass.getDeclaredConstructor(
                            argTypes);

                    // create parameter array and populate it with values to 
                    // pass to the service constructor
                    Object[] arguments
                            = {serviceConfig.getServiceClass(), serviceManager, serviceConfig};

                    // create new instance of the service using the discovered
                    // constructor and parameters
                    service = (IService) cons.newInstance(arguments);

                    // connect the factory event listeners
                    ((IEventPublisher) service).addEventListener(serviceManager);
                    serviceManager.addEventListener((IEventSubscriber) service);

                    // add the service to the service list in the factory
                    serviceManager.addService(service);
                }

                // yield processing to other threads
                Thread.yield();
            }
        } catch (Exception ex) {
            // log error if there was any exception in processing during
            // reflection or parameter discovery and throw it to allow calling
            // function to handle it
            if (service != null) {
                try {
                    serviceManager.removeService(service.getServiceConfig().getConnectionPort(), false);
                } catch (Exception exi) {
                }
            }

            throw new Exception("ServiceFactory.class, initializeServices(), "
                    + serviceName + " service load error, " + ex.getMessage());
        }
    }
}
