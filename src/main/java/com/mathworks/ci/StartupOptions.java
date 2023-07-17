
package com.mathworks.ci;

/**
 * Copyright 2023 The MathWorks, Inc.
 *
 * Describable class for Startup Options.
 *
 */

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class StartupOptions extends AbstractDescribableImpl<StartupOptions> {

    private String startupOptions;

    @DataBoundConstructor
    public StartupOptions(String startupOptions) {
        this.startupOptions = Util.fixNull(startupOptions);
    }

    public String getStartupOptions() {
        return this.startupOptions;
    }

    @Extension public static class DescriptorImpl extends Descriptor<StartupOptions> {}
}
