package org.testng.eclipse.ui.buildpath;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.buildpath.TestNGContainerInitializer;
import org.testng.eclipse.util.ResourceUtil;

public class TestNGContainerWizardPage extends WizardPage implements IClasspathContainerPage {

  public TestNGContainerWizardPage() {
    super("TestNGContainerWizardPage"); //$NON-NLS-1$
    setTitle(ResourceUtil.getString("TestNGContainerWizardPage.title"));  //$NON-NLS-1$
    setDescription(ResourceUtil.getString("TestNGContainerWizardPage.description")); //$NON-NLS-1$
    setImageDescriptor(TestNGPlugin.getImageDescriptor("wizban/library_wiz.png")); //$NON-NLS-1$
  }

  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    container.setLayout(layout);

    new Label(container, SWT.NONE).setText(ResourceUtil.getString("TestNGContainerWizardPage.message")); //$NON-NLS-1$

    setControl(container);
    Dialog.applyDialogFont(container);
  }

  public boolean finish() {
    return true;
  }

  public IClasspathEntry getSelection() {
    return JavaCore.newContainerEntry(TestNGContainerInitializer.TESTNG_PATH);
  }

  public void setSelection(IClasspathEntry containerEntry) {
  }

}
