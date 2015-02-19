/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.handler;

import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import org.openhab.binding.zigbee.ZigBeeBindingConstants;

import com.google.common.collect.Sets;

public class ZigBeeLightHandler extends BaseThingHandler implements
		ZigBeeLightListener {

	public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets
			.newHashSet(ZigBeeBindingConstants.THING_TYPE_LIGHT);

	private static final int DIM_STEPSIZE = 30;

	private String lightAddress;

	private Integer lastSentColorTemp;
	private Integer lastSentBrightness;

	private Logger logger = LoggerFactory.getLogger(ZigBeeLightListener.class);

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
//				getThing().setStatus(getBridge().getStatus());
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
			OnOffType state = OnOffType.OFF;
			if (command instanceof PercentType) {
				if (((PercentType) command).intValue() == 0) {
					state = OnOffType.OFF;
				} else {
					state = OnOffType.ON;
				}
			} else if (command instanceof OnOffType) {
				state = (OnOffType) command;
			}
			coordinatorHandler.LightOnOff(lightAddress, state);
			break;
		/*
		 * case ZigBeeBindingConstants.CHANNEL_COLORTEMPERATURE: if (command
		 * instanceof PercentType) { lightState = LightStateConverter
		 * .toColorTemperatureLightState((PercentType) command); } else if
		 * (command instanceof OnOffType) { lightState = LightStateConverter
		 * .toColorLightState((OnOffType) command); } else if (command
		 * instanceof IncreaseDecreaseType) { Integer colorTemp =
		 * lastSentColorTemp; if (colorTemp == null) { State currentState =
		 * light.getState(); if (currentState != null) { colorTemp =
		 * currentState.getColorTemperature(); } } if (colorTemp != null) { if
		 * (command == IncreaseDecreaseType.DECREASE) { colorTemp -=
		 * DIM_STEPSIZE; if (colorTemp < 0) colorTemp = 0; } else { colorTemp +=
		 * DIM_STEPSIZE; if (colorTemp > 255) colorTemp = 255; }
		 * lastSentColorTemp = colorTemp; lightState = new StateUpdate()
		 * .setColorTemperature(colorTemp); } } break; case
		 * ZigBeeBindingConstants.CHANNEL_BRIGHTNESS: if (command instanceof
		 * PercentType) { lightState = LightStateConverter
		 * .toColorLightState((PercentType) command); } else if (command
		 * instanceof OnOffType) { lightState = LightStateConverter
		 * .toColorLightState((OnOffType) command); } else if (command
		 * instanceof IncreaseDecreaseType) { Integer brightness =
		 * lastSentBrightness; if (brightness == null) { State currentState =
		 * light.getState(); if (currentState != null) { if
		 * (!currentState.isOn()) { brightness = 0; } else { brightness =
		 * currentState.getBrightness(); } } } if (brightness != null) { if
		 * (command == IncreaseDecreaseType.DECREASE) { brightness -=
		 * DIM_STEPSIZE; if (brightness < 0) brightness = 0; } else { brightness
		 * += DIM_STEPSIZE; if (brightness > 255) brightness = 255; }
		 * lastSentBrightness = brightness; lightState = new
		 * StateUpdate().setBrightness(brightness); if (brightness == 0) {
		 * lightState = lightState.setOn(false); } } } break; case
		 * ZigBeeBindingConstants.CHANNEL_COLOR: if (command instanceof HSBType)
		 * { HSBType hsbCommand = (HSBType) command; if
		 * (hsbCommand.getBrightness().intValue() == 0) { lightState =
		 * LightStateConverter .toColorLightState(OnOffType.OFF); } else {
		 * lightState = LightStateConverter .toColorLightState(hsbCommand); } }
		 * else if (command instanceof PercentType) { lightState =
		 * LightStateConverter .toColorLightState((PercentType) command); } else
		 * if (command instanceof OnOffType) { lightState = LightStateConverter
		 * .toColorLightState((OnOffType) command); } else if (command
		 * instanceof IncreaseDecreaseType) { Integer brightness =
		 * lastSentBrightness; if (brightness == null) { State currentState =
		 * light.getState(); if (currentState != null) { if
		 * (!currentState.isOn()) { brightness = 0; } else { brightness =
		 * currentState.getBrightness(); } } } if (brightness != null) { if
		 * (command == IncreaseDecreaseType.DECREASE) { brightness -=
		 * DIM_STEPSIZE; if (brightness < 0) brightness = 0; } else { brightness
		 * += DIM_STEPSIZE; if (brightness > 255) brightness = 255; }
		 * lastSentBrightness = brightness; lightState = new
		 * StateUpdate().setBrightness(brightness); if (brightness == 0) {
		 * lightState = lightState.setOn(false); } } } break;
		 */
		}
		// if (lightState != null) {
		// coordinatorHandler.updateLightState(light, lightState);
		// } else {
		// logger.warn("Command send to an unknown channel id: " + channelUID);
		// }
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
				// this.coordinatorHandler.registerLightStatusListener(this);
			} else {
				return null;
			}
		}
		return this.coordinatorHandler;
	}

	/*
	 * @Override public void onLightStateChanged(HueBridge bridge, FullLight
	 * fullLight) { if (fullLight.getId().equals(lightAddress)) {
	 * lastSentColorTemp = null; lastSentBrightness = null;
	 * 
	 * HSBType hsbType = LightStateConverter.toHSBType(fullLight .getState());
	 * if (!fullLight.getState().isOn()) { hsbType = new
	 * HSBType(hsbType.getHue(), hsbType.getSaturation(), new PercentType(0)); }
	 * updateState(new ChannelUID(getThing().getUID(), CHANNEL_COLOR), hsbType);
	 * 
	 * PercentType percentType = LightStateConverter
	 * .toColorTemperaturePercentType(fullLight.getState()); updateState(new
	 * ChannelUID(getThing().getUID(), CHANNEL_COLORTEMPERATURE), percentType);
	 * 
	 * percentType = LightStateConverter.toBrightnessPercentType(fullLight
	 * .getState()); if (!fullLight.getState().isOn()) { percentType = new
	 * PercentType(0); } updateState( new ChannelUID(getThing().getUID(),
	 * CHANNEL_BRIGHTNESS), percentType); }
	 * 
	 * }
	 * 
	 * @Override public void onLightRemoved(HueBridge bridge, FullLight light) {
	 * if (light.getId().equals(lightAddress)) {
	 * getThing().setStatus(ThingStatus.OFFLINE); } }
	 * 
	 * @Override public void onLightAdded(HueBridge bridge, FullLight light) {
	 * if (light.getId().equals(lightAddress)) {
	 * getThing().setStatus(ThingStatus.ONLINE); onLightStateChanged(bridge,
	 * light); } }
	 */
}
