package com.mathworks.ci;



import static org.junit.Assert.assertFalse;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.FreeStyleProject;
import hudson.tasks.Builder;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import com.mathworks.ci.MatlabBuilder.RunTestsAutomaticallyOption;
import com.mathworks.ci.MatlabBuilder.RunTestsWithCustomCommandOption;


/*
 * Copyright 2018 The MathWorks, Inc.
 * 
 * Test class for MatlabBuilder
 * 
 * Author : Nikhil Bhoski email : nikhil.bhoski@mathworks.in Date : 28/03/2018 (Initial draft)
 */
public class MatlabBuilderTest {

    private static TestMessage messages;
    private static String matlabExecutorAbsolutePath;
    private FreeStyleProject project;
    private MatlabBuilder matlabBuilder;
    private static URL url;
    private static String FileSeperator;

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @BeforeClass
    public static void classSetup() throws URISyntaxException, IOException {
        ClassLoader classLoader = MatlabBuilderTest.class.getClassLoader();
        if (!System.getProperty("os.name").startsWith("Win")) {
            FileSeperator = "/";
            url = classLoader.getResource("com/mathworks/ci/linux/bin/matlab.sh");
            try {
                matlabExecutorAbsolutePath = new File(url.toURI()).getAbsolutePath();

                // Need to do this operation due to bug in maven Resource copy plugin [
                // https://issues.apache.org/jira/browse/MRESOURCES-132 ]

                ProcessBuilder pb = new ProcessBuilder("chmod", "755", matlabExecutorAbsolutePath);
                pb.start();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            FileSeperator = "\\";
            url = classLoader.getResource("com/mathworks/ci/win/bin/matlab.bat");
            matlabExecutorAbsolutePath = new File(url.toURI()).getAbsolutePath();
        }
        messages = new TestMessage();
    }

    @Before
    public void testSetup() throws IOException {

        this.project = jenkins.createFreeStyleProject();
        this.matlabBuilder = new MatlabBuilder();
    }

    @After
    public void testTearDown() {
        this.project = null;
        this.matlabBuilder = null;
    }

    private String getMatlabroot(String version) throws URISyntaxException {
        ClassLoader classLoader = MatlabBuilderTest.class.getClassLoader();
        String matlabRoot = new File(
                classLoader.getResource("versioninfo/" + version + "/VersionInfo.xml").toURI())
                        .getAbsolutePath().replace(FileSeperator + "VersionInfo.xml", "");
        return matlabRoot;
    }

    /*
     * Test Case to verify if Build step contains "Run MATLAB Tests" option.
     */
    @Test
    public void verifyBuildStepWithMATLABBuilder() throws Exception {
        boolean found = false;
        this.matlabBuilder.setLocalMatlab("");
        project.getBuildersList().add(this.matlabBuilder);
        List<Builder> bl = project.getBuildersList();
        for (Builder b : bl) {
            if (b.getDescriptor().getDisplayName()
                    .equalsIgnoreCase(Message.getBuilderDisplayName())) {
                found = true;
            }
        }
        Assert.assertTrue("Build step does not contain Run MATLAB Tests option", found);
    }


    /*
     * Test To verify MATLAB is launched with default arguments and with -batch when release
     * supports -batch
     */

    @Test
    public void verifyMATLABlaunchedWithDefaultArgumentsBatch() throws Exception {
        this.matlabBuilder.setLocalMatlab(getMatlabroot("R2018b"));
        this.matlabBuilder.setTestRunTypeList(new RunTestsAutomaticallyOption());
        project.getBuildersList().add(this.matlabBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("-batch", build);
        jenkins.assertLogContains("exit(runMatlabTests", build);
        Assert.assertEquals(3, matlabBuilder.constructMatlabCommandWithBatch().size());
    }

    /*
     * Test To verify MATLAB is launched with default arguments and with -r when release supports -r
     * on windows
     */

    @Test
    public void verifyMATLABlaunchedWithDefaultArgumentsRWindows() throws Exception {
        this.matlabBuilder.setLocalMatlab(getMatlabroot("R2017a"));
        this.matlabBuilder.setTestRunTypeList(new RunTestsAutomaticallyOption());
        project.getBuildersList().add(this.matlabBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("-r", build);
        jenkins.assertLogContains("try,exit(runMatlabTests", build);
        Assert.assertEquals(9, matlabBuilder.constructDefaultMatlabCommand(false).size());
    }

    /*
     * Test To verify MATLAB is launched with default arguments and with -r when release supports -r
     * on Linux
     */

    @Test
    public void verifyMATLABlaunchedWithDefaultArgumentsRLinux() throws Exception {
        this.matlabBuilder.setLocalMatlab(getMatlabroot("R2017a"));
        this.matlabBuilder.setTestRunTypeList(new RunTestsAutomaticallyOption());
        project.getBuildersList().add(this.matlabBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("-r", build);
        jenkins.assertLogContains("try,exit(runMatlabTests", build);
        Assert.assertEquals(7, matlabBuilder.constructDefaultMatlabCommand(true).size());
    }

    /*
     * Test to verify if job fails when invalid MATLAB path is provided and Exception is thrown
     */

    @Test
    public void verifyBuilderFailsForInvalidMATLABPath() throws Exception {
        this.matlabBuilder.setLocalMatlab("/fake/matlabroot/that/does/not/exist");
        project.getBuildersList().add(this.matlabBuilder);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    /*
     * Test to verify if Build FAILS when matlab test fails
     */

    @Test
    public void verifyBuildFailureWhenMatlabException() throws Exception {
        MatlabBuilderTester tester = new MatlabBuilderTester(getMatlabroot("R2018b"),
                matlabExecutorAbsolutePath, "-positiveFail");
        // tester.setFailBuildIfTestFailureCheckBox(false);
        project.getBuildersList().add(tester);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    /*
     * Test to verify if MATLAB gets invoked and job sets to UNSTABLE when valid matlabroot is
     * provided and all test passed.
     */
    @Test
    public void verifyMatlabInvokedWithValidExecutable() throws Exception {
        MatlabBuilderTester tester = new MatlabBuilderTester(getMatlabroot("R2018b"),
                matlabExecutorAbsolutePath, "-positive");
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.SUCCESS, build);
        jenkins.assertLogContains(messages.getMatlabInvokesPositive(), build);

    }

    /*
     * Test to verify MATLAB executable path is same as provide by user
     */

    @Test
    public void verifyMatlabPointsToValidExecutable() throws Exception {
        MatlabBuilderTester tester = new MatlabBuilderTester(getMatlabroot("R2018b"),
                matlabExecutorAbsolutePath, "-positive");
        project.getBuildersList().add(tester);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.SUCCESS, build);
        jenkins.assertLogContains(matlabExecutorAbsolutePath, build);
    }

    /*
     * Test to verify Build is set to FAILED when test fails
     * 
     */

    @Test
    public void verifyBuildStatusWhenTestFails() throws Exception {
        MatlabBuilderTester tester = new MatlabBuilderTester(getMatlabroot("R2018b"),
                matlabExecutorAbsolutePath, "failTests");
        project.getBuildersList().add(tester);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    /*
     * Tests to verify if verLessThan() method compares values appropriately.
     */

    @Test
    public void verifyVerlessThan() throws Exception {
        MatlabReleaseInfo rel = new MatlabReleaseInfo(getMatlabroot("R2017a"));

        // verLessthan() will check all the versions against 9.2 which is version of R2017a
        assertFalse(rel.verLessThan(9.1));
        assertFalse(rel.verLessThan(9.0));
        assertFalse(rel.verLessThan(9.2));
        Assert.assertTrue(rel.verLessThan(9.9));
        Assert.assertTrue(rel.verLessThan(10.1));
    }

    /*
     * Test to verify if plugin invokes MATLAB with custom MATLAB command when Custom MATLAB command
     * option is selected. for -r option
     */

    @Test
    public void verifyCustomCommandInvoked() throws Exception {
        this.matlabBuilder.setLocalMatlab(getMatlabroot("R2017a"));
        RunTestsWithCustomCommandOption runOption = new RunTestsWithCustomCommandOption();
        runOption.setCustomMatlabCommand("runtests");
        this.matlabBuilder.setTestRunTypeList(runOption);
        project.getBuildersList().add(this.matlabBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("-r", build);
        jenkins.assertLogContains("try,runtests", build);
    }

    /*
     * Test to verify if plugin invokes MATLAB with custom MATLAB command when Custom MATLAB command
     * option is selected. for -batch option
     */

    @Test
    public void verifyCustomCommandInvokedForBatchMode() throws Exception {
        this.matlabBuilder.setLocalMatlab(getMatlabroot("R2018b"));
        RunTestsWithCustomCommandOption runOption = new RunTestsWithCustomCommandOption();
        runOption.setCustomMatlabCommand("runtests");
        this.matlabBuilder.setTestRunTypeList(runOption);
        project.getBuildersList().add(this.matlabBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("-batch", build);
        jenkins.assertLogContains("runtests", build);
    }
    
    @Test
    public void testFailure() throws Exception {
        Assert.assertEquals("Deliberate failure.", 5,6);
    }
}
