package com.mathworks.ci.freestyle;

/** 
 * Copyright 2019-2024 The MathWorks, Inc.  
 *  
 * MATLAB test run builder used to run all MATLAB & Simulink tests automatically and generate   
 * selected test artifacts. 
 */

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.init.Initializer;
import hudson.init.InitMilestone;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.Items;
import hudson.model.TaskListener;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

import com.mathworks.ci.Message;
import com.mathworks.ci.actions.MatlabActionFactory;
import com.mathworks.ci.actions.RunMatlabTestsAction;
import com.mathworks.ci.parameters.TestActionParameters;
import com.mathworks.ci.freestyle.options.*;

public class RunMatlabTestsBuilder extends Builder implements SimpleBuildStep {

    // Make all old values transient which protects them writing back on disk.
    private transient int buildResult;
    private transient boolean tapChkBx;
    private transient boolean junitChkBx;
    private transient boolean coberturaChkBx;
    private transient boolean stmResultsChkBx;
    private transient boolean modelCoverageChkBx;
    private transient boolean pdfReportChkBx;

    private Artifact tapArtifact = new NullArtifact();
    private Artifact junitArtifact = new NullArtifact();
    private Artifact coberturaArtifact = new NullArtifact();
    private Artifact stmResultsArtifact = new NullArtifact();
    private Artifact modelCoverageArtifact = new NullArtifact();
    private Artifact pdfReportArtifact = new NullArtifact();

    private SourceFolder sourceFolder;
    private SelectByFolder selectByFolder;
    private SelectByTag selectByTag;
    private StartupOptions startupOptions;
    private String loggingLevel = "default";
    private String outputDetail = "default";
    private boolean useParallel = false;
    private boolean strict = false;

    private MatlabActionFactory factory;

    public RunMatlabTestsBuilder(MatlabActionFactory factory) {
        this.factory = factory;
    }

    @DataBoundConstructor
    public RunMatlabTestsBuilder() {
        this(new MatlabActionFactory());
    }

    // Getter and Setters to access local members

    @DataBoundSetter
    public void setTapArtifact(TapArtifact tapArtifact) {
        this.tapArtifact = tapArtifact;
    }

    @DataBoundSetter
    public void setJunitArtifact(JunitArtifact junitArtifact) {
        this.junitArtifact = junitArtifact;
    }

    @DataBoundSetter
    public void setCoberturaArtifact(CoberturaArtifact coberturaArtifact) {
        this.coberturaArtifact = coberturaArtifact;
    }

    @DataBoundSetter
    public void setStmResultsArtifact(StmResultsArtifact stmResultsArtifact) {
        this.stmResultsArtifact = stmResultsArtifact;
    }

    @DataBoundSetter
    public void setModelCoverageArtifact(ModelCovArtifact modelCoverageArtifact) {
        this.modelCoverageArtifact = modelCoverageArtifact;
    }

    @DataBoundSetter
    public void setPdfReportArtifact(PdfArtifact pdfReportArtifact) {
        this.pdfReportArtifact = pdfReportArtifact;
    }

    @DataBoundSetter
    public void setSelectByTag(SelectByTag selectByTag) {
        this.selectByTag = selectByTag;
    }

    @DataBoundSetter
    public void setSourceFolder(SourceFolder sourceFolder) {
        this.sourceFolder = sourceFolder;
    }

    @DataBoundSetter
    public void setSelectByFolder(SelectByFolder selectByFolder) {
        this.selectByFolder = selectByFolder;
    }

    @DataBoundSetter
    public void setStartupOptions(StartupOptions startupOptions) {
        this.startupOptions = startupOptions;
    }

    @DataBoundSetter
    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    @DataBoundSetter
    public void setOutputDetail(String outputDetail) {
        this.outputDetail = outputDetail;
    }

    @DataBoundSetter
    public void setUseParallel(boolean useParallel) {
        this.useParallel = useParallel;
    }

    @DataBoundSetter
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public String getTapReportFilePath() {
        return this.getTapArtifact().getFilePath();
    }

    public Artifact getTapArtifact() {
        return this.tapArtifact;
    }

    public Artifact getJunitArtifact() {
        return this.junitArtifact;
    }

    public String getJunitReportFilePath() {
        return this.getJunitArtifact().getFilePath();
    }

    public Artifact getCoberturaArtifact() {
        return this.coberturaArtifact;
    }

    public String getCoberturaReportFilePath() {
        return this.getCoberturaArtifact().getFilePath();
    }

    public Artifact getStmResultsArtifact() {
        return this.stmResultsArtifact;
    }

    public String getStmResultsFilePath() {
        return this.getStmResultsArtifact().getFilePath();
    }

    public Artifact getModelCoverageArtifact() {
        return this.modelCoverageArtifact;
    }

    public String getModelCoverageFilePath() {
        return this.getModelCoverageArtifact().getFilePath();
    }

    public Artifact getPdfReportArtifact() {
        return this.pdfReportArtifact;
    }

    public String getPdfReportFilePath() {
        return this.getPdfReportArtifact().getFilePath();
    }

    public SelectByTag getSelectByTag() {
        return this.selectByTag;
    }

    public String getSelectByTagAsString() {
        return this.selectByTag == null
                ? null
                : selectByTag.getTestTag();
    };

    public SourceFolder getSourceFolder() {
        return this.sourceFolder;
    }

    public List<String> getSourceFolderPaths() {
        return this.sourceFolder == null
                ? null
                : this.sourceFolder.getSourceFolderStringPaths();
    }

    public SelectByFolder getSelectByFolder() {
        return this.selectByFolder;
    }

    public List<String> getSelectByFolderPaths() {
        return this.selectByFolder == null
                ? null
                : this.selectByFolder.getTestFolderStringPaths();
    }

    private Artifact getArtifactObject(boolean isChecked, Artifact returnVal) {
        // If previously checked assign valid artifact object else NullArtifact.
        return (isChecked) ? returnVal : new NullArtifact();
    }

    // Verbosity level

    public String getLoggingLevel() {
        return loggingLevel == null ? "default" : this.loggingLevel;
    }

    public String getOutputDetail() {
        return outputDetail == null ? "default" : this.outputDetail;
    }

    public boolean getStrict() {
        return this.strict;
    }

    public boolean getUseParallel() {
        return this.useParallel;
    }

    public StartupOptions getStartupOptions() {
        return this.startupOptions;
    }

    public String getStartupOptionsAsString() {
        return this.startupOptions == null
                ? ""
                : this.startupOptions.getOptions();
    }

    // To retain Backward compatibility
    protected Object readResolve() {

        /*
         * Assign appropriate artifact objects if it was selected in release 2.0.0 or
         * earlier.
         * If using a later plugin release, check if artifact objects were previously
         * serialized.
         */
        this.pdfReportArtifact = Optional.ofNullable(this.pdfReportArtifact).orElseGet(
                () -> this.getArtifactObject(pdfReportChkBx, new PdfArtifact("matlabTestArtifacts/testreport.pdf")));

        this.tapArtifact = Optional.ofNullable(this.tapArtifact).orElseGet(
                () -> this.getArtifactObject(tapChkBx, new TapArtifact("matlabTestArtifacts/taptestresults.tap")));

        this.junitArtifact = Optional.ofNullable(this.junitArtifact).orElseGet(() -> this.getArtifactObject(junitChkBx,
                new JunitArtifact("matlabTestArtifacts/junittestresults.xml")));

        this.coberturaArtifact = Optional.ofNullable(this.coberturaArtifact).orElseGet(() -> this
                .getArtifactObject(coberturaChkBx, new CoberturaArtifact("matlabTestArtifacts/cobertura.xml")));

        this.stmResultsArtifact = Optional.ofNullable(this.stmResultsArtifact)
                .orElseGet(() -> this.getArtifactObject(stmResultsChkBx,
                        new StmResultsArtifact("matlabTestArtifacts/simulinktestresults.mldatx")));

        this.modelCoverageArtifact = Optional.ofNullable(this.modelCoverageArtifact)
                .orElseGet(() -> this.getArtifactObject(modelCoverageChkBx,
                        new ModelCovArtifact("matlabTestArtifacts/coberturamodelcoverage.xml")));

        if (factory == null) {
            factory = new MatlabActionFactory();
        }

        return this;
    }

    @Extension
    public static class RunMatlabTestsDescriptor extends BuildStepDescriptor<Builder> {

        @Initializer(before = InitMilestone.PLUGINS_STARTED)
        public static void addAliases() {
            Items.XSTREAM2.addCompatibilityAlias("com.mathworks.ci.RunMatlabTestsBuilder", RunMatlabTestsBuilder.class);
            Items.XSTREAM2.addCompatibilityAlias("com.mathworks.ci.SourceFolderPaths", SourceFolderPaths.class);
            Items.XSTREAM2.addCompatibilityAlias("com.mathworks.ci.TestFolders", TestFolders.class);

            Items.XSTREAM2.addCompatibilityAlias(
                    "com.mathworks.ci.RunMatlabTestsBuilder$PdfArtifact",
                    RunMatlabTestsBuilder.PdfArtifact.class);
            Items.XSTREAM2.addCompatibilityAlias(
                    "com.mathworks.ci.RunMatlabTestsBuilder$JunitArtifact",
                    RunMatlabTestsBuilder.JunitArtifact.class);
            Items.XSTREAM2.addCompatibilityAlias(
                    "com.mathworks.ci.RunMatlabTestsBuilder$TapArtifact",
                    RunMatlabTestsBuilder.TapArtifact.class);
            Items.XSTREAM2.addCompatibilityAlias(
                    "com.mathworks.ci.RunMatlabTestsBuilder$CoberturaArtifact",
                    RunMatlabTestsBuilder.CoberturaArtifact.class);
            Items.XSTREAM2.addCompatibilityAlias(
                    "com.mathworks.ci.RunMatlabTestsBuilder$StmResultsArtifact",
                    RunMatlabTestsBuilder.StmResultsArtifact.class);
            Items.XSTREAM2.addCompatibilityAlias(
                    "com.mathworks.ci.RunMatlabTestsBuilder$ModelCovArtifact",
                    RunMatlabTestsBuilder.ModelCovArtifact.class);
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

        // Verbosity lists
        public ListBoxModel doFillLoggingLevelItems() {
            ListBoxModel items = new ListBoxModel();

            items.add("Default", "default");
            items.add("None", "none");
            items.add("Terse", "terse");
            items.add("Concise", "concise");
            items.add("Detailed", "detailed");
            items.add("Verbose", "verbose");
            return items;
        }

        public ListBoxModel doFillOutputDetailItems() {
            ListBoxModel items = new ListBoxModel();

            items.add("Default", "default");
            items.add("None", "none");
            items.add("Terse", "terse");
            items.add("Concise", "concise");
            items.add("Detailed", "detailed");
            items.add("Verbose", "verbose");
            return items;
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

        TestActionParameters params = new TestActionParameters(
                build, workspace, env, launcher, listener,
                this.getStartupOptionsAsString(),
                this.getPdfReportFilePath(),
                this.getTapReportFilePath(),
                this.getJunitReportFilePath(),
                this.getCoberturaReportFilePath(),
                this.getStmResultsFilePath(),
                this.getModelCoverageFilePath(),
                this.getSelectByTagAsString(),
                this.getLoggingLevel(),
                this.getOutputDetail(),
                this.getUseParallel(),
                this.getStrict(),
                this.getSourceFolderPaths(),
                this.getSelectByFolderPaths());
        RunMatlabTestsAction action = factory.createAction(params);

        try {
            action.run();
        } catch (Exception e) {
            build.setResult(Result.FAILURE);
        }
    }

    /*
     * Classes for each optional block in jelly file.This is restriction from
     * Stapler architecture
     * when we use <f:optionalBlock> as it creates a object for each block in JSON.
     * This could be
     * simplified by using inline=true attribute of <f:optionalBlock> however it has
     * some abrupt UI
     * scrolling issue on click and also some esthetic issue like broken gray side
     * bar appears.Some
     * discussion about this on Jenkins forum
     * https://groups.google.com/forum/#!searchin/jenkinsci-dev/
     * OptionalBlock$20action$20class%
     * 7Csort:date/jenkinsci-dev/AFYHSG3NUEI/UsVJIKoE4B8J
     * 
     */

    public static class PdfArtifact extends AbstractArtifactImpl {

        private static final String PDF_TEST_REPORT = "PDFTestReport";

        @DataBoundConstructor
        public PdfArtifact(String pdfReportFilePath) {
            super(pdfReportFilePath);
        }

        @Override
        public void addFilePathArgTo(Map<String, String> inputArgs) {
            inputArgs.put(PDF_TEST_REPORT, getFilePath());
        }
    }

    public static class TapArtifact extends AbstractArtifactImpl {

        private static final String TAP_TEST_RESULTS = "TAPTestResults";

        @DataBoundConstructor
        public TapArtifact(String tapReportFilePath) {
            super(tapReportFilePath);
        }

        @Override
        public void addFilePathArgTo(Map<String, String> inputArgs) {
            inputArgs.put(TAP_TEST_RESULTS, getFilePath());
        }
    }

    public static class JunitArtifact extends AbstractArtifactImpl {

        private static final String JUNIT_TEST_RESULTS = "JUnitTestResults";

        @DataBoundConstructor
        public JunitArtifact(String junitReportFilePath) {
            super(junitReportFilePath);
        }

        @Override
        public void addFilePathArgTo(Map<String, String> inputArgs) {
            inputArgs.put(JUNIT_TEST_RESULTS, getFilePath());
        }
    }

    public static class CoberturaArtifact extends AbstractArtifactImpl {

        private static final String COBERTURA_CODE_COVERAGE = "CoberturaCodeCoverage";

        @DataBoundConstructor
        public CoberturaArtifact(String coberturaReportFilePath) {
            super(coberturaReportFilePath);
        }

        @Override
        public void addFilePathArgTo(Map<String, String> inputArgs) {
            inputArgs.put(COBERTURA_CODE_COVERAGE, getFilePath());
        }
    }

    public static class StmResultsArtifact extends AbstractArtifactImpl {

        private static final String STM_RESULTS = "SimulinkTestResults";

        @DataBoundConstructor
        public StmResultsArtifact(String stmResultsFilePath) {
            super(stmResultsFilePath);
        }

        @Override
        public void addFilePathArgTo(Map<String, String> inputArgs) {
            inputArgs.put(STM_RESULTS, getFilePath());
        }
    }

    public static class ModelCovArtifact extends AbstractArtifactImpl {

        private static final String COBERTURA_MODEL_COVERAGE = "CoberturaModelCoverage";

        @DataBoundConstructor
        public ModelCovArtifact(String modelCoverageFilePath) {
            super(modelCoverageFilePath);
        }

        @Override
        public void addFilePathArgTo(Map<String, String> inputArgs) {
            inputArgs.put(COBERTURA_MODEL_COVERAGE, getFilePath());
        }
    }

    public static class NullArtifact implements Artifact {

        @Override
        public void addFilePathArgTo(Map<String, String> inputArgs) {

        }

        @Override
        public boolean getSelected() {
            return false;
        }

        @Override
        public String getFilePath() {
            return null;
        }

    }

    public static abstract class AbstractArtifactImpl implements Artifact {

        private String filePath;

        protected AbstractArtifactImpl(String path) {
            this.filePath = path;
        }

        public boolean getSelected() {
            return true;
        }

        public void setFilePath(String path) {
            this.filePath = path;
        }

        public String getFilePath() {
            return this.filePath;
        }
    }

    public interface Artifact {
        public void addFilePathArgTo(Map<String, String> inputArgs);

        public String getFilePath();

        public boolean getSelected();
    }

    public static final class SelectByTag extends AbstractDescribableImpl<SelectByTag> {
        private String testTag;
        private static final String SELECT_BY_TAG = "SelectByTag";

        @DataBoundConstructor
        public SelectByTag(String testTag) {
            this.testTag = Util.fixNull(testTag);
        }

        public String getTestTag() {
            return this.testTag;
        }

        public void addTagToInputArgs(List<String> inputArgsList) {
            inputArgsList.add("'" + SELECT_BY_TAG + "'" + "," + "'"
                    + getTestTag().replaceAll("'", "''") + "'");
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<SelectByTag> {
        }
    }
}
