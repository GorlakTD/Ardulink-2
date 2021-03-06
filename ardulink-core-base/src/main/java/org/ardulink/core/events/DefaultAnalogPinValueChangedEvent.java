/**
Copyright 2013 project Ardulink http://www.ardulink.org/
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
    http://www.apache.org/licenses/LICENSE-2.0
 
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.ardulink.core.events;

import org.ardulink.core.Pin.AnalogPin;

/**
 * [ardulinktitle] [ardulinkversion]
 * 
 * project Ardulink http://www.ardulink.org/
 * 
 * [adsense]
 *
 */
public class DefaultAnalogPinValueChangedEvent implements AnalogPinValueChangedEvent {

	public static DefaultAnalogPinValueChangedEvent analogPinValueChanged(AnalogPin pin, int value) {
		return new DefaultAnalogPinValueChangedEvent(pin, value);
	}

	private final AnalogPin pin;
	private final Integer value;

	public DefaultAnalogPinValueChangedEvent(AnalogPin pin, int value) {
		this.pin = pin;
		this.value = value;
	}

	public AnalogPin getPin() {
		return this.pin;
	}

	public Integer getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return "DefaultAnalogPinValueChangedEvent [pin=" + pin + ", value=" + value + "]";
	}

}
