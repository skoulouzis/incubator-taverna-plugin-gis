package org.apache.taverna.gis.client;

import org.apache.taverna.gis.client.impl.GisClientNorthImpl;

public class GisClientFactory {

    private static final GisClientFactory INSTANCE = null;

    private GisClientFactory() {
        // private constructor
    }

    public static GisClientFactory getInstance() {
        if (INSTANCE == null) {
            return new GisClientFactory();
        } else {
            return INSTANCE;
        }

    }

    public IGisClient getGisClient(String serviceURL) {
        return new GisClientNorthImpl(serviceURL);
    }

}
