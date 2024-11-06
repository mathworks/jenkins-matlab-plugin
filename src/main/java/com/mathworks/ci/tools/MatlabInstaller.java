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
import hudson.tools.ToolInstaller;
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

public class MatlabInstaller extends ToolInstaller {

    private String release;
    private String products;
    private static String DEFAULT_PRODUCT = "MATLAB";

    @DataBoundConstructor
    public MatlabInstaller(String id) {
        super(id);
    }

    public String getRelease() {
        return this.release;
    }

    @DataBoundSetter
    public void setVersion(String release) {
        this.release = release;
    }

    public String getProducts() {
        return this.products;
    }

    @DataBoundSetter
    public void setProducts(String products) {
        this.products = products;
    }

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log)
        throws IOException, InterruptedException {
        FilePath destination = preferredLocation(tool, node);
        String[] systemProperties = getSystemProperties(node);
        FilePath matlabRootPath;
        if(systemProperties[0].toLowerCase().contains("os x")) {
            matlabRootPath= new FilePath(destination, this.getRelease()+".app");
        } else {
            matlabRootPath = new FilePath(destination, this.getRelease());
        }
        String platform = getPlatform(systemProperties[0], systemProperties[1]);
        getFreshCopyOfExecutables(platform, destination);
        
        makeDir(matlabRootPath);
        int result  = installUsingMpm(node, this.getRelease (), matlabRootPath, this.getProducts (), log);
        if (result != 0) {
            throw new InstallationFailedException("Unable to install MATLAB using mpm.");
        }
        return matlabRootPath;
    }

    private int installUsingMpm(Node node, String release, FilePath destination, String products, TaskListener log)
        throws IOException, InterruptedException {

        Launcher matlabInstaller = node.createLauncher(log);
        ProcStarter installerProc = matlabInstaller.launch ();

        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(destination.getParent().getRemote() + getNodeSpecificMPMExecutor(node));
        args.add("install");
        appendReleaseToArguments(release,args, log);
        args.add("--destination=" + destination.getRemote());
        addMatlabProductsToArgs(args, products);
        installerProc.pwd(destination).cmds(args).stdout(log);
        int result;
        try {
            result = installerProc.join();
        } catch (Exception e) {
            log.getLogger().println("MATLAB installation failed " + e.getMessage());
            throw new InstallationFailedException(e.getMessage ());
        }
        return result;
    }


    private void makeDir(FilePath path) throws IOException, InterruptedException {
        if(!path.exists()){
            path.mkdirs();
            path.chmod(0777);
        }
    }

    private void appendReleaseToArguments(String release, ArgumentListBuilder args, TaskListener log) {
        String trimmedRelease = release.trim();
        String actualRelease = trimmedRelease;

        if (trimmedRelease.equalsIgnoreCase("latest") || trimmedRelease.equalsIgnoreCase(
            "latest-including-prerelease")) {
            String releaseInfoUrl =
                Message.getValue("matlab.release.info.url") + trimmedRelease;
            String releaseVersion = null;
            try {
                releaseVersion = IOUtils.toString(new URL(releaseInfoUrl),
                    StandardCharsets.UTF_8).trim();
            } catch (IOException e) {
                log.getLogger().println("Failed to fetch release version: " + e.getMessage());
            }

            if (releaseVersion != null && releaseVersion.contains("prerelease")) {
                actualRelease = releaseVersion.replace("prerelease", "");
                args.add ("--release-status=Prerelease");
            } else {
                actualRelease = releaseVersion;
            }
        }
        args.add("--release=" + actualRelease);
    }

    private void getFreshCopyOfExecutables(String platform, FilePath expectedPath)
        throws IOException, InterruptedException {
        FilePath matlabBatchPath = new FilePath(expectedPath, "matlab-batch");
        FilePath mpmPath = new FilePath(expectedPath, "mpm");

        URL mpmUrl;
        URL matlabBatchUrl;

        switch (platform) {
            case "glnxa64":
                mpmUrl = new URL(Message.getValue("tools.matlab.mpm.installer.linux"));
                matlabBatchUrl = new URL(Message.getValue("tools.matlab.batch.executable.linux"));
                break;
            case "maci64":
                mpmUrl = new URL(Message.getValue("tools.matlab.mpm.installer.maci64"));
                matlabBatchUrl = new URL(Message.getValue("tools.matlab.batch.executable.maci64"));
                break;
            case "maca64":
                mpmUrl = new URL(Message.getValue("tools.matlab.mpm.installer.maca64"));
                matlabBatchUrl = new URL(Message.getValue("tools.matlab.batch.executable.maca64"));
                break;
            default:
                throw new InstallationFailedException("Unsupported OS");
        }

        mpmPath.copyFrom(mpmUrl.openStream());
        mpmPath.chmod(0777);
        matlabBatchPath.copyFrom(matlabBatchUrl.openStream());
        matlabBatchPath.chmod(0777);
    }

    @SuppressFBWarnings(value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"},
        justification =
            "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE: Its false positive scenario for sport bug which is fixed in later versions "
                + "https://github.com/spotbugs/spotbugs/issues/1843")
    private String getNodeSpecificMPMExecutor(Node node) {
        if (!node.toComputer().isUnix()) {
            return "\\mpm.exe";
        }
        return "/mpm";
    }

    private void addMatlabProductsToArgs(ArgumentListBuilder args, String products)
        throws IOException, InterruptedException {
        args.add("--products");
        if (products.isEmpty()) {
            args.add(DEFAULT_PRODUCT);

        } else {
            if (!products.contains(DEFAULT_PRODUCT)) {
                args.add(DEFAULT_PRODUCT);
            }
            String[] productList = products.split(" ");
            for (String prod : productList) {
                args.add(prod);
            }
        }
    }

    public String getPlatform(String os, String architecture) throws InstallationFailedException {
        String value = os.toLowerCase(Locale.ENGLISH);
        if (value.contains("linux")) {
            return "glnxa64";
        } else if (value.contains("os x")) {
            if (architecture.equalsIgnoreCase("aarch64") || architecture.equalsIgnoreCase (
                "arm64")) {
                return "maca64";
            } else {
                return "maci64";
            }
        } else {
            throw new InstallationFailedException("Unsupported OS");
        }
    }

    @SuppressFBWarnings(value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"},
        justification =
            "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE: Its false positive scenario for sport bug which is fixed in later versions "
                + "https://github.com/spotbugs/spotbugs/issues/1843")
    private String[] getSystemProperties(Node node) throws IOException, InterruptedException {
        String[] properties = node.getChannel()
            .call (new GetSystemProperties("os.name", "os.arch", "os.version"));
        return properties;
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

        @POST
        public FormValidation doCheckRelease(@QueryParameter String value) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            if (value.isEmpty()) {
                return FormValidation.error(Message.getValue("tools.matlab.empty.release.error"));
            }
            return FormValidation.ok();
        }
    }
}
