package org.rhq.plugins.pi.Pi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
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

    private static final String GPIO_BASE = "/sys/class/gpio";
    private Integer pinNumber;
    private Log log = LogFactory.getLog(GPIOComponent.class);

    @Override
    public OperationResult invokeOperation(String op,
                                           Configuration configuration) throws InterruptedException, Exception {

        OperationResult operationResult = new OperationResult();
        File file = new File(GPIO_BASE + "/gpio" +pinNumber);
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
        Configuration pluginConfig = context.getPluginConfiguration();
        String value = pluginConfig.getSimpleValue("pin");
        if (value==null || value.isEmpty()) {
            throw new InvalidPluginConfigurationException("No pin provided, can not start");
        }
        this.pinNumber = Integer.valueOf(value);

        // Check if the pin is already exported. If so, nothing to do.
        File gpioPin = new File(GPIO_BASE + "/gpio" + pinNumber);
        if (gpioPin.exists()) {
            return;
        }
        setuPinForOutput();
        Configuration blinking = new Configuration();
        blinking.put(new PropertySimple("times",5));
        blinking.put(new PropertySimple("delay", 250));
        invokeOperation("blink",blinking);

    }

    private void setuPinForOutput() throws IOException, InterruptedException {
        // Set up the pin for output
        File export = new File(GPIO_BASE + "/export");
        if (!export.canWrite()) {
            throw new InvalidPluginConfigurationException("Can not export pin " + pinNumber);
        }
        FileWriter fw = new FileWriter(export);
        try {
            fw.write(""+pinNumber);
            fw.flush();
        } catch (IOException e) {
            throw e;
        } finally {
            fw.close();
        }

        Thread.sleep(100);

        File direction = new File(GPIO_BASE + "/gpio" + pinNumber + "/direction");
        if (!direction.exists()) {
            throw new InvalidPluginConfigurationException("Pin " + pinNumber + " was not exported");
        }

        fw = new FileWriter(direction);
        try {
            fw.write("out");
            fw.flush();
        } catch (IOException e) {
            throw e;
        } finally {
            fw.close();
        }
    }

    @Override
    public void stop() {

        File unexport = new File(GPIO_BASE + "/unexport");
        if (!unexport.canWrite()) {
            throw new InvalidPluginConfigurationException("Can not unexport pin " + pinNumber);
        }
        FileWriter fw = null;
        try {
            fw = new FileWriter(unexport);
            fw.write(""+pinNumber);
            fw.flush();
        } catch (IOException e) {
            log.error(e);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

    @Override
    public AvailabilityType getAvailability() {
        File gpioPin = new File(GPIO_BASE + "/gpio" + pinNumber);
        if (gpioPin.exists()) {
            return AvailabilityType.UP;
        }
        return AvailabilityType.DOWN;

    }
}
