package se.su.it.svc.server.security

import org.junit.Test

class DefaultAuthorizorTest {

  @Test
  void "Test that checkRole always returns true"() {
    DefaultAuthorizor authorizor = new DefaultAuthorizor();

    assert authorizor.checkRole(null, null)
    assert authorizor.checkRole('',   null)
    assert authorizor.checkRole(null, '')
    assert authorizor.checkRole('',   '')
    assert authorizor.checkRole('foo', 'foo')
  }
}
