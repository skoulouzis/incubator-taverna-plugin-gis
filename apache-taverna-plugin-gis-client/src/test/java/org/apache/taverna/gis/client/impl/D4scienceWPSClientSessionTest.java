/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.taverna.gis.client.impl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.opengis.ows.x11.MetadataType;
import net.opengis.wps.x100.CapabilitiesDocument;
import net.opengis.wps.x100.ExecuteDocument;
import net.opengis.wps.x100.InputDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionsDocument;
import org.apache.taverna.gis.client.IPortDataDescriptor;
import org.geotools.feature.FeatureCollection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.n52.wps.client.ExecuteRequestBuilder;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;

/**
 *
 * @author S. Koulouzis
 */
public class D4scienceWPSClientSessionTest {

    private static D4scienceWPSClientSession CLIENT;
    private static String TOKEN;
    private static String SERVICE_ENDPOINT;
    private static String URL;
    private static String processID;

    public D4scienceWPSClientSessionTest() {
    }

    @BeforeClass
    public static void setUpClass() throws IOException {
        Properties p = new Properties();
        p.load(new FileReader(new File("test.properties")));
        TOKEN = p.getProperty("gcube-token");
        SERVICE_ENDPOINT = p.getProperty("service-endpoint");
        if (!SERVICE_ENDPOINT.endsWith("?")) {
            SERVICE_ENDPOINT += "?";
        }
        URL = SERVICE_ENDPOINT + "gcube-token=" + TOKEN + "&";
        CLIENT = D4scienceWPSClientSession.getInstance();
        processID = p.getProperty("process-identifier");
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws IOException {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of connect method, of class D4scienceWPSClientSession.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testConnect() throws Exception {
        System.out.println("connect");
        boolean expResult = true;
        boolean result = CLIENT.connect(URL);
//        assertEquals(expResult, result);
    }

    @Test
    public void testExecute() throws Exception {
        boolean result = CLIENT.connect(URL);
        ProcessDescriptionType processDescription = CLIENT.getProcessDescription(URL, processID);
        ExecuteRequestBuilder executeBuilder = new ExecuteRequestBuilder(processDescription);
        Map<String, IPortDataDescriptor> inputs = new HashMap<>();
        
        ProcessDescriptionType.DataInputs dataInputs = processDescription.getDataInputs();

        for (InputDescriptionType input : processDescription.getDataInputs().getInputArray()) {
            String inputName = input.getIdentifier().getStringValue();
//            Object inputValue = inputs.get(inputName);
//            if (input.getLiteralData() != null) {
//                if (inputValue instanceof String) {
//                    executeBuilder.addLiteralData(inputName, (String) inputValue);
//                }
//            } else if (input.getComplexData() != null) {
//                if (inputValue instanceof FeatureCollection) {
//                    IData data = new GTVectorDataBinding((FeatureCollection) inputValue);
//                    executeBuilder.addComplexData(inputName, data,
//                            "http://schemas.opengis.net/gml/3.1.1/base/feature.xsd",
//                            "UTF-8", "text/xml");
//                }
//            }
//
        }
//        ExecuteDocument execute = executeBuilder.getExecute();
//        System.err.println(execute);
    }
}
