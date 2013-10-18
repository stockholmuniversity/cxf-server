package se.su.it.svc.aspect

import org.junit.Test

class SanitizeWebParametersAspectTest {

  @Test
  void "washArgs: Test that Strings are properly trimmed and washed."() {
    Object[] args = ["foo", "  bar  ", new Object()]

    def resp = SanitizeWebParametersAspect.washArgs(args)

    assert resp[0] == "foo"
    assert resp[1] == "bar"
  }
}
