package se.su.it.svc.security

import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.reflect.Whitebox
import org.spocp.SPOCPException
import org.spocp.SPOCPToken
import org.spocp.SPOCPTokenInputStream
import org.spocp.client.SPOCPConnection
import org.spocp.client.SPOCPConnectionFactoryImpl
import org.spocp.client.SPOCPResult
import se.su.it.svc.annotations.SuCxfSvcSpocpRole

import static org.easymock.EasyMock.anyObject
import static org.easymock.EasyMock.anyString
import static org.easymock.EasyMock.expect
import static org.easymock.EasyMock.verify
import static org.powermock.api.easymock.PowerMock.createMock
import static org.powermock.api.easymock.PowerMock.createPartialMock
import static org.powermock.api.easymock.PowerMock.expectPrivate
import static org.powermock.api.easymock.PowerMock.mockStaticPartial
import static org.powermock.api.easymock.PowerMock.replay
import static org.powermock.api.easymock.PowerMock.replayAll

@RunWith(PowerMockRunner.class)
@PrepareForTest([ SpocpRoleAuthorizor, SPOCPResult ])
class SpocpRoleAuthorizorTest {

  /**
   * Dummy class used for testing
   */
  @SuCxfSvcSpocpRole(role = 'foo')
  class DummyServiceWithRole {}

  /**
   * Dummy class used for testing
   */
  class DummyServiceWithoutRole {}

  @Test(expected=IllegalArgumentException.class)
  void "initialize: When missing server argument"() {
    SpocpRoleAuthorizor.initialize(null, "1");
  }

  @Test(expected=IllegalArgumentException.class)
  void "initialize: When missing port argument"() {
    SpocpRoleAuthorizor.initialize("server", null);
  }

  @Test(expected=NumberFormatException.class)
  void "initialize: When given an invalid port argument"() {
    SpocpRoleAuthorizor.initialize("server", "server");
  }

  @Test
  void "initialize: Test initializor"() {
    SpocpRoleAuthorizor.initialize("server", "1")
    assert Whitebox.getInternalState(SpocpRoleAuthorizor, "initialized") == true
    SPOCPConnectionFactoryImpl spiml = Whitebox.getInternalState(SpocpRoleAuthorizor.instance, "spocpConnectionFactory")
    assert spiml.server == "server"
    assert spiml.port == 1
  }


  @Test
  void "getInstance returns the same instance"() {

    def first = SpocpRoleAuthorizor.instance
    def second = SpocpRoleAuthorizor.instance
    assert first == second
  }

  @Test
  void "checkRole returns false for uid=null"() {
    mockStaticPartial(SpocpRoleAuthorizor, 'classNameFromURI')
    expect(SpocpRoleAuthorizor.classNameFromURI(anyObject(String))).andReturn('')
    replay(SpocpRoleAuthorizor)

    assert !SpocpRoleAuthorizor.instance.checkRole(null, "")
  }

  @Test
  void "checkRole returns false for uri=null"() {
    mockStaticPartial(SpocpRoleAuthorizor, 'classNameFromURI')
    expect(SpocpRoleAuthorizor.classNameFromURI(anyObject(String))).andReturn(null)
    replay(SpocpRoleAuthorizor)

    assert !SpocpRoleAuthorizor.instance.checkRole('foo', null)
  }

  @Test
  void "checkRole returns true for role=null"() {
    mockStaticPartial(SpocpRoleAuthorizor, 'classNameFromURI', 'getRole')
    expect(SpocpRoleAuthorizor.classNameFromURI(anyObject(String))).andReturn('')
    expect(SpocpRoleAuthorizor.getRole(anyObject(String))).andReturn(null)
    replay(SpocpRoleAuthorizor)

    assert SpocpRoleAuthorizor.instance.checkRole('foo', 'foo')
  }

  @Test
  void "checkRole returns true for true from spocp"() {
    mockStaticPartial(SpocpRoleAuthorizor, 'classNameFromURI', 'getRole', 'doSpocpCall')
    expect(SpocpRoleAuthorizor.classNameFromURI(anyObject(String))).andReturn('')
    expect(SpocpRoleAuthorizor.getRole(anyObject(String))).andReturn('role')
    replay(SpocpRoleAuthorizor)

    def mock = createPartialMock(SpocpRoleAuthorizor, 'doSpocpCall')
    expectPrivate(mock, 'doSpocpCall', 'foo', 'role')
            .andReturn(true)
    replay(mock)

    assert mock.checkRole('foo', 'foo')
  }

  @Test
  void "checkRole returns false for false from spocp"() {
    mockStaticPartial(SpocpRoleAuthorizor, 'classNameFromURI', 'getRole', 'doSpocpCall')
    expect(SpocpRoleAuthorizor.classNameFromURI(anyObject(String))).andReturn('')
    expect(SpocpRoleAuthorizor.getRole(anyObject(String))).andReturn('role')
    replay(SpocpRoleAuthorizor)

    def mock = createPartialMock(SpocpRoleAuthorizor, 'doSpocpCall')
    expectPrivate(mock, 'doSpocpCall', 'foo', 'role')
            .andReturn(false)
    replay(mock)

    assert !mock.checkRole('foo', 'foo')
  }

  @Test
  void "getRole returns the role"() {
    assert SpocpRoleAuthorizor.getRole(DummyServiceWithRole.name) == 'foo'
  }

  @Test
  void "getRole returns null for no role annotation"() {
    assert SpocpRoleAuthorizor.getRole(DummyServiceWithoutRole.name) == null
  }

  @Test
  void "getRole returns null for exception"() {
    def origClassLoader = Thread.currentThread().getContextClassLoader();

    Thread.currentThread().setContextClassLoader([ loadClass: { String name -> throw new Exception() } ] as ClassLoader)
    def role = SpocpRoleAuthorizor.getRole(DummyServiceWithRole.name)
    Thread.currentThread().setContextClassLoader(origClassLoader)

    assert role == null
  }

  @Test
  void "classNameFromURI returns null for uri=null"() {
    assert SpocpRoleAuthorizor.classNameFromURI(null) == null
  }

  @Test
  void "classNameFromURI returns null for uri="() {
    assert SpocpRoleAuthorizor.classNameFromURI("") == null
  }

  @Test
  void "classNameFromURI returns null for uri=slash"() {
    assert SpocpRoleAuthorizor.classNameFromURI("/") == null
  }

  @Test
  void "classNameFromURI returns correct class name"() {
    def name = DummyServiceWithRole.canonicalName

    assert SpocpRoleAuthorizor.classNameFromURI(name) == SpocpRoleAuthorizor.SERVICE_PACKAGE + name
  }

  @Test
  void "doSpocpCall returns false on exception when getting connection"() {
    def mockFactory = createMock(SPOCPConnectionFactoryImpl)
    expect(mockFactory.getConnection()).andThrow(new SPOCPException(''))
    replay(mockFactory)

    def authorizor = SpocpRoleAuthorizor.instance
    authorizor.spocpConnectionFactory = mockFactory

    assert !authorizor.doSpocpCall('foo', 'foo')
  }

  @Test
  void "doSpocpCall returns false on exception when running query"() {
    def mockConnection = createMock(SPOCPConnection)
    expect(mockConnection.query(anyString(), anyString())).andThrow(new SPOCPException(''))
    expect(mockConnection.logout()).andReturn(null)

    def mockFactory = createMock(SPOCPConnectionFactoryImpl)
    expect(mockFactory.getConnection()).andReturn(mockConnection)

    replayAll(mockFactory, mockConnection)

    def authorizor = SpocpRoleAuthorizor.instance
    authorizor.spocpConnectionFactory = mockFactory

    assert !authorizor.doSpocpCall('foo', 'foo')
  }

  @Test
  void "doSpocpCall returns false on non successful query"() {
    def token = new SPOCPToken()
    token.setData([] as byte[])
    def result = new SPOCPResult(new SPOCPTokenInputStream(token), null)
    result.code = SPOCPResult.SPOCP_DENIED

    def mockConnection = createMock(SPOCPConnection)
    expect(mockConnection.query(anyString(), anyString())).andReturn(result)
    expect(mockConnection.logout()).andReturn(null)

    def mockFactory = createMock(SPOCPConnectionFactoryImpl)
    expect(mockFactory.getConnection()).andReturn(mockConnection)

    replayAll(mockFactory, mockConnection)

    def authorizor = SpocpRoleAuthorizor.instance
    authorizor.spocpConnectionFactory = mockFactory

    assert !authorizor.doSpocpCall('foo', 'foo')
  }

  @Test
  void "doSpocpCall returns true on successful query"() {
    def token = new SPOCPToken()
    token.setData([] as byte[])
    def result = new SPOCPResult(new SPOCPTokenInputStream(token), null)
    result.code = SPOCPResult.SPOCP_SUCCESS

    def mockConnection = createMock(SPOCPConnection)
    expect(mockConnection.query(anyString(), anyString())).andReturn(result)
    expect(mockConnection.logout()).andReturn(null)

    def mockFactory = createMock(SPOCPConnectionFactoryImpl)
    expect(mockFactory.getConnection()).andReturn(mockConnection)

    replayAll(mockFactory, mockConnection)

    def authorizor = SpocpRoleAuthorizor.instance
    authorizor.spocpConnectionFactory = mockFactory

    assert authorizor.doSpocpCall('foo', 'foo')
  }

  @Test
  void "doSpocpCall handles exception during spocp logout"() {
    def mockConnection = createMock(SPOCPConnection)
    expect(mockConnection.query(anyString(), anyString())).andThrow(new SPOCPException(''))
    expect(mockConnection.logout()).andThrow(new SPOCPException('')).times(1)

    def mockFactory = createMock(SPOCPConnectionFactoryImpl)
    expect(mockFactory.getConnection()).andReturn(mockConnection)

    replayAll(mockFactory, mockConnection)

    def authorizor = SpocpRoleAuthorizor.instance
    authorizor.spocpConnectionFactory = mockFactory

    authorizor.doSpocpCall('foo', 'foo')

    verify(mockConnection)
  }
}
