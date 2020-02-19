package com.mathworks.ci;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import com.mathworks.ci.MatlabBuilder.RunTestsAutomaticallyOption;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Builder;

public class MatlabTestRunBuilderTest {
    

    private static TestMessage messages;
    private static String matlabExecutorAbsolutePath;
    private FreeStyleProject project;
    private MatlabBuildWrapper buildWrapper;
    private MatlabTestRunBuilder testBuilder;
    private static URL url;
    private static String FileSeperator;
    private static String VERSION_INFO_XML_FILE = "VersionInfo.xml";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @BeforeClass
    public static void classSetup() throws URISyntaxException, IOException {
        ClassLoader classLoader = MatlabTestRunBuilderTest.class.getClassLoader();
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
        this.testBuilder = new MatlabTestRunBuilder();
        this.buildWrapper = new MatlabBuildWrapper();
    }

    @After
    public void testTearDown() {
        this.project = null;
        this.testBuilder = null;
    }

    private String getMatlabroot(String version) throws URISyntaxException {
        String defaultVersionInfo = "versioninfo/R2017a/" + VERSION_INFO_XML_FILE;
        String userVersionInfo = "versioninfo/"+version+"/" + VERSION_INFO_XML_FILE;
        URL matlabRootURL = Optional.ofNullable(getResource(userVersionInfo)).orElseGet(() -> getResource(defaultVersionInfo));
        File matlabRoot = new File(matlabRootURL.toURI());
        return matlabRoot.getAbsolutePath().replace(FileSeperator + VERSION_INFO_XML_FILE,"").replace("R2017a",version);
    }
    
    private URL getResource(String resource) {
        return MatlabTestRunBuilderTest.class.getClassLoader().getResource(resource); 
    }
    
    /*
     * Test Case to verify if Build step contains "Run MATLAB Tests" option.
     */
    @Test
    public void verifyBuildStepWithMatlabTestBuilder() throws Exception {
        boolean found = false;
        setAllTestArtifacts(false, testBuilder);
        project.getBuildersList().add(testBuilder);
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
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        setAllTestArtifacts(false,this.testBuilder);
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("-batch", build);
        jenkins.assertLogContains("exit(runMatlabTests", build);
    }
    
    /*
     * Test To verify MATLAB is launched with default arguments and with -r when release supports -r
     * on windows
     */

    @Test
    public void verifyMATLABlaunchedWithDefaultArgumentsRWindows() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2017a"));
        project.getBuildWrappersList().add(this.buildWrapper);
        setAllTestArtifacts(false,this.testBuilder);
        project.getBuildersList().add(testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("-r", build);
        jenkins.assertLogContains("runMatlabTests", build);
    }
    
    /*
     * Test to verify if job fails when invalid MATLAB path is provided and Exception is thrown
     */

    @Test
    public void verifyBuilderFailsForInvalidMATLABPath() throws Exception {
        this.buildWrapper.setMatlabRootFolder("/fake/matlabroot/that/does/not/exist");
        project.getBuildWrappersList().add(this.buildWrapper);
        setAllTestArtifacts(false,this.testBuilder);
        project.getBuildersList().add(this.testBuilder);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }
    
    /*
     * Test to verify if Build FAILS when matlab test fails
     */

    @Test
    public void verifyBuildFailureWhenMatlabException() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        MatlabTestRunBuilderTester tester =
                new MatlabTestRunBuilderTester(matlabExecutorAbsolutePath, "-positiveFail");
        setAllTestArtifacts(false,tester);
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }
    
    private void setAllTestArtifacts(boolean val,MatlabTestRunBuilder testBuilder) {
        testBuilder.setCoberturaChkBx(val);
        testBuilder.setJunitChkBx(val);
        testBuilder.setModelCoverageChkBx(val);
        testBuilder.setPdfReportChkBx(val);
        testBuilder.setTapChkBx(val);
        testBuilder.setStmResultsChkBx(val);   
    }

}
