package se.su.it.svc.server.security;

public interface Authorizor {
  public boolean checkRole(String uid, String role);
}
