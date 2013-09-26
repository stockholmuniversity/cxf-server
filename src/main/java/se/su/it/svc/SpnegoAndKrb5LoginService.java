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

package se.su.it.svc;

import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.SpnegoUserPrincipal;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.B64Code;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.ietf.jgss.*;

import javax.security.auth.Subject;

/**
 * Handle Negotiate requests for mechs SPNEGO & Krb5, based on org.eclipse.jetty.security.SpnegoLoginService
 */
public class SpnegoAndKrb5LoginService extends AbstractLifeCycle implements LoginService {

  private static final Logger LOG = Log.getLogger(SpnegoAndKrb5LoginService.class);

  public static final String OID_MECH_KRB5   = "1.2.840.113554.1.2.2";
  public static final String OID_MECH_SPNEGO = "1.3.6.1.5.5.2";

  private String name;
  private String targetName;
  private IdentityService service;

  public SpnegoAndKrb5LoginService( String name, String targetName ) throws Exception{
    this.name = name;
    this.targetName = targetName;
  }

  public String getName() {
    return name;
  }

  public UserIdentity login(String username, Object credentials) {
    UserIdentity userIdentity = null;

    String encodedAuthToken = (String)credentials;

    byte[] authToken = B64Code.decode(encodedAuthToken);

    GSSManager manager = GSSManager.getInstance();
    try {
      Oid[] mechs = {
              new Oid(OID_MECH_KRB5), // Krb5
              new Oid(OID_MECH_SPNEGO) // Spnego
      };

      GSSName gssName = manager.createName(targetName, null);
      GSSCredential serverCreds = manager.createCredential(gssName, GSSCredential.INDEFINITE_LIFETIME, mechs, GSSCredential.ACCEPT_ONLY);
      GSSContext gContext = manager.createContext(serverCreds);

      if (gContext == null) {
        LOG.debug("SpnegoUserRealm: failed to establish GSSContext");
      }
      else {
        while (!gContext.isEstablished()) {
            authToken = gContext.acceptSecContext(authToken,0,authToken.length);
        }
        if (gContext.isEstablished()) {
            String clientName = gContext.getSrcName().toString();
            String role = clientName.substring(clientName.indexOf('@') + 1);

            LOG.debug("SpnegoUserRealm: established a security context");
            LOG.debug("Client Principal is: " + gContext.getSrcName());
            LOG.debug("Server Principal is: " + gContext.getTargName());
            LOG.debug("Client Default Role: " + role);

            SpnegoUserPrincipal user = new SpnegoUserPrincipal(clientName, authToken);

            Subject subject = new Subject();
            subject.getPrincipals().add(user);

            userIdentity = service.newUserIdentity(subject,user, new String[]{role});
        }
      }
    }
    catch (GSSException gsse)
    {
      LOG.warn(gsse);
    }

    return userIdentity;
  }

  public boolean validate(UserIdentity user) {
    return false;
  }

  public IdentityService getIdentityService() {
    return service;
  }

  public void setIdentityService(IdentityService service) {
    this.service = service;
  }

  public void logout(UserIdentity user) {
    // TODO: implement
  }
}
