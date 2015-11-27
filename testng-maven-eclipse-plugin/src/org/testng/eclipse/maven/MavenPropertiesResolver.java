package org.testng.eclipse.maven;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;
import org.eclipse.m2e.core.MavenPlugin;

public class MavenPropertiesResolver implements IDynamicVariableResolver {

  public String resolveValue(IDynamicVariable variable, String argument)
      throws CoreException {
    if ("settings.localRepository".equals(variable.getName())) {
      return MavenPlugin.getMaven().getLocalRepositoryPath();
    }
    return null;
  }
  
}
