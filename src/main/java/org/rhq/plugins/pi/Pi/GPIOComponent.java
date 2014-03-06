package org.rhq.plugins.pi.Pi;

import java.io.File;
import java.io.FileWriter;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.platform.LinuxPlatformComponent;

/**
 * Represents one GPIO Pin. Currently output mode only
 * @author Heiko W. Rupp
 */
public class GPIOComponent implements ResourceComponent<LinuxPlatformComponent>, OperationFacet {

    private ResourceContext<LinuxPlatformComponent> context;
    private Configuration pluginConfig;
    private Integer pinNumber;
    private Log log = LogFactory.getLog(GPIOComponent.class);

    @Override
    public OperationResult invokeOperation(String op,
                                           Configuration configuration) throws InterruptedException, Exception {

        OperationResult operationResult = new OperationResult();
        File file = new File("/sys/class/gpio/gpio"+pinNumber);
        if (!file.exists()) {
            operationResult.setErrorMessage("Pin " + pinNumber + " not provisioned");
        }
        file = new File(file,"value");
        if (!file.canWrite()) {
            operationResult.setErrorMessage("Value for pin " + pinNumber + " not writable");
        }

        if (operationResult.getErrorMessage()!=null) {
            return operationResult;
        }
        FileWriter fw = new FileWriter(file);
        if (op.equals("on")) {
            fw.write("1");
            fw.flush();
            fw.close();
        } else if (op.equals("off")) {
            fw.write("0");
            fw.flush();
            fw.close();
        } else if (op.equals("blink")) {
            String tmp = configuration.getSimpleValue("delay","1000");
            int delay = Integer.valueOf(tmp);
            tmp = configuration.getSimpleValue("times","1");
            int times = Integer.valueOf(tmp) ;
            fw.write("0");
            fw.flush();

            for (int i = 0 ; i < times ; i++ ) {
                fw.write("1");
                fw.flush();
                Thread.sleep(delay);
                fw.write("0");
                fw.flush();
                Thread.sleep(delay);
            }
            fw.close();

        } else {
            log.warn("Operation " + op + " not yet supported");
            operationResult.setErrorMessage("Operation not yet supported");
            return operationResult;
        }

        return new OperationResult("ok");

    }


    @Override
    public void start(
        ResourceContext<LinuxPlatformComponent> context) throws InvalidPluginConfigurationException, Exception {
        this.context = context;
        this.pluginConfig = context.getPluginConfiguration();
        this.pinNumber = Integer.valueOf(pluginConfig.getSimpleValue("pin"));


        // Check if our pin is already enabled

    }

    @Override
    public void stop() {
    }

    @Override
    public AvailabilityType getAvailability() {
        return AvailabilityType.UP; // TODO real impl
    }
}
