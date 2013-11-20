package se.su.it.svc.server.log

import com.sun.security.auth.UserPrincipal
import org.eclipse.jetty.http.HttpHeaders
import org.eclipse.jetty.http.HttpURI
import org.eclipse.jetty.io.Buffer
import org.eclipse.jetty.server.*
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.modules.junit4.PowerMockRunner
import org.slf4j.Logger

import static org.easymock.EasyMock.*
import static org.powermock.api.easymock.PowerMock.createPartialMockForAllMethodsExcept
import static org.powermock.api.easymock.PowerMock.replayAll

@RunWith(PowerMockRunner)
class CommonRequestLogTest {

  @Test
  void "log"() {
    def request = createMock(Request)
    def response = createMock(Response)
    def spy = createPartialMockForAllMethodsExcept(
            CommonRequestLog, "log", Request, Response)
    def buffer = [toString: {"time"}] as Buffer
    def logger = createMock(Logger)

    expect(request.getServerName()).andReturn("127.0.0.1")
    expect(request.getHeader(HttpHeaders.X_FORWARDED_FOR)).andReturn("1.2.3.4")
    expect(spy.getUserPrincipal(request)).andReturn("foobar")
    expect(request.getTimeStampBuffer()).andReturn(buffer)
    expect(request.getMethod()).andReturn("GET")
    expect(request.getUri()).andReturn(new HttpURI("/sercvices"))
    expect(request.getProtocol()).andReturn("HTTP/1.1")
    expect(spy.getStatus(request, response)).andReturn("200")
    expect(spy.getResponseLength(response)).andReturn("0")

    expect(logger.info('127.0.0.1 1.2.3.4 - foobar [time] "GET /sercvices HTTP/1.1" 200 0'))
    replayAll(request, response, logger, spy)

    CommonRequestLog.logger = logger

    spy.log(request, response)

    verify(logger)
  }

  @Test
  void "getResponseLength returns the response length"() {
    def response = createMock(Response)

    expect(response.getContentCount()).andReturn(123456L)

    replay(response)

    def ret = new CommonRequestLog().getResponseLength(response)

    assert ret == '123456'
  }

  @Test
  void "getResponseLength returns 0 for no length"() {
    def response = createMock(Response)

    expect(response.getContentCount()).andReturn(0L)

    replay(response)

    def ret = new CommonRequestLog().getResponseLength(response)

    assert ret == '0'
  }

  @Test
  void "getResponseLength returns - for negative length"() {
    def response = createMock(Response)

    expect(response.getContentCount()).andReturn(-1L)

    replay(response)

    def ret = new CommonRequestLog().getResponseLength(response)

    assert ret == '-'
  }

  @Test
  void "getStatus returns Async for async requests"() {
    def request = createMock(Request)
    def async = createMock(AsyncContinuation)

    expect(request.getAsyncContinuation()).andReturn(async)
    expect(async.isInitial()).andReturn(false)

    replayAll(request, async)

    def ret = new CommonRequestLog().getStatus(request, null)

    assert ret == 'Async'
  }

  @Test
  void "getStatus returns 404 for status below 0"() {
    def request = createMock(Request)
    def async = createMock(AsyncContinuation)
    def response = createMock(Response)

    expect(request.getAsyncContinuation()).andReturn(async)
    expect(async.isInitial()).andReturn(true)
    expect(response.getStatus()).andReturn(-1)

    replayAll(request, async, response)

    def ret = new CommonRequestLog().getStatus(request, response)

    assert ret == '404'
  }

  @Test
  void "getStatus returns 404 for status == 0"() {
    def request = createMock(Request)
    def async = createMock(AsyncContinuation)
    def response = createMock(Response)

    expect(request.getAsyncContinuation()).andReturn(async)
    expect(async.isInitial()).andReturn(true)
    expect(response.getStatus()).andReturn(0)

    replayAll(request, async, response)

    def ret = new CommonRequestLog().getStatus(request, response)

    assert ret == '404'
  }

  @Test
  void "getStatus returns status"() {
    def request = createMock(Request)
    def async = createMock(AsyncContinuation)
    def response = createMock(Response)

    expect(request.getAsyncContinuation()).andReturn(async)
    expect(async.isInitial()).andReturn(true)
    expect(response.getStatus()).andReturn(123)

    replayAll(request, async, response)

    def ret = new CommonRequestLog().getStatus(request, response)

    assert ret == '123'
  }

  @Test
  void "getStatus prepends zeros"() {
    def request = createMock(Request)
    def async = createMock(AsyncContinuation)
    def response = createMock(Response)

    expect(request.getAsyncContinuation()).andReturn(async)
    expect(async.isInitial()).andReturn(true)
    expect(response.getStatus()).andReturn(1)

    replayAll(request, async, response)

    def ret = new CommonRequestLog().getStatus(request, response)

    assert ret == '001'
  }

  @Test
  void "getUserPrincipal returns - for no auth"() {
    def request = createMock(Request)

    def ret = new CommonRequestLog().getUserPrincipal(request)

    assert ret == '-'
  }

  @Test
  void "getUserPrincipal returns user principal "() {
    def request = createMock(Request)
    def auth = createMock(Authentication.User)
    def userIdentity = createMock(UserIdentity)
    def userprincipal = new UserPrincipal("FooBar")

    expect(request.getAuthentication()).andReturn(auth)
    expect(auth.getUserIdentity()).andReturn(userIdentity)
    expect(userIdentity.getUserPrincipal()).andReturn(userprincipal)

    replayAll(request, auth, userIdentity)

    def ret = new CommonRequestLog().getUserPrincipal(request)

    assert ret == 'FooBar'
  }

  @Test
  void "start() sets started"() {
    def target = new CommonRequestLog()

    target.start()

    assert target.started
  }

  @Test
  void "stop() sets started = false"() {
    def target = new CommonRequestLog()

    target.stop()

    assert !target.started
  }

  @Test
  void "isRunning() returns started"() {
    def target = new CommonRequestLog()

    assert !target.isRunning()

    target.start()

    assert target.isRunning()
  }

  @Test
  void "isStarted() returns started"() {
    def target = new CommonRequestLog()

    assert !target.isStarted()

    target.start()

    assert target.isStarted()
  }

  @Test
  void "isStarting() returns false"() {
    assert ! new CommonRequestLog().isStarting()
  }

  @Test
  void "isStopping() returns false"() {
    assert ! new CommonRequestLog().isStopping()
  }

  @Test
  void "isStopped() returns started"() {
    def target = new CommonRequestLog()

    assert target.isStopped()

    target.start()

    assert !target.isStopped()
  }

  @Test
  void "isFailed() returns false"() {
    assert ! new CommonRequestLog().isFailed()
  }

  @Test
  void "addLifeCycleListener does nothing"() {
    new CommonRequestLog().addLifeCycleListener(null)
    assert true
  }

  @Test
  void "removeLifeCycleListener does nothing"() {
    new CommonRequestLog().removeLifeCycleListener(null)
    assert true
  }
}
