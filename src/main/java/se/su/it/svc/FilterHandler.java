package se.su.it.svc;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import sun.rmi.runtime.Log;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class FilterHandler extends AbstractHandler {
  String tmpDir = null;
  public FilterHandler(String tmpDir) {
    this.tmpDir = tmpDir;
  }
  public void handle(String target,Request baseRequest, HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException {
    if(baseRequest.getRequestURI().equalsIgnoreCase("/status.html")) {
      response.setContentType("text/html");
      response.setStatus(HttpServletResponse.SC_OK);
      Properties versionProps = new Properties();
      String noinfo = "No information available";
      try {
        FileInputStream in = new FileInputStream(tmpDir + "/webapp/WEB-INF/classes/version.properties");
        versionProps.load(in);
        in.close();
        String name = versionProps.getProperty("project.name");
        String version = versionProps.getProperty("project.version");
        String builddate = versionProps.getProperty("project.builddate");
        response.getWriter().println("Name: " + (name != null && name.length() > 0 ? name : noinfo) + "<br />");
        response.getWriter().println("Version: " + (version != null && version.length() > 0 ? version : noinfo) + "<br />");
        response.getWriter().println("Build Time: " + (builddate != null && builddate.length() > 0 ? builddate : noinfo) + "<br />");
      } catch (Exception e){
        System.out.println("Warning: Could not read version.properties file. " + e.getMessage());
        response.getWriter().println(noinfo);
      }
      response.getWriter().println("<br /><br />");
      try {
        FileInputStream in = new FileInputStream(tmpDir + "/webapp/version.properties");
        versionProps.load(in);
        in.close();
        String name = versionProps.getProperty("project.name");
        String version = versionProps.getProperty("project.version");
        String builddate = versionProps.getProperty("project.builddate");
        response.getWriter().println("Server Name: " + (name != null && name.length() > 0 ? name : noinfo) + "<br />");
        response.getWriter().println("Server Version: " + (version != null && version.length() > 0 ? version : noinfo) + "<br />");
        response.getWriter().println("Server Build Time: " + (builddate != null && builddate.length() > 0 ? builddate : noinfo) + "<br />");
      } catch (Exception e){
        System.out.println("Warning: Could not read server version.properties file. " + e.getMessage());
        response.getWriter().println(noinfo);
      }
      baseRequest.setHandled(true);
      return;
    }
    baseRequest.setHandled(false);
  }
}
