package com.mathworks.ci;

import com.gargoylesoftware.htmlunit.WebAssert;
import com.gargoylesoftware.htmlunit.html.*;
import hudson.matrix.*;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.slaves.DumbSlave;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;
import org.w3c.dom.html.HTMLElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.jvnet.hudson.test.JenkinsRule.getLog;

public class GlobalToolIntegTest {
    private FreeStyleProject project;
    private RunMatlabCommandBuilder scriptBuilder;
    private UseMatlabVersionBuildWrapper buildWrapper;
    private MatrixProject matrixProject;


    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void testSetup() throws IOException {
        this.project = jenkins.createFreeStyleProject();
        this.buildWrapper = new UseMatlabVersionBuildWrapper();
        this.matrixProject = jenkins.createProject(MatrixProject.class);
        this.scriptBuilder = new RunMatlabCommandBuilder();
    }

    @After
    public void testTearDown() {
        this.project = null;
        this.scriptBuilder = null;
        MatlabRootSetup.matlabInstDescriptor = null;
        this.buildWrapper = null;
    }

    public void useCommandFreeStyle(String command) {
        scriptBuilder.setMatlabCommand(TestData.getPropValues("matlab.command"));
        project.getBuildersList().add(scriptBuilder);
    }

    @Test
    public void verifyGlobalToolForFreeStyle() throws Exception{
        // Adding the MATLAB Global tool
        MatlabRootSetup.setMatlabInstallation("MATLAB_PATH", MatlabRootSetup.getMatlabRoot(), jenkins);

        // Selecting MATLAB that is defined as Global tool
        MatlabInstallation _inst = MatlabInstallation.getInstallation("MATLAB_PATH");
        MatlabBuildWrapperContent content = new MatlabBuildWrapperContent(_inst.getName(), null);
        buildWrapper.setMatlabBuildWrapperContent(content);
        project.getBuildWrappersList().add(buildWrapper);
        useCommandFreeStyle("version");

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        jenkins.assertLogContains(_inst.getHome(), build);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    @Test
    public void verifyBuildFailsForIncorrectToolPath() throws Exception{
        // Adding the MATLAB Global tool
        MatlabRootSetup.setMatlabInstallation("MATLAB_PATH", "matlab/incorrect/path", jenkins);
        jenkins.configRoundtrip();

        // Selecting MATLAB that is defined as Global tool
        MatlabInstallation _inst = MatlabInstallation.getInstallation("MATLAB_PATH");
        MatlabBuildWrapperContent content = new MatlabBuildWrapperContent(_inst.getName(), null);
        buildWrapper.setMatlabBuildWrapperContent(content);
        project.getBuildWrappersList().add(buildWrapper);

        scriptBuilder.setMatlabCommand(TestData.getPropValues("matlab.command"));
        project.getBuildersList().add(scriptBuilder);

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        jenkins.assertBuildStatus(Result.FAILURE, build);
        jenkins.assertLogContains("Verify global tool configuration for the specified node.", build);
    }

    //Modify it to use the Custom Option

    public void verifyUseMATLABVersionBuildPasses() throws Exception {
        HtmlPage configurePage = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlTextInput matlabRoot =configurePage.getElementByName("_.matlabRootFolder");
        matlabRoot.setValueAttribute(MatlabRootSetup.getMatlabRoot());
        HtmlButton saveButton = (HtmlButton) configurePage.getElementByName("Submit").getFirstChild().getFirstChild();
        RunMatlabCommandBuilder tester =
                new RunMatlabCommandBuilder();
        tester.setMatlabCommand(TestData.getPropValues("matlab.command"));
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String build_log = jenkins.getLog(build);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    @Test
    public void verifyAllToolsAreAvailable() throws Exception {
        // Adding the Global tools
        MatlabRootSetup.setMatlabInstallation("MATLAB_PATH_1", MatlabRootSetup.getMatlabRoot(), jenkins);
        MatlabRootSetup.setMatlabInstallation("MATLAB_PATH_2", MatlabRootSetup.getMatlabRoot().replace(TestData.getPropValues("matlab.version"), "R2020a"), jenkins);
        jenkins.configRoundtrip();
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
        MatlabRootSetup.setMatlabInstallation("MATLAB_PATH_1", MatlabRootSetup.getMatlabRoot(), jenkins);
        MatlabRootSetup.setMatlabInstallation("MATLAB_PATH_2", MatlabRootSetup.getMatlabRoot().replace(TestData.getPropValues("matlab.version"), "R2020a"), jenkins);

        List<String> list= new ArrayList<>(Arrays.asList("MATLAB_PATH_1", "MATLAB_PATH_2"));
        MatlabInstallationAxis axis = new MatlabInstallationAxis(list);
        matrixProject.setAxes(new AxisList(axis));

        scriptBuilder.setMatlabCommand("version");
        matrixProject.getBuildersList().add(scriptBuilder);
        MatrixBuild build = matrixProject.scheduleBuild2(0).get();
        List<MatrixRun> runs = build.getRuns();
        for (MatrixRun run : runs) {
            String matlabName = run.getBuildVariables().get("MATLAB");
            System.out.println(matlabName);
            Assert.assertTrue(matlabName.equalsIgnoreCase("MATLAB_PATH_1") || matlabName.equalsIgnoreCase("MATLAB_PATH_2"));
        }
        jenkins.assertBuildStatus(Result.SUCCESS,build);
        String build_log = jenkins.getLog(build);
    }

    // Verify "Use MATLAB Version" takes precedence over Global tools in Matrix Project
    @Test
    public void verifyToolMatrixPreference() throws Exception {
        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class);
        MatlabRootSetup.setMatlabInstallation("MATLAB_PATH_1", MatlabRootSetup.getMatlabRoot(), jenkins);
        MatlabRootSetup.setMatlabInstallation("MATLAB_PATH_2", MatlabRootSetup.getMatlabRoot().replace(TestData.getPropValues("matlab.version"), "R2020a"), jenkins);
        jenkins.configRoundtrip();

        List<String> list= new ArrayList<>(Arrays.asList("MATLAB_PATH_1", "MATLAB_PATH_2"));
        MatlabInstallationAxis axis = new MatlabInstallationAxis(list);
        matrixProject.setAxes(new AxisList(axis));

        String matlabRoot = MatlabRootSetup.getMatlabRoot();
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), matlabRoot));
        matrixProject.getBuildWrappersList().add(buildWrapper);

        scriptBuilder.setMatlabCommand("version");
        matrixProject.getBuildersList().add(scriptBuilder);
        MatrixBuild build = matrixProject.scheduleBuild2(0).get();
        List<MatrixRun> runs = build.getRuns();
        for (MatrixRun run : runs) {
            jenkins.assertLogContains(TestData.getPropValues("matlab.version"), run);
        }
        jenkins.assertBuildStatus(Result.SUCCESS,build);
        String build_log = jenkins.getLog(build);
    }

    @Test
    public void verifyNodes() throws Exception {
        DumbSlave slave = jenkins.createSlave("slave","Slave_machine",null);
//        project.setAssignedNode(slave);
        // Adding the MATLAB Global tool
        MatlabRootSetup.setMatlabInstallation("MATLAB_PATH", MatlabRootSetup.getMatlabRoot(), jenkins);

        // Selecting MATLAB that is defined as Global tool
        MatlabInstallation _inst = MatlabInstallation.getInstallation("MATLAB_PATH");
        MatlabBuildWrapperContent content = new MatlabBuildWrapperContent(_inst.getName(), null);
        buildWrapper.setMatlabBuildWrapperContent(content);
        project.getBuildWrappersList().add(buildWrapper);
        useCommandFreeStyle("version");

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        jenkins.assertLogContains(_inst.getHome(), build);
        String build_log = jenkins.getLog(build);

        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }
}
