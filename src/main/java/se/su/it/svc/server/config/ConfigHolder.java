package se.su.it.svc.server.config;

import java.util.Properties;

public interface ConfigHolder {
  Properties getProperties();
  void printConfiguration();
}
