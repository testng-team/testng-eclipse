package org.testng.eclipse.launch.components;


import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.model.WorkbenchLabelProvider;


public class ProjectBrowserComposite {
  protected CheckboxTreeViewer m_viewer;
  protected ILabelProvider m_labelProvider;
  protected ITreeContentProvider m_contentProvider;
  protected Object m_rootInput;
  protected ViewerFilter m_projectContentFilter;
  
  public ProjectBrowserComposite(final Composite parent) {
    m_viewer = new CheckboxTreeViewer(parent);
    m_labelProvider = new WorkbenchLabelProvider();
    m_contentProvider = new ProjectContentProvider();
    m_viewer.setLabelProvider(m_labelProvider);
    m_viewer.setContentProvider(m_contentProvider);
    m_viewer.setSelection(new StructuredSelection(new IResource[0]));
  }
  
  public void setSelection(final Object[] selectedElements) {
    m_viewer.setCheckedElements(selectedElements);
  }
  
  public void setInput(IJavaProject input) {
    if(null == input) {
      return;
    }

    if(null != m_projectContentFilter) {
      m_viewer.removeFilter(m_projectContentFilter);
    }
    
    m_rootInput = input;
    m_projectContentFilter = Filters.createProjectContentFilter(input); 
    m_viewer.addFilter(m_projectContentFilter);
    m_viewer.setSelection(new StructuredSelection(new IResource[0]));
    m_viewer.setInput(input.getProject().getParent());
  }
  
  public Tree getTree() {
    return m_viewer.getTree();
  }
  
  public Object[] getCheckedElements() {
    return m_viewer.getCheckedElements();
  }
  
  public void addViewerFilter(ViewerFilter vf) {
    m_viewer.addFilter(vf);
  }
  
  public static class ProjectContentProvider implements ITreeContentProvider {
    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
     */
    public Object[] getChildren(Object parentElement) {
      if(parentElement instanceof IContainer) {
        try {
          return ((IContainer) parentElement).members();
        } catch(CoreException ce) {
          ;
        }
      } 
        
      return null;
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
     */
    public Object getParent(Object element) {
      return ((IResource) element).getParent();
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
     */
    public boolean hasChildren(Object element) {
      IResource resource = (IResource) element;
      
      if(IResource.FILE == resource.getType()) {
        return false;
      } else {
        return true;
      }
    }

    /**
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    public Object[] getElements(Object inputElement) {
      return getChildren(inputElement);
    }

    /**
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    public void dispose() {
    }

    /**
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

  }
}
