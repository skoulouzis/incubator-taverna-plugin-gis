package org.apache.taverna.gis.ui.serviceprovider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.apache.taverna.gis.client.*;

import net.sf.taverna.t2.servicedescriptions.AbstractConfigurableServiceProvider;
import net.sf.taverna.t2.servicedescriptions.ConfigurableServiceProvider;
import net.sf.taverna.t2.servicedescriptions.CustomizedConfigurePanelProvider;
import org.apache.taverna.gis.client.impl.GisClientNorthImpl;

public class GisServiceProvider extends AbstractConfigurableServiceProvider<GisServiceProviderConfig>
        implements ConfigurableServiceProvider<GisServiceProviderConfig>,
        CustomizedConfigurePanelProvider<GisServiceProviderConfig> {

    public GisServiceProvider() {
        super(new GisServiceProviderConfig("", new ArrayList<String>()));
    }

    private static final URI PROVIDER_ID = URI
            .create("http://cs.man.ac.uk/2016/service-provider/apache-taverna2-plugin-gis");

    private final Logger LOGGER = Logger.getLogger(AddGisServiceDialog.class);

    /**
     * Do the actual search for services. Return using the callBack parameter.
     *
     * @param callBack
     */
    @Override
    public void findServiceDescriptionsAsync(FindServiceDescriptionsCallBack callBack) {
        // Use callback.status() for long-running searches

        URI serviceURI = serviceProviderConfig.getOgcServiceUri();

        callBack.status("Resolving service: " + serviceURI);

        List<GisServiceDesc> results = new ArrayList<>();

        IGisClient gisServiceClient;
        try {
            gisServiceClient = GisClientFactory.getInstance().getGisClient(getConfiguration().getOgcServiceUri().toASCIIString());

            List<String> processIdentifiers = serviceProviderConfig.getProcessIdentifiers();

            for (String processID : processIdentifiers) {
                GisServiceDesc service = new GisServiceDesc();

                // Populate the service description bean
                service.setOgcServiceUri(getConfiguration().getOgcServiceUri());
                service.setProcessIdentifier(processID);

                // TODO: Optional: set description (Set a better description)
                if (gisServiceClient instanceof GisClientNorthImpl) {
                    service.setDescription(((GisClientNorthImpl) gisServiceClient).getProcessDescription(processID));
                } else {
                    service.setDescription(processID);
                }

                // Get input ports
                List<IPortDataDescriptor> inputList = gisServiceClient.getTaverna2InputPorts(processID);

                service.setInputPortDefinitions(inputList);

                // Get output ports
                List<IPortDataDescriptor> outputList = gisServiceClient.getTaverna2OutputPorts(processID);

                service.setOutputPortDefinitions(outputList);

                results.add(service);

                // partialResults() can also be called several times from inside
                // for-loop if the full search takes a long time
                callBack.partialResults(results);
            }

        } catch (UnsupportedEncodingException | MalformedURLException ex) {
            JOptionPane.showMessageDialog(null,
                    "Could not read the service definition from "
                    + serviceURI + ":\n" + ex,
                    "Could not add service service",
                    JOptionPane.ERROR_MESSAGE);

            LOGGER.error(
                    "Failed to list GWS processes for service: "
                    + serviceURI, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(GisServiceProvider.class.getName()).log(Level.SEVERE, null, ex);
        }

        // No more results will be coming
        callBack.finished();
    }

    /**
     * Icon for service provider
     *
     * @return
     */
    @Override
    public Icon getIcon() {
        return GisServiceIcon.getIcon();
    }

    /**
     * Name of service provider, appears in right click for 'Remove service
     * provider'
     *
     * @return
     */
    @Override
    public String getName() {
        return "OGC Web Processing Service";
    }

    @Override
    public String toString() {
        return "OGC Web Processing Service " + getConfiguration().getOgcServiceUri();
    }

    /**
     *
     * @return
     */
    @Override
    public String getId() {
        return PROVIDER_ID.toASCIIString();
    }

    @Override
    protected List<? extends Object> getIdentifyingData() {
        List<String> result = new ArrayList<>();

        List<String> processIdentifiers = getConfiguration().getProcessIdentifiers();

        for (String processID : processIdentifiers) {
            result.add(getConfiguration().getOgcServiceUri() + processID);

        }

        //return Arrays.asList(getConfiguration().getOgcServiceUri(), getConfiguration().getProcessIdentifier());
        //return Arrays.asList(getConfiguration().getOgcServiceUri());
        return result;

    }

//    @Override
//    public List<GisServiceProviderConfig> getDefaultConfigurations() {
//
//        List<GisServiceProviderConfig> myDefaultConfigs = new ArrayList<>();
//
//        myDefaultConfigs.add(new GisServiceProviderConfig("http://dataminer-prototypes.d4science.org/wps/WebProcessingService?",
//                Arrays.asList("dataminer-prototypes.d4science.org")));
//
//        return myDefaultConfigs;
//
//    }
    @Override
    public void createCustomizedConfigurePanel(
            final net.sf.taverna.t2.servicedescriptions.CustomizedConfigurePanelProvider.CustomizedConfigureCallBack<GisServiceProviderConfig> callBack) {

        @SuppressWarnings("serial")
        AddGisServiceDialog addGISServiceDialog = new AddGisServiceDialog(null) {

            @Override
            protected void addRegistry(String serviceURL, List<String> processIdentifiers) {
                GisServiceProviderConfig providerConfig = new GisServiceProviderConfig(serviceURL, processIdentifiers);
                callBack.newProviderConfiguration(providerConfig);

            }

        };

        addGISServiceDialog.setVisible(true);

    }

}
