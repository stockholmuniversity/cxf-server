package se.su.it.svc.server.filter

import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

import static org.easymock.EasyMock.anyString
import static org.easymock.EasyMock.expect
import static org.powermock.api.easymock.PowerMock.mockStaticPartial
import static org.powermock.api.easymock.PowerMock.replayAll

@RunWith(PowerMockRunner)
@PrepareForTest([StatusHandler, Properties])
public class StatusHandlerTest {

  @Test
  void "createInfoText contains correct values"() {
    def result = StatusHandler.createInfoText("version.properties", "")

    assert result.contains("Name: Test")
    assert result.contains("Version: 1.2.3")
    assert result.contains("Build Time: 2013-11-27 12:13:14")
  }

  @Test
  void "createInfoText handles exception"() {
    mockStaticPartial(StatusHandler, 'attribute2Html')
    expect(StatusHandler.attribute2Html(anyString(), anyString())).andThrow(new NullPointerException("FooBar"))

    replayAll(StatusHandler)

    def result = StatusHandler.createInfoText("version.properties", "")

    assert result.contains(StatusHandler.NOINFO)
  }

  @Test
  void "attribute2Html creates correct string"() {
    def result = StatusHandler.attribute2Html("foo: ", "bar")

    assert result == "foo: bar<br />"
  }

  @Test
  void "attribute2Html handles null key"() {
    def result = StatusHandler.attribute2Html(null, "bar")

    assert result == ""
  }

  @Test
  void "attribute2Html handles null value"() {
    def result = StatusHandler.attribute2Html("foo: ", null)

    assert result == "foo: ${StatusHandler.NOINFO}<br />"
  }
}
