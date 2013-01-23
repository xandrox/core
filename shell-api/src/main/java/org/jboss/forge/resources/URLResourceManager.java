package org.jboss.forge.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface URLResourceManager {

	boolean delete(URLResource urlResource);

	InputStream getResourceInputStream(URLResource urlResource) throws IOException;

	boolean exists(URLResource urlResource);

	List<Resource<?>> listChildren(URLResource urlResource);

}
