
package com.mathworks.ci.freestyle.options;

/**
 * Copyright 2024 The MathWorks, Inc.
 *
 * Describable class for Build Options.
 */

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class BuildOptions extends AbstractDescribableImpl<BuildOptions> {

    private String options;

    @DataBoundConstructor
    public BuildOptions(String options) {
        this.options = Util.fixNull(options);
    }

    public String getOptions() {
        return this.options;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<BuildOptions> {
    }
}
