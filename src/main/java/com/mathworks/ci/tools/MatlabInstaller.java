package com.mathworks.ci.tools;
/**
 * Copyright 2024, The MathWorks, Inc.
 *
 */


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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

import java.util.Set;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class MatlabInstaller extends DownloadFromUrlInstaller {

    private String version;
    private String products;
    private static String DEFAULT_PRODUCT = "MATLAB";

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
        FilePath supportingExecutable = preferredLocation (tool, node);
        String[] systemProperties = getSystemProperties(node);
        FilePath expectedPath;
        if(systemProperties[0].toLowerCase ().contains ("os x")) {
            expectedPath = new FilePath (supportingExecutable, this.getVersion ()+".app");
        } else {
            expectedPath = new FilePath (supportingExecutable, this.getVersion ());
        }
        MatlabInstallable installable;
        try {
            installable = (MatlabInstallable) getInstallable (systemProperties);
        } catch (Exception e) {
            throw new InstallationFailedException (e.getMessage ());
        }

        getFreshCopyOfExecutables (installable, supportingExecutable);
        makeDir (expectedPath);

        int result = installUsingMpm (node, expectedPath, log);
            if (result == 0) {
                log.getLogger ().println (
                    "MATLAB installation of version " + this.getVersion ()
                        + " using mpm completed successfully!");
            }
        return expectedPath;
    }

    private int installUsingMpm (Node node, FilePath destination, TaskListener log)
        throws IOException, InterruptedException {

        Launcher matlabInstaller = node.createLauncher (log);
        ProcStarter installerProc = matlabInstaller.launch ();

        ArgumentListBuilder args = new ArgumentListBuilder ();
        args.add (destination.getParent ().getRemote () + getNodeSpecificMPMExecutor (node));
        args.add ("install");
        appendReleaseToArguments (args, log);
        args.add ("--destination=" + destination.getRemote ());
        addMatlabProductsToArgs (args);
        installerProc.pwd (destination).cmds (args).stdout (log);
        int result;
        try {
            result = installerProc.join ();
        } catch (Exception e) {
            log.getLogger ().println ("MATLAB installation failed " + e.getMessage ());
            throw new InstallationFailedException (e.getMessage ());
        }
        return result;
    }

    private void makeDir(FilePath path) throws IOException, InterruptedException {
        if(!path.exists ()){
            path.mkdirs ();
            path.chmod (0777);
        }
    }

    private void appendReleaseToArguments (ArgumentListBuilder args, TaskListener log) {
        String trimmedRelease = this.getVersion ().trim ();
        String actualRelease = trimmedRelease;

        if (trimmedRelease.equalsIgnoreCase ("latest") || trimmedRelease.equalsIgnoreCase (
            "latest-including-prerelease")) {
            String releaseInfoUrl =
                Message.getValue ("matlab.release.info.url") + trimmedRelease;
            String releaseVersion = null;
            try {
                releaseVersion = IOUtils.toString (new URL (releaseInfoUrl),
                    StandardCharsets.UTF_8).trim ();
            } catch (IOException e) {
                log.getLogger ().println ("Failed to fetch release version: " + e.getMessage ());
            }

            if (releaseVersion != null && releaseVersion.contains ("prerelease")) {
                actualRelease = releaseVersion.replace ("prerelease", "");
            } else {
                actualRelease = releaseVersion;
            }
        }
        args.add ("--release=" + actualRelease);
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

    private void addMatlabProductsToArgs (ArgumentListBuilder args)
        throws IOException, InterruptedException {
        args.add ("--products");
        if (this.getProducts ().isEmpty ()) {
            args.add (DEFAULT_PRODUCT);

        } else {
            if (!this.getProducts ().contains (DEFAULT_PRODUCT)) {
                args.add (DEFAULT_PRODUCT);
            }
            String[] productList = this.getProducts ().split (" ");
            for (String prod : productList) {
                args.add (prod);
            }
        }
    }

    public Installable getInstallable (String[] systemProperties) throws IOException {
        // Gather properties for the node to install on
        return getInstallCandidate (systemProperties[0], systemProperties[1]);
    }

    public MatlabInstallable getInstallCandidate (String osName, String architecture)
        throws InstallationFailedException {
        String platform = getPlatform (osName, architecture);
        return new MatlabInstallable (platform);
    }

    public String getPlatform (String os, String architecture) throws InstallationFailedException {
        String value = os.toLowerCase (Locale.ENGLISH);
        if (value.contains ("linux")) {
            return "glnxa64";
        } else if (value.contains ("os x")) {
            if (architecture.equalsIgnoreCase ("aarch64") || architecture.equalsIgnoreCase (
                "arm64")) {
                return "maca64";
            } else {
                return "maci64";
            }
        } else {
            throw new InstallationFailedException ("Unsupported OS");
        }
    }

    @SuppressFBWarnings(value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"},
        justification =
            "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE: Its false positive scenario for sport bug which is fixed in later versions "
                + "https://github.com/spotbugs/spotbugs/issues/1843")
    private String[] getSystemProperties(Node node) throws IOException, InterruptedException {
        String[] properties = node.getChannel ()
            .call (new GetSystemProperties ("os.name", "os.arch", "os.version"));
        return properties;
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