package com.mathworks.ci.tools;

import com.mathworks.ci.MatlabInstallation;
import com.mathworks.ci.Message;
import com.mathworks.ci.utilities.GetSystemProperties;

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

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
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

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        FilePath expectedPath = preferredLocation(tool, node);
        MatlabInstallable installable;
        try {
            installable = (MatlabInstallable) getInstallable(node);
        } catch (Exception e) {
            throw new InstallationFailedException(e.getMessage());
        }

        if (installable == null) {
            log.getLogger().println(Message.getValue("matlab.tools.auto.install.matlab.installable.error"));
            return expectedPath;
        }

        getFreshCopyOfExecutables(installable, expectedPath);

        int result = installUsingMpm(node,expectedPath,log);
        if(result == 0) {
            log.getLogger().println("MATLAB installation for version " + this.getRelease() +" using mpm is completed successfully !");
        }
        return expectedPath;
    }

  private int installUsingMpm(Node node, FilePath expectedPath, TaskListener log)
      throws IOException, InterruptedException {

        // Create installation process
        EnvVars env = node.toComputer().getEnvironment();
        Launcher matlabInstaller = node.createLauncher(log);
        ProcStarter installerProc = matlabInstaller.launch();

        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(expectedPath.getRemote() + getNodeSpecificMPMExecutor(node));
        args.add("install");
        args.add("--release=" + this.getRelease());
        args.add("--destination="+ expectedPath.getRemote());
        addMatlabProductsToArgs(args);
        installerProc.pwd(expectedPath)
                .cmds(args)
                .envs(env).stdout(log);
        int result;
        try {
            result = installerProc.join();
        } catch (Exception e) {
            throw new InstallationFailedException(e.getMessage());
        }
        return result;
    }

    private String getRelease() throws IOException {
        String trimmedRelease  = this.getVersion().trim();
        if (trimmedRelease.equalsIgnoreCase("latest") || trimmedRelease.equalsIgnoreCase("latest-including-prerelease")) {
            String getReleaseVersion = Message.getValue("matlab.release.info.url") + trimmedRelease;
            String releaseVersion = IOUtils.toString(new URL(getReleaseVersion), Charset.defaultCharset());
            return releaseVersion.trim();
        }
        return trimmedRelease;
    }

    private void getFreshCopyOfExecutables(MatlabInstallable installable, FilePath expectedPath) throws IOException, InterruptedException {
        FilePath mpmPath = installable.getMpmInstallable(expectedPath);
        FilePath mbatchPath = installable.getBatchInstallable(expectedPath);
        mpmPath.copyFrom(new URL(installable.url).openStream());
        mpmPath.chmod(0777);
        mbatchPath.copyFrom(new URL(installable.batchURL).openStream());
        mbatchPath.chmod(0777);
    }

    private String getNodeSpecificMPMExecutor(Node node) {
        final String osName;
        if(node.toComputer().isUnix()){
            osName = "/mpm";
        } else {
            osName = "\\mpm.exe";
        }
        return osName;
    }

   private void addMatlabProductsToArgs(ArgumentListBuilder args) {
        args.add("--products");
        if(!this.getProducts().isEmpty()){
            args.add("MATLAB");
            String[] productList = this.getProducts().split(" ");
            for(String prod:productList){
                args.add(prod);
            }
        } else {
            args.add("MATLAB");
        }
   }

    public Installable getInstallable(Node node) throws IOException, InterruptedException {
        // Get appropriate installable version for MATLAB.
        String release = this.getVersion();
        if (release == null) {
            return null;
        }

        // Gather properties for the node to install on
        String[] properties = node.getChannel().call(new GetSystemProperties("os.name", "os.arch", "os.version"));
        return getInstallCandidate(properties[0]);
    }

  public MatlabInstallable getInstallCandidate(String osName) throws InstallationFailedException {
        String platform = getPlatform(osName);
        return new MatlabInstallable(platform);
  }

    public String getPlatform(String os) throws InstallationFailedException {
        String value = os.toLowerCase(Locale.ENGLISH);
        if (value.contains("linux")) {
            return "glnxa64";
        } else if (value.contains("os x")){
            return "maci64";
        } else if (value.contains("windows")){
            return "win64";
        } else {
            throw new InstallationFailedException("Unsupported OS");
        }
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
}