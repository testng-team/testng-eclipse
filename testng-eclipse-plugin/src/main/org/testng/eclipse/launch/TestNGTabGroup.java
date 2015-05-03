package org.testng.eclipse.launch;

 
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;
 
public class TestNGTabGroup extends AbstractLaunchConfigurationTabGroup {
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {		
		ILaunchConfigurationTab[] tabs= new ILaunchConfigurationTab[] {
        new TestNGMainTab(),
//        new AlternateLaunchConfigurationTab(),
  			new JavaArgumentsTab(),
  			new JavaClasspathTab(),
  			new JavaJRETab(),
  			new SourceLookupTab(),
  			new EnvironmentTab(),
  			new CommonTab()
		};
		
		setTabs(tabs);
	}
}
