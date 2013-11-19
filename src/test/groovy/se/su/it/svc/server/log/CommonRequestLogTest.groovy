package se.su.it.svc.server.log

import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner)
class CommonRequestLogTest {

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
