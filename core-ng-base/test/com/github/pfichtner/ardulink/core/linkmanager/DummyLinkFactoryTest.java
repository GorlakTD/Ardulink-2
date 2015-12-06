package com.github.pfichtner.ardulink.core.linkmanager;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.github.pfichtner.ardulink.core.ConnectionBasedLink;
import com.github.pfichtner.ardulink.core.Link;
import com.github.pfichtner.ardulink.core.linkmanager.LinkManager.ConfigAttribute;
import com.github.pfichtner.ardulink.core.linkmanager.LinkManager.Configurer;
import com.github.pfichtner.ardulink.core.proto.impl.DummyProtocol;

public class DummyLinkFactoryTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void throwsExceptionOnInvalidNames() throws URISyntaxException {
		String name = "non.existing.name";
		LinkManager connectionManager = LinkManager.getInstance();
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("No factory registered for \"" + name + "\"");
		connectionManager.getConfigurer(new URI("ardulink://" + name + ""));
	}

	@Test
	public void schemaHasToBeArdulink() throws URISyntaxException {
		LinkManager connectionManager = LinkManager.getInstance();
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("schema not ardulink");
		connectionManager.getConfigurer(new URI("wrongSchema://dummy"));
	}

	@Test
	public void canCreateDummyDonnection() throws Exception {
		LinkManager connectionManager = LinkManager.getInstance();
		Link link = connectionManager.getConfigurer(
				new URI("ardulink://dummyLink")).newLink();
		assertThat(link, is(notNullValue()));
	}

	@Test
	public void canConfigureDummyConnection() throws Exception {
		LinkManager connectionManager = LinkManager.getInstance();
		String aValue = "aValue";
		int bValue = 1;
		String cValue = "cValue";
		Link link = (Link) connectionManager.getConfigurer(
				new URI("ardulink://dummyLink?a=" + aValue + "&b=" + bValue
						+ "&c=" + cValue + "&proto=dummyProto")).newLink();

		assertThat(link.getClass().getName(),
				is(ConnectionBasedLink.class.getName()));
		DummyConnection connection = (DummyConnection) ((ConnectionBasedLink) link)
				.getConnection();
		DummyLinkConfig config = connection.getConfig();
		assertThat(config.a, is(aValue));
		assertThat(config.b, is(bValue));
		assertThat(config.c, is(cValue));
		assertThat(config.protocol.getClass().getName(), is(DummyProtocol
				.getInstance().getClass().getName()));
	}

	@Test
	public void throwsExceptionOnInvalidKey() throws URISyntaxException {
		String nonExistingKey = "nonExistingKey";
		LinkManager connectionManager = LinkManager.getInstance();
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Illegal attribute " + nonExistingKey);
		connectionManager.getConfigurer(new URI("ardulink://dummyLink?"
				+ nonExistingKey + "=someValue"));
	}

	@Test
	public void canDefineChoiceValues() throws Exception {
		LinkManager connectionManager = LinkManager.getInstance();
		Configurer configurer = connectionManager.getConfigurer(new URI(
				"ardulink://dummyLink"));
		ConfigAttribute a = configurer.getAttribute("a");
		assertThat(a.hasChoiceValues(), is(TRUE));
		assertThat(a.getChoiceValues(), is(new Object[] { "aVal1", "aVal2" }));

		assertThat(configurer.getAttribute("b").hasChoiceValues(), is(FALSE));
		assertThat(configurer.getAttribute("c").hasChoiceValues(), is(FALSE));

		ConfigAttribute proto = configurer.getAttribute("proto");
		assertThat(proto.hasChoiceValues(), is(TRUE));
		assertThat(Arrays.asList(proto.getChoiceValues()),
				hasItems((Object) "dummyProto", "ardulink", "ardulink255"));
	}

	@Test
	public void cannotSetChoiceValuesThatDoNotExist_WithPreviousQuery()
			throws Exception {
		LinkManager connectionManager = LinkManager.getInstance();
		Configurer configurer = connectionManager.getConfigurer(new URI(
				"ardulink://dummyLink"));
		ConfigAttribute a = configurer.getAttribute("a");
		assertThat(a.getChoiceValues(), is(new Object[] { "aVal1", "aVal2" }));
		exception.expect(IllegalArgumentException.class);
		String invalidValue = "aVal3IsNotAvalidValue";
		exception.expectMessage(invalidValue + " is not a valid value for a, "
				+ "valid values are [aVal1, aVal2]");
		a.setValue(invalidValue);
	}

	@Test
	public void cannotSetChoiceValuesThatDoNotExist_WithoutPreviousQuery()
			throws Exception {
		LinkManager connectionManager = LinkManager.getInstance();
		Configurer configurer = connectionManager.getConfigurer(new URI(
				"ardulink://dummyLink"));
		ConfigAttribute a = configurer.getAttribute("a");
		String invalidValue = "aVal3IsNotAvalidValue";
		exception.expectMessage(invalidValue + " is not a valid value for a, "
				+ "valid values are [aVal1, aVal2]");
		a.setValue(invalidValue);
	}

	@Test
	public void canIterateRegisteredFactories() throws URISyntaxException {
		LinkManager connectionManager = LinkManager.getInstance();
		assertThat(connectionManager.listURIs(),
				is(Arrays.asList(new URI("ardulink://dummyLink"))));
	}

}
