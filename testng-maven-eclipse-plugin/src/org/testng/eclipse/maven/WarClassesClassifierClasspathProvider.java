package org.testng.eclipse.maven;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.jdt.AbstractClassifierClasspathProvider;

public class WarClassesClassifierClasspathProvider extends AbstractClassifierClasspathProvider {

  @Override
  public boolean applies(IMavenProjectFacade mavenProjectFacade, String classifier) {
    return getClassifier().equals(classifier);
  }

  @Override
  public String getClassifier() {
    return "classes";
  }

  /**
   * Adds the main classes folder to the runtime classpath.
   */
  @Override
  public void setRuntimeClasspath(Set<IRuntimeClasspathEntry> runtimeClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor) throws CoreException {
    addMainFolder(runtimeClasspath, mavenProjectFacade, monitor);
  }

  /**
   * Adds the main classes folder to the test classpath.
   */
  @Override
  public void setTestClasspath(Set<IRuntimeClasspathEntry> testClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor) throws CoreException {
    setRuntimeClasspath(testClasspath, mavenProjectFacade, monitor);
  }

}
