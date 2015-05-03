/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids: sdavids@gmx.de bug 37333 Failure Trace cannot 
 * 			navigate to non-public class in CU throwing Exception

 *******************************************************************************/
package org.testng.eclipse.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Open a test in the Java editor and reveal a given line
 */
public class OpenEditorAtLineAction extends OpenEditorAction {

	//fix for bug 37333
	private class NonPublicClassInCUCollector extends SearchRequestor {
		private IJavaElement fFound;
		
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			IJavaElement enclosingElement= (IJavaElement) match.getElement();
			String resourceName= match.getResource().getName();
			if ((enclosingElement instanceof IType) && (resourceName.equals(fCUName)))
				fFound= enclosingElement;
		}
	}
		
	private int fLineNumber;
	private String fCUName;
	
	/**
	 * Constructor for OpenEditorAtLineAction.
	 */
	public OpenEditorAtLineAction(TestRunnerViewPart testRunner, String cuName, String className, int line) {
		super(testRunner, className);
		fLineNumber= line;
		fCUName= cuName;
	}
		
	protected void reveal(ITextEditor textEditor) {
		if (fLineNumber >= 0) {
			try {
				IDocument document= textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
				textEditor.selectAndReveal(document.getLineOffset(fLineNumber-1), document.getLineLength(fLineNumber-1));
			} catch (BadLocationException x) {
				// marker refers to invalid text position -> do nothing
			}
		}
	}
	
	protected IJavaElement findElement(IJavaProject project, String className) throws CoreException {
		IJavaElement element= project.findType(className);
		
		//fix for bug 37333
		if (element == null) {
			SearchPattern pattern=	SearchPattern.createPattern(className, IJavaSearchConstants.TYPE, IJavaSearchConstants.DECLARATIONS,
					SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE | SearchPattern.R_ERASURE_MATCH);
			IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[] { project }, false);
			NonPublicClassInCUCollector requestor= new NonPublicClassInCUCollector();

			SearchEngine searchEngine= new SearchEngine();
			searchEngine.search(pattern, new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
					scope, requestor, new NullProgressMonitor());
			
			element= requestor.fFound;
		}
		
		return element;
	}

	public boolean isEnabled() {
		try {
			return getLaunchedProject().findType(getClassName()) != null;
		} catch (JavaModelException e) {
		}
		return false;
	}
}
