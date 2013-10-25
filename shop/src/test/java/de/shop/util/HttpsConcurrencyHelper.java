package de.shop.util;

import javax.ws.rs.client.Client;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.AbstractHttpClient;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
public class HttpsConcurrencyHelper {
	private final AbstractHttpClient httpClient;
	private final Client client;
	
	public HttpsConcurrencyHelper() {
		httpClient = AbstractResourceTest.newHttpClient();
		final ClientHttpEngine engine = new ApacheHttpClient4Engine(httpClient);
		client = AbstractResourceTest.getResteasyClientBuilder().httpEngine(engine).build();
	}

	public Client getHttpsClient() {
		httpClient.getCredentialsProvider().clear();
		return client;
	}
	
	public Client getHttpsClient(String username, String password) {
		final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
		httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, credentials);
		return client;
	}

	@Override
	protected void finalize() throws Throwable {
		httpClient.getConnectionManager().shutdown();
		super.finalize();
	}
}
