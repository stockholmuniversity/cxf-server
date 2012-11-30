import org.eclipse.jetty.security.SpnegoLoginService;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import se.su.it.svc.FilterHandler;
import se.su.it.svc.SuCxfAuthenticator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Properties;


public class Start {

  public static void main(String[] args) {

    // Read properties file.
    Properties properties = new Properties();
    // Begin Default properties
    //Ssl
    properties.put("ssl.port", 443);
    properties.put("ssl.keystore", "cxf-svc-server.keystore");
    properties.put("ssl.password", "changeit");
    //Spnego
    properties.put("spnego.properties", "spnego.properties");
    properties.put("spnego.realm", "SU.SE");
    // End Default properties
    FileInputStream is = null;
    try {
      is = new FileInputStream("/local/cxf-server/conf/config.properties");
      properties.load(is);
    } catch (IOException e) {
      System.out.println("Exception when trying to read configuration file /local/cxf-server/conf/config.properties, exception message was: " + e.getMessage() + ".");
      System.out.println("Using default properties for application!");
    }  finally {
      if( null != is ){
        try {
          is.close();
        } catch( IOException e ) {
        }
      }

    }

    // extracting the config properties for ssl setup
    int sslPort         = Integer.parseInt(properties.getProperty("ssl.port"));
    String sslKeystore  = properties.getProperty("ssl.keystore");
    String sslPassword  = properties.getProperty("ssl.password");

    //extracting the config for the spnegp setup
    String spnegoRealm      = properties.getProperty("spnego.realm");
    String spnegoProperties = properties.getProperty("spnego.properties");

    try {

      org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server();

      SslSocketConnector connector = new SslSocketConnector();

      connector.setPort(sslPort);
      connector.setKeystore(sslKeystore);
      connector.setPassword(sslPassword);

      server.setConnectors(new Connector[]{connector});


      URL url = Start.class.getClassLoader().getResource("Start.class");
      File warFile = new File(((JarURLConnection) url.openConnection()).getJarFile().getName());
      WebAppContext context = new WebAppContext();
      File webbAppFp = new File("webapp");
      webbAppFp.mkdir();
      context.setTempDirectory(webbAppFp);
      context.setContextPath("/");
      context.setWar(warFile.getAbsolutePath());

      FilterHandler fh = new FilterHandler(context.getTempDirectory().toString());

      HandlerList handlers = new HandlerList();
      handlers.setHandlers(new Handler[] {fh, context, new DefaultHandler() });

      server.setHandler(handlers);
      SpnegoLoginService sLoginService = new SpnegoLoginService(spnegoRealm);
      sLoginService.setConfig(spnegoProperties);
      context.getSecurityHandler().setLoginService(sLoginService);
      context.getSecurityHandler().setAuthenticator(new SuCxfAuthenticator(context));

      server.start();
      System.out.println("Server ready...");
      server.join();

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
