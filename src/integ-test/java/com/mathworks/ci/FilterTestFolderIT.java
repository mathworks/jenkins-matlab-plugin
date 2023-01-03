package com.mathworks.ci;

import com.google.common.io.Resources;
import com.google.common.base.Charsets;
import hudson.model.Result;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class FilterTestFolderIT {
    private WorkflowJob project;
    private String envScripted;
    private String envDSL;

    @Rule
    public Timeout timeout = Timeout.seconds(0);

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void testSetup() throws IOException, URISyntaxException {
        this.project = jenkins.createProject(WorkflowJob.class);
        this.envDSL = MatlabRootSetup.getEnvironmentDSL();
        this.envScripted = MatlabRootSetup.getEnvironmentScriptedPipeline();
    }

    /*
     * Utility function to add the required test data for filter tests
     */
    private String addTestData() throws MalformedURLException {
        URL zipFile = MatlabRootSetup.getRunMATLABTestsData();
        String path = "  unzip '" + zipFile.getPath() + "'" + "\n";
        return path;
    }

    /*
     * Utility function which returns the build of the project
     */
    private WorkflowRun getBuild(String script) throws Exception{
        project.setDefinition(new CpsFlowDefinition(script,true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        return build;
    }

    /*
     * Test to verify if tests are filtered scripted pipeline
     */
    @Test
    public void verifyTestsAreFiltered() throws Exception{
        String script = "node {\n" +
                envScripted + "\n" +
                addTestData()+"\n" +
                "runMATLABTests(sourceFolder:['src'], selectByFolder: ['test/TestMultiply'])\n" +
                "}";
        System.out.println(script);
        WorkflowRun build = getBuild(script);
        System.out.println(script);
        jenkins.assertLogContains("testMultiply/testMultiplication",build);
        jenkins.assertLogNotContains("testSquare/testSquareNum", build);
        jenkins.assertLogNotContains("testSum/testAddition", build);
        jenkins.assertLogNotContains("testModel/testModelSim", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    /*
     * Test to verify if tests are filtered DSL pipeline
     */
    @Test
    public void verifyTestsAreFilteredDSL() throws Exception{
        String script = "pipeline {\n" +
                "agent any" + "\n" +
                "stages{" + "\n" +
                "stage('Run MATLAB Command') {\n" +
                "steps\n" +
                "{"+
                addTestData()+"\n" +
                "runMATLABTests(sourceFolder:['src'], selectByFolder: ['test/TestMultiply'])\n" +
                "}" + "\n" +
                "}" + "\n" +
                "}" + "\n" +
                "}";
        System.out.println(script);
        WorkflowRun build = getBuild(script);
        jenkins.assertLogContains("testMultiply/testMultiplication",build);
        jenkins.assertLogNotContains("testSquare/testSquareNum", build);
        jenkins.assertLogNotContains("testSum/testAddition", build);
        jenkins.assertLogNotContains("testModel/testModelSim", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    /*
     * Test to verify no tests are run when a incorrect folder path is given
     */
    @Test
    public void verifyNoTestsAreRunForIncorrectTestPath() throws Exception{
        String script = "node {\n" +
                addTestData()+"\n" +
                "            runMATLABTests(sourceFolder:['src'], selectByFolder:[ 'test/IncorrectFolder'])\n" +
                "        }";
        WorkflowRun build = getBuild(script);
        System.out.println(script);
        jenkins.assertLogNotContains("Done setting up", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    /*
     * Test to verify that test cases with no dependency on source file are run successfully when the source folder path
     * is not added
     */
    @Test
    public void verifyIndependentTestsRunWithoutSource() throws Exception{
        String script = "node {\n" +
                addTestData()  + "\n" +
                "            runMATLABTests(selectByFolder:['test/TestSum'])\n" +
                "        }";
        WorkflowRun build = getBuild(script);
        jenkins.assertLogContains("testSum/testAddition", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    /*
     * Test to verify all the test cases are run when there is no filter
     */
    @Test
    public void verifyAllTestsRunWithNoFilter() throws Exception{
        String script = "node {\n" +
                addTestData()  + "\n" +
                "            runMATLABTests(sourceFolder:['src'])\n" +
                "        }";
        WorkflowRun build = getBuild(script);
        System.out.println(build.getLog());
        jenkins.assertLogContains("Done testMultiply", build);
        jenkins.assertLogContains("testSquare", build);
        jenkins.assertLogContains("testSum", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    /*
     * Test to verify if tests are filtered by Tags
     * Also check if multiple tests are run with same tag
     */
    @Test
    public void verifyTestsAreFilteredByTag() throws Exception{
        String script = "node {\n" +
                addTestData()  + "\n" +
                "            runMATLABTests(sourceFolder:['src'], selectByTag:'TestTag')\n" +
                "        }";
        WorkflowRun build = getBuild(script);
        jenkins.assertLogContains("Done testSquare", build);
        jenkins.assertLogContains("Done testSum", build);
        jenkins.assertLogNotContains("testMultiply/testMultiplication", build);
        jenkins.assertLogNotContains("testModel/testModelSim", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    /*
     * Test to verify no tests are run for incorrect tag
     */
    @Test
    public void verifyNoTestsRunWithIncorrectTag() throws Exception{
        String script = "node {\n" +
                addTestData()  + "\n" +
                "            runMATLABTests(sourceFolder:['src'], selectByTag:'IncorrectTag')\n" +
                "        }";
        WorkflowRun build = getBuild(script);
        jenkins.assertLogNotContains("Done setting up", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    /*
     * Test to verify if tests from a particular folder are filtered by Tags
     */
    @Test
    public void verifyTestsFromFolderWithTagAreRun() throws Exception{
        String script = "node {\n" +
                addTestData()  + "\n" +
                "            runMATLABTests(sourceFolder:['src'], selectByTag:'TestTag', selectByFolder:['test/TestSum'])\n" +
                "        }";
        WorkflowRun build = getBuild(script);
        jenkins.assertLogNotContains("testSquare/testSquareNum", build);
        jenkins.assertLogContains("testSum/testAddition", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    /*
     * Test to verify if tests from folder which are not under 'test' folder are run
     */
    @Test
    public void verifyTestsFromFolderNotUnderTESTAreRun() throws Exception {
        String script = "node {\n" +
                addTestData()  + "\n" +
                "            runMATLABTests(sourceFolder:['src'], selectByFolder:['testing/modelSimTest'])\n" +
                "        }";
        WorkflowRun build = getBuild(script);
        jenkins.assertLogNotContains("testModel/testModelSim", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    /*
     * Test to verify tests fail if the source folder are not added
     */
    @Test
    public void verifyTestFailWhenSrcIsNotAdded() throws Exception{
        String script = "node {\n" +
                addTestData()  + "\n" +
                "            runMATLABTests(selectByFolder:['test/TestSquare'])\n" +
                "        }";
        WorkflowRun build = getBuild(script);
        jenkins.assertLogContains("Undefined function 'squareNum' for input arguments of type 'double'.", build);
        jenkins.assertBuildStatus(Result.FAILURE,build);
    }

    /*
     * Test to verify source folder selection and test folder selection
     */
    @Test
    public void verifySrcFolderSelection() throws Exception {
        String script = "node {\n" +
                addTestData()  + "\n" +
                "            runMATLABTests(sourceFolder:['src/multiplySrc'], selectByFolder:['test/TestMultiply'])\n" +
                "        }";
        WorkflowRun build = getBuild(script);
        jenkins.assertLogContains("testMultiply/testMultiplication", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    /*
     * Test to verify source folder is selected in generated MATLAB code
     */
    @Test
    public void verifySourceFolderSelectionInScript() throws Exception {
        String script = "node {\n" +
                addTestData()  + "\n" +
                "            runMATLABTests(sourceFolder:['src/multiplySrc'], selectByFolder:['test/TestMultiply'])\n" +
                "        }";
        WorkflowRun build = getBuild(script);
        jenkins.assertLogContains("addpath(genpath('src/multiplySrc'));", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    /*
     * Test to verify multiple tags are not supported
     */
    @Test
    public void verifyMultipleTagsFails() throws  Exception {
        String script = "node {\n" +
                addTestData()  + "\n" +
                "            runMATLABTests(sourceFolder:['src'], selectByTag:'TestTag,TestTag1')\n" +
                "        }";
        WorkflowRun build = getBuild(script);
        jenkins.assertLogNotContains("Done setting up", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    /*
     * Test to verify code coverage can be generated for specified source files based on selected tests
     */
    @Test
    public void verifyCodeCoverage() throws Exception {
        String script = "node {\n" +
                addTestData()  + "\n" +
                "            runMATLABTests(sourceFolder:['src/multiplySrc'], selectByFolder:['test/TestMultiply'], codeCoverageCobertura: 'code-coverage/coverage.xml')\n" +
                "            cobertura coberturaReportFile: 'code-coverage/coverage.xml', enableNewApi: true, lineCoverageTargets: '100'" +
                "        }";
        WorkflowRun build = getBuild(script);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }


}
