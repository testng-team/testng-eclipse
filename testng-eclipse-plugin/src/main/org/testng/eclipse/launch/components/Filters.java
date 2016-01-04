package org.testng.eclipse.launch.components;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.testng.eclipse.ui.util.TypeParser;


public class Filters {
  private Filters() {
  }
  
  public static final ViewerFilter TEST_FILTER = new TestViewerFilter();
  
  public static final ViewerFilter SOURCE_DIRECTORY_FILTER = new SourceDirectoryFilter();

  public static final ViewerFilter createProjectContentFilter(IJavaProject ijp) {
    IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    List<IProject> rejectedElements = new ArrayList<>(allProjects.length);
    IProject iproject = ijp.getProject();
    
    for (int i= 0; i < allProjects.length; i++) {
      if (!allProjects[i].equals(iproject)) {
        rejectedElements.add(allProjects[i]);
      }
    }
    
    return new ProjectContentFilter(rejectedElements);
  }
  
  private static class ProjectContentFilter extends ViewerFilter {
    private List<IProject> m_rejectedEntries;
    
    private ProjectContentFilter(final List<IProject> rejectedEntries) {
      m_rejectedEntries = rejectedEntries;
    }
    
    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if(null == m_rejectedEntries || m_rejectedEntries.size() == 0) {
        return true;
      }
      
      for (IProject prj : m_rejectedEntries) {
        if(element.equals(prj)) {
          return false;
        }
      }

      return true;
    }
  }
  
  private static class TestViewerFilter extends ViewerFilter {
    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if(element instanceof IProject) {
        return true;
      } else if(element instanceof IFolder) {
        return hasTests((IContainer) element);
      } else if(element instanceof IFile) {
        if(isTest((IFile) element)) {
          return true;
        }
      }

      return false;
    }
  }
  
  private static class SourceDirectoryFilter extends ViewerFilter {
    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if(element instanceof IProject) {
        return true;
      } else if(element instanceof IFolder) {
        return hasSources((IContainer) element);
      } 

      return false;
    }
  }
  
  private static boolean hasTests(final IContainer folder) {
    try {
      IResource[] children = folder.members();
      
      for(IResource res : children) {
        if(res instanceof IFile) {
          if(isTest((IFile) res)) {
            return true;
          }
          else if (res instanceof IFolder) {
            if(hasTests((IContainer) res)) {
              return true;
            }
          }
        }
      }
    }
    catch(CoreException ce) {
    }
    
    return false;
  }
  
  private static boolean isTest(final IFile file) {
    return "class".equals(file.getFileExtension()) 
            || isSource(file)
            || "xml".equals(file.getFileExtension());
  }
  
  private static boolean isSource(final IFile file) {
    return "java".equals(file.getFileExtension());
  }
  
  private static boolean hasSources(final IContainer folder) {
    try {
      IResource[] children = folder.members();
      
      for(IResource res : children) {
        if(res instanceof IFile) {
          if(isSource((IFile) res)) {
            return true;
          }
        }
        else if (res instanceof IFolder) {
          if(hasSources((IContainer) res)) {
            return true;
          }
        }
      }
    }
    catch(CoreException ce) {
    }
    
    return false;
  }
  

  /**
   * An interface to filter what types contain a certain test
   * @author cbeust
   */
  public static interface ITypeFilter {
    public boolean accept(IType name);
  }
  
  public static Filters.ITypeFilter SINGLE_TEST = new Filters.ITypeFilter() {
    public boolean accept(IType type) {
      boolean result = TypeParser.parseType(type).isTestNGClass();
      return result;
    }
  };

  public static class GroupFilter implements ITypeFilter {
    private String[] m_groupNames;

    public GroupFilter(String... groupNames) {
      m_groupNames = groupNames;
    }

    public boolean accept(IType type) {
      ITestContent parsedType = TypeParser.parseType(type);
      if (! parsedType.isTestNGClass()) {
        return false;
      } else {
        for (String group : m_groupNames) {
          if (parsedType.getGroups().contains(group)) {
            return true;
          }
        }
        return false;
      }
    }
  }

  public static Filters.ITypeFilter SUITE  = new Filters.ITypeFilter() {
    public boolean accept(IType type) {
      boolean result = false;
      
//      try {
        ppp("CHECKING TESTNG.XML FOR " + type);
//        result = hasTestMethod(type);
//      }
//      catch (JavaModelException e) {
//        // ignore
//      }
      
      return result;
    }
  };  

  public static void ppp(String s) {
    System.out.println("[Filters] " + s);
  }
  
}

/////
