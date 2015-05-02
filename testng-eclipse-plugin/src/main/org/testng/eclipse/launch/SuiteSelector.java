package org.testng.eclipse.launch;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants.LaunchType;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.eclipse.util.StringUtils;

import java.util.List;

/**
 * Allow the user to select one or many suite files in this launch configuration.
 * 
 * @author cbeust
 */
public class SuiteSelector extends TestngTestSelector {

  private Button m_suiteBrowseButton;

  SuiteSelector(TestNGMainTab callback, ButtonHandler handler, Composite comp) {
    super(callback, handler, LaunchType.SUITE, comp,
        "TestNGMainTab.label.suiteTest");

    Composite fill = new Composite(comp, SWT.NONE);
    GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = 2;
    gd.verticalIndent = 0;
    gd.heightHint = 1;
    fill.setLayoutData(gd);

    //
    // Search button
    //
    m_suiteBrowseButton = new Button(comp, SWT.PUSH);
    m_suiteBrowseButton.setText(ResourceUtil.getString("TestNGMainTab.label.browsefs")); //$NON-NLS-1$

    TestngTestSelector.ButtonHandler buttonHandler = new TestngTestSelector.ButtonHandler() {
      public void handleButton() {
        FileDialog fileDialog = new FileDialog(m_suiteBrowseButton.getShell(), SWT.OPEN);
        setText(fileDialog.open());
      }
    };
    ButtonAdapter adapter = new ButtonAdapter(getTestngType(), buttonHandler);

    m_suiteBrowseButton.addSelectionListener(adapter);
    gd = new GridData();
    gd.verticalIndent = 0;
    m_suiteBrowseButton.setLayoutData(gd);

  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    List<String> suites = ConfigurationHelper.getSuites(configuration);
    setText(StringUtils.listToString(suites));
  }
}