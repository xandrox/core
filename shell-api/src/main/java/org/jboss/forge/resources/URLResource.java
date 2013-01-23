/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.resources;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.inject.Singleton;

import org.jboss.forge.project.services.ResourceFactory;
import org.jboss.forge.shell.util.PathspecParser;

/**
 * Represents an URL
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 * @author Sandro Sonntag
 */
public class URLResource extends AbstractResource<URL> {

	private URL url;
	private Resource<?> home;
	private URLResourceManager resourceManager;

	public URLResource(final ResourceFactory factory, final URL url) {

		super(factory, null);
		this.url = url;
		setFlag(ResourceFlag.Node);
		home = new PathspecParser(getResourceFactory(), null, "~").resolve()
				.get(0);
		
		Bean<URLResourceManager> bean = (Bean<URLResourceManager>) factory.getManagerInstance().getBeans(URLResourceManager.class).iterator().next();
		resourceManager = (URLResourceManager) bean.create(factory.getManagerInstance().createCreationalContext(bean));
	}

	@Override
	public boolean delete() throws UnsupportedOperationException {
		return delete(true);
	}

	@Override
	public boolean delete(boolean recursive)
			throws UnsupportedOperationException {
		return resourceManager.delete(this);
	}

	@Override
	public String getName() {
		String normalizedPath = getNormalizedPath(url.getPath());
		int lastSlash = normalizedPath.lastIndexOf('/');
		if (lastSlash != 0){
			return normalizedPath.substring(lastSlash + 1, normalizedPath.length());
		} else {
			return "/";
		}
	}

	@Override
	public String getFullyQualifiedName() {
		return getNormalizedPath(url.getPath());
	}

	@Override
	public Resource<URL> createFrom(URL url) {
		return new URLResource(resourceFactory, url);
	}

	@Override
	public URL getUnderlyingResourceObject() {
		return url;
	}

	@Override
	public InputStream getResourceInputStream() {
		try {
			return resourceManager.getResourceInputStream(this);
		} catch (IOException e) {
			throw new RuntimeException("Could not open stream", e);
		}
	}

	@Override
	public Resource<?> getChild(String name) {
		try {
			return new URLResource(resourceFactory, new URL(
					url.getProtocol(), url.getHost(), url.getPort(), url.getPath() + "/" + name));
		} catch (MalformedURLException e) {
			return null;
		}
	}

	@Override
	public boolean exists() {
		return resourceManager.exists(this);
	}

	@Override
	protected List<Resource<?>> doListResources() {
		return resourceManager.listChildren(this);
	}

	@Override
	public Resource<?> getParent() {
		String path = url.getPath();
		// remove ending slash
		String pathNoEndingSlash = getNormalizedPath(path);

		// is their a url parent Parent
		if (pathNoEndingSlash.lastIndexOf("/") == 0) {
			return home;
		} else {
			String parentPath = pathNoEndingSlash.substring(0,
					pathNoEndingSlash.lastIndexOf("/"));
			try {
				return new URLResource(getResourceFactory(), new URL(
						url.getProtocol(), url.getHost(), url.getPort(),
						parentPath));
			} catch (MalformedURLException e) {
				return null;
			}
		}
	}

	private String getNormalizedPath(String path) {
		String pathNoEndingSlash = path.lastIndexOf("/") + 1 == path.length() ? path
				.substring(0, path.length() - 1) : path;
		return pathNoEndingSlash;
	}
	
	@Override
	public String toString() {
		return getNormalizedPath(url.toString());
	}

}
