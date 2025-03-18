package com.mathworks.ci.systemtests;

import com.mathworks.ci.MatlabBuildWrapperContent;
import com.mathworks.ci.Message;
import com.mathworks.ci.TestMessage;
import com.mathworks.ci.UseMatlabVersionBuildWrapper;
import com.mathworks.ci.freestyle.RunMatlabBuildBuilder;
import com.mathworks.ci.freestyle.options.BuildOptions;
import hudson.EnvVars;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tasks.Builder;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlPage;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.*;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.assertTrue;

public class RunMatlabBuildIT {
    private FreeStyleProject project;
    private UseMatlabVersionBuildWrapper buildWrapper;
    private RunMatlabBuildBuilder runBuilder;

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
        this.runBuilder = new RunMatlabBuildBuilder();
        this.buildWrapper = new UseMatlabVersionBuildWrapper();
    }

    @After
    public void testTearDown() {
        this.project = null;
        this.runBuilder = null;
        this.buildWrapper = null;
    }

    @Test
    public void verifyBuildStepWithRunMatlab() throws Exception {
        boolean found = false;
        project.getBuildersList().add(runBuilder);
        List<Builder> bl = project.getBuildersList();
        for (Builder b : bl) {
            if (b.getDescriptor().getDisplayName().equalsIgnoreCase(
                    TestMessage.getValue("Builder.build.builder.display.name"))) {
                found = true;
            }
        }
        Assert.assertTrue("Build step does not contain Run MATLAB Build option", found);
    }

    /*
     * Test to verify if Build FAILS when matlab command fails
     */

    @Test
    public void verifyBuildFailureWhenMatlabBuildFails() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.setScm(new ExtractResourceSCM(Utilities.getURLForTestData()));

        this.runBuilder.setTasks("invalid_task");

        project.getBuildersList().add(this.runBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        jenkins.assertBuildStatus(Result.FAILURE, build);
        jenkins.assertLogContains(String.format(Message.getValue("matlab.execution.exception.prefix"), 1), build);
    }


    /* Test To Verify if Build passes when matlab command passes
     */
    @Test
    public void verifyBuildPassesWhenMatlabBuildPasses() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.setScm(new ExtractResourceSCM(Utilities.getURLForTestData()));

        this.runBuilder.setTasks("check");
        project.getBuildersList().add(this.runBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        jenkins.assertBuildStatus(Result.SUCCESS, build);
        jenkins.assertLogContains("buildtool check", build);
    }

    @Test
    public void verifyDefaultTaskForNoTaskInput() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.setScm(new ExtractResourceSCM(Utilities.getURLForTestData()));

        project.getBuildersList().add(this.runBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        // Default test task fails
        jenkins.assertBuildStatus(Result.FAILURE, build);
        // Test task runs the test
        jenkins.assertLogContains("testMultiply", build);
    }

    @Test
    public void verifyRunningMultipleTasks() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.setScm(new ExtractResourceSCM(Utilities.getURLForTestData()));

        this.runBuilder.setTasks("check dummy");
        project.getBuildersList().add(this.runBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        jenkins.assertBuildStatus(Result.SUCCESS, build);
        jenkins.assertLogContains("buildtool check dummy", build);
        jenkins.assertLogContains("In dummy task", build);
    }

    @Test
    public void verifySpecifyingBuildOptions() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.setScm(new ExtractResourceSCM(Utilities.getURLForTestData()));

        this.runBuilder.setTasks("check test dummy");
        this.runBuilder.setBuildOptions(new BuildOptions("-continueOnFailure -skip check"));
        project.getBuildersList().add(this.runBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        // 'test' task fails
        jenkins.assertBuildStatus(Result.FAILURE, build);
        jenkins.assertLogContains("buildtool check test dummy", build);
        jenkins.assertLogNotContains("In check task", build);
        jenkins.assertLogContains("In dummy task", build);
    }

    @Test
    public void verifyPipelineOnSlave() throws Exception {
        DumbSlave s = jenkins.createOnlineSlave();
        String script ="node('!built-in') { runMATLABBuild() }";

        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(script, true));
        WorkflowRun build = project.scheduleBuild2(0).get();

        jenkins.assertLogNotContains("Running on Jenkins", build);
    }

    @Test
    public void verifyBuildSupportsEnvVar() throws Exception {
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars var = prop.getEnvVars();
        var.put("TASKS", "compile");
        var.put("BUILD_OPTIONS", "-continueOnFailure -skip test");
        jenkins.jenkins.getGlobalNodeProperties().add(prop);

        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));

        project.getBuildWrappersList().add(this.buildWrapper);
        runBuilder.setTasks("$TASKS");
        runBuilder.setBuildOptions(new BuildOptions("$BUILD_OPTIONS"));
        project.getBuildersList().add(runBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        jenkins.assertLogContains("compile", build);
        jenkins.assertLogContains("-continueOnFailure -skip test", build);
    }


    @Test
    public void verifyBuildSummaryInBuildStatusPage() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.setScm(new ExtractResourceSCM(Utilities.getURLForTestData()));

        this.runBuilder.setTasks("check test dummy");
        this.runBuilder.setBuildOptions(new BuildOptions("-continueOnFailure -skip dummy"));
        project.getBuildersList().add(this.runBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        // Verify MATLAB Build Result summary
        String BuildResultSummary= getSummaryFromBuildStatus(build);
        assertTrue(BuildResultSummary.contains("Tasks run: 3"));
        assertTrue(BuildResultSummary.contains("Failed: 1"));
        assertTrue(BuildResultSummary.contains("Skipped: 1"));

        jenkins.assertBuildStatus(Result.FAILURE, build);
        jenkins.assertLogContains("buildtool check test dummy -continueOnFailure -skip dummy", build);
    }

    @Test
    public void verifyHyperlinkFromSummaryAndSidePanelAreSame() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.setScm(new ExtractResourceSCM(Utilities.getURLForTestData()));

        this.runBuilder.setTasks("check test dummy");
        this.runBuilder.setBuildOptions(new BuildOptions("-continueOnFailure -skip dummy"));
        project.getBuildersList().add(this.runBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        // Verify the hyperlink from the summary page and Tab are the same
        String buildID = getBuildIDFromSummary(build);

        // Get link of "MATLAB Build Results" tab
        String resultTableURL = getBuildPagePathFromSidePanel(build);

        assertTrue(resultTableURL.contains(buildID));
    }

    // Helper functions
    private String getBuildIDFromSummary(FreeStyleBuild build) throws IOException, SAXException {
        HtmlPage buildPage = jenkins.createWebClient().getPage(build);
        HtmlAnchor matlabLink = buildPage.getFirstByXPath("//*[@id='main-panel']/table[1]/tbody//a[contains(text(), 'MATLAB Build Results')]");
        return matlabLink.getHrefAttribute();
    }

    private String getBuildPagePathFromSidePanel(FreeStyleBuild build) throws IOException, SAXException {
        HtmlPage buildPage = jenkins.createWebClient().getPage(build);
        HtmlElement jenkinsSidePanelElement  = buildPage.getFirstByXPath("//*[@id='side-panel']/div");
        HtmlElement buildResultTab = (HtmlElement) jenkinsSidePanelElement.getChildNodes().get(5);
        HtmlAnchor href = (HtmlAnchor) buildResultTab.getChildNodes().get(0).getByXPath("//a[span[text()='MATLAB Build Results']]").get(0);
        return href.getHrefAttribute();
    }

    private String getSummaryFromBuildStatus(FreeStyleBuild build) throws IOException, SAXException {
        HtmlPage buildPage = jenkins.createWebClient().getPage(build);
        HtmlElement summaryElement = (HtmlElement) buildPage.getByXPath("//*[@id='main-panel']/table[1]/tbody/tr[3]/td[2]").get(0);
        return summaryElement.getTextContent();

    }
}
