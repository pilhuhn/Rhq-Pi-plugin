package org.rhq.plugins.pi.Pi;

import java.util.Collections;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * // TODO: Document this
 * @author Heiko W. Rupp
 */
public class GPIODiscovery implements ResourceDiscoveryComponent, ManualAddFacet {

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration configuration,
                                                      ResourceDiscoveryContext resourceDiscoveryContext) throws InvalidPluginConfigurationException {

        int pin = Integer.parseInt(configuration.getSimpleValue("pin"));

        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
            resourceDiscoveryContext.getResourceType(),
            "Pin" + pin,
            "GPIO pin " + pin,
            null,
            "A GPIO pin",
            configuration,
            null
        );

        return detail;
    }

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext resourceDiscoveryContext) throws InvalidPluginConfigurationException, Exception {
        return Collections.emptySet();
    }
}
