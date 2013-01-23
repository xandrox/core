/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.shell.plugins.builtin;

/**
 * @author Sandro Sonntag
 */
public enum HTTPContentType {
	JSON(org.apache.http.entity.ContentType.APPLICATION_JSON),
	XML(org.apache.http.entity.ContentType.APPLICATION_XML),
	TEXT(org.apache.http.entity.ContentType.TEXT_PLAIN);
	
	private final org.apache.http.entity.ContentType ct;

	HTTPContentType(org.apache.http.entity.ContentType ct) {
		this.ct = ct;
	}

	public org.apache.http.entity.ContentType toHCContentType() {
		return ct;
	}
	
}