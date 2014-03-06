
package org.rhq.plugins.pi.Pi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.platform.LinuxPlatformComponent;

@SuppressWarnings("unused")
public class OneWireComponent implements ResourceComponent<LinuxPlatformComponent> , MeasurementFacet , OperationFacet
{
    private final Log log = LogFactory.getLog(this.getClass());


    private ResourceContext<LinuxPlatformComponent> context;
    String path ;




    /**
     * Return availability of this resource
     *  @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {
        File file = new File(path);
        if (file.exists() && file.canRead()) {
            return AvailabilityType.UP;
        }
        return AvailabilityType.DOWN;
    }


    /**
     * Start the resource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext<LinuxPlatformComponent> context) throws InvalidPluginConfigurationException, Exception {

        this.context = context;
        Configuration conf = context.getPluginConfiguration();
        path = conf.getSimpleValue("path");

    }


    /**
     * Tear down the resource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() {


    }



    /**
     * Gather measurement data
     *  @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport, java.util.Set)
     */
    public  void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

         for (MeasurementScheduleRequest req : metrics) {
            if (req.getName().equals("temp")) {
                File file = new File(path);
                BufferedReader br = new BufferedReader(new FileReader(file));
                br.readLine();  // Skip over 1st line TODO check if crc=YES
                String tmp = br.readLine();
                br.close();
                String[] tmp2 = tmp.split("=");
                Double val = Double.valueOf(tmp2[1]);
                val = val / 1000;

                if (val < 50) { // The sensor sometimes reports crap, ignore it
                    MeasurementDataNumeric res = new MeasurementDataNumeric(req, val);
                    report.addData(res);
                }
            }
         }
    }



    public void startOperationFacet(OperationContext context) {

    }


    /**
     * Invokes the passed operation on the managed resource
     * @param name Name of the operation
     * @param params The method parameters
     * @return An operation result
     * @see org.rhq.core.pluginapi.operation.OperationFacet
     */
    public OperationResult invokeOperation(String name, Configuration params) throws Exception {

        OperationResult res = new OperationResult();
        if ("dummyOperation".equals(name)) {
            // TODO implement me

        }
        return res;
    }





}
