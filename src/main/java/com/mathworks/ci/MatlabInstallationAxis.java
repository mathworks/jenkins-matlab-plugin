package com.mathworks.ci;

import hudson.Extension;
import hudson.matrix.Axis;
import hudson.matrix.AxisDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;

public class MatlabInstallationAxis extends Axis {

    @DataBoundConstructor
    public MatlabInstallationAxis(List<String> values) {
        super(Message.getValue("Axis.matlab.key"), values);
    }

    @Extension
    public static class DescriptorImpl extends AxisDescriptor {
        @Override
        public String getDisplayName() {
            return Message.getValue("Axis.matlab.key");
        }

        @Override
        public boolean isInstantiable() {
            return !MatlabInstallation.isEmpty();
        }

        public MatlabInstallation[] getInstallations () {
            return MatlabInstallation.getAll();
        }
    }
}
