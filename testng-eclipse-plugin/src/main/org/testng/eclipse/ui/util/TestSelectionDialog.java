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
package org.testng.eclipse.ui.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.TwoPaneElementSelector;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants.LaunchType;
import org.testng.eclipse.launch.components.Filters;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.eclipse.util.TestSearchEngine;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

/**
 * A dialog to select a test class or a test suite from a list of types.
 */
public class TestSelectionDialog extends TwoPaneElementSelector {

	private IJavaProject m_project;
	private Object[] m_input;
	private Filters.ITypeFilter m_filter;
	private LaunchType m_testngType;

	private static class PackageRenderer extends JavaElementLabelProvider {
		public PackageRenderer() {
			super(JavaElementLabelProvider.SHOW_PARAMETERS
					| JavaElementLabelProvider.SHOW_POST_QUALIFIED
					| JavaElementLabelProvider.SHOW_ROOT);
		}

		public Image getImage(Object element) {
			Image result = null;
			if (element instanceof IType) {
				result = super.getImage(((IType) element).getPackageFragment());
			} else if (element instanceof IFile) {
				result = super.getImage(element);
			}

			return result;
		}

		public String getText(Object element) {
			String result = element.toString();
			if (element instanceof IType) {
				result = super.getText(((IType) element).getPackageFragment());
			} else if (element instanceof IFile) {
				result = ((IFile) element).getName();
			}

			return result;
		}
	}

	public static TestSelectionDialog createSuiteSelectionDialog(
			final Shell shell, final IJavaProject jproject,
			final Object[] resources) {
		TestSelectionDialog result = new TestSelectionDialog(shell,
		    LaunchType.SUITE, jproject,
				new FileLabelProvider(FileLabelProvider.SHOW_LABEL),
				new FileLabelProvider(FileLabelProvider.SHOW_LABEL_PATH),
				resources, null);
		result.setMessage(ResourceUtil
				.getString("TestNGMainTab.testdialog.selectSuite")); //$NON-NLS-1$

		return result;
	}

	public static TestSelectionDialog createTestTypeSelectionDialog(
			final Shell shell, final IJavaProject jproject,
			final Object[] types, final Filters.ITypeFilter filter) {
		return createJavaElementDialog (shell, jproject, types,
		    LaunchType.CLASS, 
				"TestNGMainTab.testdialog.selectTestClass", filter);
	}

	public static TestSelectionDialog createPackageSelectionDialog(
			final Shell shell, final IJavaProject jproject, final Object[] types) {
		return createJavaElementDialog (shell, jproject, types,
		    LaunchType.PACKAGE, 
				"TestNGMainTab.testdialog.selectPackage", null);
	}
	
	public static TestSelectionDialog createMethodSelectionDialog(
			final Shell shell, final IJavaProject jproject, final Object[] types) {
		return createJavaElementDialog (shell, jproject, types,
		    LaunchType.METHOD, 
				"TestNGMainTab.testdialog.selectMethod", null);
	}
	
	
	private static TestSelectionDialog createJavaElementDialog(final Shell shell, 
			final IJavaProject jproject, final Object[] types, 
			final LaunchType testngType, final String title, final Filters.ITypeFilter filter
			) {
		TestSelectionDialog result = new TestSelectionDialog(shell,
				testngType, jproject,
				new JavaElementLabelProvider(
						JavaElementLabelProvider.SHOW_BASICS
								| JavaElementLabelProvider.SHOW_OVERLAY_ICONS),
				new PackageRenderer(), types, filter);
		result.setMessage(ResourceUtil
				.getString(title));
		return result;
		
		
	}

	private TestSelectionDialog(final Shell shell, final LaunchType type,
			final IJavaProject jproject, final ILabelProvider mainProvider,
			final ILabelProvider detailsProvider, final Object[] input,
			final Filters.ITypeFilter filter) {
		super(shell, mainProvider, detailsProvider);

		m_testngType = type;
		m_project = jproject;
		m_input = input;
		m_filter = filter;
	}

	/*
	 * @see Window#open()
	 */
	public int open() {
		if (null == m_input) {
			switch (m_testngType) {
			case CLASS:
				m_input = new IType[0];

				try {
					m_input = TestSearchEngine.findTests(
							new Object[] { m_project }, m_filter);
				} catch (InterruptedException e) {
					return CANCEL;
				} catch (InvocationTargetException e) {
					TestNGPlugin.log(e.getTargetException());

					return CANCEL;
				}
				break;
			case SUITE:
				m_input = new IFile[0];
				try {
					m_input = TestSearchEngine
							.findSuites(new Object[] { m_project });
				} catch (InterruptedException e) {
					return CANCEL;
				} catch (InvocationTargetException e) {
					TestNGPlugin.log(e.getTargetException());

					return CANCEL;
				}
				break;
			case PACKAGE:
				m_input = new IType[0];

				try {
					m_input = TestSearchEngine.findPackages(
							new Object[] { m_project });
				} catch (InterruptedException e) {
					return CANCEL;
				} catch (InvocationTargetException e) {
					TestNGPlugin.log(e.getTargetException());

					return CANCEL;
				}
				break;
			default:
				throw new IllegalArgumentException(
						"testng type not yet implemented: " + m_testngType);
			}

		}
		setElements(m_input);

		return super.open();
	}

	private static class FileLabelProvider extends LabelProvider {
		public static final int SHOW_LABEL = 1;
		public static final int SHOW_LABEL_PATH = 2;
		public static final int SHOW_PATH_LABEL = 3;
		public static final int SHOW_PATH = 4;

		private static final String fgSeparatorFormat = "{0} - {1}"; //$NON-NLS-1$

		private WorkbenchLabelProvider fLabelProvider;
		private ILabelDecorator fDecorator;

		private int fOrder;
		private String[] fArgs = new String[2];

		public FileLabelProvider(int orderFlag) {
			fDecorator = PlatformUI.getWorkbench().getDecoratorManager()
					.getLabelDecorator();
			fLabelProvider = new WorkbenchLabelProvider();
			fOrder = orderFlag;
		}

		public void setOrder(int orderFlag) {
			fOrder = orderFlag;
		}

		public String getText(Object element) {
			if (!(element instanceof IResource)) {
				return ""; //$NON-NLS-1$
			}

			IResource resource = (IResource) element;
			String text = null;

			if ((resource == null) || !resource.exists()) {
				text = ResourceUtil
						.getString("SearchResultView.removed_resource"); //$NON-NLS-1$
			} else {
				IPath path = resource.getFullPath().removeLastSegments(1);
				if (path.getDevice() == null) {
					path = path.makeRelative();
				}
				if ((fOrder == SHOW_LABEL) || (fOrder == SHOW_LABEL_PATH)) {
					text = fLabelProvider.getText(resource);
					if ((path != null) && (fOrder == SHOW_LABEL_PATH)) {
						fArgs[0] = text;
						fArgs[1] = path.toString();
						text = MessageFormat.format(fgSeparatorFormat, fArgs);
					}
				} else {
					if (path != null) {
						text = path.toString();
					} else {
						text = ""; //$NON-NLS-1$
					}
					if (fOrder == SHOW_PATH_LABEL) {
						fArgs[0] = text;
						fArgs[1] = fLabelProvider.getText(resource);
						text = MessageFormat.format(fgSeparatorFormat, fArgs);
					}
				}
			}

			// Do the decoration
			if (fDecorator != null) {
				String decoratedText = fDecorator.decorateText(text, resource);
				if (decoratedText != null) {
					return decoratedText;
				}
			}

			return text;
		}

		public Image getImage(Object element) {
			if (!(element instanceof IResource)) {
				return null; //$NON-NLS-1$
			}

			IResource resource = (IResource) element;
			Image image = fLabelProvider.getImage(resource);
			if (fDecorator != null) {
				Image decoratedImage = fDecorator
						.decorateImage(image, resource);
				if (decoratedImage != null) {
					return decoratedImage;
				}
			}

			return image;
		}

		public void dispose() {
			super.dispose();
			fLabelProvider.dispose();
		}

		public boolean isLabelProperty(Object element, String property) {
			return fLabelProvider.isLabelProperty(element, property);
		}

	}

}
