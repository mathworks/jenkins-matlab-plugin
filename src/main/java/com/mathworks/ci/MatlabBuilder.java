package com.mathworks.ci;

/*
 * Copyright 2019 The MathWorks, Inc.
 * 
 * This is Matlab Builder class which describes the build step and its components. Builder displays
 * Build step As "Run MATLAB Tests" under Build steps. Author : Nikhil Bhoski email :
 * nikhil.bhoski@mathworks.in Date : 28/03/2018 (Initial draft)
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.apache.commons.io.FilenameUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import hudson.DescriptorExtensionList;
import hudson.EnvVars;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.AbstractProject;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class MatlabBuilder extends Builder implements SimpleBuildStep {

    private static final double BASE_MATLAB_VERSION_RUNTESTS_SUPPORT = 8.1;
    private static final double BASE_MATLAB_VERSION_BATCH_SUPPORT = 9.5;
    private static final double BASE_MATLAB_VERSION_COBERTURA_SUPPORT = 9.3;
    private int buildResult;
    private TestRunTypeList testRunTypeList;
    private String matlabRoot;
    private EnvVars env;
    private static final String MATLAB_RUNNER_TARGET_FILE =
            "Builder.matlab.runner.target.file.name";
    private static final String MATLAB_RUNNER_RESOURCE =
            "com/mathworks/ci/MatlabBuilder/runMatlabTests.m";


    @DataBoundConstructor
    public MatlabBuilder() {


    }


    // Getter and Setters to access local members

    @DataBoundSetter
    public void setMatlabRoot(String matlabRoot) {
        this.matlabRoot = matlabRoot;
    }

    @DataBoundSetter
    public void setTestRunTypeList(TestRunTypeList testRunTypeList) {
        this.testRunTypeList = testRunTypeList;
    }

    public String getMatlabRoot() {

        return this.matlabRoot;
    }

    public TestRunTypeList getTestRunTypeList() {
        return this.testRunTypeList;
    }
    
    private String getLocalMatlab() {
        return this.env == null ? getLocalMatlab(): this.env.expand(getMatlabRoot());
    }
    
    private String getCustomMatlabCommand() {
        return this.env == null ? this.getTestRunTypeList().getStringByName("customMatlabCommand")
                : this.env.expand(this.getTestRunTypeList().getStringByName("customMatlabCommand"));
    }
    private void setEnv(EnvVars env) {
        this.env = env;
    }

    @Extension
    public static class MatlabDescriptor extends BuildStepDescriptor<Builder> {
        MatlabReleaseInfo rel;
        String matlabRoot;

        public String getMatlabRoot() {
            return matlabRoot;
        }

        public void setMatlabRoot(String matlabRoot) {
            this.matlabRoot = matlabRoot;
        }

        // Overridden Method used to show the text under build dropdown
        @Override
        public String getDisplayName() {
            return Message.getBuilderDisplayName();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }

        /*
         * This is to identify which project type in jenkins this should be applicable.(non-Javadoc)
         * 
         * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
         * 
         * if it returns true then this build step will be applicable for all project type.
         */
        @Override
        public boolean isApplicable(
                @SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobtype) {
            return true;
        }

        /*
         * below descriptor will get the values of Runtype descriptors and will assign it to the
         * dropdown list of config.jelly
         */
        public DescriptorExtensionList<TestRunTypeList, Descriptor<TestRunTypeList>> getTestRunTypeDescriptor() {
            return Jenkins.getInstance().getDescriptorList(TestRunTypeList.class);
        }

        /*
         * Below methods with 'doCheck' prefix gets called by jenkins when this builder is loaded.
         * these methods are used to perform basic validation on UI elements associated with this
         * descriptor class.
         */


        public FormValidation doCheckMatlabRoot(@QueryParameter String matlabRoot) {
            setMatlabRoot(matlabRoot);
            List<Function<String, FormValidation>> listOfCheckMethods =
                    new ArrayList<Function<String, FormValidation>>();
            listOfCheckMethods.add(chkMatlabEmpty);
            listOfCheckMethods.add(chkMatlabSupportsRunTests);

            return getFirstErrorOrWarning(listOfCheckMethods, matlabRoot);
        }

        public FormValidation getFirstErrorOrWarning(
                List<Function<String, FormValidation>> validations, String matlabRoot) {
            if (validations == null || validations.isEmpty())
                return FormValidation.ok();
            for (Function<String, FormValidation> val : validations) {
                FormValidation validationResult = val.apply(matlabRoot);
                if (validationResult.kind.compareTo(Kind.ERROR) == 0
                        || validationResult.kind.compareTo(Kind.WARNING) == 0) {
                    return validationResult;
                }
            }
            return FormValidation.ok();
        }

        Function<String, FormValidation> chkMatlabEmpty = (String matlabRoot) -> {
            if (matlabRoot.isEmpty()) {
                return FormValidation.error(Message.getValue("Builder.matlab.root.empty.error"));
            }
            return FormValidation.ok();
        };
        
        Function<String, FormValidation> chkMatlabSupportsRunTests = (String matlabRoot) -> {
            final MatrixPatternResolver resolver = new MatrixPatternResolver(matlabRoot);
            if (!resolver.hasVariablePattern()) {
                try {
                    rel = new MatlabReleaseInfo(matlabRoot);
                    if (rel.verLessThan(BASE_MATLAB_VERSION_RUNTESTS_SUPPORT)) {
                        return FormValidation
                                .error(Message.getValue("Builder.matlab.test.support.error"));
                    }
                } catch (MatlabVersionNotFoundException e) {
                    return FormValidation
                            .error(Message.getValue("Builder.invalid.matlab.root.error"));
                }
            }
            return FormValidation.ok();
        };
    }

    /*
     * Below abstract class is a describable class which holds the list of Runtype options available
     * on UI dropdown
     * 
     */

    public static abstract class TestRunTypeList
            implements ExtensionPoint, Describable<TestRunTypeList> {
        public TestRunTypeList() {

        }

        // Below abstract methods provides access to the public values assigned to UI elements which
        // are displayed based on dropdown option Each Runtype option class should implement below
        // methods.

        public abstract boolean getBooleanByName(String memberName);

        public abstract String getStringByName(String memberName);

        @SuppressWarnings("unchecked")
        public Descriptor<TestRunTypeList> getDescriptor() {
            return Jenkins.getInstance().getDescriptor(getClass());
        }
    }

    /*
     * Creating type Descriptor class which acts as descriptor for each dropdown items on UI All
     * FormValidation Method related to each dropdown options class should go in this descriptor
     * class.
     * 
     */

    public static abstract class TestRunTypeDescriptor extends Descriptor<TestRunTypeList> {
        MatlabReleaseInfo rel;

        /*
         * Validation for Test artifact generator checkBoxes
         */

        public FormValidation doCheckTaCoberturaChkBx(@QueryParameter boolean taCoberturaChkBx) {
            List<Function<String, FormValidation>> listOfCheckMethods =
                    new ArrayList<Function<String, FormValidation>>();
            final String matlabRoot = Jenkins.getInstance()
                    .getDescriptorByType(MatlabDescriptor.class).getMatlabRoot();
            if (taCoberturaChkBx) {
                listOfCheckMethods.add(chkCoberturaSupport);
            }
            return Jenkins.getInstance().getDescriptorByType(MatlabDescriptor.class)
                    .getFirstErrorOrWarning(listOfCheckMethods, matlabRoot);
        }

        Function<String, FormValidation> chkCoberturaSupport = (String matlabRoot) -> {
            rel = new MatlabReleaseInfo(matlabRoot);
            final MatrixPatternResolver resolver = new MatrixPatternResolver(matlabRoot);
            if(!resolver.hasVariablePattern()) {
                try {
                    if (rel.verLessThan(BASE_MATLAB_VERSION_COBERTURA_SUPPORT)) {
                        return FormValidation
                                .warning(Message.getValue("Builder.matlab.cobertura.support.warning"));
                    }
                } catch (MatlabVersionNotFoundException e) {
                    return FormValidation.error(Message.getValue("Builder.invalid.matlab.root.error"));
                }
            }
            

            return FormValidation.ok();
        };

    }

    /*
     * Create class of type TestRunTypeList each options of dropdown should have associated class
     * created.
     * 
     */

    public static class RunTestsAutomaticallyOption extends TestRunTypeList {
        private boolean tatapChkBx;
        private boolean taJunitChkBx;
        private boolean taCoberturaChkBx;

        @DataBoundConstructor
        public RunTestsAutomaticallyOption() {
            super();
        }

        @DataBoundSetter
        public void setTatapChkBx(boolean tatapChkBx) {
            this.tatapChkBx = tatapChkBx;
        }

        @DataBoundSetter
        public void setTaJunitChkBx(boolean taJunitChkBx) {
            this.taJunitChkBx = taJunitChkBx;
        }

        @DataBoundSetter
        public void setTaCoberturaChkBx(boolean taCoberturaChkBx) {
            this.taCoberturaChkBx = taCoberturaChkBx;
        }

        public boolean getTatapChkBx() {
            return tatapChkBx;
        }

        public boolean getTaJunitChkBx() {
            return taJunitChkBx;
        }

        public boolean getTaCoberturaChkBx() {
            return taCoberturaChkBx;
        }

        @Extension
        public static final class DescriptorImpl extends TestRunTypeDescriptor {
            @Override
            public String getDisplayName() {
                return Message.getValue("builder.matlab.automatictestoption.display.name");
            }
        }

        @Override
        public boolean getBooleanByName(String memberName) {
            switch (memberName) {
                case "tatapChkBx":
                    return this.getTatapChkBx();
                case "taJunitChkBx":
                    return this.getTaJunitChkBx();
                case "taCoberturaChkBx":
                    return this.getTaCoberturaChkBx();
                default:
                    return false;
            }
        }

        @Override
        public String getStringByName(String memberName) {
            return null;
        }
    }

    public static class RunTestsWithCustomCommandOption extends TestRunTypeList {
        private String customMatlabCommand;

        @DataBoundConstructor
        public RunTestsWithCustomCommandOption() {
            super();
        }

        @DataBoundSetter
        public void setCustomMatlabCommand(String customMatlabCommand) {
            this.customMatlabCommand = customMatlabCommand;
        }

        public String getCustomMatlabCommand() {
            return this.customMatlabCommand;
        }

        @Extension
        public static final class DescriptorImpl extends TestRunTypeDescriptor {
            @Override
            public String getDisplayName() {
                return Message.getValue("builder.matlab.customcommandoption.display.name");
            }
        }

        @Override
        public boolean getBooleanByName(String memberName) {

            return false;
        }

        @Override
        public String getStringByName(String memberName) {
            switch (memberName) {
                case "customMatlabCommand":
                    return this.getCustomMatlabCommand();
                default:
                    return null;
            }
        }
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace,
            @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException {        
        final boolean isLinuxLauncher = launcher.isUnix();
        
        // Invoke MATLAB command and transfer output to standard
        // Output Console

        buildResult = execMatlabCommand(build, workspace, launcher, listener, isLinuxLauncher);

        if (buildResult != 0) {
            build.setResult(Result.FAILURE);
        }
    }

    private synchronized int execMatlabCommand(Run<?, ?> build, FilePath workspace, Launcher launcher,
            TaskListener listener, boolean isLinuxLauncher)
            throws IOException, InterruptedException {
        setEnv(build.getEnvironment(listener));
        //Copy MATLAB scratch file into the workspace
        copyMatlabScratchFileInWorkspace(MATLAB_RUNNER_RESOURCE, MATLAB_RUNNER_TARGET_FILE,
                workspace, getClass().getClassLoader());
        ProcStarter matlabLauncher;
        try {
            MatlabReleaseInfo rel = new MatlabReleaseInfo(getLocalMatlab());
            matlabLauncher = launcher.launch().pwd(workspace).envs(this.env);
            if (rel.verLessThan(BASE_MATLAB_VERSION_BATCH_SUPPORT)) {
                ListenerLogDecorator outStream = new ListenerLogDecorator(listener);
                matlabLauncher = matlabLauncher.cmds(constructDefaultMatlabCommand(isLinuxLauncher)).stderr(outStream);
            } else {
                matlabLauncher = matlabLauncher.cmds(constructMatlabCommandWithBatch()).stdout(listener);
            }
        } catch (MatlabVersionNotFoundException e) {
            listener.getLogger().println(e.getMessage());
            return 1;
        }
        return matlabLauncher.join();
    }

    public List<String> constructMatlabCommandWithBatch() {
        final String testRunMode = this.getTestRunTypeList().getDescriptor().getDisplayName();
        final String runCommand;
        final List<String> matlabDefaultArgs;
        if (!testRunMode.equalsIgnoreCase(
                Message.getValue("builder.matlab.customcommandoption.display.name"))) {
            String matlabFunctionName =
                    FilenameUtils.removeExtension(Message.getValue(MATLAB_RUNNER_TARGET_FILE));
            runCommand = "exit(" + matlabFunctionName + "("
                    + getTestRunTypeList().getBooleanByName("taJunitChkBx") + ","
                    + getTestRunTypeList().getBooleanByName("tatapChkBx") + ","
                    + getTestRunTypeList().getBooleanByName("taCoberturaChkBx") + "))";
        } else {

            runCommand = getCustomMatlabCommand();
        }

        matlabDefaultArgs =
                Arrays.asList(getLocalMatlab() + File.separator + "bin" + File.separator + "matlab",
                        "-batch", runCommand);
        
        return matlabDefaultArgs;
    }

    public List<String> constructDefaultMatlabCommand(boolean isLinuxLauncher) {
        final List<String> matlabDefaultArgs = new ArrayList<String>();
        Collections.addAll(matlabDefaultArgs, getPreRunnerSwitches());
        if (!isLinuxLauncher) {
            matlabDefaultArgs.add("-noDisplayDesktop");
        }
        Collections.addAll(matlabDefaultArgs, getRunnerSwitch());
        if (!isLinuxLauncher) {
            matlabDefaultArgs.add("-wait");
        }
        Collections.addAll(matlabDefaultArgs, getPostRunnerSwitches());
        return matlabDefaultArgs;
    }


    private String[] getPreRunnerSwitches() {
        String[] preRunnerSwitches =
                {getLocalMatlab() + File.separator + "bin" + File.separator + "matlab", "-nosplash",
                        "-nodesktop", "-noAppIcon"};
        return preRunnerSwitches;
    }

    private String[] getPostRunnerSwitches() {
        String[] postRunnerSwitch = {"-log"};
        return postRunnerSwitch;
    }

    private String[] getRunnerSwitch() {
        final String runCommand;
        final String testRunMode = this.getTestRunTypeList().getDescriptor().getDisplayName();
        if (!testRunMode.equalsIgnoreCase(
                Message.getValue("builder.matlab.customcommandoption.display.name"))) {
            String matlabFunctionName =
                    FilenameUtils.removeExtension(Message.getValue(MATLAB_RUNNER_TARGET_FILE));
            runCommand = "try,exit(" + matlabFunctionName + "("
                    + getTestRunTypeList().getBooleanByName("taJunitChkBx") + ","
                    + getTestRunTypeList().getBooleanByName("tatapChkBx") + ","
                    + getTestRunTypeList().getBooleanByName("taCoberturaChkBx")
                    + ")),catch e,disp(getReport(e,'extended')),exit(1),end";
        } else {
            runCommand = "try,eval(\"" + getCustomMatlabCommand().replaceAll("\"","\"\"")
                    + "\"),catch e,disp(getReport(e,'extended')),exit(1),end,exit";
        }

        final String[] runnerSwitch = {"-r", runCommand};
        return runnerSwitch;
    }
    
    private void copyMatlabScratchFileInWorkspace(String matlabRunnerResourcePath,
            String matlabRunnerTarget, FilePath workspace, ClassLoader classLoader)
            throws IOException, InterruptedException {
        InputStream in = classLoader.getResourceAsStream(matlabRunnerResourcePath);
        Path target =
                new File(workspace.getRemote(), Message.getValue(matlabRunnerTarget)).toPath();

        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }
    
}
