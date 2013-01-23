package org.jboss.forge.shell.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.URLResource;
import org.jboss.forge.resources.URLResourceManager;
import org.jboss.forge.shell.Shell;

@Singleton
public class HttpClientURLResourceManager implements URLResourceManager {
	
	private static final Pattern URI_PATTERN = Pattern.compile("([\"](https?://[^\"]+)[\"])|(['](https?://[^'])+['])");
	

	@Inject
	private HttpClient httpClient;

	private Executor executor;
	
	@PostConstruct
	public void init() {
		executor = Executor.newInstance(httpClient);
	}

	@Override
	public boolean delete(URLResource urlResource) {
		HttpResponse response;
		try {
			response = executor.execute(Request.Delete(urlResource.getUnderlyingResourceObject().toURI())).returnResponse();
		} catch (ClientProtocolException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			return false;
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		return response.getStatusLine().getStatusCode() <= 300;
	}

	@Override
	public InputStream getResourceInputStream(URLResource urlResource)
			throws IOException {
		try {
			return executor.execute(Request.Get(urlResource.getUnderlyingResourceObject().toURI())).returnContent().asStream();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean exists(URLResource urlResource) {
		int statusCode;
		try {
			statusCode = executor.execute(Request.Head(urlResource.getUnderlyingResourceObject().toURI())).returnResponse().getStatusLine().getStatusCode();
		} catch (ClientProtocolException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			return false;
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		return statusCode <= 300;
	}

	@Override
	public List<Resource<?>> listChildren(URLResource urlResource) {
		URL parentUrl = urlResource.getUnderlyingResourceObject();
		String content;
		try {
			content = executor.execute(Request.Get(parentUrl.toURI())).returnContent().asString();
		} catch (ClientProtocolException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			return Collections.emptyList();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		
		Matcher matcher = URI_PATTERN.matcher(content);
		TreeSet<String> urls = new TreeSet<String>();
		while (matcher.find()){
			urls.add(matcher.group(2) != null ? matcher.group(2) : matcher.group(4));
		}

		String parentUrlString = parentUrl.toString();
		ArrayList<Resource<?>> arrayList = new ArrayList<Resource<?>>();
		for (String url : urls) {
			try {
				if (url.startsWith(parentUrlString)) {
					arrayList.add(new URLResource(urlResource.getResourceFactory(), new URL(url)));
				}
			} catch (MalformedURLException e) {
			}
		}
		return arrayList;
	}

}
