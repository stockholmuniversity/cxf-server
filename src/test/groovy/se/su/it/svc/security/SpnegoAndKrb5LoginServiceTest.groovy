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

package se.su.it.svc.security

import org.eclipse.jetty.security.DefaultIdentityService
import org.eclipse.jetty.security.SpnegoUserIdentity
import org.eclipse.jetty.security.SpnegoUserPrincipal
import org.eclipse.jetty.server.UserIdentity
import org.ietf.jgss.GSSContext
import org.ietf.jgss.GSSException
import org.ietf.jgss.GSSManager
import org.ietf.jgss.GSSName
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import sun.security.jgss.GSSNameImpl

import static org.easymock.EasyMock.anyByte
import static org.easymock.EasyMock.anyInt
import static org.easymock.EasyMock.anyObject
import static org.easymock.EasyMock.expect
import static org.powermock.api.easymock.PowerMock.createMock
import static org.powermock.api.easymock.PowerMock.expectPrivate
import static org.powermock.api.easymock.PowerMock.mockStatic
import static org.powermock.api.easymock.PowerMock.replay
import static org.powermock.api.easymock.PowerMock.verify


@RunWith(PowerMockRunner.class)
@PrepareForTest([ SpnegoAndKrb5LoginService, GSSManager ])
class SpnegoAndKrb5LoginServiceTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  void "Test that constructor throws exception if no GSSContext"() {
    mockStatic(SpnegoAndKrb5LoginService)
    expectPrivate(SpnegoAndKrb5LoginService, 'setupContext', anyObject(String))
            .andReturn(null)
    replay(SpnegoAndKrb5LoginService)

    thrown.expect(IllegalStateException)
    new SpnegoAndKrb5LoginService('foo', 'bar')
  }

  @Test
  void "Test that constructor sets name"() {
    def gssContext = createMock(GSSContext)
    mockStatic(SpnegoAndKrb5LoginService)
    expectPrivate(SpnegoAndKrb5LoginService, 'setupContext', anyObject(String))
            .andReturn(gssContext)
    replay(SpnegoAndKrb5LoginService)

    def expected = 'foo'
    def actual = new SpnegoAndKrb5LoginService(expected, 'bar').name

    assert actual == expected
  }

  @Test
  void "Test that constructor sets up GSS context"() {
    def gssContext = createMock(GSSContext)
    mockStatic(SpnegoAndKrb5LoginService)
    expectPrivate(SpnegoAndKrb5LoginService, 'setupContext', 'bar')
            .andReturn(gssContext)
            .times(1)
    replay(SpnegoAndKrb5LoginService)

    new SpnegoAndKrb5LoginService('foo', 'bar')

    verify(SpnegoAndKrb5LoginService)
  }

  @Test
  void "login handles exception on no creds"() {
    def gssContext = createMock(GSSContext)
    expect(gssContext.isEstablished())
            .andReturn(false)
    expect(gssContext.acceptSecContext(anyObject(byte[]), anyInt(), anyInt()))
            .andThrow(new GSSException(GSSException.NO_CRED))
    replay(gssContext)

    mockStatic(SpnegoAndKrb5LoginService)
    expectPrivate(SpnegoAndKrb5LoginService, 'setupContext', anyObject(String))
            .andReturn(gssContext)
    replay(SpnegoAndKrb5LoginService)

    def actual = new SpnegoAndKrb5LoginService('foo', 'bar').login(null, '12345')

    assert actual == null
  }

  @Test
  void "login happy path"() {
    def name = 'jolu'
    def role = 'SU.SE'
    def nameAndRole = "$name@$role"
    def token = '12345' as byte[]

    def gssContext = createMock(GSSContext)
    def gssName = [ toString: {nameAndRole as String} ] as GSSName
    mockStatic(SpnegoAndKrb5LoginService)

    expect(gssContext.isEstablished())
            .andReturn(false)
            .times(1)
            .andReturn(true)
    expect(gssContext.acceptSecContext(anyObject(byte[]), anyInt(), anyInt()))
            .andReturn(token)
    expect(gssContext.getSrcName())
            .andReturn(gssName)
    expect(gssContext.getTargName())
            .andReturn(gssName)

    expectPrivate(SpnegoAndKrb5LoginService, 'setupContext', anyObject(String))
            .andReturn(gssContext)

    replay(gssContext)
    replay(SpnegoAndKrb5LoginService)

    def service = new SpnegoAndKrb5LoginService('foo', 'bar')
    service.identityService = new DefaultIdentityService()

    def actual = service.login(null, '12345')

    assert actual instanceof UserIdentity
    assert actual.isUserInRole(role, null)
    assert actual.subject.principals.contains(actual.userPrincipal)
    assert actual.userPrincipal instanceof SpnegoUserPrincipal
    assert actual.userPrincipal.name == nameAndRole
    assert (actual.userPrincipal as SpnegoUserPrincipal).token == token
  }

  @Test
  void "validate returns false"() {
    def gssContext = createMock(GSSContext)
    mockStatic(SpnegoAndKrb5LoginService)
    expectPrivate(SpnegoAndKrb5LoginService, 'setupContext', 'bar')
            .andReturn(gssContext)
    replay(SpnegoAndKrb5LoginService)

    assert ! new SpnegoAndKrb5LoginService('foo', 'bar').validate(null)
  }

  @Test
  void "getIdentityService returns the identity service"() {
    def gssContext = createMock(GSSContext)
    mockStatic(SpnegoAndKrb5LoginService)
    expectPrivate(SpnegoAndKrb5LoginService, 'setupContext', 'bar')
            .andReturn(gssContext)
    replay(SpnegoAndKrb5LoginService)

    def identityService = new DefaultIdentityService()
    def service = new SpnegoAndKrb5LoginService('foo', 'bar')
    service.identityService = identityService

    assert service.identityService == identityService
  }

  @Test
  void "logout don't throw exception"() {
    def gssContext = createMock(GSSContext)
    mockStatic(SpnegoAndKrb5LoginService)
    expectPrivate(SpnegoAndKrb5LoginService, 'setupContext', 'bar')
            .andReturn(gssContext)
    replay(SpnegoAndKrb5LoginService)

    new SpnegoAndKrb5LoginService('foo', 'bar').logout(null)
  }
}
