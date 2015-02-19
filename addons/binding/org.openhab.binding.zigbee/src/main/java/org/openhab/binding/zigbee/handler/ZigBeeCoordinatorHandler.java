/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.handler;

import static org.openhab.binding.zigbee.ZigBeeBindingConstants.*;

import org.bubblecloud.zigbee.ZigBeeApi;
import org.bubblecloud.zigbee.api.Device;
import org.bubblecloud.zigbee.api.ZigBeeDeviceException;
import org.bubblecloud.zigbee.api.cluster.general.OnOff;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ZigBeeCoordinatorHandler} is responsible for handling commands,
 * which are sent to one of the channels.
 * 
 * @author Chris Jackson - Initial contribution
 */
public abstract class ZigBeeCoordinatorHandler extends BaseBridgeHandler {
	protected int panId;
	protected int channelId;
	
	protected ZigBeeApi zigbeeApi;

	private Logger logger = LoggerFactory
			.getLogger(ZigBeeCoordinatorHandler.class);

	public ZigBeeCoordinatorHandler(Bridge coordinator) {
		super(coordinator);
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		// Note required!
	}

	@Override
	public void initialize() {
		logger.debug("Initializing ZigBee coordinator.");

		panId = Integer.parseInt((String) getConfig().get(PARAMETER_PANID));
		channelId = Integer.parseInt((String) getConfig().get(PARAMETER_CHANNEL));

		super.initialize();
	}

	@Override
	public void dispose() {
		// Shut down the ZigBee library
        zigbeeApi.shutdown();
		logger.debug("ZigBee network closed.");
	}

	@Override
	protected void updateStatus(ThingStatus status) {
		super.updateStatus(status);
		for (Thing child : getThing().getThings()) {
			child.setStatus(status);
		}
	}
	
    private Device getDeviceByIndexOrEndpointId(ZigBeeApi zigbeeApi, String deviceIdentifier) {
        Device device;
        device = zigbeeApi.getDevice(deviceIdentifier);
        if(device == null) {
        	logger.debug("Error finding ZigBee device with address {}", deviceIdentifier);
        }
        return device;
    }

	public boolean LightOnOff(String lightAddress, OnOffType state) {
        final Device device = getDeviceByIndexOrEndpointId(zigbeeApi, lightAddress);
        if (device == null) {
            return false;
        }
        final OnOff onOff = device.getCluster(OnOff.class);
        try {
        	if(state == OnOffType.ON) {
        		onOff.on();
        	}
        	else {
        		onOff.off();
        	}
        } catch (ZigBeeDeviceException e) {
            e.printStackTrace();
        }

        return true;
	}
}
