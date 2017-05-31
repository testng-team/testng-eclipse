package org.testng.eclipse.maven;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.profiles.core.internal.IProfileManager;
import org.eclipse.m2e.profiles.core.internal.MavenProfilesCoreActivator;
import org.eclipse.m2e.profiles.core.internal.ProfileData;
import org.eclipse.m2e.profiles.core.internal.ProfileState;
import org.testng.eclipse.launch.ITestNGLaunchConfigurationProvider;
import org.testng.eclipse.launch.LaunchConfigurationHelper;

public class MavenTestNGLaunchConfigurationProvider implements ITestNGLaunchConfigurationProvider {

  public static final Pattern PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

  @Override
  public String getVmArguments(ILaunchConfiguration launchConf) throws CoreException {
    IProject project = LaunchConfigurationHelper.getProject(launchConf);
    if (PreferenceUtils.getBoolean(project, Activator.PREF_ARGLINE)
        || PreferenceUtils.getBoolean(project, Activator.PREF_SYSPROPERTIES)) {
      String vmArgs = getVMArgsFromPom(launchConf);
      return vmArgs;
    }
    return null;
  }

  @Override
  public List<String> getEnvironment(ILaunchConfiguration launchConf) throws CoreException {
    IProject project = LaunchConfigurationHelper.getProject(launchConf);
    if (project == null || !project.hasNature(IMavenConstants.NATURE_ID)) {
      return null;
    }

    IMavenProjectFacade prjFecade = MavenPlugin.getMavenProjectRegistry().getProject(project);
    if (prjFecade == null) {
      throw new CoreException(Activator.createError(project.getName() + " is not in Maven project registry."));
    }

    IProfileManager profileManager = MavenProfilesCoreActivator.getDefault().getProfileManager();
    List<ProfileData> profiles = profileManager.getProfileDatas(prjFecade, new NullProgressMonitor());
    Model model = MavenPlugin.getMavenModelManager().readMavenModel(prjFecade.getPom());
    Xpp3Dom confDom = findPluginConfiguration(model, profiles);
    if (confDom != null) {
      if (PreferenceUtils.getBoolean(project, Activator.PREF_ENVIRON)) {
        Xpp3Dom envDom = confDom.getChild("environmentVariables");
        if (envDom != null) {
          List<String> environList = new ArrayList<>(envDom.getChildCount());
          for (Xpp3Dom varDom : envDom.getChildren()) {
            environList.add(varDom.getName() + "=\"" + varDom.getValue() + "\"");
          }
          return environList;
        }
      }
    }
    return null;
  }

  @SuppressWarnings("restriction")
  private String getVMArgsFromPom(ILaunchConfiguration launchConf) throws CoreException {
    IProject project = LaunchConfigurationHelper.getProject(launchConf);
    if (project == null || !project.hasNature(IMavenConstants.NATURE_ID)) {
      return null;
    }

    IMavenProjectFacade prjFecade = MavenPlugin.getMavenProjectRegistry().getProject(project);
    if (prjFecade == null) {
      throw new CoreException(Activator.createError(project.getName() + " is not in Maven project registry."));
    }

    MavenProject mvnProject = prjFecade.getMavenProject(new NullProgressMonitor());
    IProfileManager profileManager = MavenProfilesCoreActivator.getDefault().getProfileManager();
    List<ProfileData> selectedProfiles = profileManager.getProfileDatas(prjFecade, new NullProgressMonitor());
    Model model = mvnProject.getModel();
    Xpp3Dom confDom = findPluginConfiguration(model, selectedProfiles);
    if (confDom != null) {
      StringBuilder sb = new StringBuilder();
      if (PreferenceUtils.getBoolean(project, Activator.PREF_ARGLINE)) {
        Xpp3Dom argDom = confDom.getChild("argLine");
        if (argDom != null) {
          sb.append(argDom.getValue());
        }
      }

      if (PreferenceUtils.getBoolean(project, Activator.PREF_SYSPROPERTIES)) {
        Xpp3Dom propDom = confDom.getChild("systemPropertyVariables");
        if (propDom != null) {
          for (Xpp3Dom pDom : propDom.getChildren()) {
            sb.append(" -D").append(pDom.getName()).append("=\"").append(pDom.getValue()).append("\"");
          }
        }
      }

      String vmArgs = sb.toString();
      vmArgs = resolve(vmArgs, mvnProject, selectedProfiles, prjFecade);
      return vmArgs;
    }
    return null;
  }

  /**
   * find the configuration of maven-surefire-plugin and/or
   * maven-failsafe-plugin.
   * 
   * @param model
   *          the Maven POM model
   * @return {@code null} if not found
   */
  @SuppressWarnings("restriction")
  private Xpp3Dom findPluginConfiguration(Model model, List<ProfileData> selectedProfiles) {
    Xpp3Dom pluginConf = null;

    // found from selected profiles first, if anyone matched, use and return it.
    if (selectedProfiles != null) {
      for (ProfileData pd : selectedProfiles) {
        if (pd.getActivationState() == ProfileState.Active) {
          List<Profile> profiles = model.getProfiles();
          if (profiles != null) {
            for (Profile profile : profiles) {
              if (pd.getId().equals(profile.getId())) {
                pluginConf = findPluginConfiguration(profile.getBuild());
                if (pluginConf != null) {
                  return pluginConf;
                }
              }
            }
          }
        }
      }
    }

    // otherwise, found from project build base
    pluginConf = findPluginConfiguration(model.getBuild());

    return pluginConf;
  }

  private Xpp3Dom findPluginConfiguration(BuildBase build) {
    if (build == null) {
      return null;
    }

    // first, find from project plugins
    List<Plugin> plugins = build.getPlugins();
    Xpp3Dom pluginConf = findPluginConfiguration(plugins);

    // otherwise, find from project pluginManagement
    if (pluginConf == null) {
      PluginManagement pluginMgnt = build.getPluginManagement();
      if (pluginMgnt != null) {
        plugins = pluginMgnt.getPlugins();
        pluginConf = findPluginConfiguration(plugins);
      }
    }

    return pluginConf;
  }

  private Xpp3Dom findPluginConfiguration(List<Plugin> plugins) {
    if (plugins != null) {
      for (Plugin plugin : plugins) {
        if ("maven-surefire-plugin".equals(plugin.getArtifactId())
            || "maven-failsafe-plugin".equals(plugin.getArtifactId())) {
          Object mvnPlugConf = plugin.getConfiguration();
          if (isTargetConfiguration(mvnPlugConf)) {
            return (Xpp3Dom) mvnPlugConf;
          } else {
            List<PluginExecution> pexecs = plugin.getExecutions();
            if (pexecs != null) {
              for (PluginExecution pexec : pexecs) {
                mvnPlugConf = pexec.getConfiguration();
                if (isTargetConfiguration(mvnPlugConf)) {
                  return (Xpp3Dom) mvnPlugConf;
                }
              }
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * the configuration node is trade as target one if one of the condition
   * satisfied:
   * <ul>
   * <li>if it has argLine and it's not empty</li>
   * <li>or, if it has systemPropertyVariables and it contains children</li>
   * <li>or, if it has environmentVariables and it contains children</li> </ul
   * 
   * @param configuration
   * @return {@code true} if found
   */
  private boolean isTargetConfiguration(Object configuration) {
    if (configuration != null && configuration instanceof Xpp3Dom) {
      Xpp3Dom dom = ((Xpp3Dom) configuration);
      Xpp3Dom d = dom.getChild("argLine");
      if (d != null && !d.getValue().trim().isEmpty()) {
        return true;
      }

      d = dom.getChild("systemPropertyVariables");
      if (d != null && d.getChildCount() > 0) {
        return true;
      }

      d = dom.getChild("environmentVariables");
      if (d != null && d.getChildCount() > 0) {
        return true;
      }
    }

    return false;
  }

  private String resolve(String text, MavenProject mvnProject, List<ProfileData> selectedProfiles,
      IMavenProjectFacade prjFecade) throws CoreException {
    if (text == null) {
      return text;
    }
    if (!text.contains("${")) {
      return text;
    }

    RegexBasedInterpolator inter = new RegexBasedInterpolator();
    Model model = mvnProject.getModel();
    Properties profileProperties = collectProperties(model, selectedProfiles, prjFecade);
    try {
      inter.addValueSource(new PropertiesBasedValueSource(profileProperties));
      inter.addValueSource(new EnvarBasedValueSource());
      inter.addValueSource(new PrefixedObjectValueSource(Arrays.asList(new String[] { "pom.", "project." }), //$NON-NLS-1$ //$NON-NLS-2$
          model, false));
      inter.addValueSource(new PrefixedObjectValueSource("settings.", MavenPlugin.getMaven().getSettings()));

      text = inter.interpolate(text);
    } catch (IOException | InterpolationException e) {
      Activator.log("interpolate [" + text + "] failed: " + e.getMessage(), e);
    }
    return text;
  }

  @SuppressWarnings("restriction")
  private Properties collectProperties(Model model, List<ProfileData> selectedProfiles, IMavenProjectFacade project) {
    Properties result = new Properties();

    //
    // basic properties
    //

    // sometimes we don't have 'localRepository' defined in ~/.m2/settings.xml,
    // so we trade it as a special case.
    result.put("settings.localRepository", MavenPlugin.getMaven().getLocalRepositoryPath());
    result.put("basedir", project.getFullPath().toOSString());

    //
    // project base properties
    //

    // actually we don't really need this since Maven model already 
    // resolves the property placehoder at project level. 
    result.putAll(model.getProperties());

    //
    // profile base properties could override the project level properties/
    //

    if (selectedProfiles != null) {
      for (ProfileData pd : selectedProfiles) {
        if (pd.getActivationState() == ProfileState.Active) {
          List<Profile> profiles = model.getProfiles();
          if (profiles != null) {
            for (Profile profile : profiles) {
              if (pd.getId().equals(profile.getId())) {
                result.putAll(profile.getProperties());
              }
            }
          }
        }
      }
    }

    return result;
  }
}
