package org.rhq.plugins.pi.Pi;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.platform.LinuxPlatformComponent;

/**
 * Discovery class for RaspberryPi specific functionality.
 * For now this assumes the pidora distribution.
 */
public class OneWireDiscovery implements ResourceDiscoveryComponent<LinuxPlatformComponent>

{

    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * Run the auto-discovery
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<LinuxPlatformComponent> discoveryContext) throws Exception {
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

        /**
         * A discovered resource must have a unique key, that must
         * stay the same when the resource is discovered the next
         * time
         */

        // http://www.kompf.de/weather/pionewiremini.html
        // http://kopfkino.irosaurus.com/tutorial-ds18s20-temperatur-sensor-an-einem-raspberry-pi/

        File oneWireDevices = new File("/sys/bus/w1/devices");
        if (!oneWireDevices.exists()) {
            return Collections.emptySet();
        }

        if (!oneWireDevices.canRead() || !oneWireDevices.canExecute()) {
            log.warn("Can not read " + oneWireDevices.getAbsolutePath());
            return Collections.emptySet();
        }

        File[] devices = oneWireDevices.listFiles();

        for (File device : devices) {

            log.info("Found a " + device.getName());

            SensorType sensorType = SensorType.getType(device.getName());
            if (sensorType ==null) {
                log.warn("Unknown device type " + device.getName() + " skipping");
                continue;
            }

            Configuration configuration = new Configuration();
            configuration.put(new PropertySimple("path",device.getAbsolutePath()+File.separator+"w1_slave"));
            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                discoveryContext.getResourceType(), // ResourceType
                device.getName(),
                sensorType.name() + " sensor [" + device.getName().substring(3) + "]",
                null,
                "Temperature sensor",
                configuration,
                null
            );

            // Add to return values
            discoveredResources.add(detail);
            log.info("Discovered new ... " + device.getName());
        }
        return discoveredResources;

        }


    enum SensorType {
        DS1820("10"),
        DS18B20("28"),
        DS1822("22");

        private String familyCode;

        SensorType(String familyCode) {
            this.familyCode = familyCode;
        }

        static SensorType getType(String name) {
            for (SensorType type : SensorType.values()) {
                if (name.startsWith(type.familyCode)) {
                    return type;
                }
            }
            return null;
        }

    }
}