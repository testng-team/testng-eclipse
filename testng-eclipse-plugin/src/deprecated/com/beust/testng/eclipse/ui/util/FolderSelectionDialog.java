package org.testng.eclipse.ui.util;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.dialogs.SelectionStatusDialog;


/**
 * Class usage XXX
 * 
 * @version $Revision$
 */
public class FolderSelectionDialog extends SelectionStatusDialog {
	protected CheckboxTreeViewer m_viewer;
	protected ILabelProvider m_labelProvider;
	protected ITreeContentProvider m_contentProvider;
	protected Object m_rootInput;

	protected Set m_selection;
	protected List m_filters;
	protected Object m_focusedElement;

	public FolderSelectionDialog(	Shell parentShell,
											ILabelProvider labelProvider,
											ITreeContentProvider contentProvider) {
		super(parentShell);

		m_labelProvider = labelProvider;
		m_contentProvider = contentProvider;

		setSelectionResult(null);
		setStatusLineAboveButtons(true);

		setShellStyle(getShellStyle() | SWT.MAX | SWT.RESIZE);

		m_filters = null;
		m_focusedElement = null;
	}

	/**
	 * Sets the tree input.
	 * @param input the tree input.
	 */
	public void setInput(Object input) {
		m_rootInput = input;
	}

	
	/**
	 * @see org.eclipse.ui.dialogs.SelectionDialog#setInitialSelections(java.lang.Object[])
	 */
	public void setInitialSelections(Object[] selectedElements) {
		super.setInitialSelections(selectedElements);
		
		m_selection = new HashSet();
		for(int i = 0; i < selectedElements.length; i++) {
			m_selection.add(selectedElements[i]);
		}
	}
	
	/**
	 * Adds a filter to the tree viewer.
	 * @param filter a filter.
	 */
	public void addFilter(ViewerFilter filter) {
		if(null == m_filters) {
			m_filters = new ArrayList(4);
		}

		m_filters.add(filter);
	}

	/**
	 * Handles cancel button pressed event.
	 */
	protected void cancelPressed() {
		setSelectionResult(null);
		super.cancelPressed();
	}

	private void access$superCreate() {
		super.create();
	}

	/**
	 * @see Window#create()
	 */
	public void create() {
		BusyIndicator.showWhile(null, new Runnable() {
			public void run() {
				access$superCreate();

				List initialSelection = getInitialElementSelections();
				m_viewer.setCheckedElements(initialSelection.toArray());
				m_viewer.expandToLevel(2);

				if(null != initialSelection) {
					for(Iterator iter = initialSelection.iterator(); iter.hasNext();) {
						m_viewer.reveal(iter.next());
					}
				}

				updateOKStatus();
			}
		});
	}

	protected void updateOKStatus() {
		computeResult();

		if(null != getResult()) {
			updateStatus(new StatusInfo());
		} else {
			updateStatus(new StatusInfo(IStatus.ERROR, "")); //$NON-NLS-1$
		}
	}

	/**
	 * Creates the tree viewer.
	 * 
	 * @param parent the parent composite
	 * @return the tree viewer
	 */
	protected CheckboxTreeViewer createTreeViewer(Composite parent) {
		m_viewer = new CheckboxTreeViewer(parent, SWT.BORDER);

		m_viewer.setContentProvider(m_contentProvider);
		m_viewer.setLabelProvider(m_labelProvider);

		//	 	m_viewer.setSorter(new ResourceSorter(ResourceSorter.NAME));

		if(null != m_filters) {
			for(int i = 0; i != m_filters.size(); i++)
				m_viewer.addFilter((ViewerFilter) m_filters.get(i));
		}

		m_viewer.setInput(m_rootInput);

		return m_viewer;
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite) 
	 */
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		createMessageArea(composite);
		final CheckboxTreeViewer treeViewer = createTreeViewer(composite);

		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = convertWidthInCharsToPixels(60);
		data.heightHint = convertHeightInCharsToPixels(18);

		Tree treeWidget = treeViewer.getTree();
		treeWidget.setLayoutData(data);
		treeWidget.setFont(composite.getFont());

		if(null != m_selection) {
			for(Iterator iter = m_selection.iterator(); iter.hasNext();) {
				treeViewer.setSubtreeChecked(iter.next(), true);
			}
		}

		if(null != m_focusedElement) {
			treeViewer.setSelection(new StructuredSelection(m_focusedElement), true);
		}

		treeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				Object element = event.getElement();
				boolean add = event.getChecked();
				
				if(add) {
					m_selection.add(element);
				} else {
					m_selection.remove(element);
				}
				
				treeViewer.setSubtreeChecked(element, add);
				updateOKStatus();
			}
		});

		applyDialogFont(composite);
		
		return composite;
	}

	/**
	 * @see org.eclipse.ui.dialogs.SelectionStatusDialog#computeResult()
	 */
	protected void computeResult() {
		setSelectionResult(m_selection.toArray());
	}

	public void setInitialFocus(Object focusElement) {
		m_focusedElement = focusElement;
	}
}
