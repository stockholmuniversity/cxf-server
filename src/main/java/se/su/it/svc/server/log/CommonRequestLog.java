package se.su.it.svc.server.log;

import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

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

    Authentication authentication=request.getAuthentication();
    if (authentication instanceof Authentication.User)
      buf.append(((Authentication.User)authentication).getUserIdentity().getUserPrincipal().getName());
    else
      buf.append(" - ");

    buf.append(" [").append(request.getTimeStampBuffer().toString()).append("] ");

    buf.append("\"");
    buf.append(request.getMethod());
    buf.append(' ');
    buf.append(request.getUri().toString());
    buf.append(' ');
    buf.append(request.getProtocol());
    buf.append("\" ");

    if (request.getAsyncContinuation().isInitial()) {
      int status = response.getStatus();
      if (status <= 0)
        status = 404;
      buf.append((char)('0' + ((status / 100) % 10)));
      buf.append((char)('0' + ((status / 10) % 10)));
      buf.append((char)('0' + (status % 10)));
    }
    else
      buf.append("Async");

    long responseLength = response.getContentCount();
    if (responseLength >= 0) {
      buf.append(' ');
      if (responseLength > 99999)
        buf.append(responseLength);
      else {
        if (responseLength > 9999)
          buf.append((char)('0' + ((responseLength / 10000) % 10)));
        if (responseLength > 999)
          buf.append((char)('0' + ((responseLength / 1000) % 10)));
        if (responseLength > 99)
          buf.append((char)('0' + ((responseLength / 100) % 10)));
        if (responseLength > 9)
          buf.append((char)('0' + ((responseLength / 10) % 10)));
        buf.append((char)('0' + (responseLength) % 10));
      }
      buf.append(' ');
    }
    else
      buf.append(" - ");

    log.info((Marker)null, buf.toString());
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
