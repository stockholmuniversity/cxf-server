package se.su.it.svc.server.security;

public class DefaultAuthorizor implements Authorizor {

  /**
   * Will let anything trough.
   *
   * @param uid the uid.
   * @param role the role.
   * @return always true.
   */
  @Override
  public boolean checkRole(String uid, String role) {
    return true;
  }
}
