package com.mathworks.ci.tools;

/**
 * Copyright 2024, The MathWorks, Inc.
 */

import com.mathworks.ci.MatlabInstallation;
import com.mathworks.ci.Message;
import com.mathworks.ci.utilities.GetSystemProperties;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;

import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstallerDescriptor;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import java.nio.charset.StandardCharsets;

import java.util.Locale;

import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class MatlabInstaller extends ToolInstaller {

    private String release;
    private String products;
    private static String DEFAULT_PRODUCT = "MATLAB";

    @DataBoundConstructor
    public MatlabInstaller(String id) {
        super(id);
    }

    public String getRelease() {
        return this.release.trim();
    }

    @DataBoundSetter
    public void setRelease(String release) {
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
        FilePath toolRoot = preferredLocation(tool, node);
        makeDir(toolRoot);

        String extension = "";
        String[] systemProperties = getSystemProperties(node);
        FilePath matlabRoot;
        if (systemProperties[0].toLowerCase().contains("os x")) {
            matlabRoot = new FilePath(toolRoot, this.getRelease() + ".app");
        } else {
            matlabRoot = new FilePath(toolRoot, this.getRelease());
        }
        String platform = getPlatform(systemProperties[0], systemProperties[1]);
        if (platform == "win64") {
            extension = ".exe";
        }

        // Create temp directory
        FilePath tempDir = toolRoot.createTempDir("", "");

        // Download mpm and matlab-batch to temp directory
        FilePath mpm = fetchMpm(platform, tempDir);
        FilePath matlabBatch = fetchMatlabBatch(platform, tempDir);

        // Install with mpm
        mpmInstall(mpm, this.getRelease(), this.getProducts(), matlabRoot, node, log);

        // Copy downloaded matlab-batch to tool directory, 
        matlabBatch.copyTo(new FilePath(toolRoot, "matlab-batch"+extension));

        // Delete temp directory
        tempDir.deleteRecursive();

        return matlabRoot;
    }

    private void mpmInstall(FilePath mpmPath, String release, String products, FilePath destination, Node node, TaskListener log)
            throws IOException, InterruptedException {
        makeDir(destination);
        Launcher matlabInstaller = node.createLauncher(log);
        ProcStarter installerProc = matlabInstaller.launch();

        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(mpmPath.getRemote());
        args.add("install");
        appendReleaseToArguments(release, args, log);
        args.add("--destination=" + destination.getRemote());
        addMatlabProductsToArgs(args, products);

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        installerProc.pwd(destination).cmds(args).stdout(err);

        int result;
        try {
            result = installerProc.join();
        } catch (Exception e) {
            throw new InstallationFailedException(e.getMessage());
        }
        if (result != 0) {
            String errString = err.toString(StandardCharsets.UTF_8);
            if (errString.contains("already installed")) {
                log.getLogger().println(errString);
            } else {
                throw new InstallationFailedException(errString);
            }
        }
    }

    private void makeDir(FilePath path) throws IOException, InterruptedException {
        if (!path.exists()) {
            path.mkdirs();
            path.chmod(0777);
        }
    }

    private void appendReleaseToArguments(String release, ArgumentListBuilder args, TaskListener log) {
        String trimmedRelease = release.trim();
        String actualRelease = trimmedRelease;

        if (trimmedRelease.equalsIgnoreCase("latest") || trimmedRelease.equalsIgnoreCase(
                "latest-including-prerelease")) {
            String releaseInfoUrl = Message.getValue("matlab.release.info.url") + trimmedRelease;
            String releaseVersion = null;
            try {
                releaseVersion = IOUtils.toString(new URL(releaseInfoUrl),
                        StandardCharsets.UTF_8).trim();
            } catch (IOException e) {
                log.getLogger().println("Failed to fetch release version: " + e.getMessage());
            }

            if (releaseVersion != null && releaseVersion.contains("prerelease")) {
                actualRelease = releaseVersion.replace("prerelease", "");
                args.add("--release-status=Prerelease");
            } else {
                actualRelease = releaseVersion;
            }
        }
        args.add("--release=" + actualRelease);
    }

    private FilePath fetchMpm(String platform, FilePath destination)
            throws IOException, InterruptedException {
        URL mpmUrl;
        String extension = "";

        switch (platform) {
            case "glnxa64":
                mpmUrl = new URL(Message.getValue("tools.matlab.mpm.installer.linux"));
                break;
            case "maci64":
                mpmUrl = new URL(Message.getValue("tools.matlab.mpm.installer.maci64"));
                break;
            case "maca64":
                mpmUrl = new URL(Message.getValue("tools.matlab.mpm.installer.maca64"));
                break;
            default:
                throw new InstallationFailedException("Unsupported OS");
        }

        // Download mpm
        FilePath mpmPath = new FilePath(destination, "mpm" + extension);
        try {
            mpmPath.copyFrom(mpmUrl.openStream());
            mpmPath.chmod(0777);
        } catch (IOException | InterruptedException e) {
            throw new InstallationFailedException("Unable to setup mpm.");
        }

        return mpmPath;
    }

    private FilePath fetchMatlabBatch(String platform, FilePath destination)
            throws IOException, InterruptedException {
        URL matlabBatchUrl;
        String extension = "";

        switch (platform) {
            case "glnxa64":
                matlabBatchUrl = new URL(Message.getValue("tools.matlab.batch.executable.linux"));
                break;
            case "maci64":
                matlabBatchUrl = new URL(Message.getValue("tools.matlab.batch.executable.maci64"));
                break;
            case "maca64":
                matlabBatchUrl = new URL(Message.getValue("tools.matlab.batch.executable.maca64"));
                break;
            default:
                throw new InstallationFailedException("Unsupported OS");
        }

        // Download matlab-batch
        FilePath matlabBatchPath = new FilePath(destination, "matlab-batch" + extension);
        try {
            matlabBatchPath.copyFrom(matlabBatchUrl.openStream());
            matlabBatchPath.chmod(0777);
        } catch (IOException | InterruptedException e) {
            throw new InstallationFailedException("Unable to setup matlab-batch.");
        }

        return matlabBatchPath;
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
            if (architecture.equalsIgnoreCase("aarch64") || architecture.equalsIgnoreCase(
                    "arm64")) {
                return "maca64";
            } else {
                return "maci64";
            }
        } else {
            throw new InstallationFailedException("Unsupported OS");
        }
    }

    private String[] getSystemProperties(Node node) throws IOException, InterruptedException {
        VirtualChannel channel = node.getChannel();
        if (channel == null) {
            throw new InstallationFailedException("Unable to connect to Node");
        }
        String[] properties = channel
                .call(new GetSystemProperties("os.name", "os.arch", "os.version"));
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

        public FormValidation doCheckRelease(@QueryParameter String value) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            if (value.isEmpty()) {
                return FormValidation.error(Message.getValue("tools.matlab.empty.release.error"));
            }
            return FormValidation.ok();
        }
    }
}
