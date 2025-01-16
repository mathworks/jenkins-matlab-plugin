package com.mathworks.ci.freestyle;

/**
 * Copyright 2019-2024 The MathWorks, Inc.
 * 
 * Script builder used to run custom MATLAB commands or scripts.
 */

import hudson.util.FormValidation;
import java.io.IOException;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
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
import com.mathworks.ci.parameters.CommandActionParameters;
import com.mathworks.ci.actions.MatlabActionFactory;
import com.mathworks.ci.actions.RunMatlabCommandAction;
import com.mathworks.ci.freestyle.options.StartupOptions;
import org.kohsuke.stapler.verb.POST;

public class RunMatlabCommandBuilder extends Builder implements SimpleBuildStep {
    // Deprecated
    private transient int buildResult;

    // In use
    private String matlabCommand;
    private StartupOptions startupOptions;

    private MatlabActionFactory factory;

    public RunMatlabCommandBuilder(MatlabActionFactory factory) {
        this.factory = factory;
    }

    @DataBoundConstructor
    public RunMatlabCommandBuilder() {
        this(new MatlabActionFactory());
    }

    // Getter and Setters to access local members
    @DataBoundSetter
    public void setMatlabCommand(String matlabCommand) {
        this.matlabCommand = matlabCommand;
    }

    @DataBoundSetter
    public void setStartupOptions(StartupOptions startupOptions) {
        this.startupOptions = startupOptions;
    }

    public String getMatlabCommand() {
        return this.matlabCommand;
    }

    public StartupOptions getStartupOptions() {
        return this.startupOptions;
    }

    public String getStartupOptionsAsString() {
        return this.startupOptions == null
                ? ""
                : this.startupOptions.getOptions();
    }

    @Extension
    public static class RunMatlabCommandDescriptor extends BuildStepDescriptor<Builder> {

        @Initializer(before = InitMilestone.PLUGINS_STARTED)
        public static void addAliases() {
            Items.XSTREAM2.addCompatibilityAlias("com.mathworks.ci.RunMatlabCommandBuilder",
                    RunMatlabCommandBuilder.class);
        }

        // Overridden Method used to show the text under build dropdown
        @Override
        public String getDisplayName() {
            return Message.getValue("Builder.script.builder.display.name");
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

        @POST
        public FormValidation doCheckMatlabCommand(@QueryParameter String value) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            if (value.isEmpty()) {
                return FormValidation.error(Message.getValue("matlab.empty.command.error"));
            }
            return FormValidation.ok();
        }
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace,
            @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException {

        // Get the environment variables specific to the this build
        final EnvVars env = build.getEnvironment(listener);

        CommandActionParameters params = new CommandActionParameters(
                build, workspace, env,
                launcher, listener,
                getStartupOptionsAsString(),
                getMatlabCommand());
        RunMatlabCommandAction action = factory.createAction(params);

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
