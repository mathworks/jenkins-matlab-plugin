package com.mathworks.ci;

import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class MatlabInstallation extends ToolInstallation implements EnvironmentSpecific<MatlabInstallation>, NodeSpecific<MatlabInstallation> {
    private static final long serialVersionUID = 1L;

    @DataBoundConstructor
    public MatlabInstallation(String name, @CheckForNull String home, List<? extends ToolProperty<?>> properties) {
        super(Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim(home), properties);
    }

    @Override public MatlabInstallation forEnvironment(EnvVars envVars) {
        return new MatlabInstallation(getName(), envVars.expand(getHome()), getProperties().toList());
    }

    @Override
    public MatlabInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new MatlabInstallation(getName(), translateFor(node, log), getProperties().toList());
    }

    @Override
    public void buildEnvVars(EnvVars env) {
        String pathToExecutable = getHome() + "/bin";
        env.put("PATH+matlabroot", pathToExecutable);
    }

    @Extension @Symbol("matlab")
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
