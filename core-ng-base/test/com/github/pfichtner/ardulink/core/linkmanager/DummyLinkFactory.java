package com.github.pfichtner.ardulink.core.linkmanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.github.pfichtner.ardulink.core.Connection;
import com.github.pfichtner.ardulink.core.Link;
import com.github.pfichtner.ardulink.core.ConnectionBasedLink;
import com.github.pfichtner.ardulink.core.linkmanager.LinkConfig;
import com.github.pfichtner.ardulink.core.linkmanager.LinkFactory;
import com.github.pfichtner.ardulink.core.linkmanager.DummyLinkFactory.DummyLinkConfig;
import com.github.pfichtner.ardulink.core.proto.api.Protocol;
import com.github.pfichtner.ardulink.core.proto.api.Protocols;
import com.github.pfichtner.ardulink.core.proto.impl.ArdulinkProtocol;

public class DummyLinkFactory implements LinkFactory<DummyLinkConfig> {

	public static class DummyLinkConfig implements LinkConfig {

		public String a;
		public int b;
		@Named("c")
		public String c;
		public Protocol protocol;

		@Named("a")
		public void setPort(String a) {
			this.a = a;
		}

		@Named("b")
		public void theNameOfTheSetterDoesNotMatter(int b) {
			this.b = b;
		}

		@Named("proto")
		public void setProtocol(String protocol) {
			this.protocol = Protocols.getByName(protocol);
		}

		@PossibleValueFor("a")
		public String[] possibleValuesForAtttribute_A() {
			return new String[] { "aVal1", "aVal2" };
		}

		@PossibleValueFor("proto")
		public static String[] getProtocolsMayAlsoBeStatic() {
			List<String> names = Protocols.list();
			return names.toArray(new String[names.size()]);
		}

	}

	public static class DummyConnection implements Connection {

		private final DummyLinkConfig config;
		private List<Listener> listeners = new ArrayList<Listener>();

		public DummyConnection(DummyLinkConfig config) {
			this.config = config;
		}

		@Override
		public void close() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void write(byte[] bytes) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addListener(Listener listener) {
			this.listeners.add(listener);
		}

		@Override
		public void removeListener(Listener listener) {
			this.listeners.remove(listener);
		}

		public DummyLinkConfig getConfig() {
			return config;
		}

	}

	@Override
	public String getName() {
		return "dummy";
	}

	@Override
	public Link newLink(DummyLinkConfig config) {
		return new ConnectionBasedLink(new DummyConnection(config),
				ArdulinkProtocol.instance());
	}

	@Override
	public DummyLinkConfig newLinkConfig() {
		return new DummyLinkConfig();
	}

}