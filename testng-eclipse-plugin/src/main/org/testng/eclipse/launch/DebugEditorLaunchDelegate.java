package org.testng.eclipse.launch;

public class DebugEditorLaunchDelegate extends AbstractTestNGLaunchDelegate {

  protected String getLaunchMode() {
    return "debug";
  }

  protected String getCommandPrefix() {
    return "Debug as";
  }

  protected String getTestShortcut() {
    return "M3+M2+D N";
  }

  protected String getSuiteShortcut() {
    return "M3+M2+D G";
  }
}
