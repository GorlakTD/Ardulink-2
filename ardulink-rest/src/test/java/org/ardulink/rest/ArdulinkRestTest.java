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

package org.ardulink.rest;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.port;
import static java.lang.Boolean.parseBoolean;
import static java.lang.System.identityHashCode;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.ardulink.core.Pin.analogPin;
import static org.ardulink.core.Pin.digitalPin;
import static org.ardulink.core.Pin.Type.ANALOG;
import static org.ardulink.core.Pin.Type.DIGITAL;
import static org.ardulink.core.proto.impl.ALProtoBuilder.alpProtocolMessage;
import static org.ardulink.core.proto.impl.ALProtoBuilder.ALPProtocolKey.ANALOG_PIN_READ;
import static org.ardulink.core.proto.impl.ALProtoBuilder.ALPProtocolKey.DIGITAL_PIN_READ;
import static org.ardulink.core.proto.impl.ALProtoBuilder.ALPProtocolKey.START_LISTENING_ANALOG;
import static org.ardulink.core.proto.impl.ALProtoBuilder.ALPProtocolKey.START_LISTENING_DIGITAL;
import static org.ardulink.testsupport.mock.TestSupport.getMock;
import static org.ardulink.util.Integers.tryParse;
import static org.ardulink.util.ServerSockets.freePort;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.ardulink.core.AbstractListenerLink;
import org.ardulink.core.Link;
import org.ardulink.core.Pin;
import org.ardulink.core.Pin.AnalogPin;
import org.ardulink.core.Pin.DigitalPin;
import org.ardulink.core.Pin.Type;
import org.ardulink.core.Tone;
import org.ardulink.core.convenience.Links;
import org.ardulink.core.events.AnalogPinValueChangedEvent;
import org.ardulink.core.events.DefaultAnalogPinValueChangedEvent;
import org.ardulink.core.events.DefaultDigitalPinValueChangedEvent;
import org.ardulink.core.events.DigitalPinValueChangedEvent;
import org.ardulink.core.events.EventListener;
import org.ardulink.core.events.PinValueChangedEvent;
import org.ardulink.core.messages.api.FromDeviceMessage;
import org.ardulink.core.messages.api.FromDeviceMessagePinStateChanged;
import org.ardulink.core.proto.api.Protocol;
import org.ardulink.core.proto.impl.ArdulinkProtocol2;
import org.ardulink.testsupport.mock.ThreadLocalMockLinkFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * [ardulinktitle] [ardulinkversion]
 * 
 * project Ardulink http://www.ardulink.org/
 * 
 * [adsense]
 *
 */
public class ArdulinkRestTest {

	private static final String HEADER_PIN = "Pin";
	private static final String HEADER_TYPE = "Type";

	@Before
	public void setup() {
		port = freePort();
	}

	@Test
	public void canSwitchDigitalPin() throws Exception {
		try (Link link = Links.getLink("ardulink://mock")) {
			Link mock = getMock(link);
			try (CamelContext context = startCamelRest("ardulink://mock")) {
				int pin = 5;
				boolean state = true;
				given().body(state).post("/pin/digital/{pin}", pin).then().statusCode(200);
				verify(mock).switchDigitalPin(digitalPin(pin), state);
				context.stop();
			}
			verify(mock).close();
		}
	}

	@Test
	public void canSwitchAnalogPin() throws Exception {
		try (Link link = Links.getLink("ardulink://mock")) {
			Link mock = getMock(link);
			try (CamelContext context = startCamelRest("ardulink://mock")) {
				int pin = 9;
				int value = 123;
				given().body(value).post("/pin/analog/{pin}", pin).then().statusCode(200);
				verify(mock).switchAnalogPin(analogPin(pin), value);
				context.stop();
			}
			verify(mock).close();
		}
	}

	@Test
	public void canReadDigitalPin() throws Exception {
		int pin = 5;
		boolean state = true;
		try (AbstractListenerLink link = createAbstractListenerLink(
				new DefaultDigitalPinValueChangedEvent(digitalPin(pin), state))) {
			ThreadLocalMockLinkFactory.setLink(link);
			try (CamelContext context = startCamelRest("ardulink://threadlocalmock")) {
				given().get("/pin/digital/{pin}", pin).then().statusCode(200).body(is(String.valueOf(state)));
				context.stop();
			}
		}
	}

	@Test
	public void canReadAnalogPin() throws Exception {
		int pin = 7;
		int value = 456;
		try (AbstractListenerLink link = createAbstractListenerLink(
				new DefaultAnalogPinValueChangedEvent(analogPin(pin), value))) {
			ThreadLocalMockLinkFactory.setLink(link);
			try (CamelContext context = startCamelRest("ardulink://threadlocalmock")) {
				given().get("/pin/analog/{pin}", pin).then().statusCode(200).body(is(String.valueOf(value)));
				context.stop();
			}
		}
	}

	private CamelContext startCamelRest(String arduino) throws Exception {
		CamelContext context = new DefaultCamelContext();
		context.addRoutes(new RouteBuilder() {
			@Override
			public void configure() throws Exception {
				BlockingQueue<FromDeviceMessagePinStateChanged> messages = new ArrayBlockingQueue<FromDeviceMessagePinStateChanged>(
						16);
				String readAnalog = "direct:readAnalog-" + identityHashCode(this);
				String readDigital = "direct:readDigital-" + identityHashCode(this);
				String switchAnalog = "direct:switchAnalog-" + identityHashCode(this);
				String switchDigital = "direct:switchDigital-" + identityHashCode(this);
				restConfiguration().host("localhost").port(port);
				rest("/pin") //
						.consumes("application/json").produces("application/json") //
						.get("/analog/{pin}").to(readAnalog) //
						.get("/digital/{pin}").to(readDigital) //
						.post("/analog/{pin}").to(switchAnalog) //
						.post("/digital/{pin}").to(switchDigital) //
				;
				from(readAnalog).process(exchange -> readAnalog(exchange)).to(arduino)
						.process(exchange -> readQueue(exchange, messages));
				from(readDigital).process(exchange -> readDigital(exchange)).to(arduino)
						.process(exchange -> readQueue(exchange, messages));
				from(switchAnalog).process(exchange -> switchAnalog(exchange)).to(arduino);
				from(switchDigital).process(exchange -> switchDigital(exchange)).to(arduino);
				writeArduinoMessagesTo(arduino, messages);
			}

			private void readQueue(Exchange exchange, BlockingQueue<FromDeviceMessagePinStateChanged> messages)
					throws InterruptedException {
				FromDeviceMessagePinStateChanged polled = messages.poll(1, SECONDS);

				Message message = exchange.getMessage();
				if (polled == null) {
					message.setBody(null);
					return;
				}
				Pin pin = polled.getPin();
				if (Integer.compare(pin.pinNum(), ((int) message.getHeader(HEADER_PIN))) == 0) {
					message.setBody(polled.getValue(), String.class);
					// TODO now we should stop listening
				} else {
					messages.offer(polled);
				}
			}

			private void readAnalog(Exchange exchange) {
				Type type = ANALOG;
				Message message = exchange.getMessage();
				int pin = readPin(type, message);
				setHeaders(message, type, pin)
						.setBody(alpProtocolMessage(START_LISTENING_ANALOG).forPin(pin).withoutValue());
			}

			private void readDigital(Exchange exchange) {
				Type type = DIGITAL;
				Message message = exchange.getMessage();
				int pin = readPin(type, message);
				setHeaders(message, type, pin)
						.setBody(alpProtocolMessage(START_LISTENING_DIGITAL).forPin(pin).withoutValue());
			}

			private Message setHeaders(Message message, Type type, int pin) {
				message.setHeader(HEADER_PIN, pin);
				message.setHeader(HEADER_TYPE, type);
				return message;
			}

			private int readPin(Type type, Message message) {
				Object pinRaw = message.getHeader("pin");
				return tryParse(String.valueOf(pinRaw)).getOrThrow("Pin %s not parseable", pinRaw);
			}

			private void writeArduinoMessagesTo(String arduino,
					BlockingQueue<FromDeviceMessagePinStateChanged> messages) {
				Protocol proto = ArdulinkProtocol2.instance();
				from(arduino).process(exchange -> {
					String body = exchange.getMessage().getBody(String.class);
					FromDeviceMessage fromDevice = proto.fromDevice(body.getBytes());
					if (fromDevice instanceof FromDeviceMessagePinStateChanged) {
						messages.add((FromDeviceMessagePinStateChanged) fromDevice);
					}
				});
			}

			private void switchDigital(Exchange exchange) {
				Message message = exchange.getMessage();
				Object pinRaw = message.getHeader("pin");
				String stateRaw = message.getBody(String.class);
				int pin = tryParse(String.valueOf(pinRaw)).getOrThrow("Pin %s not parseable", pinRaw);
				boolean state = parseBoolean(stateRaw);
				message.setBody(alpProtocolMessage(DIGITAL_PIN_READ).forPin(pin).withState(state));
			}

			private void switchAnalog(Exchange exchange) {
				Message message = exchange.getMessage();
				Object pinRaw = message.getHeader("pin");
				String valueRaw = message.getBody(String.class);
				int pin = tryParse(String.valueOf(pinRaw)).getOrThrow("Pin %s not parseable", pinRaw);
				int value = tryParse(valueRaw).getOrThrow("Value %s not parseable", valueRaw);
				message.setBody(alpProtocolMessage(ANALOG_PIN_READ).forPin(pin).withValue(value));
			}

		});
		context.start();
		return context;
	}

	private AbstractListenerLink createAbstractListenerLink(PinValueChangedEvent... events) {
		return new AbstractListenerLink() {

			@Override
			public Link addListener(EventListener listener) throws IOException {
				Link link = super.addListener(listener);
				for (PinValueChangedEvent event : events) {
					if (event instanceof AnalogPinValueChangedEvent) {
						fireStateChanged((AnalogPinValueChangedEvent) event);
					} else if (event instanceof DigitalPinValueChangedEvent) {
						fireStateChanged((DigitalPinValueChangedEvent) event);
					}
				}
				return link;
			}

			@Override
			public long switchDigitalPin(DigitalPin digitalPin, boolean value) throws IOException {
				return 0;
			}

			@Override
			public long switchAnalogPin(AnalogPin analogPin, int value) throws IOException {
				return 0;
			}

			@Override
			public long stopListening(Pin pin) throws IOException {
				return 0;
			}

			@Override
			public long startListening(Pin pin) throws IOException {
				return 0;
			}

			@Override
			public long sendTone(Tone tone) throws IOException {
				return 0;
			}

			@Override
			public long sendNoTone(AnalogPin analogPin) throws IOException {
				return 0;
			}

			@Override
			public long sendKeyPressEvent(char keychar, int keycode, int keylocation, int keymodifiers,
					int keymodifiersex) throws IOException {
				return 0;
			}

			@Override
			public long sendCustomMessage(String... messages) throws IOException {
				return 0;
			}
		};
	}
}
