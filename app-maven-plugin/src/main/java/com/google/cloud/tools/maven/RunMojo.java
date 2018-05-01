/*
 * Copyright 2016 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.maven;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.api.devserver.RunConfiguration;
import com.google.cloud.tools.maven.CloudSdkAppEngineFactory.SupportedDevServerVersion;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/** Run App Engine Development App Server synchronously. */
@Mojo(name = "run", defaultPhase = LifecyclePhase.DEPLOY)
public class RunMojo extends CloudSdkMojo implements RunConfiguration {

  /**
   * Path to a yaml file, or a directory containing yaml files, or a directory containing WEB-INF/
   * web.xml. Defaults to <code>${project.build.directory}/${project.build.finalName}</code>.
   */
  @Parameter(alias = "devserver.services", property = "app.devserver.services", required = true)
  protected List<File> services;

  /**
   * Version of the dev app server to use to run the services. Supported values are "1" and
   * "2-alpha". (default: "1")
   */
  @Parameter(
    alias = "devserver.version",
    property = "app.devserver.version",
    required = true,
    defaultValue = "1"
  )
  protected String devserverVersion;

  /** Host name to which application modules should bind. (default: localhost) */
  @Parameter(alias = "devserver.host", property = "app.devserver.host")
  protected String host;

  /** Lowest port to which application modules should bind. (default: 8080) */
  @Parameter(alias = "devserver.port", property = "app.devserver.port")
  protected Integer port;

  /**
   * Host name to which the admin server should bind. (default: localhost)
   *
   * <p><i>Supported only for devserver version 2-alpha.</i>
   */
  @Parameter(alias = "devserver.adminHost", property = "app.devserver.adminHost")
  protected String adminHost;

  /**
   * Port to which the admin server should bind. (default: 8000)
   *
   * <p><i>Supported only for devserver version 2-alpha.</i>
   */
  @Parameter(alias = "devserver.adminPort", property = "app.devserver.adminPort")
  protected Integer adminPort;

  /**
   * Name of the authorization domain to use. (default: gmail.com)
   *
   * <p><i>Supported only for devserver version 2-alpha.</i>
   */
  @Parameter(alias = "devserver.authDomain", property = "app.devserver.authDomain")
  protected String authDomain;

  /**
   * Path to the data (datastore, blobstore, etc.) associated with the application. (default: None)
   *
   * <p><i>Supported only for devserver version 2-alpha.</i>
   */
  @Parameter(alias = "devserver.storagePath", property = "app.devserver.storagePath")
  protected File storagePath;

  /**
   * Path to a file used to store datastore contents (defaults to a file in --storage_path if not
   * set). (default: None)
   *
   * <p><i>Supported only for devserver version 2-alpha.</i>
   */
  @Parameter(alias = "devserver.datastorePath", property = "app.devserver.datastorePath")
  protected File datastorePath;

  /**
   * The log level below which logging messages generated by application code will not be displayed
   * on the console. Options: debug, info, warning, critical, error. (default: info)
   *
   * <p><i>Supported only for devserver version 2-alpha.</i>
   */
  @Parameter(alias = "devserver.logLevel", property = "app.devserver.logLevel")
  protected String logLevel;

  /**
   * The maximum number of runtime instances that can be started for a particular module - the value
   * can be an integer, in what case all modules are limited to that number of instances or a
   * comma-seperated list of module:max_instances e.g. "default:5,backend:3".o (default: None)
   *
   * <p><i>Supported only for devserver version 2-alpha.</i>
   */
  @Parameter(alias = "devserver.maxModuleInstances", property = "app.devserver.maxModuleInstances")
  protected Integer maxModuleInstances;

  /**
   * Use mtime polling for detecting source code changes - useful if modifying code from a remote
   * machine using a distributed file system. (default: False)
   *
   * <p><i>Supported only for devserver version 2-alpha.</i>
   */
  @Parameter(
    alias = "devserver.useMtimeFileWatcher",
    property = "app.devserver.useMtimeFileWatcher"
  )
  protected Boolean useMtimeFileWatcher;

  /**
   * Override the application's threadsafe configuration - the value can be a boolean, in which case
   * all modules threadsafe setting will be overridden or a comma- separated list of
   * module:threadsafe_override e.g. "default:False,backend:True". (default: None)
   *
   * <p><i>Supported only for devserver version 2-alpha.</i>
   */
  @Parameter(alias = "devserver.threadsafeOverride", property = "app.devserver.threadsafeOverride")
  protected String threadsafeOverride;

  /**
   * The script to run at the startup of new Python runtime instances (useful for tools such as
   * debuggers. (default: None)
   *
   * <p><i>Supported only for devserver version 2-alpha.</i>
   */
  @Parameter(
    alias = "devserver.pythonStartupScript",
    property = "app.devserver.pythonStartupScript"
  )
  protected String pythonStartupScript;

  /**
   * The arguments made available to the script specified in devserver.pythonStartupScript.
   * (default: None)
   *
   * <p><i>Supported only for devserver version 2-alpha.</i>
   */
  @Parameter(alias = "devserver.pythonStartupArgs", property = "app.devserver.pythonStartupArgs")
  protected String pythonStartupArgs;

  /**
   * Additional arguments to pass to the java command when launching an instance of the app. May be
   * specified more than once. Example: "-Xmx1024m -Xms256m" (default: None)
   */
  @Parameter(alias = "devserver.jvmFlags", property = "app.devserver.jvmFlags")
  protected List<String> jvmFlags;

  /**
   * For custom VM Runtime, specify an entrypoint for custom runtime modules. This is required when
   * such modules are present. Include "{port}" in the string (without quotes) to pass the port
   * number in as an argument. For instance: "gunicorn -b localhost:{port} mymodule:application".
   * (default: )
   *
   * <p><i>Supported only for devserver version 2-alpha.</i>
   */
  @Parameter(alias = "devserver.customEntrypoint", property = "app.devserver.customEntrypoint")
  protected String customEntrypoint;

  /**
   * Specify the default runtimes you would like to use. Valid runtimes for Java are ['java',
   * 'custom', 'java7']. (default: )
   *
   * <p><i>Supported only for devserver version 2-alpha.</i>
   */
  @Parameter(alias = "devserver.runtime", property = "app.devserver.runtime")
  protected String runtime;

  /**
   * Make files specified in the app.yaml "skip_files" or "static" handles readable by the
   * application. (default: False)
   *
   * <p><i>Supported only for devserver version 2-alpha.</i>
   */
  @Parameter(alias = "devserver.allowSkippedFiles", property = "app.devserver.allowSkippedFiles")
  protected Boolean allowSkippedFiles;

  /**
   * Port to which the server for API calls should bind. (default: 0)
   *
   * <p><i>Supported only for devserver version 2-alpha.</i>
   */
  @Parameter(alias = "devserver.apiPort", property = "app.devserver.apiPort")
  protected Integer apiPort;

  /**
   * Restart instances automatically when files relevant to their module are changed. (default:
   * True)
   *
   * <p><i>Supported only for devserver version 2-alpha.</i>
   */
  @Parameter(alias = "devserver.automaticRestart", property = "app.devserver.automaticRestart")
  protected Boolean automaticRestart;

  /**
   * The log level below which logging messages generated by the development server will not be
   * displayed on the console (this flag is more useful for diagnosing problems in dev_appserver.py
   * rather than in application code). {debug,info,warning,critical,error}. (default: info)
   *
   * <p><i>Supported only for devserver version 2-alpha.</i>
   */
  @Parameter(
    alias = "devserver.devAppserverLogLevel",
    property = "app.devserver.devAppserverLogLevel"
  )
  protected String devAppserverLogLevel;

  /**
   * Skip checking for SDK updates. (if false, use .appcfg_nag to decide) (default: False)
   *
   * <p><i>Supported only for devserver version 2-alpha.</i>
   */
  @Parameter(alias = "devserver.skipSdkUpdateCheck", property = "app.devserver.skipSdkUpdateCheck")
  protected Boolean skipSdkUpdateCheck;

  /** Default Google Cloud Storage bucket name. (default: None) */
  @Parameter(
    alias = "devserver.defaultGcsBucketName",
    property = "app.devserver.defaultGcsBucketName"
  )
  protected String defaultGcsBucketName;

  /**
   * Clear the datastore before running. (default: False)
   *
   * <p><i>Supported only for devserver version 2-alpha.</i>
   */
  @Parameter(alias = "devserver.clearDatastore", property = "app.devserver.clearDatastore")
  protected Boolean clearDatastore;

  /** Environment variables passed to the devappserver process. */
  @Parameter(alias = "devserver.environment", property = "app.devserver.environment")
  protected Map<String, String> environment;

  /** Environment variables passed to the devappserver process. */
  @Parameter(
    alias = "devserver.additionalArguments",
    property = "app.devserver.additionalArguments"
  )
  protected List<String> additionalArguments;

  // RunAsyncMojo should override #runServer(version) so that other configuration changing code
  // shared between these classes is executed
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    SupportedDevServerVersion convertedVersion = convertDevserverVersionString();
    if (services == null || services.isEmpty()) {
      Build build = mavenProject.getBuild();
      services =
          Collections.singletonList(
              new File(build.getDirectory()).toPath().resolve(build.getFinalName()).toFile());
    }
    verifyAppEngineStandardApp();
    runServer(convertedVersion);
  }

  protected void runServer(SupportedDevServerVersion convertedVersion) {
    try {
      getAppEngineFactory().devServerRunSync(convertedVersion).run(this);
    } catch (AppEngineException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Verifies that {@code version} is of the supported values.
   *
   * @throws MojoExecutionException if {@code version} cannot be converted to {@link
   *     SupportedDevServerVersion}
   */
  protected SupportedDevServerVersion convertDevserverVersionString()
      throws MojoExecutionException {
    try {
      return SupportedDevServerVersion.parse(devserverVersion);
    } catch (IllegalArgumentException ex) {
      throw new MojoExecutionException("Invalid version", ex);
    }
  }

  /** Determine if the built application is a Standard Environment app. */
  protected boolean isStandardEnvironmentApp() {
    return mavenProject != null
        && mavenProject.getBuild() != null
        && new File(
                mavenProject.getBuild().getDirectory()
                    + "/"
                    + mavenProject.getBuild().getFinalName()
                    + "/WEB-INF/appengine-web.xml")
            .exists();
  }

  /**
   * Verifies that the current build is an App Engine Standard Environment app. If it's not, an
   * exception will be thrown.
   */
  protected void verifyAppEngineStandardApp() throws MojoExecutionException {
    if (!isStandardEnvironmentApp()) {
      throw new MojoExecutionException(
          "Dev App Server does not support App Engine Flexible Environment applications.");
    }
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public Integer getPort() {
    return port;
  }

  @Override
  public String getAdminHost() {
    return adminHost;
  }

  @Override
  public Integer getAdminPort() {
    return adminPort;
  }

  @Override
  public String getAuthDomain() {
    return authDomain;
  }

  @Override
  public File getStoragePath() {
    return storagePath;
  }

  @Override
  public String getLogLevel() {
    return logLevel;
  }

  @Override
  public Integer getMaxModuleInstances() {
    return maxModuleInstances;
  }

  @Override
  public Boolean getUseMtimeFileWatcher() {
    return useMtimeFileWatcher;
  }

  @Override
  public String getThreadsafeOverride() {
    return threadsafeOverride;
  }

  @Override
  public String getPythonStartupScript() {
    return pythonStartupScript;
  }

  @Override
  public String getPythonStartupArgs() {
    return pythonStartupArgs;
  }

  @Override
  public List<String> getJvmFlags() {
    return jvmFlags;
  }

  @Override
  public String getCustomEntrypoint() {
    return customEntrypoint;
  }

  @Override
  public String getRuntime() {
    return runtime;
  }

  @Override
  public Boolean getAllowSkippedFiles() {
    return allowSkippedFiles;
  }

  @Override
  public Integer getApiPort() {
    return apiPort;
  }

  @Override
  public Boolean getAutomaticRestart() {
    return automaticRestart;
  }

  @Override
  public String getDevAppserverLogLevel() {
    return devAppserverLogLevel;
  }

  @Override
  public Boolean getSkipSdkUpdateCheck() {
    return skipSdkUpdateCheck;
  }

  @Override
  public String getDefaultGcsBucketName() {
    return defaultGcsBucketName;
  }

  @Override
  public Boolean getClearDatastore() {
    return clearDatastore;
  }

  @Override
  public List<File> getServices() {
    return services;
  }

  @Override
  public File getDatastorePath() {
    return datastorePath;
  }

  @Override
  public Map<String, String> getEnvironment() {
    return environment;
  }

  @Override
  public List<String> getAdditionalArguments() {
    return additionalArguments;
  }
}
