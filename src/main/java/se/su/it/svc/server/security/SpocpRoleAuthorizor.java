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

package se.su.it.svc.server.security;

import org.slf4j.LoggerFactory;
import org.spocp.client.SPOCPConnection;
import org.spocp.client.SPOCPConnectionFactoryImpl;
import org.spocp.client.SPOCPResult;

public class SpocpRoleAuthorizor implements Authorizor {
  public static final String SERVICE_PACKAGE = "se.su.it.svc.";

  private static SpocpRoleAuthorizor instance = null;

  static final org.slf4j.Logger logger = LoggerFactory.getLogger(SpocpRoleAuthorizor.class);

  private SPOCPConnectionFactoryImpl spocpConnectionFactory = null;

  /**
   * Create a new authorizor & set up the spocp connection factory
   */
  private SpocpRoleAuthorizor() {}

  /**
   * Get an instance of this singleton.
   *
   * @return a SpocpRoleAuthorizor
   */
  public static synchronized SpocpRoleAuthorizor getInstance() {
    if (instance == null) {
      instance = new SpocpRoleAuthorizor();
    }
    return instance;
  }

  public void setSpocpConnectionFactory(SPOCPConnectionFactoryImpl spocpConnectionFactory) {
    this.spocpConnectionFactory = spocpConnectionFactory;
  }

  /**
   * Check the uid against a role for the class of the specified uri.
   *
   * @param uid the user uid.
   * @param role the uri to get the role for.
   *
   * @return true if the user has the role, otherwise false.
   */
  @Override
  public final boolean checkRole(String uid, String role) {
    if (spocpConnectionFactory == null) {
      throw new IllegalStateException("No SPOCPConnectionFactoryImpl has been set.");
    }

    boolean authorized;

    if (role == null) {
      logger.debug("No SPOCP authentication for user=': " + uid + "', role='" + role + "'.");
      authorized = true;
    } else if (uid == null) {
      logger.debug("No uid to check for role='" + role + "'.");
      authorized = false;
    } else {
      authorized = doSpocpCall(uid.replaceAll("[/@].*$", ""), role);
      logger.debug("SPOCP result for " + uid + " in role " + role + ": " + authorized);
    }

    return authorized;
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
