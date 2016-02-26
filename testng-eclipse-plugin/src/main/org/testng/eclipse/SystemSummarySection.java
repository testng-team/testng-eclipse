package org.testng.eclipse;

import java.io.PrintWriter;
import java.net.URL;
import java.util.Properties;

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.about.ISystemSummarySection;
import org.osgi.framework.Bundle;

public class SystemSummarySection implements ISystemSummarySection {

  @Override
  public void write(PrintWriter writer) {
    Bundle bundle = Platform.getBundle(TestNGPlugin.PLUGIN_ID);
    URL fileURL = bundle.getEntry("git.properties");
    if (fileURL == null) {
      fileURL = this.getClass().getClassLoader().getResource("/git.properties");
    }
    Properties props = new Properties();
    try {
      props.load(fileURL.openStream());

      props.list(writer);
    } catch (Exception e) {
      TestNGPlugin.log(e);
    }
  }

}
