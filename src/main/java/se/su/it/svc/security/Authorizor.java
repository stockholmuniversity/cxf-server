package se.su.it.svc.security;

public interface Authorizor {
  public boolean checkRole(String uid, String role);
}
