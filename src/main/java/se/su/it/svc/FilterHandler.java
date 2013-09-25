/*
 * Copyright (c) 2013, IT Services, Stockholm University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of Stockholm University nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package se.su.it.svc;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 */
public class FilterHandler extends AbstractHandler {

  /**
   *
   */
  private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FilterHandler.class);

  /**
   *
   */
  private String tmpDir = null;

  /**
   *
   * @param tmpDir
   */
  public FilterHandler(final String tmpDir) {
    this.tmpDir = tmpDir;
  }

  /**
   *
   * @param target
   * @param baseRequest
   * @param request
   * @param response
   * @throws IOException
   * @throws ServletException
   */
  public final void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    if (baseRequest.getRequestURI().equalsIgnoreCase("/status.html")
        || baseRequest.getRequestURI().equalsIgnoreCase("/")) {
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
      } catch (Exception e) {
        logger.debug("Warning: Could not read version.properties file. ", e);
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
      } catch (Exception e) {
        logger.debug("Warning: Could not read server version.properties file. ", e);
        response.getWriter().println(noinfo);
      }

      baseRequest.setHandled(true);
      return;
    }
    baseRequest.setHandled(false);
  }
}
