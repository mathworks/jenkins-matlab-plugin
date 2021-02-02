package com.mathworks.ci;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

public class TestFolders extends AbstractDescribableImpl<TestFolders> {

    private String testFolders;

    @DataBoundConstructor
    public TestFolders(String testFolders) {
        this.testFolders = testFolders;
    }

    public String getTestFolders() {
        return this.testFolders;
    }

    @Extension public static final class DescriptorImpl extends Descriptor<TestFolders> {}
}
