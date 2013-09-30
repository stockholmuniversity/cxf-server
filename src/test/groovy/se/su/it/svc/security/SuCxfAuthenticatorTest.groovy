package se.su.it.svc.security

import org.eclipse.jetty.security.UserAuthentication
import org.eclipse.jetty.security.authentication.DeferredAuthentication
import org.eclipse.jetty.server.Authentication
import org.eclipse.jetty.server.UserIdentity
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.reflect.Whitebox

import javax.servlet.ServletRequest
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.security.Principal

import static org.easymock.EasyMock.anyObject
import static org.easymock.EasyMock.anyString
import static org.easymock.EasyMock.expect
import static org.powermock.api.easymock.PowerMock.createMock
import static org.powermock.api.easymock.PowerMock.createPartialMock
import static org.powermock.api.easymock.PowerMock.expectPrivate
import static org.powermock.api.easymock.PowerMock.mockStatic
import static org.powermock.api.easymock.PowerMock.replay
import static org.powermock.api.easymock.PowerMock.replayAll

@RunWith(PowerMockRunner)
@PrepareForTest([SuCxfAuthenticator, SpocpRoleAuthorizor])
class SuCxfAuthenticatorTest {

  @Test
  void "validateRequest returns _deferred if WSDL"() {
    def deferred = createMock(DeferredAuthentication)

    def mock = createPartialMock(SuCxfAuthenticator, 'isWsdlRequest')
    Whitebox.setInternalState(mock, "_deferred", deferred)
    expectPrivate(mock, 'isWsdlRequest', anyObject()).andReturn(true)
    replay(mock)

    def ret = mock.validateRequest(null, null, false)

    assert ret == deferred
  }

  @Test
  void "validateRequest returns Authentication from superclass if not a UserAuthentication"() {
    def mockAuthentication = createMock(Authentication)
    def mockRequest        = createMock(HttpServletRequest)
    def mockResponse       = createMock(HttpServletResponse)

    def mock = createPartialMock(SuCxfAuthenticator, 'doValidateRequest', 'isWsdlRequest')
    expectPrivate(mock, 'doValidateRequest', mockRequest, mockResponse, false).andReturn(mockAuthentication)
    expectPrivate(mock, 'isWsdlRequest', anyObject()).andReturn(false)
    replay(mock)

    def ret = mock.validateRequest(mockRequest, mockResponse, false)

    assert ret == mockAuthentication
  }

  @Test
  void "validateRequest returns unauthenticated if userIdentity == null"() {
    def mockAuthentication = createMock(UserAuthentication)
    def mockRequest        = createMock(HttpServletRequest)
    def mockResponse       = createMock(HttpServletResponse)

    expect(mockAuthentication.getUserIdentity()).andReturn(null)

    def mock = createPartialMock(SuCxfAuthenticator, 'doValidateRequest', 'isWsdlRequest')
    expectPrivate(mock, 'doValidateRequest', mockRequest, mockResponse, false).andReturn(mockAuthentication)
    expectPrivate(mock, 'isWsdlRequest', anyObject()).andReturn(false)
    replayAll(mock, mockAuthentication)

    def ret = mock.validateRequest(mockRequest, mockResponse, false)

    assert ret == Authentication.UNAUTHENTICATED
  }

  @Test
  void "validateRequest returns unauthenticated if userIdentity_userPrincipal == null"() {
    def mockAuthentication = createMock(UserAuthentication)
    def mockIdentity       = createMock(UserIdentity)
    def mockRequest        = createMock(HttpServletRequest)
    def mockResponse       = createMock(HttpServletResponse)

    expect(mockIdentity.getUserPrincipal()).andReturn(null)
    expect(mockAuthentication.getUserIdentity()).andReturn(mockIdentity)

    def mock = createPartialMock(SuCxfAuthenticator, 'doValidateRequest', 'isWsdlRequest')
    expectPrivate(mock, 'doValidateRequest', mockRequest, mockResponse, false).andReturn(mockAuthentication)
    expectPrivate(mock, 'isWsdlRequest', anyObject()).andReturn(false)
    replayAll(mock, mockAuthentication, mockIdentity)

    def ret = mock.validateRequest(mockRequest, mockResponse, false)

    assert ret == Authentication.UNAUTHENTICATED
  }

  @Test
  void "validateRequest returns unauthenticated if checkRole fails"() {
    def mockAuthentication = createMock(UserAuthentication)
    def mockIdentity       = createMock(UserIdentity)
    def mockAuthorizor     = createMock(SpocpRoleAuthorizor)
    def mockPrincipal      = createMock(Principal)
    def mockRequest        = createMock(HttpServletRequest)
    def mockResponse       = createMock(HttpServletResponse)

    mockStatic(SpocpRoleAuthorizor)
    expect(SpocpRoleAuthorizor.getInstance()).andReturn(mockAuthorizor)

    expect(mockRequest.getRequestURI()).andReturn('')
    expect(mockPrincipal.getName()).andReturn('')
    expect(mockAuthorizor.checkRole(anyString(), anyString())).andReturn(false)
    expect(mockIdentity.getUserPrincipal()).andReturn(mockPrincipal).anyTimes()
    expect(mockAuthentication.getUserIdentity()).andReturn(mockIdentity)

    def mock = createPartialMock(SuCxfAuthenticator, 'doValidateRequest', 'isWsdlRequest')
    expectPrivate(mock, 'doValidateRequest', mockRequest, mockResponse, false).andReturn(mockAuthentication)
    expectPrivate(mock, 'isWsdlRequest', anyObject()).andReturn(false)
    replayAll(mock, mockAuthentication, mockIdentity, mockAuthorizor, mockPrincipal, mockRequest, SpocpRoleAuthorizor)

    def ret = mock.validateRequest(mockRequest, mockResponse, false)

    assert ret == Authentication.UNAUTHENTICATED
  }

  @Test
  void "validateRequest returns authentication if checkRole succeeds"() {
    def mockAuthentication = createMock(UserAuthentication)
    def mockIdentity       = createMock(UserIdentity)
    def mockAuthorizor     = createMock(SpocpRoleAuthorizor)
    def mockPrincipal      = createMock(Principal)
    def mockRequest        = createMock(HttpServletRequest)
    def mockResponse       = createMock(HttpServletResponse)

    mockStatic(SpocpRoleAuthorizor)
    expect(SpocpRoleAuthorizor.getInstance()).andReturn(mockAuthorizor)

    expect(mockRequest.getRequestURI()).andReturn('')
    expect(mockPrincipal.getName()).andReturn('')
    expect(mockAuthorizor.checkRole(anyString(), anyString())).andReturn(true)
    expect(mockIdentity.getUserPrincipal()).andReturn(mockPrincipal).anyTimes()
    expect(mockAuthentication.getUserIdentity()).andReturn(mockIdentity)

    def mock = createPartialMock(SuCxfAuthenticator, 'doValidateRequest', 'isWsdlRequest')
    expectPrivate(mock, 'doValidateRequest', mockRequest, mockResponse, false).andReturn(mockAuthentication)
    expectPrivate(mock, 'isWsdlRequest', anyObject()).andReturn(false)
    replayAll(mock, mockAuthentication, mockIdentity, mockAuthorizor, mockPrincipal, mockRequest, SpocpRoleAuthorizor)

    def ret = mock.validateRequest(mockRequest, mockResponse, false)

    assert ret == mockAuthentication
  }

  @Test
  void "isWsdlRequest returns true if queryString==wsdl"() {
    def request = createMock(HttpServletRequest)
    expect(request.getQueryString()).andReturn('wsdl')
    replay(request)

    SuCxfAuthenticator authenticator = new SuCxfAuthenticator()
    def ret = Whitebox.<Boolean> invokeMethod(authenticator, 'isWsdlRequest', request)

    assert ret
  }

  @Test
  void "isWsdlRequest returns true if queryString==WSDL"() {
    def request = createMock(HttpServletRequest)
    expect(request.getQueryString()).andReturn('WSDL')
    replay(request)

    SuCxfAuthenticator authenticator = new SuCxfAuthenticator()
    def ret = Whitebox.<Boolean> invokeMethod(authenticator, 'isWsdlRequest', request)

    assert ret
  }

  @Test
  void "isWsdlRequest returns false if queryString!=wsdl"() {
    def request = createMock(HttpServletRequest)
    expect(request.getQueryString()).andReturn('foo')
    replay(request)

    SuCxfAuthenticator authenticator = new SuCxfAuthenticator()
    def ret = Whitebox.<Boolean> invokeMethod(authenticator, 'isWsdlRequest', request)

    assert !ret
  }

  @Test
  void "isWsdlRequest returns false if queryString==null"() {
    def request = createMock(HttpServletRequest)
    expect(request.getQueryString()).andReturn(null)
    replay(request)

    SuCxfAuthenticator authenticator = new SuCxfAuthenticator()
    def ret = Whitebox.<Boolean> invokeMethod(authenticator, 'isWsdlRequest', request)

    assert !ret
  }

  @Test
  void "isWsdlRequest returns false if request != HttpServletRequest"() {
    def request = createMock(ServletRequest)

    SuCxfAuthenticator authenticator = new SuCxfAuthenticator()
    def ret = Whitebox.<Boolean> invokeMethod(authenticator, 'isWsdlRequest', request)

    assert !ret
  }
}
