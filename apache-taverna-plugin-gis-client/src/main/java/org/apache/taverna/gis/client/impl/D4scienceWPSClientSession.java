/**
 * ﻿Copyright (C) 2007 - 2016 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.taverna.gis.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.opengis.ows.x11.ExceptionReportDocument;
import net.opengis.ows.x11.OperationDocument.Operation;
import net.opengis.wps.x100.CapabilitiesDocument;
import net.opengis.wps.x100.ExecuteDocument;
import net.opengis.wps.x100.ExecuteResponseDocument;
import net.opengis.wps.x100.ProcessBriefType;
import net.opengis.wps.x100.ProcessDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionsDocument;
import net.opengis.wps.x100.impl.ProcessDescriptionTypeImpl;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import org.n52.wps.client.ClientCapabiltiesRequest;
import org.n52.wps.client.WPSClientException;

/**
 * Contains some convenient methods to access and manage WebProcessingSerivces
 * in a very generic way.
 *
 * This is implemented as a singleton.
 *
 * @author foerster
 */
public class D4scienceWPSClientSession {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(D4scienceWPSClientSession.class);
    private static final String OGC_OWS_URI = "http://www.opengeospatial.net/ows";
    private static final String SUPPORTED_VERSION = "1.0.0";
    
    private static D4scienceWPSClientSession session;
    private final Map<String, CapabilitiesDocument> loggedServices;
    private XmlOptions options = null;
    private boolean useConnectURL = false;
    private String connectURL;
    private String securityToken;
    private static final String SECURITY_TOCKEN_NAME = "gcube-token";

    // a Map of <url, all available process descriptions>
    private final Map<String, ProcessDescriptionsDocument> processDescriptionsCache;

    /**
     * Initializes a WPS client session.
     *
     */
    private D4scienceWPSClientSession() {
        options = new XmlOptions();
        options.setLoadStripWhitespace();
        options.setLoadTrimTextBuffer();
        loggedServices = new HashMap<>();
        processDescriptionsCache = new HashMap<>();
    }

    /*
	 * @result An instance of a WPS Client session.
     */
    public static D4scienceWPSClientSession getInstance() {
        if (session == null) {
            session = new D4scienceWPSClientSession();
        }
        return session;
    }

    /**
     * This resets the D4scienceWPSClientSession. This might be necessary, to
     * get rid of old service entries/descriptions. However, the session has to
     * be repopulated afterwards.
     */
    public static void reset() {
        session = new D4scienceWPSClientSession();
    }

    /**
     * Connects to a WPS and retrieves Capabilities plus puts all available
     * Descriptions into cache.
     *
     * @param url the entry point for the service. This is used as id for
     * further identification of the service.
     * @return true, if connect succeeded, false else.
     * @throws WPSClientException
     * @throws java.io.UnsupportedEncodingException
     * @throws java.net.MalformedURLException
     */
    public boolean connect(String url) throws WPSClientException, UnsupportedEncodingException, MalformedURLException {
        return connect(url, false);
    }

    /**
     * Connects to a WPS and retrieves Capabilities plus puts all available
     * Descriptions into cache.
     *
     * @param url the entry point for the service. This is used as id for
     * further identification of the service.
     * @param useConnectURL use the URL that was passed for communication with
     * the WPS and not the URLs from the Operations Metadata
     * @return true, if connect succeeded, false else.
     * @throws WPSClientException
     * @throws java.io.UnsupportedEncodingException
     * @throws java.net.MalformedURLException
     */
    public boolean connect(String url, boolean useConnectURL) throws WPSClientException, UnsupportedEncodingException, MalformedURLException {
        LOGGER.info("Connecting to: " + url);
        this.useConnectURL = useConnectURL;
        this.connectURL = url;
        if (securityToken == null) {
            Map<String, String> map = getUrlParameterMap(new URL(url));
            if (map != null) {
                securityToken = map.get(SECURITY_TOCKEN_NAME);
            }
        }
        
        if (useConnectURL) {
            LOGGER.info("Use connect URL for further communication: " + connectURL);
        }
        if (loggedServices.containsKey(url)) {
            LOGGER.info("Service already registered: " + url);
            return false;
        }
        CapabilitiesDocument capsDoc = retrieveCapsViaGET(url);
        if (capsDoc != null) {
            LOGGER.debug("Got capabilities document");
            loggedServices.put(url, capsDoc);
        }
        ProcessDescriptionsDocument processDescs = describeAllProcesses(url);
        if (processDescs != null && capsDoc != null) {
            LOGGER.debug("Got process description document");
            processDescriptionsCache.put(url, processDescs);
            return true;
        }
        LOGGER.warn("retrieving caps failed, caps are null");
        return false;
    }

    /**
     * removes a service from the session
     *
     * @param url
     */
    public void disconnect(String url) {
        if (loggedServices.containsKey(url)) {
            loggedServices.remove(url);
            processDescriptionsCache.remove(url);
            LOGGER.info("service removed successfully: " + url);
        }
    }

    /**
     * returns the serverIDs of all loggedServices
     *
     * @return
     */
    public List<String> getLoggedServices() {
        return new ArrayList<>(loggedServices.keySet());
    }

    /**
     * informs you if the descriptions for the specified service is already in
     * the session. in normal case it should return true :)
     *
     * @param serverID
     * @return success
     */
    public boolean descriptionsAvailableInCache(String serverID) {
        return processDescriptionsCache.containsKey(serverID);
    }

    /**
     * returns the cached processdescriptions of a service.
     *
     * @param serverID
     * @return success
     * @throws IOException
     */
    private ProcessDescriptionsDocument getProcessDescriptionsFromCache(String wpsUrl) throws IOException {
        if (!descriptionsAvailableInCache(wpsUrl)) {
            try {
                connect(wpsUrl);
            } catch (WPSClientException e) {
                throw new IOException("Could not initialize WPS " + wpsUrl);
            }
        }
        return processDescriptionsCache.get(wpsUrl);
    }

    /**
     * return the processDescription for a specific process from Cache.
     *
     * @param serverID
     * @param processID
     * @return a ProcessDescription for a specific process from Cache.
     * @throws IOException
     */
    public ProcessDescriptionType getProcessDescription(String serverID, String processID) throws IOException {
        ProcessDescriptionsDocument processDescriptionsDoc = getProcessDescriptionsFromCache(serverID);
        ProcessDescriptionsDocument.ProcessDescriptions processDescriptions = processDescriptionsDoc.getProcessDescriptions();
        ProcessDescriptionType[] processes = processDescriptions.getProcessDescriptionArray();
        for (ProcessDescriptionType process : processes) {
            if (process.getIdentifier().getStringValue().equals(processID)) {
                return process;
            }
        }
        return null;
    }

    /**
     * Delivers all ProcessDescriptions from a WPS
     *
     * @param wpsUrl the URL of the WPS
     * @return An Array of ProcessDescriptions
     * @throws IOException
     */
    public ProcessDescriptionType[] getAllProcessDescriptions(String wpsUrl) throws IOException {
        return getProcessDescriptionsFromCache(wpsUrl).getProcessDescriptions().getProcessDescriptionArray();
    }

    /**
     * looks up, if the service exists already in session.
     *
     * @param serverID
     * @return
     */
    public boolean serviceAlreadyRegistered(String serverID) {
        return loggedServices.containsKey(serverID);
    }

    /**
     * provides you the cached capabilities for a specified service.
     *
     * @param url
     * @return
     */
    public CapabilitiesDocument getWPSCaps(String url) {
        return loggedServices.get(url);
    }

    /**
     * retrieves all current available ProcessDescriptions of a WPS. Mention: to
     * get the current list of all processes, which will be requested, the
     * cached capabilities will be used. Please keep that in mind. the retrieved
     * descriptions will not be cached, so only transient information!
     *
     * @param url
     * @return
     * @throws WPSClientException
     */
    public ProcessDescriptionsDocument describeAllProcesses(String url) throws WPSClientException {
        CapabilitiesDocument doc = loggedServices.get(url);
        if (doc == null) {
            LOGGER.warn("serviceCaps are null, perhaps server does not exist");
            return null;
        }
        //Sending http://dataminer4-proto.d4science.org/wps/WebProcessingService?identifier=all&Request=DescribeProcess&Service=WPS&version=1.0.0&
        //Throws an exeption:org.n52.wps.server.ExceptionReport: Algorithm does not exist: SPATIAL_DISTRIBUTION_OF_CORRELATION...
//        String[] processIDs = new String[]{"all"};
//        return describeProcess(processIDs, url);
        return getProcessDescriptionInBatches(url, doc);
        
    }

    /**
     * retrieves the desired description for a service. the retrieved
     * information will not be held in cache!
     *
     * @param processIDs one or more processIDs
     * @param serverID
     * @return
     * @throws WPSClientException
     */
    public ProcessDescriptionsDocument describeProcess(List<String> processIDs, String serverID) throws WPSClientException {
        String url = connectURL;
        if (!useConnectURL) {
            CapabilitiesDocument caps = this.loggedServices.get(serverID);
            Operation[] operations = caps.getCapabilities().getOperationsMetadata().getOperationArray();
            
            for (Operation operation : operations) {
                if (operation.getName().equals("DescribeProcess")) {
                    url = operation.getDCPArray()[0].getHTTP().getGetArray()[0].getHref();
                    break;
                }
            }
            if (url == null) {
                throw new WPSClientException("Capabilities do not contain any information about the entry point for DescribeProcess operation.");
            }
        }
        return retrieveDescriptionViaGET(processIDs, url);
    }

    /**
     * Executes a process at a WPS
     *
     * @param url url of server not the entry additionally defined in the caps.
     * @param execute Execute document
     * @return either an ExecuteResponseDocument or an InputStream if asked for
     * RawData or an Exception Report
     */
    private Object execute(String serverID, ExecuteDocument execute, boolean rawData) throws WPSClientException {
        String url = connectURL;
        if (!useConnectURL) {
            CapabilitiesDocument caps = loggedServices.get(serverID);
            Operation[] operations = caps.getCapabilities().getOperationsMetadata().getOperationArray();
            for (Operation operation : operations) {
                if (operation.getName().equals("Execute")) {
                    url = operation.getDCPArray()[0].getHTTP().getPostArray()[0].getHref();
                    break;
                }
            }
            if (url == null) {
                throw new WPSClientException(
                        "Capabilities do not contain any information about the entry point for Execute operation.");
            }
        }
        execute.getExecute().setVersion(SUPPORTED_VERSION);
        return retrieveExecuteResponseViaPOST(url, execute, rawData);
    }

    /**
     * Executes a process at a WPS
     *
     * @param serverID
     * @param execute Execute document
     * @return either an ExecuteResponseDocument or an InputStream if asked for
     * RawData or an Exception Report
     * @throws org.n52.wps.client.WPSClientException
     */
    public Object execute(String serverID, ExecuteDocument execute) throws WPSClientException {
        if (execute.getExecute().isSetResponseForm() == true && execute.getExecute().isSetResponseForm() == true && execute.getExecute().getResponseForm().isSetRawDataOutput() == true) {
            return execute(serverID, execute, true);
        } else {
            return execute(serverID, execute, false);
        }
        
    }
    
    private CapabilitiesDocument retrieveCapsViaGET(String url) throws WPSClientException {
        ClientCapabiltiesRequest req = new ClientCapabiltiesRequest();
        url = req.getRequest(url);
        url = addSecurityTocken(url);
        LOGGER.debug("GET: " + url);
        try {
            URL urlObj = new URL(url);
            urlObj.getContent();
            InputStream is = urlObj.openStream();
            Document doc = checkInputStream(is);
            return CapabilitiesDocument.Factory.parse(doc, options);
        } catch (MalformedURLException e) {
            throw new WPSClientException("Capabilities URL seems to be unvalid: " + url, e);
        } catch (IOException e) {
            throw new WPSClientException("Error occured while retrieving capabilities from url: " + url, e);
        } catch (XmlException e) {
            throw new WPSClientException("Error occured while parsing XML", e);
        }
    }
    
    private ProcessDescriptionsDocument retrieveDescriptionViaGET(List<String> processIDs, String url) throws WPSClientException {
        D4scienceClientDescribeProcessRequest req = new D4scienceClientDescribeProcessRequest();
        req.setIdentifier(processIDs);
        String requestURL = req.getRequest(url);
        requestURL = addSecurityTocken(requestURL);
        LOGGER.debug("GET: " + requestURL);
        try {
            URL urlObj = new URL(requestURL);
            InputStream is = urlObj.openStream();
            Document doc = checkInputStream(is);
            return ProcessDescriptionsDocument.Factory.parse(doc, options);
        } catch (MalformedURLException e) {
            throw new WPSClientException("URL seems not to be valid", e);
        } catch (IOException e) {
            throw new WPSClientException("Error occured while receiving data", e);
        } catch (XmlException e) {
            throw new WPSClientException("Error occured while parsing ProcessDescription document", e);
        }
    }
    
    private InputStream retrieveDataViaPOST(XmlObject obj, String urlString) throws WPSClientException {
        try {
            urlString = addSecurityTocken(urlString);
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            LOGGER.debug("POST: " + urlString + " " + obj.toString());
            
            conn.setRequestProperty("Accept-Encoding", "gzip");
            conn.setRequestProperty("Content-Type", "text/xml");
            conn.setDoOutput(true);
            obj.save(conn.getOutputStream());
            InputStream input;
            String encoding = conn.getContentEncoding();
            if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
                input = new GZIPInputStream(conn.getInputStream());
            } else {
                input = conn.getInputStream();
            }
            return input;
        } catch (MalformedURLException e) {
            throw new WPSClientException("URL seems to be unvalid", e);
        } catch (IOException e) {
            throw new WPSClientException("Error while transmission", e);
        }
    }
    
    private Document checkInputStream(InputStream is) throws WPSClientException {
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        fac.setNamespaceAware(true);
        try {
            Document doc = fac.newDocumentBuilder().parse(is);
            if (getFirstElementNode(doc.getFirstChild()).getLocalName().equals("ExceptionReport") && getFirstElementNode(doc.getFirstChild()).getNamespaceURI().equals(OGC_OWS_URI)) {
                try {
                    ExceptionReportDocument exceptionDoc = ExceptionReportDocument.Factory.parse(doc);
                    LOGGER.debug(exceptionDoc.xmlText(options));
                    throw new WPSClientException("Error occured while executing query", exceptionDoc);
                } catch (XmlException e) {
                    throw new WPSClientException("Error while parsing ExceptionReport retrieved from server", e);
                }
            }
            return doc;
        } catch (SAXException e) {
            throw new WPSClientException("Error while parsing input.", e);
        } catch (IOException e) {
            throw new WPSClientException("Error occured while transfer", e);
        } catch (ParserConfigurationException e) {
            throw new WPSClientException("Error occured, parser is not correctly configured", e);
        }
    }
    
    private Node getFirstElementNode(Node node) {
        if (node == null) {
            return null;
        }
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            return node;
        } else {
            return getFirstElementNode(node.getNextSibling());
        }
        
    }

    /**
     * either an ExecuteResponseDocument or an InputStream if asked for RawData
     * or an Exception Report
     *
     * @param url
     * @param doc
     * @param rawData
     * @return
     * @throws WPSClientException
     */
    private Object retrieveExecuteResponseViaPOST(String url, ExecuteDocument doc, boolean rawData) throws WPSClientException {
        InputStream is = retrieveDataViaPOST(doc, url);
        if (rawData) {
            return is;
        }
        Document documentObj = checkInputStream(is);
        ExceptionReportDocument erDoc = null;
        try {
            return ExecuteResponseDocument.Factory.parse(documentObj);
        } catch (XmlException e) {
            try {
                erDoc = ExceptionReportDocument.Factory.parse(documentObj);
            } catch (XmlException e1) {
                throw new WPSClientException("Error occured while parsing executeResponse", e);
            }
            return erDoc;
        }
    }
    
    public String[] getProcessNames(String url) throws IOException {
        ProcessDescriptionType[] processes = getProcessDescriptionsFromCache(url).getProcessDescriptions().getProcessDescriptionArray();
        String[] processNames = new String[processes.length];
        for (int i = 0; i < processNames.length; i++) {
            processNames[i] = processes[i].getIdentifier().getStringValue();
        }
        return processNames;
    }

    /**
     * Executes a process at a WPS
     *
     * @param url url of server not the entry additionally defined in the caps.
     * @param executeAsGETString KVP Execute request
     * @return either an ExecuteResponseDocument or an InputStream if asked for
     * RawData or an Exception Report
     * @throws org.n52.wps.client.WPSClientException
     */
    public Object executeViaGET(String url, String executeAsGETString) throws WPSClientException {
        url = url + executeAsGETString;
        try {
            url = addSecurityTocken(url);
            URL urlObj = new URL(url);
            InputStream is = urlObj.openStream();
            
            if (executeAsGETString.toUpperCase().contains("RAWDATA")) {
                return is;
            }
            Document doc = checkInputStream(is);
            ExceptionReportDocument erDoc = null;
            try {
                return ExecuteResponseDocument.Factory.parse(doc);
            } catch (XmlException e) {
                try {
                    erDoc = ExceptionReportDocument.Factory.parse(doc);
                } catch (XmlException e1) {
                    throw new WPSClientException("Error occured while parsing executeResponse", e);
                }
                throw new WPSClientException("Error occured while parsing executeResponse", erDoc);
            }
        } catch (MalformedURLException e) {
            throw new WPSClientException("Capabilities URL seems to be unvalid: " + url, e);
        } catch (IOException e) {
            throw new WPSClientException("Error occured while retrieving capabilities from url: " + url, e);
        }
        
    }
    
    private ProcessDescriptionsDocument getProcessDescriptionInBatches(String url, CapabilitiesDocument doc) throws WPSClientException {
        ProcessBriefType[] processes = doc.getCapabilities().getProcessOfferings().getProcessArray();
        List<String> processIDs = new ArrayList<>();
        List<ProcessDescriptionType> processDescriptionsList = new ArrayList<>();
        for (int i = 0; i < processes.length; i++) {
            processIDs.add(processes[i].getIdentifier().getStringValue());
            if (i > 0 && (i % 20) == 0) {
                ProcessDescriptionsDocument.ProcessDescriptions processDescriptionsDoc = describeProcess(processIDs, url).getProcessDescriptions();
                processDescriptionsList.addAll(Arrays.asList(processDescriptionsDoc.getProcessDescriptionArray()));
                processIDs = new ArrayList<>();
            }
        }
        if (processDescriptionsList.size() < processes.length) {
            ProcessDescriptionsDocument.ProcessDescriptions processDescriptionsDoc = describeProcess(processIDs, url).getProcessDescriptions();
            processDescriptionsList.addAll(Arrays.asList(processDescriptionsDoc.getProcessDescriptionArray()));
        }
        ProcessDescriptionType[] processDescriptionsArray = new ProcessDescriptionTypeImpl[processDescriptionsList.size()];
        for (int i = 0; i < processDescriptionsList.size(); i++) {
            processDescriptionsArray[i] = processDescriptionsList.get(i);
        }
        
        ProcessDescriptionsDocument processDescriptionsDocument = ProcessDescriptionsDocument.Factory.newInstance();
        processDescriptionsDocument.addNewProcessDescriptions().setProcessDescriptionArray(processDescriptionsArray);
        
        return processDescriptionsDocument;
    }

    /**
     * Code from
     * https://stackoverflow.com/questions/13592236/parse-a-uri-string-into-name-value-collection
     *
     * @param url
     * @return
     * @throws UnsupportedEncodingException
     */
    private Map<String, String> getUrlParameterMap(URL url) throws UnsupportedEncodingException {
        String query = url.getQuery();
        if (query != null && query.length() > 1) {
            Map<String, String> query_pairs = new LinkedHashMap<>();
            
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
            }
            return query_pairs;
        }
        return null;
    }
    
    private String addSecurityTocken(String requestURL) {
        if (securityToken != null && !requestURL.contains(SECURITY_TOCKEN_NAME)) {
            if (requestURL.endsWith("&")) {
                requestURL += SECURITY_TOCKEN_NAME + "=" + securityToken + "&";
            } else {
                requestURL += "&" + SECURITY_TOCKEN_NAME + "=" + securityToken + "&";
            }
        }
        return requestURL;
    }
}
