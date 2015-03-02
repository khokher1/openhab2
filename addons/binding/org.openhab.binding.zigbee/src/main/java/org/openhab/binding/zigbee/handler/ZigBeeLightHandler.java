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
import java.util.Set;

import org.bubblecloud.zigbee.api.cluster.impl.api.core.Attribute;
import org.bubblecloud.zigbee.api.cluster.impl.api.core.ReportListener;
import org.bubblecloud.zigbee.api.cluster.impl.api.core.ZigBeeClusterException;
import org.bubblecloud.zigbee.api.cluster.impl.attribute.Attributes;
import org.bubblecloud.zigbee.api.cluster.impl.general.ColorControlCluster;
import org.bubblecloud.zigbee.api.cluster.impl.general.LevelControlCluster;
import org.bubblecloud.zigbee.api.cluster.impl.general.OnOffCluster;
import org.openhab.binding.zigbee.ZigBeeBindingConstants;

import com.google.common.collect.Sets;

public class ZigBeeLightHandler extends BaseThingHandler implements
		ZigBeeEventListener, ReportListener  {

	public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets
			.newHashSet(ZigBeeBindingConstants.THING_TYPE_COLOR_DIMMABLE_LIGHT);

	private static final int DIM_STEPSIZE = 30;

	private String lightAddress;
	private Attribute attrOnOff;
	private Attribute attrLevel;
	private Attribute attrColorX;
	private Attribute attrColorY;

	private Integer currentColorTemp;
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
		lightAddress = configAddress;
		attrOnOff = null;
		attrLevel = null;
		attrColorX = null;
		attrColorY = null;
	}

	protected void bridgeHandlerInitialized(ThingHandler thingHandler, Bridge bridge) {
		coordinatorHandler = (ZigBeeCoordinatorHandler) thingHandler;

		if (lightAddress != null) {
			if (coordinatorHandler != null) {
				coordinatorHandler.subscribeEvents(lightAddress, this);
				// getThing().setStatus(getBridge().getStatus());
			}
		}
	}

	@Override
	public void dispose() {
		logger.debug("Handler disposes. Unregistering listener.");
		if (lightAddress != null) {
			if (coordinatorHandler != null) {
				coordinatorHandler.unsubscribeEvents(lightAddress, this);
			}
			lightAddress = null;
		}
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		if (coordinatorHandler == null) {
			logger.warn("Coordinator handler not found. Cannot handle command without coordinator.");
			return;
		}

		switch (channelUID.getId()) {
		case ZigBeeBindingConstants.CHANNEL_SWITCH:
			if (command instanceof PercentType) {
				if (((PercentType) command).intValue() == 0) {
					currentOnOff = OnOffType.OFF;
				} else {
					currentOnOff = OnOffType.ON;
				}
			} else if (command instanceof OnOffType) {
				currentOnOff = (OnOffType) command;
			}
			coordinatorHandler.LightPower(lightAddress, currentOnOff);
			break;

		case ZigBeeBindingConstants.CHANNEL_BRIGHTNESS:
			int level = 0;
			if (command instanceof PercentType) {
				level = ((PercentType) command).intValue();
			} else if (command instanceof OnOffType) {
				if ((OnOffType) command == OnOffType.ON) {
					level = 100;
				} else {
					level = 0;
				}
			}
			coordinatorHandler.LightBrightness(lightAddress, level);
			break;

		case ZigBeeBindingConstants.CHANNEL_COLOR:
			if (!(command instanceof HSBType)) {
				return;
			}
			coordinatorHandler.LightColor(lightAddress, (HSBType) command);
			break;
		}
	}

	@Override
	public void onEndpointStateChange() {
		try {
			if (attrOnOff != null) {
				updateStateOnOff((boolean)attrOnOff.getValue());
			}

			if (attrLevel != null) {
				updateStateLevel((int)attrLevel.getValue());
			}

			if (attrColorX != null && attrColorY != null) {
				updateStateColor((int)attrColorX.getValue(), (int)attrColorY.getValue());
			}
		} catch (ZigBeeClusterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void openDevice() {
		attrOnOff = coordinatorHandler.openAttribute(lightAddress,
				OnOffCluster.ID, Attributes.ON_OFF, this);

		if (this.getThing().getThingTypeUID().equals(ZigBeeBindingConstants.THING_TYPE_ON_OFF_LIGHT) == false) {
			attrLevel = coordinatorHandler.openAttribute(lightAddress,
					LevelControlCluster.ID, Attributes.CURRENT_LEVEL, this);
		}

		if (this.getThing().getThingTypeUID().equals(ZigBeeBindingConstants.THING_TYPE_COLOR_DIMMABLE_LIGHT)) {
			attrColorX = coordinatorHandler.openAttribute(lightAddress,
					ColorControlCluster.ID, Attributes.CURRENT_X, null);
			attrColorY = coordinatorHandler.openAttribute(lightAddress,
					ColorControlCluster.ID, Attributes.CURRENT_Y, null);
		}
	}

	@Override
	public void closeDevice() {
		coordinatorHandler.closeAttribute(attrOnOff, this);
		coordinatorHandler.closeAttribute(attrLevel, this);
		coordinatorHandler.closeAttribute(attrColorX, null);
		coordinatorHandler.closeAttribute(attrColorY, null);
	}

	@Override
	public void receivedReport(String endPointId, short clusterId,
			Dictionary<Attribute, Object> reports) {
		logger.debug("ZigBee attribute reports {} from {}", reports, endPointId);
		if (attrOnOff != null) {
			Object value = reports.get(attrOnOff);
			if (value != null) {
				updateStateOnOff((boolean)value);
			}
		}
		if (attrLevel != null) {
			Object value = reports.get(attrLevel);
			if (value != null) {
				updateStateLevel((int)value);
			}
		}
	}

	private void updateStateOnOff(boolean onOff) {
		currentOnOff = onOff == true ? OnOffType.ON : OnOffType.OFF;
		updateState(new ChannelUID(getThing().getUID(), ZigBeeBindingConstants.CHANNEL_SWITCH), currentOnOff);
	}

	private void updateStateLevel(int level) {
		PercentType chanPercent;
		if (currentOnOff == OnOffType.OFF) {
			level = 0;
		}
		chanPercent = new PercentType ((int)(level * 100.0 / 254.0 + 0.5));
		updateState(new ChannelUID(getThing().getUID(), ZigBeeBindingConstants.CHANNEL_BRIGHTNESS), chanPercent);
	}

	private void updateStateColor(int colorX, int colorY) {
/*      TODO implement Rgb.cie2rgb, and saturation
		Integer stateTemp = (Integer) coordinatorHandler.attributeRead(lightAddress,
		ZigBeeApiConstants.CLUSTER_ID_COLOR_CONTROL, Attributes.COLOR_TEMPERATURE.getId());
		Rgb rgb = Rgb.cie2rgb(stateX / 65536.0, stateY / 65536.0, 1);
		updateState(new ChannelUID(getThing().getUID(), ZigBeeBindingConstants.CHANNEL_COLOR), rgb);
		updateState(new ChannelUID(getThing().getUID(), ZigBeeBindingConstants.CHANNEL_COLORTEMPERATURE), chanSat);
*/
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
