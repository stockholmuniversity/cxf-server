/*
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

package se.su.it.svc.security;

import org.slf4j.LoggerFactory;
import org.spocp.client.SPOCPConnection;
import org.spocp.client.SPOCPConnectionFactoryImpl;
import org.spocp.client.SPOCPResult;
import se.su.it.svc.annotations.SuCxfSvcSpocpRole;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class SpocpRoleAuthorizor {
  public static final String SERVICE_PACKAGE = "se.su.it.svc.";

  private static boolean initialized = false;

  private static SpocpRoleAuthorizor instance;

  static final org.slf4j.Logger logger = LoggerFactory.getLogger(SpocpRoleAuthorizor.class);

  private SPOCPConnectionFactoryImpl spocpConnectionFactory = new SPOCPConnectionFactoryImpl();

  /**
   * Create a new authorizor & set up the spocp connection factory
   */
  private SpocpRoleAuthorizor() {}

  /**
   * Initializes the SpocpRoleAuthenticator, needs to be done before getInstance can be used.
   *
   * @param server
   * @param port
   */
  public static synchronized void initialize(String server, String port) {
    if (server == null) {
      throw new IllegalArgumentException("Spocp server must be specified.");
    }

    Integer spocpPort = Integer.parseInt(port);

    if (!initialized) {
      instance = new SpocpRoleAuthorizor();
      instance.spocpConnectionFactory.setPort(spocpPort);
      instance.spocpConnectionFactory.setServer(server);
      initialized = true;
      logger.info("SpocpRoleAuthorizor has been initialized.");
    } else {
      logger.info("SpocpRoleAuthorizor has already been initialized.");
    }
  }

  /**
   * Get an instance of this singleton.
   *
   * @return a SpocpRoleAuthorizor
   */
  public static SpocpRoleAuthorizor getInstance() {
    if (!initialized) {
      throw new IllegalStateException("Role Authorizor has not yet been initialized.");
    }
    return instance;
  }

  /**
   * Check the uid against a role for the class of the specified uri.
   *
   * @param uid the user uid.
   * @param uri the uri to get the role for.
   *
   * @return true if the user has the role, otherwise false.
   */
  public final boolean checkRole(String uid, String uri) {
    boolean authorized = false;

    String className = classNameFromURI(uri);
    if (uid != null && className != null) {

      String role = getRole(className);
      if (role != null && role.length() > 0) {
        authorized = doSpocpCall(uid.replaceAll("[/@].*$", ""), role);
        logger.debug("SPOCP result for " + uid + " in role " + role + ": " + authorized);
      } else {
        logger.debug("No SPOCP Role authentication for: " + className + ". Call will be let through.");
        return true;
      }
    }

    return authorized;
  }

  /**
   * Get the role for the specified class name.
   *
   * @param className the name of the class to get the role for.
   * @return the role name.
   */
  protected static String getRole(String className) {
    String role = null;
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    try {
      // TODO: Can we do this without reflection? Do we need the other class loader?
      Class serviceClass = classLoader.loadClass(className);
      Class annotationClass = classLoader.loadClass(SuCxfSvcSpocpRole.class.getName());

      Annotation annotation = serviceClass.getAnnotation(annotationClass);

      /** If class has the requested annotation we check the role. */
      if (annotation != null) {
        Method m = annotation.getClass().getMethod("role", null);
        role = (String) m.invoke(annotation, null);
      }
    } catch (Exception e) {
      // Swallow exceptions & return null
      logger.error("Could not figure out class name from request. Faulty classname:" + className, e);
    }

    return role;
  }

  /**
   * Get the class name corresponding to the specified uri.
   *
   * @param uri the uri.
   * @return the class name.
   */
  protected static String classNameFromURI(String uri) {
    String className = null;

    if (uri != null) {
      className = SERVICE_PACKAGE + uri.replaceAll("/", "");

      if (className.equals(SERVICE_PACKAGE)) {
        className = null;
      }
    }

    return className;
  }

  /**
   * Make the SPOCP call to check if a user has a role.
   *
   * @param uid the uid of the user
   * @param role the role to look for.
   * @return true if the user has the role, false otherwise.
   */
  private boolean doSpocpCall(String uid, String role) {
    boolean result = false;

    SPOCPConnection spocp = null;
    try {
      spocp = spocpConnectionFactory.getConnection();
      if (spocp != null) {
        String q = "(j2ee-role (identity (uid " + uid + ") (realm SU.SE)) (role " + role + "))";
        SPOCPResult res = spocp.query("/", q);
        result = res.getResultCode() == SPOCPResult.SPOCP_SUCCESS;
      }
    } catch (Exception ex) {
      logger.error("Could not check SPOCP Role: " + role, ex);
    } finally {
      try {
        if (spocp != null) {
          spocp.logout();
        }
      } catch (Exception ignore) {}
    }

    return result;
  }
}
