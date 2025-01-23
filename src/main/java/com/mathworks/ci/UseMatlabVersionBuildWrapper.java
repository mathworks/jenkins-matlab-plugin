package com.mathworks.ci;

/**
 * Copyright 2019-2024 The MathWorks, Inc.
 *
 * This class is BuildWrapper which accepts the "matlabroot" from user and updates the PATH varible with it.
 * which could be later used across build.
 */

import hudson.model.Item;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import hudson.matrix.MatrixProject;
import hudson.model.Computer;
import org.kohsuke.stapler.AncestorInPath;
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
import org.kohsuke.stapler.verb.POST;

public class UseMatlabVersionBuildWrapper extends SimpleBuildWrapper {

    private String matlabRootFolder;
    private EnvVars env;
    private String matlabInstallationName;

    @DataBoundConstructor
    public UseMatlabVersionBuildWrapper() {
    }

    public String getMatlabRootFolder() {
        return this.matlabRootFolder;
    }

    public String getMatlabInstallationHome(Computer cmp, TaskListener listener, EnvVars env)
            throws IOException, InterruptedException {
        return Utilities.getNodeSpecificHome(this.matlabInstallationName,
                cmp.getNode(), listener, env).getRemote();
    }

    public String getMatlabInstallationName() {
        /*
         * For backward compatibility assign installation name to custom
         * if matlabRootFolder is not null.
         */
        if (this.matlabRootFolder != null && !this.matlabRootFolder.isEmpty()) {
            this.matlabInstallationName = Message.getValue("matlab.custom.location");
        }
        return matlabInstallationName;
    }

    @DataBoundSetter
    public void setMatlabBuildWrapperContent(MatlabBuildWrapperContent matlabBuildWrapperContent) {
        if (matlabBuildWrapperContent != null) {
            this.matlabInstallationName = matlabBuildWrapperContent.getMatlabInstallationName();
            this.matlabRootFolder = matlabBuildWrapperContent.getMatlabRootFolder();
        }
    }

    private String getNodeSpecificMatlab(Computer cmp, TaskListener listener)
            throws IOException, InterruptedException {
        String matlabroot = getMatlabRootFolder();
        // If matlabroot is null use matlab installation path
        if (matlabroot == null || matlabroot.isEmpty()) {
            matlabroot = getMatlabInstallationHome(cmp, listener, this.env);
        }

        return this.env == null ? matlabroot : this.env.expand(matlabroot);
    }

    private void setEnv(EnvVars env) {
        this.env = env;
    }

    @Extension
    public static final class UseMatlabVersionDescriptor extends BuildWrapperDescriptor {

        MatlabReleaseInfo rel;
        private boolean isMatrix;
        private final String customLocation = Message.getValue("matlab.custom.location");
        private final String matlabAxisWarning = Message.getValue("Use.matlab.version.axis.warning");
        private AbstractProject<?, ?> project;

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            this.project = item;
            isMatrix = item instanceof MatrixProject;
            return true;
        }

        @Override
        public String getDisplayName() {
            return Message.getValue("Buildwrapper.display.name");
        }

        public MatlabInstallation[] getInstallations() {
            ArrayList<MatlabInstallation> arr;
            arr = new ArrayList<>(Arrays.asList(MatlabInstallation.getAll()));
            arr.add(new MatlabInstallation(customLocation));
            MatlabInstallation[] temp = new MatlabInstallation[arr.size()];
            return arr.toArray(temp);
        }

        public String getCustomLocation() {
            return customLocation;
        }

        public boolean getIsMatrix() {
            return isMatrix;
        }

        public boolean checkAxisAdded() {
            if (!isMatrix) {
                return false;
            }
            return MatlabItemListener.getMatlabAxisCheckForPrj(project.getFullName()) && !MatlabInstallation.isEmpty();
        }

        public String getMatlabAxisWarning() {
            return matlabAxisWarning;
        }

        /*
         * Below methods with 'doCheck' prefix gets called by jenkins when this builder
         * is loaded.
         * these methods are used to perform basic validation on UI elements associated
         * with this
         * descriptor class.
         */
        @POST
        public FormValidation doCheckMatlabRootFolder(@QueryParameter String matlabRootFolder,
                @AncestorInPath Item item) {
            if (item == null) {
                return FormValidation.ok();
            }
            item.checkPermission(Item.CONFIGURE);
            List<Function<String, FormValidation>> listOfCheckMethods = new ArrayList<Function<String, FormValidation>>();
            listOfCheckMethods.add(chkMatlabEmpty);
            listOfCheckMethods.add(chkMatlabSupportsRunTests);

            return FormValidationUtil.getFirstErrorOrWarning(listOfCheckMethods, matlabRootFolder);
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
    public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher,
            TaskListener listener, EnvVars initialEnvironment)
            throws IOException, InterruptedException {
        // Set Environment variable
        setEnv(initialEnvironment);

        String nodeSpecificMatlab = getNodeSpecificMatlab(Computer.currentComputer(), listener)
                + getNodeSpecificExecutable(launcher);
        FilePath matlabExecutablePath = new FilePath(launcher.getChannel(), nodeSpecificMatlab);
        if (!matlabExecutablePath.exists()) {
            throw new MatlabNotFoundError(Message.getValue("matlab.not.found.error"));
        }
        FilePath matlabBinDir = matlabExecutablePath.getParent();
        if (matlabBinDir == null) {
            throw new MatlabNotFoundError(Message.getValue("matlab.not.found.error"));
        }

        // Add "matlabroot" without bin as env variable which will be available across
        // the build.
        context.env("matlabroot", nodeSpecificMatlab);
        // Add matlab bin to path to invoke MATLAB directly on command line.
        context.env("PATH+matlabroot", matlabBinDir.getRemote());
        ;
        listener.getLogger().println("\n" + String.format(Message.getValue("matlab.added.to.path.from"),
                matlabBinDir.getRemote()) + "\n");
    }

    private String getNodeSpecificExecutable(Launcher launcher) {
        return (launcher.isUnix()) ? "/bin/matlab" : "\\bin\\matlab.exe";
    }

}
