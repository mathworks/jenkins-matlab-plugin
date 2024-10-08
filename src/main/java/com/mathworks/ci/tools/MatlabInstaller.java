package com.mathworks.ci.tools;

import com.mathworks.ci.MatlabInstallation;
import com.mathworks.ci.Message;
import com.mathworks.ci.utilities.GetSystemProperties;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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

import hudson.util.FormValidation;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class MatlabInstaller extends DownloadFromUrlInstaller {

    private String version;
    private String products;

    @DataBoundConstructor
    public MatlabInstaller (String id) {
        super (id);
    }

    public String getVersion () {
        return this.version;
    }

    @DataBoundSetter
    public void setVersion (String version) {
        this.version = version;
    }

    public String getProducts () {
        return this.products;
    }

    @DataBoundSetter
    public void setProducts (String products) {
        this.products = products;
    }

    @Override
    public FilePath performInstallation (ToolInstallation tool, Node node, TaskListener log)
        throws IOException, InterruptedException {
        FilePath expectedPath = preferredLocation (tool, node);
        MatlabInstallable installable;
        try {
            installable = (MatlabInstallable) getInstallable (node);
        } catch (Exception e) {
            throw new InstallationFailedException (e.getMessage ());
        }
        getFreshCopyOfExecutables (installable, expectedPath);

        int result = installUsingMpm (node, expectedPath, log);
        if (result == 0) {
            log.getLogger ().println (
                "MATLAB installation for version " + this.getVersion ()
                    + " using mpm is completed successfully !");
        }
        return expectedPath;
    }

    private int installUsingMpm (Node node, FilePath expectedPath, TaskListener log)
        throws IOException {

        Launcher matlabInstaller = node.createLauncher (log);
        ProcStarter installerProc = matlabInstaller.launch ();

        ArgumentListBuilder args = new ArgumentListBuilder ();
        args.add (expectedPath.getRemote () + getNodeSpecificMPMExecutor (node));
        args.add ("install");
        appendReleaseToArguments (args, log);
        args.add ("--destination=" + expectedPath.getRemote ());
        addMatlabProductsToArgs (args);
        installerProc.pwd (expectedPath).cmds (args).stdout (log);
        int result;
        try {
            result = installerProc.join ();
        } catch (Exception e) {
            log.getLogger ().println ("MATLAB installation failed" + e.getMessage ());
            throw new InstallationFailedException (e.getMessage ());
        }
        return result;
    }

    private void appendReleaseToArguments (ArgumentListBuilder args, TaskListener log) {
        try {
            String trimmedRelease = this.getVersion ().trim ();
            String actualRelease = trimmedRelease;

            if (trimmedRelease.equalsIgnoreCase ("latest") || trimmedRelease.equalsIgnoreCase (
                "latest-including-prerelease")) {
                String releaseInfoUrl =
                    Message.getValue ("matlab.release.info.url") + trimmedRelease;
                String releaseVersion = IOUtils.toString (new URL (releaseInfoUrl),
                    StandardCharsets.UTF_8).trim ();

                if (releaseVersion.contains ("prerelease")) {
                    actualRelease = releaseVersion.replace ("prerelease", "");
                    args.add ("--release-status=Prerelease");
                } else {
                    actualRelease = releaseVersion;
                }
            }
            args.add ("--release=" + actualRelease);
        } catch (IOException e) {
            log.getLogger().println("Failed to fetch release version: " + e.getMessage ());
        }
    }

    private void getFreshCopyOfExecutables (MatlabInstallable installable, FilePath expectedPath)
        throws IOException, InterruptedException {
        FilePath mpmPath = installable.getMpmInstallable (expectedPath);
        FilePath mbatchPath = installable.getBatchInstallable (expectedPath);
        mpmPath.copyFrom (new URL (installable.url).openStream ());
        mpmPath.chmod (0777);
        mbatchPath.copyFrom (new URL (installable.batchURL).openStream ());
        mbatchPath.chmod (0777);
    }

    @SuppressFBWarnings(value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"},
        justification =
            "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE: Its false positive scenario for sport bug which is fixed in later versions "
                + "https://github.com/spotbugs/spotbugs/issues/1843")
    private String getNodeSpecificMPMExecutor (Node node) {
        if (!node.toComputer ().isUnix ()) {
            return "\\mpm.exe";
        }
        return "/mpm";
    }

    private void addMatlabProductsToArgs (ArgumentListBuilder args) {
        args.add ("--products");
        if (!this.getProducts ().isEmpty ()) {
            args.add ("MATLAB");
            String[] productList = this.getProducts ().split (" ");
            for (String prod : productList) {
                args.add (prod);
            }
        } else {
            args.add ("MATLAB");
        }
    }

    @SuppressFBWarnings(value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"},
        justification =
            "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE: Its false positive scenario for sport bug which is fixed in later versions "
                + "https://github.com/spotbugs/spotbugs/issues/1843")
    public Installable getInstallable (Node node) throws IOException, InterruptedException {
        // Gather properties for the node to install on
        String[] properties = node.getChannel ()
            .call (new GetSystemProperties ("os.name", "os.arch", "os.version"));
        return getInstallCandidate (properties[0]);
    }

    public MatlabInstallable getInstallCandidate (String osName)
        throws InstallationFailedException {
        String platform = getPlatform (osName);
        return new MatlabInstallable (platform);
    }

    public String getPlatform (String os) throws InstallationFailedException {
        String value = os.toLowerCase (Locale.ENGLISH);
        if (value.contains ("linux")) {
            return "glnxa64";
        } else if (value.contains ("os x")) {
            return "maci64";
        } else if (value.contains ("windows")) {
            return "win64";
        } else {
            throw new InstallationFailedException ("Unsupported OS");
        }
    }

    @Extension
    public static final class DescriptorImpl extends ToolInstallerDescriptor<MatlabInstaller> {

        public String getDisplayName () {
            return Message.getValue ("matlab.tools.auto.install.display.name");
        }

        @Override
        public boolean isApplicable (Class<? extends ToolInstallation> toolType) {
            return toolType == MatlabInstallation.class;
        }

        @POST
        public FormValidation doCheckVersion (@QueryParameter String value) {
            Jenkins.get ().checkPermission (Jenkins.ADMINISTER);
            if (value.isEmpty ()) {
                return FormValidation.error (Message.getValue ("tools.matlab.empty.version.error"));
            }
            return FormValidation.ok ();
        }
    }
}