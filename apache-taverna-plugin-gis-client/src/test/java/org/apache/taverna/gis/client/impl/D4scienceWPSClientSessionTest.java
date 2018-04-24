/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.taverna.gis.client.impl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author S. Koulouzis
 */
public class D4scienceWPSClientSessionTest {

    private static D4scienceWPSClientSession CLIENT;
    private static String TOKEN;
    private static String SERVICE_ENDPOINT;
    private static String URL;

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
        assertEquals(expResult, result);
    }
}
