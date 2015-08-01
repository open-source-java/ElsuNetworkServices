package elsu.network.service.factory;

import elsu.network.services.*;
import elsu.common.*;
import elsu.support.*;
import java.util.*;
import java.io.*;
import org.apache.commons.lang3.*;

/**
 * ConfigLoader is the base class for factory. The core purpose is to load the
 * app.config provided through the application command line arguments or the
 * default app.config stored as the resource in the jar file.
 * <p>
 * app.config once extracted from the jar file is not over-written every time
 * but reused allowing the user to change the extracted app.config file.
 * <p>
 * The configuration load is done using the direct XPath references to the node
 * properties and recursive nodes are processed by first collecting the node
 * names into a list and then iterating over the list.
 * <p>
 * log4j.properties is also extracted upon initial run of the program. Logging
 * is configured during the initial load.
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 * @version .51
 */
public class ConfigLoader {
    // <editor-fold desc="class private storage">

    // static property for app.config store and extraction from jar file
    public static String APPCONFIG = "app.config";
    
    // static property for data format across the application for display 
    // purposes
    public static String DTGFORMAT = "YYYMMDD HH24:mm:ss";
    
    // variable to store the xml document object from XMLReader class
    protected XMLReader _xmlr = null;
    
    // store all application wide properties from app.config
    private Map<String, String> _applicationProperties
            = new HashMap<>();
    
    // store all service configuration properties from app.config
    private Map<String, Object> _serviceProperties
            = new HashMap<>();
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    /**
     * ConfigLoader() no-argument constructor is used to load the default
     * app.config which is set through the static APPCONFIG variable prior to
     * instantiation of the class.
     * <p>
     * Constructor will try to extract the stored app.config in the application
     * jar file if available.
     *
     * @throws Exception
     */
    public ConfigLoader() throws Exception {
        try {
            String configFile;

            // check if this class is inherited (class name will not match)
            // if yes, then load framework.config first
            if (!this.getClass().getName().equals("elsu.network.service.factory.ConfigLoader")) {
                configFile = (new File(getClass().getName().replace(".", "\\"))).getParent()
                        + "\\framework.config";

                // extract file to local file system
                extractConfigFile(configFile);

                // try to create the XML reader instance for XML document parsing
                // using the app.config file location
                _xmlr = new XMLReader(configFile);

                // load the config into application or service properties hashMaps
                initializeConfig(true);
            }

            // check is app.config and log4j.properties file is stored in the
            // application; note, if variable already contains a path then 
            // external config is used view package extraction
            if (!ServiceFactory.APPCONFIG.contains("\\")
                    && !ServiceFactory.APPCONFIG.contains("/")) {
                configFile
                        = (new File(getClass().getName().replace(".", "\\"))).getParent()
                        + "\\" + ServiceFactory.APPCONFIG;
            } else {
                configFile = ServiceFactory.APPCONFIG;
            }

            // extract file to local file system
            extractConfigFile(configFile);

            // try to create the XML reader instance for XML document parsing
            // using the app.config file location
            _xmlr = new XMLReader(configFile);

            // display the config to the user
            showConfig();

            // load the config into application or service properties hashMaps
            initializeConfig(false);
        } catch (Exception ex) {
            // display exception to the user and exit
            System.out.println(getClass().toString() + "//" + ex.getMessage());
            throw new Exception(getClass().toString() + ", initialize(), " + ex.getMessage());
        }
    }

    /**
     * ConfigLoader(...) constructor is used to load custom configuration passed
     * through the string variable. Normally used by control service to pass
     * custom XML sent from the client.
     *
     * @param configData is the XML data passed from the calling function.
     * @throws Exception
     */
    public ConfigLoader(String configData) throws Exception {
        try {
            // try to create the XML reader instance for XML document parsing
            // using the app.config file location
            _xmlr = new XMLReader(configData);

            // display the config to the user
            showConfig();

            // load the config into application or service properties hashMaps
            initializeConfig(true);
        } catch (Exception ex) {
            // display exception to the user and exit
            System.out.println(getClass().toString() + "//" + ex.getMessage());
        }
    }

    /**
     * initializeConfig() clears the storage application and service hashMaps
     * and then loads the app.config using XPath to reference each property.
     *
     * @param initialization is true if this is the first time properties are
     * being loaded
     * @throws Exception
     */
    private void initializeConfig(boolean initialization) throws Exception {
        // clear the properties only for initalization
        if (initialization) {
            // clear the storage hashMaps
            getApplicationProperties().clear();
            getServiceProperties().clear();
        }

        // load the app.config into memory
        loadNodes(_xmlr.getDocument());
    }
    // </editor-fold>

    // <editor-fold desc="class getter/setters">
    /**
     * getApplicationProperties() method returns the hashMap containing the
     * application properties key/value pair extracted from the app.config
     * application.attributes section
     *
     * @return <code>hashMap</code> key/value set of all application properties
     */
    public Map<String, String> getApplicationProperties() {
        return this._applicationProperties;
    }

    /**
     * getApplicationProperties() method returns the hashMap containing the each
     * services properties key/value pair extracted from the app.config
     * services.service section.
     * <p>
     * Child services properties are added to the ServiceConfig in either
     * subscriber or publisher properties based on child service type.
     *
     * @return <code>hashMap</code> key/value set of all application properties
     */
    public Map<String, Object> getServiceProperties() {
        return this._serviceProperties;
    }
    // </editor-fold>

    // <editor-fold desc="class methods">
    /**
     * extractConfigFile(...) method verifies if the external config exists, if
     * not, it tries to extract the config file from jar file. If either are
     * unsuccessful, exception is thrown to notify user of missing config.
     *
     * @param filename location of the config file
     * @throws Exception
     */
    private void extractConfigFile(String filename) throws Exception {
        // create a reference to the location of the configuration file
        File cf = new File(filename);

        // if the file does not exist, try to extract it from the jar resource
        if (!cf.exists()) {
            // notify the user we are extracting the store app.config
            System.out.println("extracting config file: " + filename);

            // create directories
            cf.getParentFile().mkdirs();

            // open the input stream from the jar resource
            BufferedReader configIFile = null;
            configIFile = new BufferedReader(
                    new InputStreamReader(
                            getClass().getClassLoader().getResourceAsStream(
                                    filename.replace("\\", "/"))));

            // declare storage for the output file
            BufferedWriter configOFile = null;

            // if input file if valid, then extract the data
            if (configIFile != null) {
                try {
                    // open the output file
                    configOFile = new BufferedWriter(new FileWriter(cf));

                    // declare storage for the data from the input stream
                    String line;

                    // loop the config file, read each line until no more data
                    while ((line = configIFile.readLine()) != null) {
                        // write the data to the output file and insert the new
                        // line terminator after each line
                        configOFile.write(line + GlobalStack.LINESEPARATOR);

                        // yield processing to other threads
                        Thread.yield();
                    }

                    // notify user the status of the config file
                    System.out.println("config file extracted successfully");
                } catch (Exception ex) {
                    // if exception during processing, return it to the user
                    throw new Exception(getClass().toString() + "//"
                            + ex.getMessage());
                } finally {
                    // close the input file to prevent resource leaks
                    try {
                        configIFile.close();
                    } catch (Exception exi) {
                    }

                    // close the output file to prevent resource leaks
                    if (configOFile != null) {
                        try {
                            configOFile.flush();
                        } catch (Exception exi) {
                        }
                        try {
                            configOFile.close();
                        } catch (Exception exi) {
                        }
                    }
                }
            }
        } else {
            // config file already existed, notify user we are using it
            System.out.println("using config file: " + filename);
        }
    }

    /**
     * loadBaseConfig(...) method parses the app.config for each section and
     * first loads the list of node names; example list of services, list of
     * attributes, or list of child services.
     *
     * @param config is the config object for the service being loaded
     * @param name of the service being loaded
     * @see loadNodes(...)
     */
    private void loadBaseConfig(ServiceConfig config, String name) {
        // store the service name in the config object
        config.setServiceName(name);

        // store the core service properties which are common to all services
        // property - connection port (default error)
        // property - service class
        // property - startup type (default Manual)
        // property - ignore maximum connections limit (default false)
        // property - service max connections (default 10)
        config.setConnectionPort(Integer.parseInt(_xmlr.getNodeValueByXPath(
                "/application/services/service[@name='" + name + "']/port")));
        config.setServiceClass(_xmlr.getNodeValueByXPath(
                "/application/services/service[@name='" + name + "']/class"));

        try {
            config.setStartupType(ServiceStartupType.valueOf(
                    _xmlr.getNodeValueByXPath(
                            "/application/services/service[@name='" + name
                            + "']/startupType")));
        } catch (Exception ex) {
            System.out.println(getClass().toString()
                    + ", loadBaseConfig(), invalid startupType, "
                    + ex.getMessage());
            config.setStartupType(ServiceStartupType.MANUAL);
        }

        String svcType = _xmlr.getNodeValueByXPath(
                "/application/services/service[@name='" + name + "']/serviceType").toUpperCase();
        if (svcType.equals(ServiceType.SERVER.name())) {
            config.setServiceType(ServiceType.SERVER);
        } else if (svcType.equals(ServiceType.CLIENT.name())) {
            config.setServiceType(ServiceType.CLIENT);
        } else {
            System.out.println(getClass().toString()
                    + ", loadBaseConfig(), invalid serviceType, set to CLIENT");
            config.setServiceType(ServiceType.CLIENT);
        }

        try {
            config.isIgnoreConnectionLimit(Boolean.valueOf(
                    _xmlr.getNodeValueByXPath(
                            "/application/services/service[@name='" + name
                            + "']/ignoreConnectionLimit")));
        } catch (Exception ex) {
            System.out.println(getClass().toString()
                    + ", loadBaseConfig(), invalid ignoreConnectionLimit, "
                    + ex.getMessage());
            config.isIgnoreConnectionLimit(false);
        }

        try {
            config.setMaximumConnections(Integer.parseInt(
                    _xmlr.getNodeValueByXPath(
                            "/application/services/service[@name='" + name
                            + "']/maxConnections")));
        } catch (Exception ex) {
            System.out.println(getClass().toString()
                    + ", loadBaseConfig(), invalid maxConnections, "
                    + ex.getMessage());
            config.setMaximumConnections(10);
        }

        // store the service properties.  these are custom properties and
        // can be changed without impacting the core property parsing
        // collects the list of attributes key names from the application block
        org.w3c.dom.NodeList attributes = _xmlr.getNodeListByXPath(
                "/application/services/service[@name='" + name
                + "']/attributes/key/@name");
        ArrayList<String> attributesList = new ArrayList<>();
        for (int i = 0; i < attributes.getLength(); i++) {
            attributesList.add(attributes.item(i).getNodeValue());

            // yield processing to other threads
            Thread.yield();
        }

        // retrive the value of the attribute key name
        for (String key : attributesList) {
            String value = _xmlr.getNodeValueByXPath(
                    "/application/services/service[@name='" + name
                    + "']/attributes/key[@name='" + key + "']");
            config.getAttributes().put(key, value);

            // yield processing to other threads
            Thread.yield();
        }
    }

    /**
     * loadNodes(...) method is a recursive method which parses the app.config
     * XML file piece by piece and calls other methods passing the node
     * reference
     *
     * @param node is the value of the current XML node being processed.
     * @see loadBaseConfig
     * @throws Exception
     */
    protected void loadNodes(org.w3c.dom.Node node) throws Exception {
        // store the application properties.  these are custom properties and
        // can be changed without impacting the core property parsing
        // collects the list of attribute key names from the application block
        org.w3c.dom.NodeList attributes = _xmlr.getNodeListByXPath(
                "/application/elsuFramework/attributes/key/@name");
        ArrayList<String> attributesList = new ArrayList<>();
        for (int i = 0; i < attributes.getLength(); i++) {
            attributesList.add(attributes.item(i).getNodeValue());

            // yield processing to other threads
            Thread.yield();
        }

        // retrive the value of the attribute key name
        for (String key : attributesList) {
            String value = _xmlr.getNodeValueByXPath(
                    "/application/elsuFramework/attributes/key[@name='" + key + "']");

            // remove old value if loaded from framework.config
            getApplicationProperties().remove(key);
            getApplicationProperties().put(key, value);

            // yield processing to other threads
            Thread.yield();
        }

        // retrive the log property value from the application attributes 
        if (getApplicationProperties().get("log.config") != null) {
            // log attribute value is defined, set the static variable to the 
            // log property file location; also, check if path is provided as
            // part of the file name - if yes, then ignore class path
            String configFile;

            if (!getApplicationProperties().get("log.config").toString().contains(
                    "\\")
                    && !getApplicationProperties().get("log.config").toString().contains(
                            "/")) {
                configFile
                        = (new File(getClass().getName().replace(".", "\\"))).getParent()
                        + "\\"
                        + getApplicationProperties().get("log.config").toString();
            } else {
                configFile
                        = getApplicationProperties().get("log.config").toString();
            }

            Log4JManager.LOG4JCONFIG = configFile;

            // check if the log property file exists, if not extract it 
            extractConfigFile(Log4JManager.LOG4JCONFIG);

            // create the instance of the Log4JManager using the properties file
            new Log4JManager("dataLogger");
        }

        // collects the list of services defined in the app.config under the
        // services.service path
        org.w3c.dom.NodeList svcNodes = _xmlr.getNodeListByXPath(
                "/application/services/service/@name");
        ArrayList<String> svcList = new ArrayList<>();
        for (int i = 0; i < svcNodes.getLength(); i++) {
            svcList.add(svcNodes.item(i).getNodeValue());

            // show the entry in the log as info
            Log4JManager.LOG.info(svcNodes.item(i).getNodeValue());

            // yield processing to other threads
            Thread.yield();
        }

        // collects the list of child service parent service list defined in 
        // the app.config under the childServices.forService path
        org.w3c.dom.NodeList childServices = _xmlr.getNodeListByXPath(
                "/application/childServices/forService/@name");
        ArrayList<String> childSvcList = new ArrayList<>();
        for (int i = 0; i < childServices.getLength(); i++) {
            childSvcList.add(childServices.item(i).getNodeValue());

            // yield processing to other threads
            Thread.yield();
        }

        // for each service in the array list, read the config for core service
        // properties and any child properties defined
        for (String svcName : svcList) {
            // show entry in log for the service being processed
            Log4JManager.LOG.info(".. loading config for service (" + svcName
                    + ")");

            // create storage object for the service config
            ServiceConfig config = new ServiceConfig();

            // call reference method to load base config properties
            loadBaseConfig(config, svcName);

            // scan the list of child services and see if there is one matching
            // the service being configured under node childServices.forService
            for (String childSvcName : childSvcList) {
                // if the name matches a parent service, load the child services
                if (childSvcName.equals(svcName)) {
                    // create storage object for the child service config
                    ServiceConfig childSvcConfig = null;

                    // collects the list of child services defined in the 
                    // app.config under the childServices.forService.services path
                    org.w3c.dom.NodeList childSvcConnections
                            = _xmlr.getNodeListByXPath(
                                    "/application/childServices/forService[@name='"
                                    + childSvcName
                                    + "']/services/childService/@name");
                    ArrayList<String> childSvcConnectionList
                            = new ArrayList<>();
                    for (int i = 0; i < childSvcConnections.getLength(); i++) {
                        childSvcConnectionList.add(
                                childSvcConnections.item(i).getNodeValue());

                        // yield processing to other threads
                        Thread.yield();
                    }

                    // for each child service, create and load the configuration
                    // based on if the child service is a subscriber or a 
                    // publisher
                    for (String connection : childSvcConnectionList) {
                        // allocate memory for the child service properties
                        childSvcConfig = new ServiceConfig();

                        // store the child service name in the config object
                        childSvcConfig.setServiceName(connection);

                        // store the core service properties which are common 
                        // to all child services
                        // property - connection port (default error)
                        // property - service class
                        // property - startup type (default Manual)
                        // property - ignore maximum connections limit (default false)
                        // property - service max connections (default 10)
                        childSvcConfig.setConnectionPort(Integer.parseInt(
                                _xmlr.getNodeValueByXPath(
                                        "/application/childServices/forService[@name='"
                                        + childSvcName
                                        + "']/services/childService[@name='"
                                        + connection + "']/port")));
                        childSvcConfig.setServiceClass(
                                _xmlr.getNodeValueByXPath(
                                        "/application/childServices/forService[@name='"
                                        + childSvcName
                                        + "']/services/childService[@name='"
                                        + connection + "']/class"));

                        try {
                            childSvcConfig.setStartupType(
                                    ServiceStartupType.valueOf(
                                            _xmlr.getNodeValueByXPath(
                                                    "/application/childServices/forService[@name='"
                                                    + childSvcName
                                                    + "']/services/childService[@name='"
                                                    + connection
                                                    + "']/startupType")));
                        } catch (Exception ex) {
                            System.out.println(getClass().toString()
                                    + ", loadNodes(), invalid child service startupType, "
                                    + ex.getMessage());
                            childSvcConfig.setStartupType(
                                    ServiceStartupType.MANUAL);
                        }

                        String childSvcType = _xmlr.getNodeValueByXPath(
                                "/application/childServices/forService[@name='"
                                + childSvcName
                                + "']/services/childService[@name='"
                                + connection + "']/serviceType").toUpperCase();
                        if (childSvcType.equals(ServiceType.SUBSCRIBER.name())) {
                            childSvcConfig.setServiceType(ServiceType.SUBSCRIBER);
                            config.getSubscribers().add(childSvcConfig);
                        } else {
                            childSvcConfig.setServiceType(ServiceType.PUBLISHER);
                            config.getPublishers().add(childSvcConfig);
                        }

                        try {
                            childSvcConfig.isIgnoreConnectionLimit(
                                    Boolean.valueOf(_xmlr.getNodeValueByXPath(
                                                    "/application/childServices/forService[@name='"
                                                    + childSvcName
                                                    + "']/services/childService[@name='"
                                                    + connection
                                                    + "']/ignoreConnectionLimit")));
                        } catch (Exception ex) {
                            System.out.println(getClass().toString()
                                    + ", loadNodes(), invalid child service ignoreConnectionLimit, "
                                    + ex.getMessage());
                            childSvcConfig.isIgnoreConnectionLimit(false);
                        }

                        try {
                            childSvcConfig.setMaximumConnections(
                                    Integer.parseInt(_xmlr.getNodeValueByXPath(
                                                    "/application/childServices/forService[@name='"
                                                    + childSvcName
                                                    + "']/services/childService[@name='"
                                                    + connection
                                                    + "']/maxConnections")));
                        } catch (Exception ex) {
                            System.out.println(getClass().toString()
                                    + ", loadNodes(), invalid maxConnections, "
                                    + ex.getMessage());
                            childSvcConfig.setMaximumConnections(10);
                        }

                        // store the child service properties.  these are 
                        // custom properties and can be changed without 
                        // impacting the core property parsing collects the 
                        // list of attributes key names from the application 
                        // block
                        attributes = _xmlr.getNodeListByXPath(
                                "/application/childServices/forService[@name='"
                                + childSvcName
                                + "']/services/childService[@name='"
                                + connection + "']/attributes/key/@name");
                        ArrayList<String> childAttributesList
                                = new ArrayList<>();
                        for (int i = 0; i < attributes.getLength(); i++) {
                            childAttributesList.add(
                                    attributes.item(i).getNodeValue());

                            // yield processing to other threads
                            Thread.yield();
                        }

                        // retrive the value of the attribute key name
                        for (String key : childAttributesList) {
                            String value = _xmlr.getNodeValueByXPath(
                                    "/application/childServices/forService[@name='"
                                    + childSvcName
                                    + "']/services/childService[@name='"
                                    + connection + "']/attributes/key[@name='"
                                    + key + "']");
                            childSvcConfig.getAttributes().put(key, value);

                            // yield processing to other threads
                            Thread.yield();
                        }

                        // yield processing to other threads
                        Thread.yield();
                    }
                }

                // yield processing to other threads
                Thread.yield();
            }

            // store the service config into the factory hashMap
            getServiceProperties().put(svcName, config);

            // display the config to the user for review
            Log4JManager.LOG.info(".. " + config.toString());

            // yield processing to other threads
            Thread.yield();
        }
    }

    /**
     * showConfig() method displays the configuration to the console output.
     *
     */
    private void showConfig() {
        showConfigNodes(_xmlr.getDocument(), 1);

        //System.out.println("------------");
        //org.w3c.dom.NodeList nl = _xmlr.getNodesByElement("connections");
        //for (int i = 0; i < nl.getLength(); i++) {
        //    showConfigNodes(nl.item(i), 1);
        //}
        //System.out.println("------------");
        //nl = _xmlr.getNodesByElement("service");
        //for (int i = 0; i < nl.getLength(); i++) {
        //    System.out.println("---" + nl.item(i).getNodeName());
        //    showConfigNodes(nl.item(i), 1);
        //}
    }

    /**
     * showConfigNodes(...) method is used to recursively scan the XML config
     * file and display the nodes in tree format.
     *
     * @param parent
     * @param level level of the node, increased for each child-node to allow
     * tabbed tree display output
     *
     */
    protected void showConfigNodes(org.w3c.dom.Node parent, int level) {
        // create a local class to display node value/text and associated
        // node attributes
        class SubShowNode {

            // loop through the node attributes for the node passed
            String displayNodeAttributes(org.w3c.dom.Node node) {
                // create string build object to support string concatanation
                StringBuilder sb = new StringBuilder();

                // retrieve node attributes (if any)
                ArrayList nAttributes = _xmlr.getNodeAttributes(node);

                // loop through the attributes array and append them to the
                // string builder object
                if (nAttributes != null) {
                    for (Object na : nAttributes) {
                    // append the attribute details (key/text) to the string
                        // builder object
                        sb.append(" [ATTR=").append(((org.w3c.dom.Node) na).getNodeName())
                                .append("//")
                                .append(((org.w3c.dom.Node) na).getNodeValue())
                                .append("]");

                        // yield processing to other threads
                        Thread.yield();
                    }
                }

                // return the string builder representation as a string
                return sb.toString();
            }
        }

        // declare the showNode class to allow methods to reference the display
        // method to prevent duplicaion in code
        SubShowNode showNode = new SubShowNode();

        // retrieve the child nodes for processing
        ArrayList nodes = _xmlr.getNodeChildren(parent);

        // if node level is 1, then this is root node, display it with no
        // indentation
        if (level == 1) {
            // display the parent node name
            String data = StringUtils.repeat('~', level) + parent.getNodeName();

            // use the sub function to extract node attributes
            data += showNode.displayNodeAttributes(parent);

            // display all collected data to the user output
            System.out.println(data);
        }

        // parse the list of child nodes for the node being processed
        for (Object node : nodes) {
            // display the parent node name
            String data = StringUtils.repeat('\t', level)
                    + ((org.w3c.dom.Node) node).getNodeName();

            // use the sub function to extract node attributes
            data += showNode.displayNodeAttributes((org.w3c.dom.Node) node);

            // if node has a text value, display the text
            if (_xmlr.getNodeText((org.w3c.dom.Node) node) != null) {
                data += " (TEXT=" + _xmlr.getNodeText((org.w3c.dom.Node) node)
                        + ")";
            }

            // display all collected data to the user output
            System.out.println(data);

            // recall the function (recursion) to see if the node has child 
            // nodes and preocess them in hierarchial level
            showConfigNodes((org.w3c.dom.Node) node, (level + 1));

            // yield processing to other threads
            Thread.yield();
        }
    }
    // </editor-fold>
}
