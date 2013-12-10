package se.su.it.svc.server.aspect

import org.apache.cxf.message.Message
import org.apache.cxf.phase.PhaseInterceptorChain
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.reflect.Whitebox

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

import static org.easymock.EasyMock.expect
import static org.powermock.api.easymock.PowerMock.*

@RunWith(PowerMockRunner)
@PrepareForTest([AuditAspect, PhaseInterceptorChain])
class AuditAspectTest {

  class DummyCalss {

    public void nothing() { }

    public void method1() { }

    public void method2(String s) { }

    @Override
    public String toString() {
      "FooBar"
    }
  }

  @Test
  void "getMethod gets correct method"() {
    def ret = Whitebox.invokeMethod(new AuditAspect(),
            'getMethod',
            DummyCalss,
            'method1',
            new Object[0]
    )

    assert ret == DummyCalss.getMethod('method1')
  }

  @Test
  void "getMethod gets correct method with args"() {
    def ret = Whitebox.invokeMethod(new AuditAspect(),
            'getMethod',
            DummyCalss,
            'method2',
            ['foo'] as Object[]
    )

    assert ret == DummyCalss.getMethod('method2', String)
  }

  @Test(expected = NoSuchMethodException)
  void "getMethod with unknown method"() {
    Whitebox.invokeMethod(new AuditAspect(),
            'getMethod',
            DummyCalss,
            'method3',
            [] as Object[]
    )
  }

  @Test
  void "getId happy path"() {
    def session = createMock(HttpSession)
    expect(session.getId()).andReturn('ID')

    def request = createMock(HttpServletRequest)
    expect(request.getSession()).andReturn(session)

    def message = createMock(Message)
    expect(message.get('HTTP.REQUEST')).andReturn(request)

    mockStatic(PhaseInterceptorChain)
    expect(PhaseInterceptorChain.getCurrentMessage()).andReturn(message)

    replayAll(PhaseInterceptorChain, message, request, session)

    def ret = new AuditAspect().getId()

    assert ret == "ID"
  }

  @Test
  void "getId handles exception"() {
    mockStatic(PhaseInterceptorChain)
    expect(PhaseInterceptorChain.getCurrentMessage()).andThrow(new NullPointerException('foo'))
    replay(PhaseInterceptorChain)

    def ret = new AuditAspect().getId()

    assert ret == ""
  }

  @Test
  void "objectsToString happy path"() {
    def objs = ["foo", true, 1, new DummyCalss()] as Object[]

    def ret =  new AuditAspect().objectsToString(objs)

    assert ret == "[foo, true, 1, FooBar]"
  }

  @Test
  void "objectsToString handles null"() {
    def ret =  new AuditAspect().objectsToString(null)

    assert ret == "null"
  }
}
