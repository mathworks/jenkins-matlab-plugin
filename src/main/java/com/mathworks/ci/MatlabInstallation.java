package com.mathworks.ci;

/**
 * Copyright 2020-2024 The MathWorks, Inc.
 *
 * Describable class for adding MATLAB installations in Jenkins Global Tool configuration.
 */

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class MatlabInstallation extends ToolInstallation
        implements EnvironmentSpecific<MatlabInstallation>, NodeSpecific<MatlabInstallation> {
    private static final long serialVersionUID = 1L;

    @DataBoundConstructor
    public MatlabInstallation(String name, @CheckForNull String home, List<? extends ToolProperty<?>> properties) {
        super(Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim(home), properties);
    }

    /*
     * Constructor for Custom object
     */

    public MatlabInstallation(String name) {
        super(name, null, null);
    }

    @Override
    public MatlabInstallation forEnvironment(EnvVars envVars) {
        return new MatlabInstallation(getName(), envVars.expand(getHome()), getProperties().toList());
    }

    @Override
    public MatlabInstallation forNode(@Nonnull Node node, TaskListener log) throws IOException, InterruptedException {
        return new MatlabInstallation(getName(), translateFor(node, log), getProperties().toList());
    }

    @SuppressFBWarnings(value = {
            "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE" }, justification = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE: Its false positive scenario for sport bug which is fixed in later versions "
                    + "https://github.com/spotbugs/spotbugs/issues/1843")
    @Override
    public void buildEnvVars(EnvVars env) {
        String pathToExecutable = getHome() + "/bin";
        env.put("PATH+matlabroot", pathToExecutable);
        Jenkins jenkinsInstance = Jenkins.getInstanceOrNull();
        if (jenkinsInstance != null) {
            if (jenkinsInstance.getChannel() != null) {
                FilePath batchExecutablePath = new FilePath(jenkinsInstance.getChannel(), getHome());
                if (batchExecutablePath.getParent() != null) {
                    env.put("PATH+matlab_batch", batchExecutablePath.getParent().getRemote());
                }
            }
        }
    }

    public static MatlabInstallation[] getAll() {
        return Jenkins.get().getDescriptorByType(DescriptorImpl.class).getInstallations();
    }

    public static boolean isEmpty() {
        return getAll().length == 0;
    }

    public static MatlabInstallation getInstallation(String name) {
        for (MatlabInstallation inst : getAll()) {
            if (name.equals(inst.getName())) {
                return inst;
            }
        }
        return null;
    }

    @Extension
    @Symbol("matlab")
    public static class DescriptorImpl extends ToolDescriptor<MatlabInstallation> {
        @CopyOnWrite
        private volatile MatlabInstallation[] installations = new MatlabInstallation[0];

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "MATLAB";
        }

        @Override
        public MatlabInstallation[] getInstallations() {
            return Arrays.copyOf(installations, installations.length);
        }

        @Override
        public MatlabInstallation newInstance(StaplerRequest req, JSONObject formData) {
            return (MatlabInstallation) req.bindJSON(clazz, formData);
        }

        public void setInstallations(MatlabInstallation... matlabInstallations) {
            this.installations = matlabInstallations;
            save();
        }
    }
}
