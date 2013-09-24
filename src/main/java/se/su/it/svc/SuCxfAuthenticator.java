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

import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.SpnegoAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.webapp.WebAppContext;
import org.spocp.client.SPOCPConnection;
import org.spocp.client.SPOCPConnectionFactory;
import org.spocp.client.SPOCPConnectionFactoryImpl;
import org.spocp.client.SPOCPResult;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class SuCxfAuthenticator extends SpnegoAuthenticator {

  private static final int SPNEGO_TOKEN_DEFAULT_LENGTH = 10;
  private static final int SPOCP_DEFAULT_PORT = 4751;
  private static final String SPOCP_DEFAULT_SERVER = "spocp.su.se";
  private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SuCxfAuthenticator.class);
  private WebAppContext myContext = null;

  public SuCxfAuthenticator(WebAppContext context) {
    super();
    myContext = context;
  }

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

    logger.info("Intercepting request from " + request.getRemoteAddr() +  ".");

    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    String header = req.getHeader(HttpHeaders.AUTHORIZATION);

    if (!mandatory || isWsdlRequest(req)) {
      logger.info("Validation is not mandatory for request from " + request.getRemoteAddr() + " deferring authentication.");
      return _deferred;
    }

    logger.info("Validating request from " + request.getRemoteAddr());

    // check to see if we have authorization headers required to continue
    if (header == null) {

      try {
        if (_deferred.isDeferred(res)) {
          logger.info("Request is not authenticated.");
          return Authentication.UNAUTHENTICATED;
        }

        logger.info("SpengoAuthenticator: Sending challenge");
        res.setHeader(HttpHeaders.WWW_AUTHENTICATE, HttpHeaders.NEGOTIATE);
        res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return Authentication.SEND_CONTINUE;
      } catch (IOException ioe) {
        throw new ServerAuthException(ioe);
      }

    } else if (header.startsWith(HttpHeaders.NEGOTIATE)) {
      String spnegoToken = header.substring(SPNEGO_TOKEN_DEFAULT_LENGTH);

      UserIdentity user = _loginService.login(null, spnegoToken);

      if (user != null) {

        if (checkRole(user.getUserPrincipal().getName(), ((HttpServletRequest) request).getRequestURI())) {
          logger.info("SpengoAuthenticator:" + user.getUserPrincipal().getName() + " authenticated!");
          return new UserAuthentication(getAuthMethod(), user);
        }
        logger.info("SpengoAuthenticator: User <" + user.getUserPrincipal().getName() + "> Did not pass spocp rule check!");
      } else {
        logger.info("SpengoAuthenticator: Authorization failed, no principal!");
      }
    }

    try {
      logger.info("SpengoAuthenticator: Authorization failed!");
      ((HttpServletResponse) response).setHeader(HttpHeaders.WWW_AUTHENTICATE, "realm=\"" + _loginService.getName() + '"');
      ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return Authentication.SEND_CONTINUE;
    } catch (Exception e) {
      logger.error("SpengoAuthenticator: Exception when sending SC_UNAUTHORIZED)", e);
      return Authentication.SEND_CONTINUE;
    }
  }

  /**
   *
   * @param uid
   * @param rURI
   * @return
   */
  public final boolean checkRole(String uid, String rURI) {
    boolean ok = false;
    String trueUid = uid.replaceAll("/.*$", "");
    trueUid = trueUid.replaceAll("@.*$", "");

    String theClass = "se.su.it.svc." + rURI.replaceAll("/", "");
    String role = "";
    if (theClass.equals("se.su.it.svc.")) {
      return false;
    }//No service, wsdl or status.html on url
    try {
      Class annoClass = this.myContext.getClassLoader().loadClass(theClass);
      Annotation[] annotations = annoClass.getAnnotations();
      for (Annotation annotation : annotations) {
        if (annotation.annotationType().getName().equalsIgnoreCase("se.su.it.svc.annotations.SuCxfSvcSpocpRole")) {
          Method[] methods = annotation.getClass().getMethods();
          for (Method m : methods) {
            if (m.getName().equals("role")) {
              role = (String) m.invoke(annotation, null);
              logger.debug("Using SPOCP Role: " + role);
              break;
            }
          }
        }
      }
    } catch (Exception e) {
      logger.error("Could not figure out class from request URI:" + rURI + ". Faulty classname:" + theClass, e);
      return ok;
    }
    if (role != null && role.length() > 0) {
      SPOCPConnection spocp = null;
      SPOCPConnectionFactoryImpl impl = new SPOCPConnectionFactoryImpl();
      impl.setPort(SPOCP_DEFAULT_PORT);
      impl.setServer(SPOCP_DEFAULT_SERVER);
      try {
        SPOCPConnectionFactory factory = impl;
        spocp = factory.getConnection();
        if (spocp != null) {
          String q = "(j2ee-role (identity (uid " + trueUid + ") (realm SU.SE)) (role " + role + "))";
          SPOCPResult res = spocp.query("/", q);
          ok = res.getResultCode() == SPOCPResult.SPOCP_SUCCESS;
        }
      } catch (Exception ex) {
        logger.error("Could not check SPOCP Role: " + role, ex);
        ex.printStackTrace();
      } finally {
        try {
          if (spocp != null) {
            spocp.logout();
          }
        } catch (Exception ignore) {
        }
      }
      try {
        if (spocp != null) {
          spocp.logout();
        }
      } catch (Exception ignore) {
      }
    } else {
      logger.info("No SPOCP Role authentication for: " + theClass + ". Call will be let through.");
      return true;
    }
    return (ok);
  }

  /**
   * Utility method for figuring out if a request is done for a wsdl.
   * @param request
   * @return
   */
  private boolean isWsdlRequest(final HttpServletRequest request) {
    return (request != null && request.getQueryString() != null && request.getQueryString().equalsIgnoreCase("wsdl"));
  }

}
