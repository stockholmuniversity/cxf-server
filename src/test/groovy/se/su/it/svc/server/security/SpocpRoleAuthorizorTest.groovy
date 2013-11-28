package se.su.it.svc.server.security

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.spocp.SPOCPException
import org.spocp.SPOCPToken
import org.spocp.SPOCPTokenInputStream
import org.spocp.client.SPOCPConnection
import org.spocp.client.SPOCPConnectionFactoryImpl
import org.spocp.client.SPOCPResult
import se.su.it.svc.server.annotations.AuthzRole

import static org.easymock.EasyMock.*
import static org.powermock.api.easymock.PowerMock.*

@RunWith(PowerMockRunner.class)
@PrepareForTest([ SpocpRoleAuthorizor, SPOCPResult ])
class SpocpRoleAuthorizorTest {

  /**
   * Dummy class used for testing
   */
  @AuthzRole(role = 'foo')
  class DummyServiceWithRole {}

  /**
   * Dummy class used for testing
   */
  class DummyServiceWithoutRole {}

  @Before
  void setup() {
    SpocpRoleAuthorizor.instance.spocpConnectionFactory = new SPOCPConnectionFactoryImpl()
  }

  @Test
  void "getInstance returns the same instance"() {
    def first = SpocpRoleAuthorizor.instance
    def second = SpocpRoleAuthorizor.instance
    assert first == second
  }

  @Test
  void "checkRole returns false for uid=null"() {
    assert !SpocpRoleAuthorizor.instance.checkRole(null, "")
  }

  @Test
  void "checkRole returns true for role=null"() {
    assert SpocpRoleAuthorizor.instance.checkRole('foo', null)
  }

  @Test
  void "checkRole returns true for true from spocp"() {
    def mock = createPartialMock(SpocpRoleAuthorizor, 'doSpocpCall')
    expectPrivate(mock, 'doSpocpCall', 'foo', 'role')
            .andReturn(true)
    replay(mock)
    mock.spocpConnectionFactory = new SPOCPConnectionFactoryImpl()

    assert mock.checkRole('foo', 'role')
  }

  @Test
  void "checkRole returns false for false from spocp"() {
    def mock = createPartialMock(SpocpRoleAuthorizor, 'doSpocpCall')
    expectPrivate(mock, 'doSpocpCall', 'foo', 'role')
            .andReturn(false)
    replay(mock)
    mock.spocpConnectionFactory = new SPOCPConnectionFactoryImpl()

    assert !mock.checkRole('foo', 'role')
  }

  @Test(expected = IllegalStateException)
  void "checkRole throws exception for no spocpConnectionFactory"() {
    SpocpRoleAuthorizor.instance.spocpConnectionFactory = null

    assert !SpocpRoleAuthorizor.instance.checkRole('foo', 'role')
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
  void "doSpocpCall handles spocp is null"() {

    def mockFactory = createMock(SPOCPConnectionFactoryImpl)
    expect(mockFactory.getConnection()).andReturn(null)

    replayAll(mockFactory)

    def authorizor = SpocpRoleAuthorizor.instance
    authorizor.spocpConnectionFactory = mockFactory

    authorizor.doSpocpCall('foo', 'foo')
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
