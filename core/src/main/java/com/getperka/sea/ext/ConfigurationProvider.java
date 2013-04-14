package com.getperka.sea.ext;

public interface ConfigurationProvider {
  void accept(ConfigurationVisitor visitor);
}
