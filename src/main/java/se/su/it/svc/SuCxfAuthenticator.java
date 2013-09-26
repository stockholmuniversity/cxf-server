package se.su.it.svc;

/**
 * Copyright (c) 2013, IT Services, Stockholm University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of Stockholm University nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.SpnegoAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class SuCxfAuthenticator extends SpnegoAuthenticator {

  private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SuCxfAuthenticator.class);

  /**
   *
   * @param request
   * @param response
   * @param mandatory
   * @return
   * @throws ServerAuthException
   */
  public final Authentication validateRequest(final ServletRequest request,
                                              final ServletResponse response,
                                              final boolean mandatory) throws ServerAuthException {
    if (isWsdlRequest(request)) {
      logger.debug("WSDL request, sending deferred.");
      return _deferred;
    }

    Authentication authentication = super.validateRequest(request, response, mandatory);

    if (authentication instanceof UserAuthentication) {
      UserAuthentication userAuthentication = (UserAuthentication) authentication;
      UserIdentity identity = userAuthentication.getUserIdentity();

      if (identity != null && identity.getUserPrincipal() != null) {
        HttpServletRequest httpRequest = ((HttpServletRequest) request);

        if (SpocpRoleAuthorizor.getInstance().checkRole(identity.getUserPrincipal().getName(), httpRequest.getRequestURI())) {
          return authentication;
        }
      }

      return Authentication.UNAUTHENTICATED;
    }

    return authentication;
  }

  /**
   * Utility method for figuring out if a request is done for a wsdl.
   * @param request
   * @return
   */
  private boolean isWsdlRequest(final ServletRequest request) {
    if (request instanceof HttpServletRequest) {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      String query = httpRequest.getQueryString();

      return query != null && query.equalsIgnoreCase("wsdl");
    }

    return false;
  }

}
