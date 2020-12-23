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
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;


public class FilterTestFolderInteg {
    private WorkflowJob project;
    private String environment;
    private String gitRepo;

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void testSetup() throws IOException, URISyntaxException {
        this.project = jenkins.createProject(WorkflowJob.class);
        this.environment = getEnvironmentPath();
        this.gitRepo = getGitRepo();
    }
    /*
     * This method returns the environment path needed to be set for pipeline scripts
     */
    private String getEnvironmentPath() throws URISyntaxException {
        String installedPath;
        String binPath = "";

        if (System.getProperty("os.name").startsWith("Win")) {
            installedPath = TestData.getPropValues("matlab.windows.installed.path");
            binPath = installedPath +"/bin;";
        }
        else if(System.getProperty("os.name").startsWith("Linux")){
            installedPath = TestData.getPropValues("matlab.linux.installed.path");
            binPath = installedPath + "/bin:";
        }
        else {
            installedPath = TestData.getPropValues("matlab.mac.installed.path");
            binPath = installedPath + "/bin:";
        }
        environment = "env.PATH =" + '"' + binPath + "${env.PATH}" + '"';
        return environment;
    }

    /*
     * Utility function which returns the git details for pipeline scripts
     */
    private String getGitRepo() {
        String gitURI = "git branch:" + "'" + TestData.getPropValues("github.branch") + "'" +", url:" +"'" +TestData.getPropValues("github.repo.path")+ "'";
        return gitURI;
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
     * Test to verify if tests are filtered
     */
    @Test
    public void verifyTestsAreFiltered() throws Exception{
        String script = "node {\n" +
                            environment + "\n" +
                            gitRepo  + "\n" +
                            "runMATLABTests(sourceFolder:['src'], selectByFolder: ['test/TestMultiply'])\n" +
                        "}";

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
                         environment + "\n" +
                         gitRepo + "\n" +
                "            runMATLABTests(sourceFolder:['src'], selectByFolder:[ 'test/IncorrectFolder'])\n" +
                "        }";
        WorkflowRun build = getBuild(script);
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
                            environment + "\n" +
                            gitRepo  + "\n" +
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
                            environment + "\n" +
                            gitRepo  + "\n" +
                "            runMATLABTests(sourceFolder:['src'])\n" +
                "        }";
        WorkflowRun build = getBuild(script);
        jenkins.assertLogContains("testMultiply/testMultiplication", build);
        jenkins.assertLogContains("testSquare/testSquareNum", build);
        jenkins.assertLogContains("testSum/testAddition", build);
        jenkins.assertLogContains("testModel/testModelSim", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    /*
     * Test to verify if tests are filtered by Tags
     * Also check if multiple tests are run with same tag
     */
    @Test
    public void verifyTestsAreFilteredByTag() throws Exception{
        String script = "node {\n" +
                            environment + "\n" +
                            gitRepo  + "\n" +
                "            runMATLABTests(sourceFolder:['src'], selectByTag:'TestTag')\n" +
                "        }";
        WorkflowRun build = getBuild(script);
        jenkins.assertLogContains("testSquare/testSquareNum", build);
        jenkins.assertLogContains("testSum/testAddition", build);
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
                        environment + "\n" +
                        gitRepo  + "\n" +
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
                        environment + "\n" +
                        gitRepo  + "\n" +
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
    public void verifyTestsFromFolderNotUnderTESTAreRun() throws Exception{
        String script = "node {\n" +
                        environment + "\n" +
                        gitRepo  + "\n" +
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
                        environment + "\n" +
                        gitRepo  + "\n" +
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
                        environment + "\n" +
                        gitRepo  + "\n" +
                "            runMATLABTests(sourceFolder:['src/multiplySrc'], selectByFolder:['test/TestMultiply'])\n" +
                "        }";
        WorkflowRun build = getBuild(script);
        jenkins.assertLogContains("testMultiply/testMultiplication", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }
}