/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.shell.plugins.builtin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
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
import org.jboss.forge.project.services.ResourceFactory;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellColor;
import org.jboss.forge.shell.events.PickupResource;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Help;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeIn;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.util.OSUtils;
import org.jboss.forge.shell.util.ResourceUtil;

/**
 * @author <a href="mailto:sso@adorsys.de">Sandro Sonntag</a>
 */
@Alias("http")
@Help("Commands for handeling RESTful webservices")
public class HTTPPlugin implements Plugin {
	

	private Executor executor;
	private final Event<PickupResource> pickUp;
	private final ResourceFactory factory;
	private Shell shell;
	
	@PostConstruct
	public void init() {
	}
	
	@Inject
	public HTTPPlugin(HttpClient httpClient, Shell shell, Event<PickupResource> pickUp, ResourceFactory factory) {
		this.shell = shell;
		this.pickUp = pickUp;
		this.factory = factory;
		executor = Executor.newInstance(httpClient);
	}

	@Command(help="send a http post request - request content can be piped as input")
	public void post(@PipeIn InputStream pipeIn,
			@Option(description = "path", defaultValue = ".") final Resource<?> path,
			@Option( name = "content-type", shortName = "c", required = true) final HTTPContentType contentType,
			@Option( name = "accept", shortName = "a", required = false) final HTTPContentType accept,
			@Option( name = "include", shortName = "i", help = "Include protocol headers in the output (H/F)", flagOnly=true) final boolean include,
			final PipeOut out) throws ClientProtocolException, IOException {
		
		if(pipeIn == null) {
			shell.println(ShellColor.RED, "post command requires a pipe in content");
			return;
		}
		
		String url = path.getUnderlyingResourceObject().toString();
		Request req = Request.Post(url);
		executeRequest(accept, contentType, include, out, req, pipeIn);
	}
	
	@Command(help="send a http post request - request content can be piped as input")
	public void put(@PipeIn InputStream pipeIn, 
			@Option(description = "path", defaultValue = ".") final Resource<?> path,
			@Option( name = "content-type", shortName = "c", required = true) final HTTPContentType contentType,
			@Option( name = "accept", shortName = "a", required = false) final HTTPContentType accept,
			@Option( name = "include", shortName = "i", help = "Include protocol headers in the output (H/F)", flagOnly=true) final boolean include, 
			final PipeOut out) throws ClientProtocolException, IOException {
		if(pipeIn == null) {
			shell.println(ShellColor.RED, "put command requires a pipe in content");
			return;
		}
		String url = path.getUnderlyingResourceObject().toString();
		Request req = Request.Put(url);
		executeRequest(accept,contentType, include, out, req, pipeIn);
	}

	private void executeRequest(final HTTPContentType accept,
			HTTPContentType contentType, final boolean include, final PipeOut pipeOut, Request req, InputStream pipeIn)
			throws ClientProtocolException, IOException {
		
		File pipe = File.createTempFile("forge", ".pipein");
		pipe.createNewFile();
        OutputStream out = new FileOutputStream(pipe);
        try
        {
           byte buf[] = new byte[1024];
           int len;
           while ((len = pipeIn.read(buf)) > 0)
           {
              out.write(buf, 0, len);
           }
        }
        finally
        {
           if (pipeIn != null)
        	   pipeIn.close();

           out.flush();
           out.close();
           if (OSUtils.isWindows())
           {
              System.gc();
           }
        }
		req.bodyFile(pipe, contentType.toHCContentType());
		if (accept != null) {
			req.addHeader("accept", accept.toString());
		}
		try {
			executor.execute(req).handleResponse(new ResponseHandler<Void>() {

				@Override
				public Void handleResponse(HttpResponse response)
						throws ClientProtocolException, IOException {
					if (include) {
						printResponseHeaders(response, pipeOut);
						printStatus(response, pipeOut);
					}
					HttpEntity entity = response.getEntity();
					if (entity !=  null) {
						pipeOut.write(EntityUtils.toByteArray(entity));
					}
					
					Header location = response.getFirstHeader("Location");
					if (location != null) {
						pickUp.fire(new PickupResource((Resource<?>) ResourceUtil.parsePathspec(factory, null, location.getValue())));
					}
					
					return null;
				}
			});
		} finally {
			pipe.delete();
		}
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
