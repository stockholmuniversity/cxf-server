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

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import se.su.it.svc.filter.FilterHandler;
import se.su.it.svc.security.SpnegoAndKrb5LoginService;
import se.su.it.svc.security.SuCxfAuthenticator;

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;


public class Start {
  private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Start.class);

  public static final String DEFAULT_LOG_FILE_NAME_PROPERTY_KEY = "log.file";
  public static final String DEFAULT_CONFIG_FILE_NAME_PROPERTY_KEY = "config.properties";

  public static final String DEFAULT_SERVER_PREFIX = "cxf-server.";

  public static final String PORT_PROPERTY_KEY                = DEFAULT_SERVER_PREFIX + "http.port";
  public static final String BIND_ADDRESS_PROPERTY_KEY        = DEFAULT_SERVER_PREFIX + "bind.address";
  public static final String SSL_ENABLED_PROPERTY_KEY         = DEFAULT_SERVER_PREFIX + "ssl.enabled";
  public static final String SSL_KEYSTORE_PROPERTY_KEY        = DEFAULT_SERVER_PREFIX + "ssl.keystore";
  public static final String SSL_PASSWORD_PROPERTY_KEY        = DEFAULT_SERVER_PREFIX + "ssl.password";
  public static final String SPNEGO_CONFIG_FILE_PROPERTY_KEY  = DEFAULT_SERVER_PREFIX + "spnego.conf";
  public static final String SPNEGO_REALM_PROPERTY_KEY        = DEFAULT_SERVER_PREFIX + "spnego.realm";
  public static final String SPNEGO_KDC_PROPERTY_KEY          = DEFAULT_SERVER_PREFIX + "spnego.kdc";
  public static final String SPNEGO_PROPERTIES_PROPERTY_KEY   = DEFAULT_SERVER_PREFIX + "spnego.properties";

  private static final ArrayList<String> mandatoryProperties = new ArrayList<String>() {{
    add(PORT_PROPERTY_KEY);
    add(BIND_ADDRESS_PROPERTY_KEY);
    add(SSL_ENABLED_PROPERTY_KEY);
    add(SPNEGO_CONFIG_FILE_PROPERTY_KEY);
    add(SPNEGO_REALM_PROPERTY_KEY);
    add(SPNEGO_KDC_PROPERTY_KEY);
    add(SPNEGO_PROPERTIES_PROPERTY_KEY);
  }};


  public static void main(String[] args) {
    // TODO: Handle config file as an arg?
    String logfile = System.getProperty(DEFAULT_LOG_FILE_NAME_PROPERTY_KEY);

    if (logfile != null) {
      ((org.apache.log4j.DailyRollingFileAppender) LogManager.getRootLogger().getAppender("A")).setFile(logfile);
      ((org.apache.log4j.DailyRollingFileAppender) LogManager.getRootLogger().getAppender("A")).activateOptions();
    }

    if (System.getProperty("DEBUG") != null) {
      LogManager.getRootLogger().setLevel(Level.DEBUG);
    }

    // Begin Check if properties file is defined as define argument
    Properties properties = loadProperties();

    // End Check if properties file is defined as define argument
    int httpPort = Integer.parseInt(properties.getProperty(PORT_PROPERTY_KEY).trim());
    String jettyBindAddress = properties.getProperty(BIND_ADDRESS_PROPERTY_KEY);
    // extracting the config properties for ssl setup
    boolean sslEnabled = Boolean.parseBoolean(properties.getProperty(SSL_ENABLED_PROPERTY_KEY));
    String sslKeystore = properties.getProperty(SSL_KEYSTORE_PROPERTY_KEY);
    String sslPassword = properties.getProperty(SSL_PASSWORD_PROPERTY_KEY);

    //extracting the config for the spnegp setup
    String spnegoConfigFileName = properties.getProperty(SPNEGO_CONFIG_FILE_PROPERTY_KEY);
    String spnegoRealm = properties.getProperty(SPNEGO_REALM_PROPERTY_KEY);
    String spnegoKdc = properties.getProperty(SPNEGO_KDC_PROPERTY_KEY);
    String spnegoPropertiesFileName = properties.getProperty(SPNEGO_PROPERTIES_PROPERTY_KEY);

    try {

      org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server();

      if (sslEnabled) {
        SslSocketConnector connector = new SslSocketConnector();

        connector.setPort(httpPort);
        if (jettyBindAddress != null && jettyBindAddress.length() > 0) {
          connector.setHost(jettyBindAddress);
        }
        connector.setKeystore(sslKeystore);
        connector.setPassword(sslPassword);

        server.setConnectors(new Connector[]{connector});
      } else {
        SocketConnector connector = new SocketConnector();
        connector.setPort(httpPort);
        if (jettyBindAddress != null && jettyBindAddress.length() > 0) {
          connector.setHost(jettyBindAddress);
        }
        server.setConnectors(new Connector[]{connector});
      }

      URL url = Start.class.getClassLoader().getResource("Start.class");
      File warFile = new File(((JarURLConnection) url.openConnection()).getJarFile().getName());
      WebAppContext context = new WebAppContext();
      File webbAppFp = new File("webapp");
      webbAppFp.mkdir();
      context.setTempDirectory(webbAppFp);
      context.setContextPath("/");
      context.setWar(warFile.getAbsolutePath());

      // Add webapp to threads context classpath
      ClassLoader ctcl = Thread.currentThread().getContextClassLoader();
      URLClassLoader urlcl = new URLClassLoader(new URL[]{webbAppFp.toURI().toURL()}, ctcl);
      Thread.currentThread().setContextClassLoader(urlcl);

      FilterHandler fh = new FilterHandler(context.getTempDirectory().toString());

      HandlerList handlers = new HandlerList();
      handlers.setHandlers(new Handler[]{fh, context, new DefaultHandler()});

      server.setHandler(handlers);

      System.setProperty("java.security.krb5.realm", spnegoRealm);
      System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
      System.setProperty("java.security.auth.login.config", "=file:" + spnegoConfigFileName);
      System.setProperty("java.security.krb5.kdc", spnegoKdc);

      Properties spnegoProperties = new Properties();
      Resource resource = Resource.newResource(spnegoPropertiesFileName);
      spnegoProperties.load(resource.getInputStream());

      SpnegoAndKrb5LoginService loginService = new SpnegoAndKrb5LoginService(spnegoRealm, spnegoProperties.getProperty("targetName"));
      context.getSecurityHandler().setLoginService(loginService);
      context.getSecurityHandler().setAuthenticator(new SuCxfAuthenticator());

      Thread monitor = new MonitorThread();
      monitor.start();
      server.start();
      logger.info("Server ready...");
      server.join();

    } catch (Exception ex) {
      logger.error("Server startup failed.", ex);
    }
  }

  private static Properties loadProperties() {
    Properties properties = new Properties();
    String definedConfigFileName = System.getProperty(DEFAULT_CONFIG_FILE_NAME_PROPERTY_KEY);

    logger.info("The configuration variable config.properties was set to <" + definedConfigFileName.trim() + ">.\r\n Checking properties in file...");

    try {
      File file = new File(definedConfigFileName.trim());
      if (!file.exists()) {
        logger.error("<" + definedConfigFileName.trim() + "> the file was not found. Quitting....");
        System.exit(10);
      }
      FileInputStream is = new FileInputStream(definedConfigFileName.trim());
      properties.load(is);
      is.close();
      if (!checkDefinedConfigFileProperties(properties)) {
        System.exit(10);
      }
    } catch (Exception e) {
      logger.error("<" + definedConfigFileName.trim() + ">, got an exception trying to access file. Quitting....", e);
      System.exit(10);
    }

    logger.info("\n");
    logger.info("*** Properties ***");
    for (Object property : properties.keySet()) {
      logger.info(property + " => " + properties.get(property));
    }

    return properties;
  }

  private static boolean checkDefinedConfigFileProperties(Properties properties) {
    List<String> notFoundList = new ArrayList<String>();

    for (String mandatoryProperty : mandatoryProperties) {
      if (mandatoryProperty.equals(SSL_ENABLED_PROPERTY_KEY)) {
        boolean useSSL = Boolean.parseBoolean(properties.getProperty(SSL_ENABLED_PROPERTY_KEY));
        if (useSSL) {
          if (properties.get(SSL_KEYSTORE_PROPERTY_KEY) == null) {
            notFoundList.add(SSL_KEYSTORE_PROPERTY_KEY);
          }
          if (properties.get(SSL_PASSWORD_PROPERTY_KEY) == null) {
            notFoundList.add(SSL_PASSWORD_PROPERTY_KEY);
          }
        }
      } else {
        if (properties.get(mandatoryProperty) == null) {
          notFoundList.add(mandatoryProperty);
        }
      }
    }

    if (notFoundList.size() <= 0) {
      return true;
    }

    for (String notFound : notFoundList) {
      logger.error("Property <" + notFound + ">   ...not found");
    }

    // End check for mandatory properties
    logger.error("Quitting because mandatory properties was missing...");
    return false;  //To change body of created methods use File | Settings | File Templates.
  }

  private static class MonitorThread extends Thread {
    private static final String APP = "se.su.it.svc";
    private static final String JETTY = "org.eclipse.jetty";
    private static final String CXF = "org.apache.cxf";
    private static final String SPRING = "org.springframework";

    private FileChannel fc;
    private MappedByteBuffer mem;

    public MonitorThread() {
      setDaemon(true);
      setName("MonitorThread");
      try {
        fc = new RandomAccessFile("/tmp/cxf-server-tmp.txt", "rw").getChannel();
        mem = fc.map(FileChannel.MapMode.READ_WRITE, 0, 1);
      } catch (Exception e) {
      }
    }

    @Override
    public void run() {
      logger.info("Running monitor thread");
      try {
        while (true) {
          byte req = mem.get(0);
          Thread.sleep(2);
          if (req != 0) {
            mem.put(0, (byte) 0);
            selectFunction(req);
            req = 0;
          }
        }
      } catch (Exception e) {
      }
    }

    private void selectFunction(byte b) {
      switch (b) {
        case 1:
          LogManager.getRootLogger().setLevel(Level.ALL);
          break;
        case 2:
          LogManager.getRootLogger().setLevel(Level.TRACE);
          break;
        case 3:
          LogManager.getRootLogger().setLevel(Level.DEBUG);
          break;
        case 4:
          LogManager.getRootLogger().setLevel(Level.INFO);
          break;
        case 5:
          LogManager.getRootLogger().setLevel(Level.WARN);
          break;
        case 6:
          LogManager.getRootLogger().setLevel(Level.FATAL);
          break;
        case 7:
          LogManager.getRootLogger().setLevel(Level.ERROR);
          break;
        case 8:
          LogManager.getRootLogger().setLevel(Level.OFF);
          break;
        case 9:
          LogManager.getLogger(APP).setLevel(Level.ALL);
          break;
        case 10:
          LogManager.getLogger(APP).setLevel(Level.TRACE);
          break;
        case 11:
          LogManager.getLogger(APP).setLevel(Level.DEBUG);
          break;
        case 12:
          LogManager.getLogger(APP).setLevel(Level.INFO);
          break;
        case 13:
          LogManager.getLogger(APP).setLevel(Level.WARN);
          break;
        case 14:
          LogManager.getLogger(APP).setLevel(Level.FATAL);
          break;
        case 15:
          LogManager.getLogger(APP).setLevel(Level.ERROR);
          break;
        case 16:
          LogManager.getLogger(APP).setLevel(Level.OFF);
          break;
        case 17:
          LogManager.getLogger(JETTY).setLevel(Level.ALL);
          break;
        case 18:
          LogManager.getLogger(JETTY).setLevel(Level.TRACE);
          break;
        case 19:
          LogManager.getLogger(JETTY).setLevel(Level.DEBUG);
          break;
        case 20:
          LogManager.getLogger(JETTY).setLevel(Level.INFO);
          break;
        case 21:
          LogManager.getLogger(JETTY).setLevel(Level.WARN);
          break;
        case 22:
          LogManager.getLogger(JETTY).setLevel(Level.FATAL);
          break;
        case 23:
          LogManager.getLogger(JETTY).setLevel(Level.ERROR);
          break;
        case 24:
          LogManager.getLogger(JETTY).setLevel(Level.OFF);
          break;
        case 25:
          LogManager.getLogger(CXF).setLevel(Level.ALL);
          break;
        case 26:
          LogManager.getLogger(CXF).setLevel(Level.TRACE);
          break;
        case 27:
          LogManager.getLogger(CXF).setLevel(Level.DEBUG);
          break;
        case 28:
          LogManager.getLogger(CXF).setLevel(Level.INFO);
          break;
        case 29:
          LogManager.getLogger(CXF).setLevel(Level.WARN);
          break;
        case 30:
          LogManager.getLogger(CXF).setLevel(Level.FATAL);
          break;
        case 31:
          LogManager.getLogger(CXF).setLevel(Level.ERROR);
          break;
        case 32:
          LogManager.getLogger(CXF).setLevel(Level.OFF);
          break;
        case 33:
          LogManager.getLogger(SPRING).setLevel(Level.ALL);
          break;
        case 34:
          LogManager.getLogger(SPRING).setLevel(Level.TRACE);
          break;
        case 35:
          LogManager.getLogger(SPRING).setLevel(Level.DEBUG);
          break;
        case 36:
          LogManager.getLogger(SPRING).setLevel(Level.INFO);
          break;
        case 37:
          LogManager.getLogger(SPRING).setLevel(Level.WARN);
          break;
        case 38:
          LogManager.getLogger(SPRING).setLevel(Level.FATAL);
          break;
        case 39:
          LogManager.getLogger(SPRING).setLevel(Level.ERROR);
          break;
        case 40:
          LogManager.getLogger(SPRING).setLevel(Level.OFF);
          break;
        default:
          break;
      }
    }
  }


}
