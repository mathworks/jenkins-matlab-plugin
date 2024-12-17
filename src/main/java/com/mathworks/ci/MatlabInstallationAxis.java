package com.mathworks.ci;

/**
 * Copyright 2020-2024 The MathWorks, Inc.
 *
 * Describable class for MATLAB Axis that provides a list of configured MATLAB installation for
 * generating matrix configurations.
 */

import hudson.Extension;
import hudson.matrix.Axis;
import hudson.matrix.AxisDescriptor;
import hudson.matrix.MatrixProject;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MatlabInstallationAxis extends Axis {

    @DataBoundConstructor
    public MatlabInstallationAxis(List<String> values) {
        super(Message.getValue("Axis.matlab.key"), evaluateValues(values));
    }

    static private List<String> evaluateValues(List<String> values) {
        // Add default configuration is values are null or not selected.
        if (values == null || values.isEmpty()) {
            values = new ArrayList<>(Arrays.asList("default"));
        }
        return values;
    }

    @Extension
    public static class DescriptorImpl extends AxisDescriptor {

        @Override
        public String getDisplayName() {
            return Message.getValue("Axis.matlab.key");
        }

        @Override
        public boolean isInstantiable() {
            return !isMatlabInstallationEmpty();
        }

        public boolean checkUseMatlabVersion(Object it) {
            return MatlabItemListener.getMatlabBuildWrapperCheckForPrj(((MatrixProject) it).getFullName())
                    && !isMatlabInstallationEmpty();
        }

        public MatlabInstallation[] getInstallations() {
            return MatlabInstallation.getAll();
        }

        public String getUseMatlabWarning() {
            return Message.getValue("Axis.use.matlab.warning");
        }

        public boolean isMatlabInstallationEmpty() {
            return MatlabInstallation.isEmpty();
        }

        public String getNoInstallationError() {
            return Message.getValue("Axis.no.installed.matlab.error");
        }
    }
}
