package org.jboss.forge.shell.util;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.forge.shell.Shell;


public class HttpClientFactory {
	
	@Inject
	Shell shell;
	
	@Singleton
	@Produces
	public HttpClient createHttpClient() {
		DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
		defaultHttpClient.setCredentialsProvider(new BasicCredentialsProvider() {
			
			@Override
			public Credentials getCredentials(AuthScope authscope) {
				Credentials credentials = super.getCredentials(authscope);
				if (credentials == null) {
					shell.println("Please enter HTTP Credentials for: " + authscope.toString());
					String username = shell.prompt("Username");
					String password = shell.promptSecret("Password");
					credentials = new UsernamePasswordCredentials(username, password);
					super.setCredentials(authscope, credentials);
				}
				return credentials;
			}
			
		});
		return defaultHttpClient;
	}
	
}
