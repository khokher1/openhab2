/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.handler;

import static org.openhab.binding.zigbee.ZigBeeBindingConstants.*;

import java.util.EnumSet;

import org.bubblecloud.zigbee.ZigBeeApi;
import org.bubblecloud.zigbee.network.model.DiscoveryMode;
import org.eclipse.smarthome.config.core.Configuration;
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
 * The {@link ZigBeeCoordinatorCC2530Handler} is responsible for handling
 * commands, which are sent to one of the channels.
 * 
 * @author Chris Jackson - Initial contribution
 */
public class ZigBeeCoordinatorCC2530Handler extends ZigBeeCoordinatorHandler {
	private String portId;

	private Logger logger = LoggerFactory
			.getLogger(ZigBeeCoordinatorCC2530Handler.class);

	public ZigBeeCoordinatorCC2530Handler(Bridge coordinator) {
		super(coordinator);
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		// Note required!
	}

	@Override
	public void initialize() {
		logger.debug("Initializing ZigBee CC2530EMK bridge handler.");

		portId = (String) getConfig().get(PARAMETER_PORT);

		// Call the parent to finish any global initialisation
		super.initialize();

		logger.debug(
				"ZigBee Coordinator CC2530 opening Port:'{}' PAN:{}, Channel:{}",
				portId, Integer.toHexString(panId),
				Integer.toString(channelId));
		
		// TODO: Some of this needs to move to the parent class
		// TODO: Only the port initialisation should be done here and then pass
		// TODO: This to the parent to handle the protocol.
		// TODO: Needs splitting IO in the library!
        final EnumSet<DiscoveryMode> discoveryModes = DiscoveryMode.ALL;
        //discoveryModes.remove(DiscoveryMode.LinkQuality);
        zigbeeApi = new ZigBeeApi(portId, panId, channelId, false, discoveryModes);
        if (!zigbeeApi.startup()) {
            logger.debug("Unable to start ZigBee network");
            
            // TODO: Close the network!
            
            
        } else {
            logger.debug("ZigBee network started");
            
            waitForNetwork();
        }

	}

	@Override
	public void dispose() {
	}

	@Override
	protected void updateStatus(ThingStatus status) {
		super.updateStatus(status);
		for (Thing child : getThing().getThings()) {
			child.setStatus(status);
		}
	}
}
