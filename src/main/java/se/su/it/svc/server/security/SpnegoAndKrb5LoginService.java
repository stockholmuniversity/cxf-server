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

import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.SpnegoUserPrincipal;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.B64Code;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.ietf.jgss.*;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;

/**
 * Handle Negotiate requests for mechs SPNEGO & Krb5, based on org.eclipse.jetty.security.SpnegoLoginService
 */
public final class SpnegoAndKrb5LoginService extends AbstractLifeCycle implements LoginService {

  /**
   * OID for mech Krb5.
   */
  public static final String OID_MECH_KRB5   = "1.2.840.113554.1.2.2";

  /**
   * OID for mech spnego.
   */
  public static final String OID_MECH_SPNEGO = "1.3.6.1.5.5.2";

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SpnegoAndKrb5LoginService.class);

  private final String name;
  private IdentityService service;
  private final GSSName gssName;
  private final Oid[] mechs;
  private final GSSManager manager;

  public SpnegoAndKrb5LoginService( String name, String targetName ) throws GSSException {
    this.name = name;

    manager = GSSManager.getInstance();
    gssName = manager.createName(targetName, null);

    mechs = new Oid[] {
            new Oid(OID_MECH_KRB5), // Krb5
            new Oid(OID_MECH_SPNEGO) // Spnego
    };
  }

  /**
   * @see org.eclipse.jetty.security.LoginService#getName()
   */
  public String getName() {
    return name;
  }

  /**
   * Login a user using GSSAPI through SPNEGO or KRB5 mechs.
   *
   * @param username will not be used, credentials contains all that's necessary.
   * @param credentials a auth token String. Expect ClassCastException for anything else.
   * @return a UserIdentity if we succeed or null if we don't.
   */
  public UserIdentity login(String username, Object credentials) {
    byte[] authToken = B64Code.decode((String)credentials);
    //Just to make sure no other api's have changed this setting such as Apache LDAP API version 1.0.0-M24 in
    //LdapNetworkConnection.java on line 1558.
    //If set to true GSSAPI negotiation fails in HTTP accept.
    System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");

    try {
      GSSContext gssContext = setupContext();

      if (gssContext == null) {
        throw new IllegalStateException("GSS: Failed to establish GSSContext");
      }

      while (!gssContext.isEstablished()) {
        authToken = gssContext.acceptSecContext(authToken,0,authToken.length);
      }

      GSSName srcName = gssContext.getSrcName();
      String clientName = srcName.toString();
      String role = clientName.substring(clientName.indexOf('@') + 1);

      LOG.debug("GSS: Established a security context");
      LOG.debug("GSS: Client Principal is: " + srcName);
      LOG.debug("GSS: Server Principal is: " + gssContext.getTargName());
      LOG.debug("GSS: Client Default Role: " + role);

      SpnegoUserPrincipal user = new SpnegoUserPrincipal(clientName, authToken);
      Subject subject = new Subject();
      subject.getPrincipals().add(user);

      return service.newUserIdentity(subject, user, new String[]{role});
    } catch (GSSException gsse) {
      // Can't throw exception forward due to interface implementation
      LOG.info("GSS: Failed while validating credentials: " + gsse.getMessage());
      LOG.debug("", gsse);
    }

    return null;
  }

  /**
   * Validate a user identity.
   *
   * @param user the UserIdentity to validate.
   * @return always false
   * @see LoginService#validate(org.eclipse.jetty.server.UserIdentity)
   */
  public boolean validate(UserIdentity user) {
    return false; // A previously created user identity is never valid.
  }

  /**
   * @see org.eclipse.jetty.security.LoginService#getIdentityService() ()
   */
  public IdentityService getIdentityService() {
    return service;
  }

  /**
   * @see LoginService#setIdentityService(org.eclipse.jetty.security.IdentityService)
   */
  public void setIdentityService(IdentityService service) {
    this.service = service;
  }

  /**
   * Not implemented, not needed
   */
  public void logout(UserIdentity user) {
    // No need to implement.
  }

  /**
   * Setup & return a GSSContext.
   *
   * @return a GSSContext.
   * @throws GSSException if something goes wrong with the setup of the context.
   */
  private GSSContext setupContext() throws GSSException {
    GSSContext context = null;

    if (manager != null) {
      GSSCredential serverCreds = manager.createCredential(gssName, GSSCredential.INDEFINITE_LIFETIME, mechs, GSSCredential.ACCEPT_ONLY);
      context = manager.createContext(serverCreds);
    }

    return context;
  }
}
