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
import net.opengis.wps.x100.OutputDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType;
import org.apache.commons.lang.StringUtils;
import org.apache.taverna.gis.client.ComplexDataFormat;
import org.apache.taverna.gis.client.ComplexPortDataDescriptor;
import org.apache.taverna.gis.client.IPortDataDescriptor;
import org.geotools.feature.FeatureCollection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.n52.wps.client.ExecuteRequestBuilder;
import org.n52.wps.client.ExecuteResponseAnalyser;
import org.n52.wps.client.WPSClientException;
import org.n52.wps.io.IParser;
import org.n52.wps.io.ParserFactory;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;

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
    public static void setUpClass() throws IOException, WPSClientException {
        Properties p = new Properties();
        p.load(new FileReader(new File("test.properties")));
        TOKEN = p.getProperty("gcube-token");
        SERVICE_ENDPOINT = p.getProperty("service-endpoint");
        if (!SERVICE_ENDPOINT.endsWith("?")) {
            SERVICE_ENDPOINT += "?";
        }
        URL = SERVICE_ENDPOINT + "gcube-token=" + TOKEN + "&";
        CLIENT = D4scienceWPSClientSession.getInstance();
        CLIENT.connect(URL);
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

        ProcessDescriptionType processDescription = CLIENT
                .getProcessDescription(URL, processID);

        ExecuteResponseDocument resp = executeProcess(URL, processDescription, inputs);
//        InputType input = getInputs(inputs, describeProcessDocument);

        Map<String, List<IData>> inputData = null;
//        handleComplexValueReference(input, describeProcessDocument, SERVICE_ENDPOINT, inputData);

    }

    private ExecuteResponseDocument executeProcess(String url, ProcessDescriptionType processDescription,
            Map<String, Object> inputs) throws Exception {
        ExecuteRequestBuilder executeBuilder = new ExecuteRequestBuilder(processDescription);

        executeBuilder = prepareExecuteBuilderInput(processID, inputs, processDescription, executeBuilder);

        Map<String, IPortDataDescriptor> outputs = new HashMap<>();

//        executeBuilder = prepareExecuteBuilderOutput(outputs, processDescription, executeBuilder);
//        executeBuilder.setMimeTypeForOutput("text/xml", "non_deterministic_output");
//        executeBuilder.setSchemaForOutput(
//                "http://schemas.opengis.net/gml/3.1.1/base/feature.xsd",
//                "non_deterministic_output");
        ExecuteDocument execute = executeBuilder.getExecute();
        System.err.println(execute);
        execute.getExecute().setService("WPS");

        Object responseObject = CLIENT.execute(url, execute);
        System.err.println(responseObject);
        if (responseObject instanceof ExecuteResponseDocument) {
            ExecuteResponseDocument response = (ExecuteResponseDocument) responseObject;
//            ExecuteResponseAnalyser analyser = new ExecuteResponseAnalyser(
//                    execute, response, processDescription);
//            IData data = (IData) analyser.getComplexDataByIndex(0,
//                    GTVectorDataBinding.class);
            return response;
        }
//        throw new Exception("Exception: " + responseObject.toString());
        return null;
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

    private ExecuteRequestBuilder prepareExecuteBuilderInput(String processID, Map<String, Object> inputs, ProcessDescriptionType processDescription, ExecuteRequestBuilder executeBuilder) throws IOException, WPSClientException {
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
        return executeBuilder;
    }

    private ExecuteRequestBuilder prepareExecuteBuilderOutput(Map<String, IPortDataDescriptor> outputs,
            ProcessDescriptionType processDescription, ExecuteRequestBuilder executeBuilder)
            throws IOException, Exception {

        OutputDescriptionType[] processOutputList = getProcessOutputs(processDescription);

        for (OutputDescriptionType output : processOutputList) {
            String outputName = output.getIdentifier().getStringValue();

            if (outputs.containsKey(outputName)) {
                if (output.isSetComplexOutput()) {
                    ComplexDataFormat selectedFormat = checkComplexDataSupportedFormats(
                            (ComplexPortDataDescriptor) outputs.get(outputName));

                    if (selectedFormat != null) {
                        // "text/xml" if null
                        String mimeType = selectedFormat.getMimeType();
                        if (mimeType == null) {
                            mimeType = "text/xml";
                        }
                        executeBuilder.setMimeTypeForOutput(mimeType, outputName);
                        // sample schema "http://schemas.opengis.net/gml/3.1.1/base/feature.xsd"
                        String schema = selectedFormat.getSchema();
                        if (schema == null) {
                            schema = "http://schemas.opengis.net/gml/3.1.1/base/feature.xsd";
                        }
                        executeBuilder.setSchemaForOutput(schema, outputName);
                        String encding = selectedFormat.getEncoding();
                        if (encding != null) {
                            executeBuilder.setEncodingForOutput(encding, outputName);
                        }
                    }
                }
            }
        }
        return executeBuilder;
    }

    private OutputDescriptionType[] getProcessOutputs(ProcessDescriptionType processDescription) {
        OutputDescriptionType[] result = {};

        ProcessDescriptionType.ProcessOutputs dataOutputs = processDescription.getProcessOutputs();

        if (dataOutputs == null) {
            return result;
        }

        result = dataOutputs.getOutputArray();

        return result;

    }

    private ComplexDataFormat checkComplexDataSupportedFormats(ComplexPortDataDescriptor complexPort) {
        // Check if the selected format (mimeType, encoding, schema)
        // is supported by the service
        ComplexDataFormat selectedFormat = complexPort.getComplexFormat();
//        List<ComplexDataFormat> supportedComplexFormats = complexPort.getSupportedComplexFormats();
        // TODO: Check if contains should not be case sensitive
        //This is always return false.
//        if (!supportedComplexFormats.contains(selectedFormat)) {
//            logger.warn("Provided format not supported.");
//
//            // TODO: Should throw exception or set to default?
//            ComplexDataFormat defaultFormat = complexPort.getDefaultComplexFormat();
//            if (defaultFormat == null) {
////				throw new IllegalArgumentException(
////						"Unsupported format: MimeType=" + selectedFormat.getMimeType() + " Schema=" 
////								+ selectedFormat.getSchema() + " Encoding=" + selectedFormat.getEncoding());
//
//                defaultFormat = new ComplexDataFormat(null, null, null);
//                
//            }
//            
//            selectedFormat = defaultFormat;
//            
//        }

        return selectedFormat;

    }

}
