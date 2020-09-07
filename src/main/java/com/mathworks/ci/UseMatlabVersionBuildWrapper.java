package com.mathworks.ci;

/**
 * Copyright 2019-2020 The MathWorks, Inc.
 * 
 * This class is BuildWrapper which accepts the "matlabroot" from user and updates the PATH varible with it.
 * which could be later used across build.
 * 
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildWrapper;

public class UseMatlabVersionBuildWrapper extends SimpleBuildWrapper {

    
	private String matlabRootFolder;
    private EnvVars env;

    @DataBoundConstructor
    public UseMatlabVersionBuildWrapper() {}

    public String getMatlabRootFolder() {
        return this.matlabRootFolder;
    }

    @DataBoundSetter
    public void setMatlabRootFolder(String matlabRootFolder) {
        this.matlabRootFolder = matlabRootFolder;
    }

    private String getLocalMatlab() {
        return this.env == null ? getMatlabRootFolder() : this.env.expand(getMatlabRootFolder());
    }

    private void setEnv(EnvVars env) {
        this.env = env;
    }

    
    @Extension
    public static final class UseMatlabVersionDescriptor extends BuildWrapperDescriptor {

        MatlabReleaseInfo rel;
        String matlabRootFolder;

        public String getMatlabRootFolder() {
            return matlabRootFolder;
        }

        public void setMatlabRootFolder(String matlabRootFolder) {
            this.matlabRootFolder = matlabRootFolder;
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Message.getValue("Buildwrapper.display.name");
        }


        /*
         * Below methods with 'doCheck' prefix gets called by jenkins when this builder is loaded.
         * these methods are used to perform basic validation on UI elements associated with this
         * descriptor class.
         */


        public FormValidation doCheckMatlabRootFolder(@QueryParameter String matlabRootFolder) {
            setMatlabRootFolder(matlabRootFolder);
            List<Function<String, FormValidation>> listOfCheckMethods =
                    new ArrayList<Function<String, FormValidation>>();
            listOfCheckMethods.add(chkMatlabEmpty);
            listOfCheckMethods.add(chkMatlabSupportsRunTests);

            return FormValidationUtil.getFirstErrorOrWarning(listOfCheckMethods,matlabRootFolder);
        }

        Function<String, FormValidation> chkMatlabEmpty = (String matlabRootFolder) -> {
            if (matlabRootFolder.isEmpty()) {
                return FormValidation.error(Message.getValue("Builder.matlab.root.empty.error"));
            }
            return FormValidation.ok();
        };

        Function<String, FormValidation> chkMatlabSupportsRunTests = (String matlabRootFolder) -> {
            final MatrixPatternResolver resolver = new MatrixPatternResolver(matlabRootFolder);
            if (!resolver.hasVariablePattern()) {
                try {
                    FilePath matlabRootPath = new FilePath(new File(matlabRootFolder));
                    rel = new MatlabReleaseInfo(matlabRootPath);
                    if (rel.verLessThan(
                            MatlabBuilderConstants.BASE_MATLAB_VERSION_RUNTESTS_SUPPORT)) {
                        return FormValidation
                                .error(Message.getValue("Builder.matlab.test.support.error"));
                    }
                } catch (MatlabVersionNotFoundException e) {
                    return FormValidation
                            .warning(Message.getValue("Builder.invalid.matlab.root.warning"));
                }
            }
            return FormValidation.ok();
        };
    }

    @Override
    public synchronized void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher,
            TaskListener listener, EnvVars initialEnvironment)
            throws IOException, InterruptedException {
        // Set Environment variable

        setEnv(initialEnvironment);
        
        FilePath matlabExecutablePath = new FilePath(launcher.getChannel(),
                getLocalMatlab() + "/bin/" + getNodeSpecificExecutable(launcher));

        if (!matlabExecutablePath.exists()) {
            throw new MatlabNotFoundError(Message.getValue("matlab.not.found.error"));
        }
        // Add "matlabroot" without bin as env variable which will be available across the build.
        context.env("matlabroot", getLocalMatlab());
        // Add matlab bin to path to invoke MATLAB directly on command line.
        context.env("PATH+matlabroot", getLocalMatlab() + "/bin");     
    }

    private String getNodeSpecificExecutable(Launcher launcher) {
        return (launcher.isUnix()) ? "matlab" : "matlab.exe";
    }
}
