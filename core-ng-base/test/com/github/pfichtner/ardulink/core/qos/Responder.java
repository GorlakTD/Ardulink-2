package com.github.pfichtner.ardulink.core.qos;

import java.io.Closeable;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zu.ardulink.util.Lists;

import com.github.pfichtner.ardulink.core.StreamReader;
import com.github.pfichtner.ardulink.core.proto.api.Protocol;
import com.github.pfichtner.ardulink.core.proto.impl.ArdulinkProtocol255;
import com.github.pfichtner.ardulink.core.proto.impl.ArdulinkProtocolN;

public class Responder implements Closeable {

	private final static Logger logger = LoggerFactory
			.getLogger(Responder.class);

	interface ReponseGenerator {

		boolean matches(String received);

		String getResponse();

	}

	public class Adder implements ReponseGenerator {

		private String whenReceived;
		private String thenRespond;

		public Adder(String whenReceived) {
			this.whenReceived = whenReceived;
		}

		public void thenRespond(String thenRespond) {
			this.thenRespond = thenRespond;
			data.add(this);
		}

		@Override
		public boolean matches(String received) {
			return whenReceived.equals(received);
		}

		@Override
		public String getResponse() {
			return thenRespond;
		}

	}

	public Matcher matcher;

	public class RegexAdder implements ReponseGenerator {

		private Pattern whenReceived;
		private String thenRespond;

		public RegexAdder(Pattern whenReceived) {
			this.whenReceived = whenReceived;
		}

		public void thenRespond(String thenRespond) {
			this.thenRespond = thenRespond;
			data.add(this);
		}

		@Override
		public boolean matches(String received) {
			matcher = whenReceived.matcher(received);
			return matcher.matches();
		}

		@Override
		public String getResponse() {
			return String.format(thenRespond, collectGroups());
		}

		private Object[] collectGroups() {
			int groupCount = matcher.groupCount();
			Object[] groups = new Object[groupCount];
			for (int i = 0; i < groupCount; i++) {
				groups[i] = matcher.group(i + 1);
			}
			return groups;
		}

		public void thenDoNotRespond() {
			// does nothing
		}

	}

	private final List<ReponseGenerator> data = Lists.newArrayList();
	private final PipedInputStream is2;
	private final PipedOutputStream os1;

	public Responder() throws IOException {
		final Protocol protocol = ArdulinkProtocolN.instance();
		PipedInputStream is1 = new PipedInputStream();
		os1 = new PipedOutputStream(is1);

		is2 = new PipedInputStream();
		final PipedOutputStream os2 = new PipedOutputStream(is2);

		new StreamReader(is1) {
			@Override
			protected void received(byte[] bytes) throws Exception {
				String received = new String(bytes);
				logger.info("Received {}", received);

				for (ReponseGenerator generator : data) {
					if (generator.matches(received)) {
						String response = generator.getResponse();
						logger.info("Responding {}", response);
						os2.write(response.getBytes());
						os2.write(protocol.getSeparator());
						os2.flush();
					} else {
						logger.warn("No responder for {}", received);
					}
				}
			}
		}.runReaderThread(new String(ArdulinkProtocol255.instance()
				.getSeparator()));
	}

	public PipedOutputStream getOutputStream() {
		return os1;
	}

	public PipedInputStream getInputStream() {
		return is2;
	}

	public Adder whenReceive(String whenReceived) {
		return new Adder(whenReceived);
	}

	public RegexAdder whenReceive(Pattern pattern) {
		return new RegexAdder(pattern);
	}

	@Override
	public void close() throws IOException {
		is2.close();
		os1.close();
	}

}