package se.su.it.svc.server.security;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.slf4j.LoggerFactory;

public class FileRoleAuthorizor implements Authorizor {
  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SpocpRoleAuthorizor.class);
  private String itsPathToRoles = null;
  private Properties itsRoleProperties = null;

  private FileRoleAuthorizor() {
    LOG.debug("Initializing FileRoleAuthorizor");
  }

  public boolean checkRole(String uid, String role) {
    boolean ok = false;
    if(null!=itsRoleProperties) {
      String principalsString = itsRoleProperties.getProperty(role,"");
      if(principalsString != null) {
        principalsString.replaceAll("\\s+","");
        List<String> principalsList = Arrays.asList(principalsString.split(","));
        ok = principalsList.contains(uid);
      }
    }
    return ok;
  }

  public String getPathToRoles() {
    return itsPathToRoles;
  }

  public void setPathToRoles(String pathToRoles) {
    itsPathToRoles = pathToRoles;
    java.io.FileInputStream source = null;
    try {
      source = new FileInputStream(pathToRoles);
      itsRoleProperties = new Properties();
      itsRoleProperties.load(source);
    } catch (IOException exception) {
    } finally {
      if (null!=source) {
        try {
          source.close();
        } catch(Throwable exception) {
        }
        source = null;
      }
    }
  }
}
