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
package org.apache.taverna.gis.client.impl;

import java.util.List;
import org.n52.wps.client.AbstractClientGETRequest;

public class D4scienceDescribeProcessRequest extends AbstractClientGETRequest {

    private static final String IDENTIFIER_REQ_PARAM_NAME = "identifier";
    private static final String REQUEST_REQ_PARAM_VALUE = "DescribeProcess";

    D4scienceDescribeProcessRequest() {
        super();
        setRequestParamValue(REQUEST_REQ_PARAM_VALUE);
    }

    public void setIdentifier(List<String> ids) {
        String idsString = "";
        for (int i = 0; i < ids.size(); i++) {
            idsString = idsString + ids.get(i);
            if (i != ids.size() - 1) {
                idsString = idsString + ",";
            }
        }
        requestParams.put(IDENTIFIER_REQ_PARAM_NAME, idsString);
    }

    @Override
    public boolean valid() {
        return true;
    }

}
