/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.handler;

import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Set;

import org.bubblecloud.zigbee.api.ZigBeeApiConstants;
import org.bubblecloud.zigbee.api.cluster.impl.api.core.Attribute;
import org.openhab.binding.zigbee.ZigBeeBindingConstants;

import com.google.common.collect.Sets;

public class ZigBeeLightHandler extends BaseThingHandler implements
		ZigBeeEventListener {

	public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets
			.newHashSet(ZigBeeBindingConstants.THING_TYPE_COLOR_DIMMABLE_LIGHT);

	private static final int DIM_STEPSIZE = 30;

	private String lightAddress;

	private Integer currentColorTemp;
	private Integer currentBrightness;
	private OnOffType currentOnOff;
	private Color currentHSB;

	private Logger logger = LoggerFactory.getLogger(ZigBeeEventListener.class);

	private ZigBeeCoordinatorHandler coordinatorHandler;

	public ZigBeeLightHandler(Thing zigbeeDevice) {
		super(zigbeeDevice);
	}

	@Override
	public void initialize() {
		logger.debug("Initializing ZigBee light handler.");
		final String configAddress = (String) getConfig().get(
				ZigBeeBindingConstants.PARAMETER_MACADDRESS);
		if (configAddress != null) {
			lightAddress = configAddress;

			if (getCoordinatorHandler() != null) {
				// getThing().setStatus(getBridge().getStatus());
			}
		}
	}

	@Override
	public void dispose() {
		logger.debug("Handler disposes. Unregistering listener.");
		if (lightAddress != null) {
			ZigBeeCoordinatorHandler coordinatorHandler = getCoordinatorHandler();
			if (coordinatorHandler != null) {
				// coordinatorHandler.unregisterLightStatusListener(this);
			}
			lightAddress = null;
		}
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		ZigBeeCoordinatorHandler coordinatorHandler = getCoordinatorHandler();
		if (coordinatorHandler == null) {
			logger.warn("Coordinator handler not found. Cannot handle command without coordinator.");
			return;
		}

		switch (channelUID.getId()) {
		case ZigBeeBindingConstants.CHANNEL_SWITCH:
			OnOffType statePower = OnOffType.OFF;
			if (command instanceof PercentType) {
				if (((PercentType) command).intValue() == 0) {
					statePower = OnOffType.OFF;
				} else {
					statePower = OnOffType.ON;
				}
			} else if (command instanceof OnOffType) {
				statePower = (OnOffType) command;
			}
			coordinatorHandler.LightPower(lightAddress, statePower);
			statePower = currentOnOff;
			break;

		case ZigBeeBindingConstants.CHANNEL_BRIGHTNESS:
			int stateBrightness = 0;
			if (command instanceof PercentType) {
				stateBrightness = ((PercentType) command).intValue();
			} else if (command instanceof OnOffType) {
				if ((OnOffType) command == OnOffType.ON) {
					stateBrightness = 100;
				} else {
					stateBrightness = 0;
				}
			}
			if (stateBrightness == 0) {
				coordinatorHandler.LightPower(lightAddress, OnOffType.OFF);
				currentOnOff = OnOffType.OFF;
			} else {
				if(currentOnOff == OnOffType.OFF) {
					coordinatorHandler.LightPower(lightAddress, OnOffType.ON);
					currentOnOff = OnOffType.ON;
				}
				coordinatorHandler.LightBrightness(lightAddress,
						stateBrightness);
			}
			break;

		case ZigBeeBindingConstants.CHANNEL_COLOR:
			if (!(command instanceof HSBType)) {
				return;
			}
			coordinatorHandler.LightColor(lightAddress, (HSBType) command);
			break;
		}

		updateState(new ChannelUID(getThing().getUID(), ZigBeeBindingConstants.CHANNEL_SWITCH), currentOnOff);
	}

	private synchronized ZigBeeCoordinatorHandler getCoordinatorHandler() {
		if (this.coordinatorHandler == null) {
			Bridge bridge = getBridge();
			if (bridge == null) {
				return null;
			}
			ThingHandler handler = bridge.getHandler();
			if (handler instanceof ZigBeeCoordinatorHandler) {
				this.coordinatorHandler = (ZigBeeCoordinatorHandler) handler;
				this.coordinatorHandler.subscribeEvents(lightAddress, this);
			} else {
				return null;
			}
		}
		return this.coordinatorHandler;
	}

	@Override
	public void onEndpointStateChange() {
		boolean statePower = (boolean) coordinatorHandler.attributeRead(lightAddress,
				ZigBeeApiConstants.CLUSTER_ID_ON_OFF, 0);
		OnOffType stateSwitch;
		if(statePower == true) {
			currentOnOff = OnOffType.ON;
		}
		else {
			currentOnOff = OnOffType.OFF;			
		}
		updateState(new ChannelUID(getThing().getUID(), ZigBeeBindingConstants.CHANNEL_SWITCH), currentOnOff);
		
		
		currentBrightness = (Integer) coordinatorHandler.attributeRead(lightAddress,
				ZigBeeApiConstants.CLUSTER_ID_LEVEL_CONTROL, 0);
//		currentHue = (Integer) coordinatorHandler.attributeRead(lightAddress,
//				ZigBeeApiConstants.CLUSTER_ID_COLOR_CONTROL, 0);

	}

	@Override
	public void onAttributeUpdate(final Dictionary<Attribute, Object> reports) {
		final Enumeration<Attribute> attributes = reports.keys();
		while (attributes.hasMoreElements()) {
			final Attribute attribute = attributes.nextElement();
			final Object value = reports.get(attribute);
			logger.debug("{}: {}={}", lightAddress, attribute.getName(), value);

			if (attribute.getId() == 0) {

			}
		}
	}

	/*
	 * @Override public void onLightRemoved(HueBridge bridge, FullLight light) {
	 * 
	 * if (light.getId().equals(lightAddress)) {
	 * getThing().setStatus(ThingStatus.OFFLINE); } }
	 * 
	 * @Override public void onLightAdded(HueBridge bridge, FullLight light) {
	 * if (light.getId().equals(lightAddress)) {
	 * getThing().setStatus(ThingStatus.ONLINE); onLightStateChanged(bridge,
	 * light); } }
	 */

}
