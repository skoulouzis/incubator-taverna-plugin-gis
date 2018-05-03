/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.taverna.gis.client.impl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.opengis.ows.x11.CodeType;
import net.opengis.wps.x100.ComplexDataDescriptionType;
import net.opengis.wps.x100.ComplexDataType;
import net.opengis.wps.x100.ExecuteDocument;
import net.opengis.wps.x100.ExecuteResponseDocument;
import net.opengis.wps.x100.InputDescriptionType;
import net.opengis.wps.x100.InputReferenceType;
import net.opengis.wps.x100.InputType;
import net.opengis.wps.x100.LiteralDataType;
import net.opengis.wps.x100.ProcessDescriptionType;
import org.apache.commons.lang.StringUtils;
import org.geotools.feature.FeatureCollection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.n52.wps.client.ExecuteRequestBuilder;
import org.n52.wps.client.ExecuteResponseAnalyser;
import org.n52.wps.io.IParser;
import org.n52.wps.io.ParserFactory;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.datahandler.parser.GML2BasicParser;
import org.n52.wps.io.datahandler.parser.GML3BasicParser;
import org.n52.wps.io.datahandler.parser.SimpleGMLParser;

import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.RepositoryManager;
import org.n52.wps.server.request.strategy.ReferenceStrategyRegister;

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

//    /**
//     * Test of connect method, of class D4scienceWPSClientSession.
//     *
//     * @throws java.lang.Exception
//     */
//    @Test
//    public void testConnect() throws Exception {
//        System.out.println("connect");
//        boolean expResult = true;
//        boolean result = CLIENT.connect(URL);
////        assertEquals(expResult, result);
//    }
    @Test
    public void testExecute() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put(
                "input_file_name",
                "http://data.d4science.org/cUx5QkJ3bit6U0prekxKNzI3K25pVWw5Um96M3Qzc2NHbWJQNStIS0N6Yz0");

        ProcessDescriptionType describeProcessDocument = requestDescribeProcess(URL, processID);

        IData data = executeProcess(URL, describeProcessDocument, inputs);
//        InputType input = getInputs(inputs, describeProcessDocument);

        Map<String, List<IData>> inputData = null;
//        handleComplexValueReference(input, describeProcessDocument, SERVICE_ENDPOINT, inputData);

    }

    private ProcessDescriptionType requestDescribeProcess(String url,
            String processID) throws IOException {

        D4scienceWPSClientSession wpsClient = D4scienceWPSClientSession.getInstance();

        ProcessDescriptionType processDescription = wpsClient
                .getProcessDescription(url, processID);

        InputDescriptionType[] inputList = processDescription.getDataInputs()
                .getInputArray();

        for (InputDescriptionType input : inputList) {
            System.out.println(input.getIdentifier().getStringValue());
        }
        return processDescription;
    }

    private IData executeProcess(String url, ProcessDescriptionType processDescription,
            Map<String, Object> inputs) throws Exception {
        ExecuteRequestBuilder executeBuilder = new ExecuteRequestBuilder(processDescription);

        for (InputDescriptionType input : processDescription.getDataInputs()
                .getInputArray()) {
            String inputName = input.getIdentifier().getStringValue();
            Object inputValue = inputs.get(inputName);
            if (input.getLiteralData() != null) {
                if (inputValue instanceof String) {
                    executeBuilder.addLiteralData(inputName,
                            (String) inputValue);
                }
            } else if (input.getComplexData() != null) {
                // Complexdata by value
                if (inputValue instanceof FeatureCollection) {
                    IData data = new GTVectorDataBinding(
                            (FeatureCollection) inputValue);
                    executeBuilder
                            .addComplexData(
                                    inputName,
                                    data,
                                    "http://schemas.opengis.net/gml/3.1.1/base/feature.xsd",
                                    "UTF-8", "text/xml");
                }
                // Complexdata Reference
                if (inputValue instanceof String) {
                    executeBuilder
                            .addComplexDataReference(
                                    inputName,
                                    (String) inputValue,
                                    null,
                                    null, null);
                }

                if (inputValue == null && input.getMinOccurs().intValue() > 0) {
                    throw new IOException("Property not set, but mandatory: "
                            + inputName);
                }
            }
        }
        executeBuilder.setMimeTypeForOutput("text/xml", "non_deterministic_output");
        executeBuilder.setSchemaForOutput(
                "http://schemas.opengis.net/gml/3.1.1/base/feature.xsd",
                "non_deterministic_output");
        ExecuteDocument execute = executeBuilder.getExecute();
        System.err.println(execute);
        execute.getExecute().setService("WPS");
        D4scienceWPSClientSession wpsClient = D4scienceWPSClientSession.getInstance();
        Object responseObject = wpsClient.execute(url, execute);
        if (responseObject instanceof ExecuteResponseDocument) {
            ExecuteResponseDocument response = (ExecuteResponseDocument) responseObject;
            ExecuteResponseAnalyser analyser = new ExecuteResponseAnalyser(
                    execute, response, processDescription);
            IData data = (IData) analyser.getComplexDataByIndex(0,
                    GTVectorDataBinding.class);
            return data;
        }
//        throw new Exception("Exception: " + responseObject.toString());
        return null;
    }

    private void handleComplexValueReference(InputType input, ProcessDescriptionType processDesc,
            String algorithmIdentifier, Map<String, List<IData>> inputData) throws ExceptionReport {
        String inputID = input.getIdentifier().getStringValue();

        ReferenceStrategyRegister register = ReferenceStrategyRegister.getInstance();
//        ReferenceInputStream stream = register.resolveReference(input);

//        String dataURLString = input.getReference().getHref();
        /**
         * initialize data format with default values defaults and overwrite
         * with defaults from request if applicable
         */
        InputDescriptionType inputPD = null;
        for (InputDescriptionType tempDesc : processDesc.getDataInputs().getInputArray()) {
            if (inputID.equals(tempDesc.getIdentifier().getStringValue())) {
                inputPD = tempDesc;
                break;
            }
        }
        if (inputPD == null) { // check if there is a corresponding input identifier in the process description

            throw new RuntimeException("Input cannot be found in description for " + processDesc.getIdentifier().getStringValue() + "," + inputID);
        }

        //select parser
        //1. mimeType set?
        //yes--> set it
        //1.1 schema/encoding set?
        //yes-->set it
        //not-->set default values for parser with matching mime type
        //no--> look in http stream
        //2. mimeType set in http stream
        //yes -->set it
        //2.1 schema/encoding set?
        //yes-->set it
        //not-->set default values for parser with matching mime type
        //no--> schema or/and encoding are set?
        //yes-->use it, look if only one mime type can be found
        //not-->use default values
        String schema = null;
        String mimeType = null;
        String encoding = null;

        // overwrite with data format from request if appropriate
        InputReferenceType referenceData = input.getReference();

        if (referenceData.isSetMimeType() && referenceData.getMimeType() != null) {
            //mime type in request
            mimeType = referenceData.getMimeType();
            ComplexDataDescriptionType format = null;

            String defaultMimeType = inputPD.getComplexData().getDefault().getFormat().getMimeType();

            boolean canUseDefault = false;
            if (defaultMimeType.equalsIgnoreCase(mimeType)) {
                ComplexDataDescriptionType potentialFormat = inputPD.getComplexData().getDefault().getFormat();
                if (referenceData.getSchema() != null && referenceData.getEncoding() == null) {
                    if (referenceData.getSchema().equalsIgnoreCase(potentialFormat.getSchema())) {
                        canUseDefault = true;
                        format = potentialFormat;
                    }
                }
                if (referenceData.getSchema() == null && referenceData.getEncoding() != null) {
                    if (referenceData.getEncoding().equalsIgnoreCase(potentialFormat.getEncoding())) {
                        canUseDefault = true;
                        format = potentialFormat;
                    }

                }
                if (referenceData.getSchema() != null && referenceData.getEncoding() != null) {
                    if (referenceData.getSchema().equalsIgnoreCase(potentialFormat.getSchema()) && referenceData.getEncoding().equalsIgnoreCase(potentialFormat.getEncoding())) {
                        canUseDefault = true;
                        format = potentialFormat;
                    }

                }
                if (referenceData.getSchema() == null && referenceData.getEncoding() == null) {
                    canUseDefault = true;
                    format = potentialFormat;
                }

            }
            if (!canUseDefault) {
                ComplexDataDescriptionType[] formats = inputPD.getComplexData().getSupported().getFormatArray();
                for (ComplexDataDescriptionType potentialFormat : formats) {
                    if (potentialFormat.getMimeType().equalsIgnoreCase(mimeType)) {
                        if (referenceData.getSchema() != null && referenceData.getEncoding() == null) {
                            if (referenceData.getSchema().equalsIgnoreCase(potentialFormat.getSchema())) {
                                format = potentialFormat;
                            }
                        }
                        if (referenceData.getSchema() == null && referenceData.getEncoding() != null) {
                            if (referenceData.getEncoding().equalsIgnoreCase(potentialFormat.getEncoding())) {
                                format = potentialFormat;
                            }

                        }
                        if (referenceData.getSchema() != null && referenceData.getEncoding() != null) {
                            if (referenceData.getSchema().equalsIgnoreCase(potentialFormat.getSchema()) && referenceData.getEncoding().equalsIgnoreCase(potentialFormat.getEncoding())) {
                                format = potentialFormat;
                            }

                        }
                        if (referenceData.getSchema() == null && referenceData.getEncoding() == null) {
                            format = potentialFormat;
                        }
                    }
                }
            }
            if (format == null) {
                throw new ExceptionReport("Possibly multiple or none matching generators found for the input data with id = \"" + inputPD.getIdentifier().getStringValue() + "\". Is the MimeType (\"" + referenceData.getMimeType() + "\") correctly set?", ExceptionReport.INVALID_PARAMETER_VALUE);
                //throw new ExceptionReport("Could not determine format of the input data (id= \"" + inputPD.getIdentifier().getStringValue() + "\"), given the mimetype \"" + referenceData.getMimeType() + "\"", ExceptionReport.INVALID_PARAMETER_VALUE);

            }

            mimeType = format.getMimeType();

            if (format.isSetEncoding()) {
                //no encoding provided--> select default one for mimeType
                encoding = format.getEncoding();
            }

            if (format.isSetSchema()) {
                //no encoding provided--> select default one for mimeType
                schema = format.getSchema();
            }

        } else {
            // mimeType not in request, fetch mimetype from reference response			
            mimeType = "GML2";
            if (mimeType.contains("GML2")) {
                mimeType = "text/xml; subtype=gml/2.0.0";
            }
            if (mimeType.contains("GML3")) {
                mimeType = "text/xml; subtype=gml/3.0.0";
            }
            ComplexDataDescriptionType format = null;

            if (mimeType != null) {
                String defaultMimeType = inputPD.getComplexData().getDefault().getFormat().getMimeType();

                boolean canUseDefault = false;
                if (defaultMimeType.equalsIgnoreCase(mimeType)) {
                    ComplexDataDescriptionType potentialFormat = inputPD.getComplexData().getDefault().getFormat();
                    if (referenceData.getSchema() != null && referenceData.getEncoding() == null) {
                        if (referenceData.getSchema().equalsIgnoreCase(potentialFormat.getSchema())) {
                            canUseDefault = true;
                            format = potentialFormat;
                        }
                    }
                    if (referenceData.getSchema() == null && referenceData.getEncoding() != null) {
                        if (referenceData.getEncoding().equalsIgnoreCase(potentialFormat.getEncoding())) {
                            canUseDefault = true;
                            format = potentialFormat;
                        }

                    }
                    if (referenceData.getSchema() != null && referenceData.getEncoding() != null) {
                        if (referenceData.getSchema().equalsIgnoreCase(potentialFormat.getSchema()) && referenceData.getEncoding().equalsIgnoreCase(potentialFormat.getEncoding())) {
                            canUseDefault = true;
                            format = potentialFormat;
                        }

                    }
                    if (referenceData.getSchema() == null && referenceData.getEncoding() == null) {
                        canUseDefault = true;
                        format = potentialFormat;
                    }

                }
                if (!canUseDefault) {
                    ComplexDataDescriptionType[] formats = inputPD.getComplexData().getSupported().getFormatArray();
                    for (ComplexDataDescriptionType potentialFormat : formats) {
                        if (!StringUtils.isBlank(potentialFormat.getMimeType()) && potentialFormat.getMimeType().equalsIgnoreCase(mimeType)) {
                            if (referenceData.getSchema() != null && referenceData.getEncoding() == null) {
                                if (referenceData.getSchema().equalsIgnoreCase(potentialFormat.getSchema())) {
                                    format = potentialFormat;
                                }
                            }
                            if (referenceData.getSchema() == null && referenceData.getEncoding() != null) {
                                if (referenceData.getEncoding().equalsIgnoreCase(potentialFormat.getEncoding())) {
                                    format = potentialFormat;
                                }

                            }
                            if (referenceData.getSchema() != null && referenceData.getEncoding() != null) {
                                if (referenceData.getSchema().equalsIgnoreCase(potentialFormat.getSchema()) && referenceData.getEncoding().equalsIgnoreCase(potentialFormat.getEncoding())) {
                                    format = potentialFormat;
                                }

                            }
                            if (referenceData.getSchema() == null && referenceData.getEncoding() == null) {
                                format = potentialFormat;
                            }
                        }
                    }
                }
                if (format == null) {
                    //throw new ExceptionReport("Could not determine intput format. Possibly multiple or none matching generators found. MimeType Set?", ExceptionReport.INVALID_PARAMETER_VALUE);
                    // TODO Review error message
                    throw new ExceptionReport("Could not determine input format because none of the supported formats match the given schema (\"" + referenceData.getSchema() + "\") and encoding (\"" + referenceData.getEncoding() + "\"). (A mimetype was not specified)", ExceptionReport.INVALID_PARAMETER_VALUE);

                }

                mimeType = format.getMimeType();

                if (format.isSetEncoding()) {
                    //no encoding provided--> select default one for mimeType
                    encoding = format.getEncoding();
                }

                if (format.isSetSchema()) {
                    //no encoding provided--> select default one for mimeType
                    schema = format.getSchema();
                }
            }

            if (mimeType == null && !referenceData.isSetEncoding() && !referenceData.isSetSchema()) {
                //nothing set, use default values
                schema = inputPD.getComplexData().getDefault().getFormat().getSchema();
                mimeType = inputPD.getComplexData().getDefault().getFormat().getMimeType();
                encoding = inputPD.getComplexData().getDefault().getFormat().getEncoding();

            } else //do a smart search an look if a mimeType can be found for either schema and/or encoding
             if (mimeType == null) {
                    if (referenceData.isSetEncoding() && !referenceData.isSetSchema()) {
                        //encoding set only
                        ComplexDataDescriptionType encodingFormat = null;
                        String defaultEncoding = inputPD.getComplexData().getDefault().getFormat().getEncoding();
                        int found = 0;
                        String foundEncoding = null;
                        if (defaultEncoding.equalsIgnoreCase(referenceData.getEncoding())) {
                            foundEncoding = inputPD.getComplexData().getDefault().getFormat().getEncoding();
                            encodingFormat = inputPD.getComplexData().getDefault().getFormat();
                            found = found + 1;
                        } else {
                            ComplexDataDescriptionType[] formats = inputPD.getComplexData().getSupported().getFormatArray();
                            for (ComplexDataDescriptionType tempFormat : formats) {
                                if (tempFormat.getEncoding().equalsIgnoreCase(referenceData.getEncoding())) {
                                    foundEncoding = tempFormat.getEncoding();
                                    encodingFormat = tempFormat;
                                    found = found + 1;
                                }
                            }
                        }

                        if (found == 1) {
                            encoding = foundEncoding;
                            mimeType = encodingFormat.getMimeType();
                            if (encodingFormat.isSetSchema()) {
                                schema = encodingFormat.getSchema();
                            }
                        } else {
                            throw new ExceptionReport("Request incomplete. Could not determine a suitable input format based on the given input [mime Type missing and given encoding not unique]", ExceptionReport.MISSING_PARAMETER_VALUE);
                        }

                    }
                    if (referenceData.isSetSchema() && !referenceData.isSetEncoding()) {
                        //schema set only
                        ComplexDataDescriptionType schemaFormat = null;
                        String defaultSchema = inputPD.getComplexData().getDefault().getFormat().getSchema();
                        int found = 0;
                        String foundSchema = null;
                        if (defaultSchema.equalsIgnoreCase(referenceData.getSchema())) {
                            foundSchema = inputPD.getComplexData().getDefault().getFormat().getSchema();
                            schemaFormat = inputPD.getComplexData().getDefault().getFormat();
                            found = found + 1;
                        } else {
                            ComplexDataDescriptionType[] formats = inputPD.getComplexData().getSupported().getFormatArray();
                            for (ComplexDataDescriptionType tempFormat : formats) {
                                if (tempFormat.getEncoding().equalsIgnoreCase(referenceData.getSchema())) {
                                    foundSchema = tempFormat.getSchema();
                                    schemaFormat = tempFormat;
                                    found = found + 1;
                                }
                            }
                        }

                        if (found == 1) {
                            schema = foundSchema;
                            mimeType = schemaFormat.getMimeType();
                            if (schemaFormat.isSetEncoding()) {
                                encoding = schemaFormat.getEncoding();
                            }
                        } else {
                            throw new ExceptionReport("Request incomplete. Could not determine a suitable input format based on the given input [mime Type missing and given schema not unique]", ExceptionReport.MISSING_PARAMETER_VALUE);
                        }

                    }
                    if (referenceData.isSetEncoding() && referenceData.isSetSchema()) {
                        //schema and encoding set

                        //encoding
                        String defaultEncoding = inputPD.getComplexData().getDefault().getFormat().getEncoding();

                        List<ComplexDataDescriptionType> foundEncodingList = new ArrayList<ComplexDataDescriptionType>();
                        if (defaultEncoding.equalsIgnoreCase(referenceData.getEncoding())) {
                            foundEncodingList.add(inputPD.getComplexData().getDefault().getFormat());

                        } else {
                            ComplexDataDescriptionType[] formats = inputPD.getComplexData().getSupported().getFormatArray();
                            for (ComplexDataDescriptionType tempFormat : formats) {
                                if (tempFormat.getEncoding().equalsIgnoreCase(referenceData.getEncoding())) {
                                    foundEncodingList.add(tempFormat);
                                }
                            }

                            //schema
                            List<ComplexDataDescriptionType> foundSchemaList = new ArrayList<>();
                            String defaultSchema = inputPD.getComplexData().getDefault().getFormat().getSchema();
                            if (defaultSchema.equalsIgnoreCase(referenceData.getSchema())) {
                                foundSchemaList.add(inputPD.getComplexData().getDefault().getFormat());
                            } else {
                                formats = inputPD.getComplexData().getSupported().getFormatArray();
                                for (ComplexDataDescriptionType tempFormat : formats) {
                                    if (tempFormat.getEncoding().equalsIgnoreCase(referenceData.getSchema())) {
                                        foundSchemaList.add(tempFormat);
                                    }
                                }
                            }

                            //results
                            ComplexDataDescriptionType foundCommonFormat = null;
                            for (ComplexDataDescriptionType encodingFormat : foundEncodingList) {
                                for (ComplexDataDescriptionType schemaFormat : foundSchemaList) {
                                    if (encodingFormat.equals(schemaFormat)) {
                                        foundCommonFormat = encodingFormat;
                                    }
                                }

                            }

                            if (foundCommonFormat != null) {
                                mimeType = foundCommonFormat.getMimeType();
                                if (foundCommonFormat.isSetEncoding()) {
                                    encoding = foundCommonFormat.getEncoding();
                                }
                                if (foundCommonFormat.isSetSchema()) {
                                    schema = foundCommonFormat.getSchema();
                                }
                            } else {
                                throw new ExceptionReport("Request incomplete. Could not determine a suitable input format based on the given input [mime Type missing and given encoding and schema are not unique]", ExceptionReport.MISSING_PARAMETER_VALUE);
                            }

                        }

                    }

                }

        }

        IParser parser = null;
        try {
            Class<?> algorithmInputClass = RepositoryManager.getInstance().getInputDataTypeForAlgorithm(algorithmIdentifier, inputID);
            if (algorithmInputClass == null) {
                throw new RuntimeException("Could not determine internal input class for input" + inputID);
            }

            parser = ParserFactory.getInstance().getParser(schema, mimeType, encoding, algorithmInputClass);

            if (parser == null) {
                //throw new ExceptionReport("Error. No applicable parser found for " + schema + "," + mimeType + "," + encoding, ExceptionReport.NO_APPLICABLE_CODE);
                throw new ExceptionReport("Error. No applicable parser found for schema=\"" + schema + "\", mimeType=\"" + mimeType + "\", encoding=\"" + encoding + "\"", ExceptionReport.NO_APPLICABLE_CODE);

            }
        } catch (RuntimeException e) {
            throw new ExceptionReport("Error obtaining input data", ExceptionReport.NO_APPLICABLE_CODE, e);
        }

        /**
         * **PROXY****
         */
        /*String decodedURL = URLDecoder.decode(dataURLString);
			decodedURL = decodedURL.replace("&amp;", "&");
			if(decodedURL.indexOf("&BBOX")==-1){
				decodedURL = decodedURL.replace("BBOX", "&BBOX");
				decodedURL = decodedURL.replace("outputFormat", "&outputFormat");
				decodedURL = decodedURL.replace("SRS", "&SRS");
				decodedURL = decodedURL.replace("REQUEST", "&REQUEST");
				decodedURL = decodedURL.replace("VERSION", "&VERSION");
				decodedURL = decodedURL.replace("SERVICE", "&SERVICE");
				decodedURL = decodedURL.replace("format", "&format");
			}*/
        //lookup WFS
//        if (dataURLString.toUpperCase().contains("REQUEST=GETFEATURE")
//                && dataURLString.toUpperCase().contains("SERVICE=WFS")) {
//            if (parser instanceof SimpleGMLParser) {
//                parser = new GML2BasicParser();
//            }
//            if (parser instanceof GML2BasicParser && !dataURLString.toUpperCase().contains("OUTPUTFORMAT=GML2")) {
//                //make sure we get GML2
//                dataURLString = dataURLString + "&outputFormat=GML2";
//            }
//            if (parser instanceof GML3BasicParser && !dataURLString.toUpperCase().contains("OUTPUTFORMAT=GML3")) {
//                //make sure we get GML3
//                dataURLString = dataURLString + "&outputFormat=GML3";
//            }
//        }
//        IData parsedInputData = parser.parse(stream, mimeType, schema);
        //enable maxxoccurs of parameters with the same name.
//        if (inputData.containsKey(inputID)) {
//            List<IData> list = inputData.get(inputID);
//            list.add(parsedInputData);
//            inputData.put(inputID, list);
//        } else {
//            List<IData> list = new ArrayList<IData>();
//            list.add(parsedInputData);
//            inputData.put(inputID, list);
//        }
    }

    private InputType getInputs(Map<String, Object> inputs, ProcessDescriptionType describeProcessDocument) throws IOException {
        InputType inputType = InputType.Factory.newInstance();
        for (InputDescriptionType input : describeProcessDocument.getDataInputs()
                .getInputArray()) {
            String inputName = input.getIdentifier().getStringValue();
            Object inputValue = inputs.get(inputName);
            if (input.getLiteralData() != null) {
                if (inputValue instanceof String) {
                    LiteralDataType literalData = LiteralDataType.Factory.newInstance();
                    inputType.addNewData().setLiteralData(literalData);
//                    executeBuilder.addLiteralData(inputName,
//                            (String) inputValue);
                }
            } else if (input.getComplexData() != null) {
                // Complexdata by value
                if (inputValue instanceof FeatureCollection) {
                    IData data = new GTVectorDataBinding(
                            (FeatureCollection) inputValue);
                    ComplexDataType complexData = ComplexDataType.Factory.newInstance();

                    inputType.addNewData().setComplexData(complexData);
//                    executeBuilder
//                            .addComplexData(
//                                    inputName,
//                                    data,
//                                    "http://schemas.opengis.net/gml/3.1.1/base/feature.xsd",
//                                    "UTF-8", "text/xml");
                }
                // Complexdata Reference
                if (inputValue instanceof String) {
                    ComplexDataType complexData = ComplexDataType.Factory.newInstance();
                    inputType.addNewData().setComplexData(complexData);
//                    executeBuilder
//                            .addComplexDataReference(
//                                    inputName,
//                                    (String) inputValue,
//                                    "http://schemas.opengis.net/gml/3.1.1/base/feature.xsd",
//                                    "UTF-8", "text/xml");
                }

                if (inputValue == null && input.getMinOccurs().intValue() > 0) {
                    throw new IOException("Property not set, but mandatory: "
                            + inputName);
                }
            }
        }
        CodeType identifier = CodeType.Factory.newInstance();
        identifier.setStringValue("input_file_name");
        inputType.setIdentifier(identifier);
        InputReferenceType reference = InputReferenceType.Factory.newInstance();
        reference.setMimeType("text/xml");
        inputType.setReference(reference);
        return inputType;
    }

}
