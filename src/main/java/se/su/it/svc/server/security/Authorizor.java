package se.su.it.svc.server.security;

public interface Authorizor {
  boolean checkRole(String uid, String role);
}
