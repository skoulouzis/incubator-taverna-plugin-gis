package org.apache.taverna.gis.client.impl;

/*
 * Copyright (C) 2007-2017 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *       • Apache License, version 2.0
 *       • Apache Software License, version 1.0
 *       • GNU Lesser General Public License, version 3
 *       • Mozilla Public License, versions 1.0, 1.1 and 2.0
 *       • Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
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

import org.n52.wps.client.WPSClientException;
import org.n52.wps.client.ClientCapabiltiesRequest;

/**
 * Contains some convenient methods to access and manage WebProcessingSerivces
 * in a very generic way.
 *
 * This is implemented as a singleton.
 *
 * @author S. Koulouzis
 */
public class D4scienceWPSClientSession {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(D4scienceWPSClientSession.class);
    private static final String OGC_OWS_URI = "http://www.opengeospatial.net/ows";
    private static final String SUPPORTED_VERSION = "1.0.0";

    private static D4scienceWPSClientSession session;
    private final Map<String, CapabilitiesDocument> capabilitiesDocumentCache;
    private XmlOptions options = null;

    // a Map of <url, all available process descriptions>
    private final Map<String, ProcessDescriptionsDocument> processDescriptionsCache;
    private String securityToken;
    private static final String SECURITY_TOCKEN_NAME = "gcube-token";

    /**
     * Initializes a WPS client session.
     *
     */
    private D4scienceWPSClientSession() {
        options = new XmlOptions();
        options.setLoadStripWhitespace();
        options.setLoadTrimTextBuffer();
        capabilitiesDocumentCache = new HashMap<>();
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
     * @throws WPSClientException if an exception occurred while trying to
     * connect
     * @throws java.io.UnsupportedEncodingException
     * @throws java.net.MalformedURLException
     */
    public boolean connect(String url) throws WPSClientException, UnsupportedEncodingException, MalformedURLException {
        LOGGER.info("CONNECT");
        if (securityToken == null) {
            Map<String, String> map = getUrlParameterMap(new URL(url));
            securityToken = map.get(SECURITY_TOCKEN_NAME);
        }
        if (capabilitiesDocumentCache.containsKey(url)) {
            LOGGER.info("Service already registered: " + url);
            return false;
        }
        CapabilitiesDocument capsDoc = retrieveCapsViaGET(url);
        if (capsDoc != null) {
            capabilitiesDocumentCache.put(url, capsDoc);
        }
        ProcessDescriptionsDocument processDescs = describeAllProcesses(url);
        if (processDescs != null && capsDoc != null) {
            processDescriptionsCache.put(url, processDescs);
            return true;
        }
        LOGGER.warn("retrieving caps failed, caps are null");
        return false;
    }

    /**
     * removes a service from the session
     *
     * @param url the url of the service that should be disconnected
     */
    public void disconnect(String url) {
        if (capabilitiesDocumentCache.containsKey(url)) {
            capabilitiesDocumentCache.remove(url);
            processDescriptionsCache.remove(url);
            LOGGER.info("service removed successfully: " + url);
        }
    }

    /**
     * returns the serverIDs of all capabilitiesDocumentCache
     *
     * @return a list of logged service URLs
     */
    public List<String> getLoggedServices() {
        return new ArrayList<>(capabilitiesDocumentCache.keySet());
    }

    /**
     * informs you if the descriptions for the specified service is already in
     * the session. in normal case it should return true :)
     *
     * @param serverID the URL of the WPS server
     * @return success if process descriptions are cached for the WPS server
     */
    public boolean descriptionsAvailableInCache(String serverID) {
        return processDescriptionsCache.containsKey(serverID);
    }

    /**
     * returns the cached processdescriptions of a service.
     *
     * @param serverID the URL of the WPS server
     * @return success if process descriptions are cached for the WPS server
     * @throws IOException if an exception occurred while trying to connect to
     * the WPS
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
     * @param serverID the URL of the WPS server
     * @param processID the id of the process
     * @return a ProcessDescription for a specific process from Cache.
     * @throws IOException if an exception occurred while trying to connect
     */
    public ProcessDescriptionType getProcessDescription(String serverID, String processID) throws IOException {
        ProcessDescriptionType[] processes = getProcessDescriptionsFromCache(serverID).getProcessDescriptions().getProcessDescriptionArray();
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
     * @throws IOException if an exception occurred while trying to connect
     */
    public ProcessDescriptionType[] getAllProcessDescriptions(String wpsUrl) throws IOException {
        return getProcessDescriptionsFromCache(wpsUrl).getProcessDescriptions().getProcessDescriptionArray();
    }

    /**
     * looks up, if the service exists already in session.
     *
     * @param serverID the URL of the WPS
     * @return true if the WPS was already connected
     */
    public boolean serviceAlreadyRegistered(String serverID) {
        return capabilitiesDocumentCache.containsKey(serverID);
    }

    /**
     * provides you the cached capabilities for a specified service.
     *
     * @param url the URL of the WPS
     * @return the <code>CapabilitiesDocument</code> of the WPS
     */
    public CapabilitiesDocument getWPSCaps(String url) {
        return capabilitiesDocumentCache.get(url);
    }

    /**
     * retrieves all current available ProcessDescriptions of a WPS. Mention: to
     * get the current list of all processes, which will be requested, the
     * cached capabilities will be used. Please keep that in mind. the retrieved
     * descriptions will not be cached, so only transient information!
     *
     * @param url the URL of the WPS
     * @return a process descriptions document containing all process
     * descriptions of this WPS
     * @throws WPSClientException if an exception occurred while trying to
     * connect
     */
    public ProcessDescriptionsDocument describeAllProcesses(String url) throws WPSClientException {
        CapabilitiesDocument doc = capabilitiesDocumentCache.get(url);
        if (doc == null) {
            LOGGER.warn("serviceCaps are null, perhaps server does not exist");
            return null;
        }
        ProcessBriefType[] processes = doc.getCapabilities().getProcessOfferings().getProcessArray();
        List<String> processIDs = new ArrayList<>();
        List<ProcessDescriptionType> processDescriptionsList = new ArrayList<>();
        for (int i = 0; i < processes.length; i++) {
            processIDs.add(processes[i].getIdentifier().getStringValue());
            if (i > 0 && (i % 70) == 0) {
                ProcessDescriptionsDocument.ProcessDescriptions processDescriptionsDoc = describeProcess(processIDs, url).getProcessDescriptions();
                processDescriptionsList.addAll(Arrays.asList(processDescriptionsDoc.getProcessDescriptionArray()));
                processIDs = new ArrayList<>();
            }
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
     * retrieves the desired description for a service. the retrieved
     * information will not be held in cache!
     *
     * @param processIDs one or more processIDs
     * @param serverID the URL of the WPS
     * @return a process descriptions document containing the process
     * descriptions for the ids
     * @throws WPSClientException if an exception occurred while trying to
     * connect
     */
    public ProcessDescriptionsDocument describeProcess(List<String> processIDs, String serverID) throws WPSClientException {
        CapabilitiesDocument caps = this.capabilitiesDocumentCache.get(serverID);
        Operation[] operations = caps.getCapabilities().getOperationsMetadata().getOperationArray();
        String url = null;
        for (Operation operation : operations) {
            if (operation.getName().equals("DescribeProcess")) {
                url = operation.getDCPArray()[0].getHTTP().getGetArray()[0].getHref();
            }
        }
        if (url == null) {
            throw new WPSClientException("Missing DescribeOperation in Capabilities");
        }
        return retrieveDescriptionViaGET(processIDs, url);
    }

    /**
     * Executes a process at a WPS
     *
     * @param serverID url of server not the entry additionally defined in the
     * caps.
     * @param execute Execute document
     * @param rawData indicates whether a output should be requested as raw data
     * (works only if just one output is requested)
     * @return either an ExecuteResponseDocument or an InputStream if asked for
     * RawData or an Exception Report
     */
    private Object execute(String serverID, ExecuteDocument execute, boolean rawData) throws WPSClientException {
        CapabilitiesDocument caps = capabilitiesDocumentCache.get(serverID);
        Operation[] operations = caps.getCapabilities().getOperationsMetadata().getOperationArray();
        String url = null;
        for (Operation operation : operations) {
            if (operation.getName().equals("Execute")) {
                url = operation.getDCPArray()[0].getHTTP().getPostArray()[0].getHref();
            }
        }
        if (url == null) {
            throw new WPSClientException("Caps does not contain any information about the entry point for process execution");
        }
        execute.getExecute().setVersion(SUPPORTED_VERSION);
        return retrieveExecuteResponseViaPOST(url, execute, rawData);
    }

    /**
     * Executes a process at a WPS
     *
     * @param serverID url of server not the entry additionally defined in the
     * caps.
     * @param execute Execute document
     * @return either an ExecuteResponseDocument or an InputStream if asked for
     * RawData or an Exception Report
     * @throws WPSClientException if an exception occurred during execute
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
        MyClientDescribeProcessRequest req = new MyClientDescribeProcessRequest();
        req.setIdentifier(processIDs);
        String requestURL = req.getRequest(url);
        requestURL = addSecurityTocken(requestURL);
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
            e.printStackTrace();
            throw new WPSClientException("Error occured while parsing ProcessDescription document", e);
        }
    }

    private InputStream retrieveDataViaPOST(XmlObject obj, String urlString) throws WPSClientException {
        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
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
     * @param url the URL of the WPS server
     * @param doc the <code>ExecuteDocument</code> that should be send to the
     * server
     * @param rawData indicates whether a output should be requested as raw data
     * (works only if just one output is requested)
     * @return either an ExecuteResponseDocument or an InputStream if asked for
     * RawData or an Exception Report
     * @throws WPSClientException if an exception occurred during execute
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
     * @throws WPSClientException if an exception occurred during execute
     */
    public Object executeViaGET(String url, String executeAsGETString) throws WPSClientException {
        url = url + executeAsGETString;
        try {
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

    /**
     * Code from
     * https://stackoverflow.com/questions/13592236/parse-a-uri-string-into-name-value-collection
     *
     * @param url
     * @return
     * @throws UnsupportedEncodingException
     */
    private Map<String, String> getUrlParameterMap(URL url) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        String query = url.getQuery();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
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
