/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.testng.eclipse.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.texteditor.ITextEditor;
import org.testng.eclipse.util.ResourceUtil;

/**
 * Abstract Action for opening a Java editor.
 */
public abstract class OpenEditorAction extends Action {
  private String fClassName;
  private TestRunnerViewPart fTestRunner;
  private final boolean        fActivate;

  /**
   * Constructor for OpenEditorAction.
   */
  protected OpenEditorAction(TestRunnerViewPart testRunner, String testClassName) {
    this(testRunner, testClassName, true);
  }

  public OpenEditorAction(TestRunnerViewPart testRunner, String className, boolean activate) {
    super(ResourceUtil.getString("OpenEditorAction.action.label")); //$NON-NLS-1$ // FIXME
    fClassName = className;
    fTestRunner = testRunner;
    fActivate = activate;
  }

  @Override
  public void run() {
    ITextEditor textEditor = null;
    try {
      IJavaElement element = findElement(getLaunchedProject(), fClassName);
      if(element == null) {
        MessageDialog.openError(getShell(),
                                ResourceUtil.getString("OpenEditorAction.error.cannotopen.title"), //$NON-NLS-1$
                                ResourceUtil.getFormattedString("OpenEditorAction.error.cannotopen.message", fClassName)); //$NON-NLS-1$
        return;
      }
      textEditor = (ITextEditor) EditorUtility.openInEditor(element, fActivate);
    }
    catch(CoreException e) {
      ErrorDialog.openError(getShell(),
                            ResourceUtil.getString("OpenEditorAction.error.dialog.title"), //$NON-NLS-1$
                            ResourceUtil.getFormattedString("OpenEditorAction.error.dialog.message", fClassName), //$NON-NLS-1$
                            e.getStatus());
      return;
    }
    if(textEditor == null) {
      return;
    }
    reveal(textEditor);
  }

  protected Shell getShell() {
    return fTestRunner.getSite().getShell();
  }

  protected IJavaProject getLaunchedProject() {
    return fTestRunner.getLaunchedProject();
  }

  protected String getClassName() {
    return fClassName;
  }

  protected abstract IJavaElement findElement(IJavaProject project, String className)
  throws CoreException;

  protected abstract void reveal(ITextEditor editor);

}
