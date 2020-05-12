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

package org.ardulink.core.serial.nrrxtx;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.ardulink.util.Preconditions.checkState;

import java.io.IOException;

import org.ardulink.core.ConnectionBasedLink;
import org.ardulink.core.Link;
import org.ardulink.core.StreamConnection;
import org.ardulink.core.convenience.LinkDelegate;
import org.ardulink.core.linkmanager.LinkFactory;
import org.ardulink.core.linkmanager.LinkFactory.Alias;
import org.ardulink.core.qos.QosLink;

import gnu.io.NRSerialPort;

/**
 * [ardulinktitle] [ardulinkversion]
 * 
 * project Ardulink http://www.ardulink.org/
 * 
 * [adsense]
 *
 */
public class SerialLinkFactory implements LinkFactory<SerialLinkConfig> {

	@Override
	public String getName() {
		return "serial-nrrxtx";
	}

	@Override
	public LinkDelegate newLink(SerialLinkConfig config) throws IOException {
		final NRSerialPort serialPort = new NRSerialPort(config.getPort(), config.getBaudrate());
		StreamConnection connection = new StreamConnection(serialPort.getInputStream(), serialPort.getOutputStream(),
				config.getProto());

		ConnectionBasedLink connectionBasedLink = new ConnectionBasedLink(connection, config.getProto());
		Link link = config.isQos() ? new QosLink(connectionBasedLink) : connectionBasedLink;

		waitForArdulink(config, connectionBasedLink);
		return new LinkDelegate(link) {
			@Override
			public void close() throws IOException {
				super.close();
				serialPort.disconnect();
			}
		};
	}

	private void waitForArdulink(SerialLinkConfig config, ConnectionBasedLink link) {
		if (config.isPingprobe()) {
			checkState(link.waitForArduinoToBoot(config.getWaitsecs(), SECONDS),
					"Waited for arduino to boot but no response received");
		} else {
			try {
				SECONDS.sleep(config.getWaitsecs());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	@Override
	public SerialLinkConfig newLinkConfig() {
		return new SerialLinkConfig();
	}

}