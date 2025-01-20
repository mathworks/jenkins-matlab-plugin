package com.mathworks.ci.systemTests;

import com.mathworks.ci.*;
import com.mathworks.ci.freestyle.RunMatlabCommandBuilder;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.slaves.DumbSlave;
import org.htmlunit.WebAssert;
import org.htmlunit.html.*;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.*;
import org.junit.rules.Timeout;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertTrue;

public class GlobalToolIT {
    private FreeStyleProject project;
    private RunMatlabCommandBuilder scriptBuilder;
    private UseMatlabVersionBuildWrapper buildWrapper;

    @Rule
    public Timeout timeout = Timeout.seconds(0);

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @BeforeClass
    public static void checkMatlabRoot() {
        // Check if the MATLAB_ROOT environment variable is defined
        String matlabRoot = System.getenv("MATLAB_ROOT");
        Assume.assumeTrue("Not running tests as MATLAB_ROOT environment variable is not defined", matlabRoot != null && !matlabRoot.isEmpty());
    }

    @Before
    public void testSetup() throws IOException {
        this.project = jenkins.createFreeStyleProject();
        this.buildWrapper = new UseMatlabVersionBuildWrapper();
        this.scriptBuilder = new RunMatlabCommandBuilder();
    }

    @After
    public void testTearDown() {
        this.project = null;
        this.scriptBuilder = null;
        Utilities.matlabInstDescriptor = null;
        this.buildWrapper = null;
    }

    @Test
    public void verifyGlobalToolForFreeStyle() throws Exception{
        // Adding the MATLAB Global tool
        Utilities.setMatlabInstallation("MATLAB_PATH", Utilities.getMatlabRoot(), jenkins);

        // Selecting MATLAB that is defined as Global tool
        MatlabInstallation _inst = MatlabInstallation.getInstallation("MATLAB_PATH");
        MatlabBuildWrapperContent content = new MatlabBuildWrapperContent(_inst.getName(), null);
        buildWrapper.setMatlabBuildWrapperContent(content);
        project.getBuildWrappersList().add(buildWrapper);
        scriptBuilder.setMatlabCommand("disp('apple')");
        project.getBuildersList().add(scriptBuilder);

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        jenkins.assertLogContains(_inst.getHome(), build);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
        jenkins.assertLogContains("apple", build);
    }

    @Test
    public void verifyBuildFailsForIncorrectToolPath() throws Exception{
        // Adding the MATLAB Global tool
        Utilities.setMatlabInstallation("MATLAB_PATH", "matlab/incorrect/path", jenkins);

        // Selecting MATLAB that is defined as Global tool
        MatlabInstallation _inst = MatlabInstallation.getInstallation("MATLAB_PATH");
        MatlabBuildWrapperContent content = new MatlabBuildWrapperContent(_inst.getName(), null);
        buildWrapper.setMatlabBuildWrapperContent(content);
        project.getBuildWrappersList().add(buildWrapper);

        scriptBuilder.setMatlabCommand("ver");
        project.getBuildersList().add(scriptBuilder);

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        jenkins.assertBuildStatus(Result.FAILURE, build);
        jenkins.assertLogContains("Verify global tool configuration for the specified node.", build);
        jenkins.assertLogContains("matlab/incorrect/path", build);
    }

    @Test
    public void verifyAllToolsAreAvailable() throws Exception {
        // Adding the Global tools
        String matlabRoot = System.getenv("MATLAB_ROOT");
        String matlabRoot22b = System.getenv("MATLAB_ROOT_22b");
        Assume.assumeTrue("Not running tests as MATLAB_ROOT_22b environment variable is not defined", matlabRoot22b != null && !matlabRoot22b.isEmpty());

        Utilities.setMatlabInstallation("MATLAB_PATH_1", matlabRoot, jenkins);
        Utilities.setMatlabInstallation("MATLAB_PATH_2", matlabRoot22b, jenkins);

        // Getting the Web page for UI testing
        HtmlPage configurePage = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput matlabver=configurePage.getElementByName("com-mathworks-ci-UseMatlabVersionBuildWrapper");
        matlabver.setChecked(true);
        Thread.sleep(2000);
        List<HtmlSelect> matlabver_1=configurePage.getByXPath("//select[contains(@class, \"dropdownList\")]");
        // One of the drop downlists should have three options
        boolean allOptionsAvailable = false;
        for (HtmlSelect e : matlabver_1){
            if(e.getOptionSize() == 3){
                if(e.getOption(0).getValueAttribute().equals("MATLAB_PATH_1") &&
                   e.getOption(1).getValueAttribute().equals("MATLAB_PATH_2") &&
                   e.getOption(2).getValueAttribute().equals("Custom...")) {
                    allOptionsAvailable = true;
                }
            }
        }
        assertTrue(allOptionsAvailable);
    }

    @Test
    public void verifyGlobalToolMatrix() throws Exception {
        Utilities.setMatlabInstallation("MATLAB_PATH_1", Utilities.getMatlabRoot(), jenkins);
        Utilities.setMatlabInstallation("MATLAB_PATH_2", Utilities.getMatlabRoot(), jenkins);

        List<String> list= new ArrayList<>(Arrays.asList("MATLAB_PATH_1", "MATLAB_PATH_2"));
        MatlabInstallationAxis axis = new MatlabInstallationAxis(list);
        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class);;
        matrixProject.setAxes(new AxisList(axis));

        scriptBuilder.setMatlabCommand("version");
        matrixProject.getBuildersList().add(scriptBuilder);
        MatrixBuild build = matrixProject.scheduleBuild2(0).get();
        List<MatrixRun> runs = build.getRuns();
        // ToDo: Add more relevant verification statement
        for (MatrixRun run : runs) {
            String matlabName = run.getBuildVariables().get("MATLAB");
            Assert.assertTrue(matlabName.equalsIgnoreCase("MATLAB_PATH_1") || matlabName.equalsIgnoreCase("MATLAB_PATH_2"));
        }
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    // Tool config
    @Test
    public void verifyGlobalToolDSLPipeline() throws Exception {
        Utilities.setMatlabInstallation("MATLAB_PATH_1", Utilities.getMatlabRoot(), jenkins);
        String script = "pipeline {\n" +
                "   agent any\n" +
                Utilities.getEnvironmentDSL() + "\n" +
                "   tools {\n" +
                "       matlab 'MATLAB_PATH_1'\n" +
                "   }\n" +
                "    stages{\n" +
                "        stage('Run MATLAB Command') {\n" +
                "            steps\n" +
                "            {\n" +
                "               runMATLABCommand 'version'\n" +
                "            }       \n" +
                "        }                \n" +
                "    } \n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }
    @Test
    public void verifyGlobalToolScriptedPipeline() throws Exception {
        Utilities.setMatlabInstallation("MATLAB_PATH_1", Utilities.getMatlabRoot(), jenkins);
        String script = "node {\n" +
                "    def matlabver\n" +
                "    stage('Run MATLAB Command') {\n" +
                "        matlabver = tool 'MATLAB_PATH_1'\n" +
                "        if (isUnix()){\n" +
                "            env.PATH = \"${matlabver}/bin:${env.PATH}\"   // Linux or macOS agent\n" +
                "        }else{\n" +
                "            env.PATH = \"${matlabver}\\\\bin;${env.PATH}\"   // Windows agent\n" +
                "        }     \n" +
                "        runMATLABCommand 'version'\n" +
                "    }\n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    /*
     * Test to verify if Matrix build passes .
     */

    private WorkflowRun getPipelineBuild(String script) throws Exception{
        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(script,true));
        return project.scheduleBuild2(0).get();
    }

}