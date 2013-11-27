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
import java.util.Properties;

/**
 * A filter that shows status read from property files.
 */
public class StatusHandler extends AbstractHandler {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(StatusHandler.class);

  private final String statusText;

  private static final String PROJECT_NAME_KEY = "project.name";
  private static final String PROJECT_VERSION_KEY = "project.version";
  private static final String PROJECT_BUILD_DATE_KEY = "project.builddate";

  private static final String NOINFO = "No information available";

  /**
   * Create a new handler.
   */
  public StatusHandler() {
    StringBuilder sb = new StringBuilder();

    sb.append(createInfoText("WEB-INF/classes/version.properties", ""));
    sb.append("<br /><br />");
    sb.append(createInfoText("version.properties", "Server"));

    statusText = sb.toString();
  }

  /**
   * Create a html block from properties in the supplied file.
   *
   * @param filePath path to the file containing the properties
   * @param propertyPrefix a prefix to add to the output in front of every property
   * @return a html string
   */
  private static String createInfoText(String filePath, String propertyPrefix) {
    StringBuilder sb = new StringBuilder();
    Properties versionProps = new Properties();

    URLClassLoader cl = (URLClassLoader) Thread.currentThread().getContextClassLoader();

    InputStream inputStream = cl.getResourceAsStream(filePath);
    try {
      versionProps.load(inputStream);
      String name = attribute2Html(propertyPrefix + "Name:", versionProps.getProperty(PROJECT_NAME_KEY));
      String version = attribute2Html(propertyPrefix + "Version:", versionProps.getProperty(PROJECT_VERSION_KEY));
      String build = attribute2Html(propertyPrefix + "Build Time:", versionProps.getProperty(PROJECT_BUILD_DATE_KEY));

      sb.append(name).append(version).append(build);
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
   * Create html for supplied key & value
   *
   * @param key the key
   * @param value the value
   * @return html string
   */
  private static String attribute2Html(String key, String value) {
    StringBuilder sb = new StringBuilder();
    sb.append(key);

    if (value != null && value.length() > 0)
      sb.append(value);
    else
      sb.append(NOINFO);

    sb.append("<br />");

    return sb.toString();
  }

  /**
   * @see org.eclipse.jetty.server.Handler#handle(String, org.eclipse.jetty.server.Request, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  @Override
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
