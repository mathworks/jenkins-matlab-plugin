package com.mathworks.ci.tools;

import com.mathworks.ci.MatlabInstallation;
import com.mathworks.ci.Message;
import com.sun.akuma.CLibrary.FILE;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstallerDescriptor;
import hudson.util.ArgumentListBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import jenkins.security.MasterToSlaveCallable;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class MatlabInstaller extends DownloadFromUrlInstaller {
    private String version;
    private String products;

    @DataBoundConstructor
    public MatlabInstaller(String id) {
        super(id);
    }

    public String getVersion(){
        return this.version;
    }

    @DataBoundSetter
    public void setVersion(String version) {
        this.version = version;
    }

    public String getProducts() { return this.products; }

    @DataBoundSetter
    public void setProducts(String products) {
        this.products = products;
    }

    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        FilePath expectedPath = preferredLocation(tool, node);
        MatlabInstallable installable;
        try {
            installable = (MatlabInstallable) getInstallable(node);
        } catch (InstallationFailedException e) {
            throw new InstallationFailedException(e.getMessage());
        }

        if (installable == null) {
            log.getLogger().println(Message.getValue("matlab.tools.auto.install.matlab.installable.error"));
            return expectedPath;
        }

        if (isUpToDate(expectedPath, installable)) {
            return expectedPath;
        }

        FilePath matlabVersionFile = new FilePath(expectedPath, "VersionInfo.xml");
        if(matlabVersionFile.exists()) {
            // If MTALB found at given location then just pull matlab-bacth and mpm executables.
            getFreshCopyOfExecutables(installable, expectedPath);
        } else {
            // Cleanup before initial installation if last install was incomplete for any reason
            performCleanup(expectedPath);
            getFreshCopyOfExecutables(installable, expectedPath);

            // Create installation process
            EnvVars env = node.toComputer().getEnvironment();
            Launcher matlabInstaller = node.createLauncher(log);
            ProcStarter installerProc = matlabInstaller.launch();

            ArgumentListBuilder args = new ArgumentListBuilder();
            args.add(expectedPath.getRemote() + getNodeSpecificMPMExecutor(node)); // can use installable here
            args.add("install");
            args.add("--release=" + this.getVersion());
            args.add("--destination="+ expectedPath.getRemote());
            args.add(getMatlabProducts());
            installerProc.pwd(expectedPath)
                    .cmds(args)
                    .envs(env).stdout(log);
            try{
                int result = installerProc.join();
            } catch (Exception e) {
                throw new InstallationFailedException(e.getMessage());
            }
        }
        return new FilePath(node.getChannel(), expectedPath.getRemote());
    }

    private void performCleanup(FilePath preferedLocation) throws IOException, InterruptedException {
        preferedLocation.deleteContents();
    }
    private void getFreshCopyOfExecutables(MatlabInstallable installable, FilePath expectedPath) throws IOException, InterruptedException {
        FilePath mpmPath = installable.getMpmInstallable(expectedPath);
        FilePath mbatchPath = installable.getBatchInstallable(expectedPath);
        mpmPath.copyFrom(new URL(installable.url));
        mpmPath.chmod(0777);
        mbatchPath.copyFrom(new URL(installable.batchURL));
        mbatchPath.chmod(0777);
    }

  private String getNodeSpecificMPMExecutor(Node node) {
      final String osName;
      if (node.toComputer().isUnix()) {
          osName = "/mpm";
      } else {
          osName = "\\mpm.exe";
      }
      return osName;
  }

  private String getMatlabProducts(){
        if(!this.getProducts().isEmpty()){
            return "--products=MATLAB "+ this.getProducts().trim();
        } else {
            return "--products=MATLAB";
        }
  }

    private Installable getInstallable(Node node) throws IOException, InterruptedException {
        // Get the GMATLAB release version that we want to install
        String release = this.getVersion();//this.id;
        if (release == null) {
            return null;
        }

        // Gather properties for the node to install on
        String[] properties = node.getChannel().call(new GetSystemProperties("os.name", "os.arch", "os.version"));

        // Get the best matching install candidate for this node
        return getInstallCandidate(properties[0]);
    }

  public MatlabInstallable getInstallCandidate(String osName) throws InstallationFailedException {
    String platform = getPlatform(osName);

    return new MatlabInstallable(platform);
  }

    private String getPlatform(String os) throws InstallationFailedException {
        String value = os.toLowerCase(Locale.ENGLISH);
        if (value.contains("linux")) {
            return "glnxa64";
        }
        if (value.contains("os x")) {
            return "maci64";
        }
        if (value.contains("windows")) {
            return "win64";
        }
        throw new InstallationFailedException("Unsupported OS");
    }

    @Extension
    public static final class DescriptorImpl extends ToolInstallerDescriptor<MatlabInstaller> {
        public String getDisplayName() {
            return Message.getValue("matlab.tools.auto.install.display.name");
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType == MatlabInstallation.class;
        }
    }

    private class GetSystemProperties extends MasterToSlaveCallable<String[], InterruptedException> {
        private static final long serialVersionUID = 1L;

        private final String[] properties;

        GetSystemProperties(String... properties) {
            this.properties = properties;
        }

        public String[] call() {
            String[] values = new String[properties.length];
            for (int i = 0; i < properties.length; i++) {
                values[i] = System.getProperty(properties[i]);
            }
            return values;
        }
    }
}