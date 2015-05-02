package org.testng.eclipse.launch;

public class RunEditorLaunchDelegate extends AbstractTestNGLaunchDelegate {

  protected String getLaunchMode() {
    return "run";
  }

  protected String getCommandPrefix() {
    return "Run as";
  }

  protected String getTestShortcut() {
    return "M3+M2+X N";
  }

  protected String getSuiteShortcut() {
    return "M3+M2+X G";
  }
}
