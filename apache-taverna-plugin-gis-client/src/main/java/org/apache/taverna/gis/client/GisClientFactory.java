package org.apache.taverna.gis.client;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import org.apache.taverna.gis.client.impl.GisClientNorthImpl;
import org.n52.wps.client.WPSClientException;

public class GisClientFactory {

    private static GisClientFactory instance = null;

    private GisClientFactory() {
        // private constructor
    }

    public static GisClientFactory getInstance() {
        if (instance == null) {
            return new GisClientFactory();
        } else {
            return instance;
        }

    }

    public IGisClient getGisClient(String serviceURL) throws WPSClientException, MalformedURLException, UnsupportedEncodingException {
        return new GisClientNorthImpl(serviceURL);
    }

}
