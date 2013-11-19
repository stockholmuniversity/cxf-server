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

package se.su.it.svc.server.log;

import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonRequestLog implements RequestLog {
  static final Logger log = LoggerFactory.getLogger("RequestLog");

  boolean started = false;

  @Override
  public void log(Request request, Response response) {
    StringBuilder buf = new StringBuilder();
    buf.append(request.getServerName());
    buf.append(" ");

    String addr = request.getHeader(HttpHeaders.X_FORWARDED_FOR);
    if (addr == null)
      addr = request.getRemoteAddr();
    buf.append(addr);

    buf.append(" - ");

    buf.append(getUserPrincipal(request));

    buf.append(" [").append(request.getTimeStampBuffer().toString()).append("] ");

    buf.append("\"");
    buf.append(request.getMethod());
    buf.append(' ');
    buf.append(request.getUri().toString());
    buf.append(' ');
    buf.append(request.getProtocol());
    buf.append("\" ");

    buf.append(getStatus(request, response));

    buf.append(getResponseLength(response));

    log.info(buf.toString());
  }

  protected String getUserPrincipal(Request request) {
    String user = "-";

    Authentication authentication = request.getAuthentication();
    if (authentication instanceof Authentication.User)
      user = ((Authentication.User) authentication).getUserIdentity().getUserPrincipal().getName();

    return user;
  }

  protected String getStatus(Request request, Response response) {
    StringBuilder buf = new StringBuilder();

    if (request.getAsyncContinuation().isInitial()) {
      int status = response.getStatus();
      if (status <= 0)
        status = 404;
      buf.append((char) ('0' + ((status / 100) % 10)));
      buf.append((char) ('0' + ((status / 10) % 10)));
      buf.append((char) ('0' + (status % 10)));
    } else
      buf.append("Async");

    return buf.toString();
  }

  protected String getResponseLength(Response response) {
    StringBuilder buf = new StringBuilder();
    long responseLength = response.getContentCount();

    if (responseLength >= 0) {
      buf.append(' ');
      if (responseLength > 99999)
        buf.append(responseLength);
      else {
        if (responseLength > 9999)
          buf.append((char) ('0' + ((responseLength / 10000) % 10)));
        if (responseLength > 999)
          buf.append((char) ('0' + ((responseLength / 1000) % 10)));
        if (responseLength > 99)
          buf.append((char) ('0' + ((responseLength / 100) % 10)));
        if (responseLength > 9)
          buf.append((char) ('0' + ((responseLength / 10) % 10)));
        buf.append((char) ('0' + (responseLength) % 10));
      }
      buf.append(' ');
    } else
      buf.append(" - ");

    return buf.toString();
  }

  @Override
  public void start() throws Exception {
    started = true;
  }

  @Override
  public void stop() throws Exception {
    started = false;
  }

  @Override
  public boolean isRunning() {
    return started;
  }

  @Override
  public boolean isStarted() {
    return started;
  }

  @Override
  public boolean isStarting() {
    return false;
  }

  @Override
  public boolean isStopping() {
    return false;
  }

  @Override
  public boolean isStopped() {
    return !started;
  }

  @Override
  public boolean isFailed() {
    return false;
  }

  @Override
  public void addLifeCycleListener(Listener listener) {
    // Not yet implemented
  }

  @Override
  public void removeLifeCycleListener(Listener listener) {
    // Not yet implemented
  }
}
