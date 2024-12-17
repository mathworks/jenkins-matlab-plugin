package com.mathworks.ci.freestyle;

/**
 * Copyright 2022-2024 The MathWorks, Inc.
 */

import java.io.IOException;
import javax.annotation.Nonnull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.init.Initializer;
import hudson.init.InitMilestone;
import hudson.model.Items;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

import com.mathworks.ci.Message;
import com.mathworks.ci.actions.MatlabActionFactory;
import com.mathworks.ci.actions.RunMatlabBuildAction;
import com.mathworks.ci.parameters.BuildActionParameters;
import com.mathworks.ci.freestyle.options.*;

public class RunMatlabBuildBuilder extends Builder implements SimpleBuildStep {
    // Deprecated
    private transient int buildResult;

    // In use
    private String tasks;
    private StartupOptions startupOptions;
    private BuildOptions buildOptions;

    private MatlabActionFactory factory;

    public RunMatlabBuildBuilder(MatlabActionFactory factory) {
        this.factory = factory;
    }

    @DataBoundConstructor
    public RunMatlabBuildBuilder() {
        this(new MatlabActionFactory());
    }

    // Getter and Setters to access local members
    @DataBoundSetter
    public void setTasks(String tasks) {
        this.tasks = tasks;
    }

    @DataBoundSetter
    public void setStartupOptions(StartupOptions startupOptions) {
        this.startupOptions = startupOptions;
    }

    @DataBoundSetter
    public void setBuildOptions(BuildOptions buildOptions) {
        this.buildOptions = buildOptions;
    }

    public String getTasks() {
        return this.tasks;
    }

    public StartupOptions getStartupOptions() {
        return this.startupOptions;
    }

    public String getStartupOptionsAsString() {
        return this.startupOptions == null
                ? ""
                : this.startupOptions.getOptions();
    }

    public BuildOptions getBuildOptions() {
        return this.buildOptions;
    }

    public String getBuildOptionsAsString() {
        return this.buildOptions == null
                ? null
                : this.buildOptions.getOptions();
    }

    @Extension
    public static class RunMatlabBuildDescriptor extends BuildStepDescriptor<Builder> {

        @Initializer(before = InitMilestone.PLUGINS_STARTED)
        public static void addAliases() {
            Items.XSTREAM2.addCompatibilityAlias("com.mathworks.ci.RunMatlabBuildBuilder", RunMatlabBuildBuilder.class);
        }

        // Overridden Method used to show the text under build dropdown
        @Override
        public String getDisplayName() {
            return Message.getValue("Builder.build.builder.display.name");
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }

        /*
         * This is to identify which project type in jenkins this should be
         * applicable.(non-Javadoc)
         * 
         * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
         * 
         * if it returns true then this build step will be applicable for all project
         * type.
         */
        @Override
        public boolean isApplicable(
                @SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobtype) {
            return true;
        }
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace,
            @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException {

        // Get the environment variable specific to the this build
        final EnvVars env = build.getEnvironment(listener);

        BuildActionParameters params = new BuildActionParameters(
                build, workspace, env, launcher, listener,
                this.getStartupOptionsAsString(),
                this.getTasks(),
                this.getBuildOptionsAsString());
        RunMatlabBuildAction action = factory.createAction(params);

        try {
            action.run();
        } catch (Exception e) {
            build.setResult(Result.FAILURE);
        }
    }

    // Added for backwards compatibility:
    // Called when object is loaded from persistent data.
    protected Object readResolve() {
        if (factory == null) {
            factory = new MatlabActionFactory();
        }

        return this;
    }
}
