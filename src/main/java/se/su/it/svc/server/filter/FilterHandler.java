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

package se.su.it.svc.server.filter;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 */
public class FilterHandler extends AbstractHandler {

  /**
   *
   */
  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(FilterHandler.class);

  private final String statusText;

  private static final String PROJECT_NAME_KEY = "project.name";
  private static final String PROJECT_VERSION_KEY = "project.version";
  private static final String PROJECT_BUILD_DATE_KEY = "project.builddate";

  private static final String NOINFO = "No information available";

  /**
   *
   */
  public FilterHandler() {

    StringBuilder sb = new StringBuilder();

    URLClassLoader cl = (URLClassLoader) Thread.currentThread().getContextClassLoader();

    sb.append(createInfoText(cl, "WEB-INF/classes/version.properties", new LinkedHashMap<String, String>() {{
      put(PROJECT_NAME_KEY, "Name: ");
      put(PROJECT_VERSION_KEY, "Version: ");
      put(PROJECT_BUILD_DATE_KEY, "Build Time: ");
    }}));

    sb.append("<br /><br />");

    sb.append(createInfoText(cl, "version.properties", new LinkedHashMap<String, String>() {{
      put(PROJECT_NAME_KEY, "Server Name: ");
      put(PROJECT_VERSION_KEY, "Server Version: ");
      put(PROJECT_BUILD_DATE_KEY, "Server Build Time: ");
    }}));


    statusText = sb.toString();
  }

  private static String createInfoText(URLClassLoader cl, String filePath, Map attributes) {
    StringBuilder sb = new StringBuilder();

    Properties versionProps = new Properties();

    InputStream inputStream = cl.getResourceAsStream(filePath);
    try {
      versionProps.load(inputStream);
      String name = versionProps.getProperty(PROJECT_NAME_KEY);
      String version = versionProps.getProperty(PROJECT_VERSION_KEY);
      String builddate = versionProps.getProperty(PROJECT_BUILD_DATE_KEY);
      sb.append(attributes.get(PROJECT_NAME_KEY))
          .append(name != null && name.length() > 0 ? name : NOINFO)
          .append("<br />");
      sb.append(attributes.get(PROJECT_VERSION_KEY))
          .append(version != null && version.length() > 0 ? version : NOINFO)
          .append("<br />");
      sb.append(attributes.get(PROJECT_BUILD_DATE_KEY))
          .append(builddate != null && builddate.length() > 0 ? builddate : NOINFO)
          .append("<br />");
    } catch (Exception e) {
      LOG.debug("Warning: Could not read version.properties file. ", e);
      sb.append(NOINFO);
    } finally {
      try {
        inputStream.close();
      } catch (IOException ioe) {
        LOG.error("Could not close stream when reading resource", ioe);
      }
    }
    return sb.toString();
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
      response.getWriter().println(statusText);
      baseRequest.setHandled(true);
      return;
    }
    baseRequest.setHandled(false);
  }
}
