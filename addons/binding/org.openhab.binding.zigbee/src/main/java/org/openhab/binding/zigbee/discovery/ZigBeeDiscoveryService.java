/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.discovery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bubblecloud.zigbee.api.Device;
import org.bubblecloud.zigbee.api.DeviceListener;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.zigbee.ZigBeeBindingConstants;
import org.openhab.binding.zigbee.handler.ZigBeeCoordinatorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ZigBeeDiscoveryService} tracks ZigBee lights which are associated
 * to coordinator.
 * 
 * @author Chris Jackson - Initial contribution
 *
 */
public class ZigBeeDiscoveryService extends AbstractDiscoveryService implements
		DeviceListener {
	private final Logger logger = LoggerFactory
			.getLogger(ZigBeeDiscoveryService.class);

	private final static int SEARCH_TIME = 60;

	private ZigBeeCoordinatorHandler coordinatorHandler;

	public ZigBeeDiscoveryService(ZigBeeCoordinatorHandler coordinatorHandler) {
		super(SEARCH_TIME);
		this.coordinatorHandler = coordinatorHandler;
	}

	public void activate() {
		logger.debug("Activating ZigBee discovery service for {}",
				coordinatorHandler.getThing().getUID());

		// Listen for device events
		coordinatorHandler.addDeviceListener(this);

//		startScan();
	}

	@Override
	public void deactivate() {
		logger.debug("Deactivating ZigBee discovery service for {}",
				coordinatorHandler.getThing().getUID());

		// Remove the listener
		coordinatorHandler.removeDeviceListener(this);
	}

	@Override
	public Set<ThingTypeUID> getSupportedThingTypes() {
		return ZigBeeBindingConstants.SUPPORTED_DEVICE_TYPES_UIDS;
	}

	@Override
	public void startScan() {
		logger.debug("Starting ZigBee scan for {}", coordinatorHandler
				.getThing().getUID());

		List<Device> devices = coordinatorHandler.getDeviceList();
		if (devices != null) {
			for (Device device : devices) {
				deviceAdded(device);
			}
		}
		// Start the search for new devices
		// coordinatorHandler.startDeviceDiscovery();
	}

	private ThingUID getThingUID(Device device) {
		ThingUID bridgeUID = coordinatorHandler.getThing().getUID();

		// Our thing ID is based on the ZigBee device type.
		ThingTypeUID thingTypeUID = new ThingTypeUID(
				ZigBeeBindingConstants.BINDING_ID, device.getDeviceType()
						.replaceAll("\\s+", ""));

		if (getSupportedThingTypes().contains(thingTypeUID)) {
			String thingId = String.format("%04X", device.getNetworkAddress());
			ThingUID thingUID = new ThingUID(thingTypeUID, bridgeUID, thingId);
			return thingUID;
		} else {
			return null;
		}
	}

	@Override
	public void deviceAdded(Device device) {
		logger.debug("Device discovery: {} {} {} {}", device.getIEEEAddress(),
				device.getDeviceType(), device.getProfileId());

		ThingUID thingUID = getThingUID(device);
		if (thingUID != null) {
			ThingUID bridgeUID = coordinatorHandler.getThing().getUID();
			Map<String, Object> properties = new HashMap<>(1);
			properties.put(ZigBeeBindingConstants.PARAMETER_MACADDRESS,
					device.getIEEEAddress());
			DiscoveryResult discoveryResult = DiscoveryResultBuilder
					.create(thingUID)
					.withProperties(properties)
					.withBridge(bridgeUID)
					.withLabel(device.getDeviceType())
					.build();

			thingDiscovered(discoveryResult);
		} else {
			logger.debug("Discovered unknown device type '{}' at address {}",
					device.getDeviceType(), device.getIEEEAddress());
		}
	}

	@Override
	public void deviceUpdated(Device device) {
		// Nothing to do here!
	}

	@Override
	public void deviceRemoved(Device device) {
		ThingUID thingUID = getThingUID(device);

		if (thingUID != null) {
			thingRemoved(thingUID);
		}
	}
	
    @Override
    protected void startBackgroundDiscovery() {
    }
}