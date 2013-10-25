package de.shop.util.rest;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.CONFLICT;

import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import de.shop.util.interceptor.Log;
import de.shop.util.persistence.ConcurrentDeletedException;


/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
@Provider
@Log
public class ConcurrentDeletedExceptionMapper implements ExceptionMapper<ConcurrentDeletedException> {
	@Context
	private HttpHeaders headers;
	
	@Inject
	private Messages messages;
	
	@Override
	public Response toResponse(ConcurrentDeletedException e) {
		final String msg = messages.getMessage(headers, e.getMessageKey(), e.getId());
		final Response response = Response.status(CONFLICT)
		                                  .type(TEXT_PLAIN)
		                                  .entity(msg)
		                                  .build();
		return response;
	}

}
