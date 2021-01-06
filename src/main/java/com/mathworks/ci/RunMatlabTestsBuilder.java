package com.mathworks.ci;

/** 
 * Copyright 2019-2020 The MathWorks, Inc.  
 *  
 * MATLAB test run builder used to run all MATLAB & Simulink tests automatically and generate   
 * selected test artifacts. 
 *  
 */

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Computer;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;
import hudson.Launcher.ProcStarter;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class RunMatlabTestsBuilder extends Builder implements SimpleBuildStep, MatlabBuild {

    private int buildResult;
    private EnvVars env;

    // Make all old values transient which protects them writing back on disk.
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
    

    @DataBoundConstructor
    public RunMatlabTestsBuilder() {

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
    public SourceFolder getSourceFolder() {
        return this.sourceFolder;
    }
    
    public SelectByFolder getSelectByFolder(){
    	return this.selectByFolder;
    }

    private Artifact getArtifactObject(boolean isChecked, Artifact returnVal)  {
        // If previously checked assign valid artifact object else NullArtifact.
        return (isChecked) ? returnVal : new NullArtifact();
    }
    
    private void setEnv(EnvVars env) {
        this.env = env;
    }

    private EnvVars getEnv() {
        return this.env;
    }
    
    // To retain Backward compatibility
    protected Object readResolve() {

        /*
        * Assign appropriate artifact objects if it was selected in release 2.0.0 or earlier.
        * If using a later plugin release, check if artifact objects were previously serialized.
        * */
        this.pdfReportArtifact = Optional.ofNullable(this.pdfReportArtifact).orElseGet(() ->
                this.getArtifactObject(pdfReportChkBx, new PdfArtifact("matlabTestArtifacts/testreport.pdf"))
        );

        this.tapArtifact = Optional.ofNullable(this.tapArtifact).orElseGet(() ->
                this.getArtifactObject(tapChkBx, new TapArtifact("matlabTestArtifacts/taptestresults.tap"))
        );

        this.junitArtifact = Optional.ofNullable(this.junitArtifact).orElseGet(() ->
                this.getArtifactObject(junitChkBx, new JunitArtifact("matlabTestArtifacts/junittestresults.xml"))
        );

        this.coberturaArtifact = Optional.ofNullable(this.coberturaArtifact).orElseGet(() ->
                this.getArtifactObject(coberturaChkBx, new CoberturaArtifact("matlabTestArtifacts/cobertura.xml"))
        );

        this.stmResultsArtifact = Optional.ofNullable(this.stmResultsArtifact).orElseGet(() ->
                this.getArtifactObject(stmResultsChkBx, new StmResultsArtifact("matlabTestArtifacts/simulinktestresults.mldatx"))
        );

        this.modelCoverageArtifact = Optional.ofNullable(this.modelCoverageArtifact).orElseGet(() ->
                this.getArtifactObject(modelCoverageChkBx, new ModelCovArtifact("matlabTestArtifacts/coberturamodelcoverage.xml"))
        );

        return this;
    }
    
    
    
    @Extension
    public static class RunMatlabTestsDescriptor extends BuildStepDescriptor<Builder> {

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
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace,
            @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException {

        // Set the environment variable specific to the this build
        setEnv(build.getEnvironment(listener));

        // Invoke MATLAB command and transfer output to standard
        // Output Console

        buildResult = execMatlabCommand(workspace, launcher, listener, getEnv());

        if (buildResult != 0) {
            build.setResult(Result.FAILURE);
        }
    }

    private synchronized int execMatlabCommand(FilePath workspace, Launcher launcher,
            TaskListener listener, EnvVars envVars) throws IOException, InterruptedException {

        /*
         * Handle the case for using MATLAB Axis for multi conf projects by adding appropriate
         * matlabroot to env PATH
         * */
        Utilities.addMatlabToEnvPathFrmAxis(Computer.currentComputer(), listener, envVars);

        final String uniqueTmpFldrName = getUniqueNameForRunnerFile();
        ProcStarter matlabLauncher;
        try {
            FilePath genScriptLocation =
                    getFilePathForUniqueFolder(launcher, uniqueTmpFldrName, workspace);

            matlabLauncher = getProcessToRunMatlabCommand(workspace, launcher, listener, envVars,
                    constructCommandForTest(genScriptLocation), uniqueTmpFldrName);
            
            // copy genscript package in temp folder and write a runner script.
            prepareTmpFldr(genScriptLocation, getRunnerScript(
                    MatlabBuilderConstants.TEST_RUNNER_SCRIPT, envVars.expand(getInputArguments())));

            return matlabLauncher.pwd(workspace).join();
        } catch (Exception e) {
            listener.getLogger().println(e.getMessage());
            return 1;
        } finally {
            // Cleanup the runner File from tmp directory
            FilePath matlabRunnerScript =
                    getFilePathForUniqueFolder(launcher, uniqueTmpFldrName, workspace);
            if (matlabRunnerScript.exists()) {
                matlabRunnerScript.deleteRecursive();
            }
        }
    }
    
    public String constructCommandForTest(FilePath scriptPath) {
        final String matlabScriptName = getValidMatlabFileName(scriptPath.getBaseName());
        final String runCommand = "addpath('" + scriptPath.getRemote().replaceAll("'", "''")
                + "'); " + matlabScriptName;
        return runCommand;
    }

    // Concatenate the input arguments
    private String getInputArguments() {

        final List<String> inputArgsList = new ArrayList<String>();
        final Map<String,String> args = new HashMap<String,String>();
        
        final List<Artifact> artifactList =
                new ArrayList<Artifact>(Arrays.asList(getPdfReportArtifact(), getTapArtifact(),
                        getJunitArtifact(), getStmResultsArtifact(), getCoberturaArtifact(),
                        getModelCoverageArtifact()));
        
        inputArgsList.add("'Test'");

        for (Artifact artifact : artifactList) {
            artifact.addFilePathArgTo(args);
        }

        args.forEach((key, val) -> inputArgsList.add("'" + key + "'" + "," + "'" + val.replaceAll("'", "''") + "'"));

        /*
        * Add source folder options to argument.
        * For source folder we create a MATLAB cell array and add it to input argument list.
        * */
        SourceFolder sf = getSourceFolder();
        if(sf != null && !sf.getSourceFolderPaths().isEmpty()){
            sf.addSourceToInputArgs(inputArgsList, Utilities.getCellArrayFrmList(sf.getSourceFolderPaths().stream()
                    .map(SourceFolderPaths::getSrcFolderPath)
                    .collect(Collectors.toList())));
        }
        
        // Add Test folders
        if (getSelectByFolder() != null && !getSelectByFolder().getTestFolderPaths().isEmpty()) {
            getSelectByFolder().addSourceToInputArgs(inputArgsList,
                    Utilities.getCellArrayFrmList(getSelectByFolder().getTestFolderPaths().stream()
                            .map(TestFolders::getTestFolders).collect(Collectors.toList())));
        }

        // Add Tag to arguments
        if (getSelectByTag() != null && !getSelectByTag().getTestTag().isEmpty()) {
            getSelectByTag().addTagToInputArgs(inputArgsList);
        }

        return String.join(",", inputArgsList);
    }

    
    /*
     * Classes for each optional block in jelly file.This is restriction from Stapler architecture
     * when we use <f:optionalBlock> as it creates a object for each block in JSON. This could be
     * simplified by using inline=true attribute of <f:optionalBlock> however it has some abrupt UI
     * scrolling issue on click and also some esthetic issue like broken gray side bar appears.Some
     * discussion about this on Jenkins forum
     * https://groups.google.com/forum/#!searchin/jenkinsci-dev/OptionalBlock$20action$20class%
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
