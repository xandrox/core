/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.shell.plugins.builtin;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.URLResource;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellColor;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.DefaultCommand;
import org.jboss.forge.shell.plugins.Help;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresResource;
import org.jboss.forge.shell.plugins.Topic;

/**
 * @author Sandro Sonntag
 */
@Alias("ls")
@RequiresResource({ URLResource.class })
@Topic("File & Resources")
@Help("Prints the contents of the current URL")
public class LsURLPlugin implements Plugin {
	
	@Inject
	private HttpClient httpClient;

	@Inject
	private Shell shell;

	private Executor executor;
	
	@PostConstruct
	public void init() {
		executor = Executor.newInstance(httpClient);
	}

	@DefaultCommand
	public void run(
			@Option(description = "path", defaultValue = ".") final Resource<?>[] paths,
			@Option(flagOnly = true, name = "all", shortName = "a", required = false) final boolean showAll,
			@Option(flagOnly = true, name = "list", shortName = "l", required = false) final boolean list,
			@Option( name = "format", shortName = "f", required = false) final HTTPContentType format,
			final PipeOut out) throws ClientProtocolException, IOException {
		
		URLResource resource = (URLResource) shell.getCurrentResource();
		Request req = Request.Get(resource.getUnderlyingResourceObject().toString());
		if (format != null) {
			req.addHeader("accept", format.toString());
		}
		executor.execute(req).handleResponse(new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				if (showAll) {
					printResponseHeaders(response, out);
					printStatus(response, out);
				}
				HttpEntity entity = response.getEntity();
				if (entity !=  null) {
					out.write(EntityUtils.toByteArray(entity));
					if (!out.isPiped()){
						out.println();
					}
				}
				return null;
			}
		});
	}
	

	private void printStatus(HttpResponse response, PipeOut out) {
		StatusLine sl = response.getStatusLine();
		out.print(ShellColor.BOLD, sl.getProtocolVersion() + " ");
		
		ShellColor status;
		if (sl.getStatusCode() < 300) {
			status = ShellColor.GREEN;
		} else if (sl.getStatusCode() < 500) {
			status = ShellColor.YELLOW;
		} else {
			status = ShellColor.RED;
		}
		
		out.print(status, sl.getStatusCode() + " ");
		out.println(ShellColor.BOLD, sl.getReasonPhrase());
		
	}

	private void printResponseHeaders(HttpResponse response, PipeOut out) {
		Header[] allHeaders = response.getAllHeaders();
		for (Header header : allHeaders) {
			out.print(ShellColor.CYAN, StringUtils.rightPad(header.getName(), 20, ".")  + ": ");
			out.print(ShellColor.WHITE, header.getValue());
			out.println();
		}
		
	}
}