package com.mathworks.ci;

/**
 * Copyright 2020-2024 The MathWorks, Inc.
 */

import hudson.matrix.AxisList;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.jvnet.hudson.test.JenkinsRule.NO_PROPERTIES;

import com.mathworks.ci.freestyle.RunMatlabTestsBuilder;

public class MatlabInstallationTest {

    private MatlabInstallation.DescriptorImpl matlabInstDescriptor;
    private static URL url;
    private static String FileSeperator;
    private static String VERSION_INFO_XML_FILE = "VersionInfo.xml";
    private static String matlabExecutorAbsolutePath;

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @BeforeClass
    public static void classSetup() throws URISyntaxException, IOException {
        ClassLoader classLoader = RunMatlabTestsBuilderTest.class.getClassLoader();
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
    }

    @Before
    public void testSetup() {
        this.matlabInstDescriptor = jenkins.getInstance().getDescriptorByType(MatlabInstallation.DescriptorImpl.class);
    }

    @After
    public void testTearDown() {
        this.matlabInstDescriptor = null;
    }

    private MatlabInstallation setMatlabInstallation(String name, String home) {
        MatlabInstallation[] prevInst = getMatlabInstallation();
        ArrayList<MatlabInstallation> newInst = new ArrayList<>(Arrays.asList(prevInst));
        MatlabInstallation newMatlabInstallation = new MatlabInstallation(name, home, NO_PROPERTIES);
        newInst.add(newMatlabInstallation);
        MatlabInstallation[] setInst = new MatlabInstallation[newInst.size()];
        matlabInstDescriptor.setInstallations(newInst.toArray(setInst));
        return newMatlabInstallation;
    }

    private MatlabInstallation[] getMatlabInstallation() {
        // static method to return all installations
        return MatlabInstallation.getAll();
    }

    /*
     * Test to verify global tool configuration for MATLAB by doing a configuration
     * round trip.
     */
    @Test
    public void verifyRoundTripInstallation() throws Exception {
        MatlabInstallation matlabInst = setMatlabInstallation("R2019b", "C:\\FakePath\\MATLAB\\R2019b");
        MatlabInstallation matlabInst2 = setMatlabInstallation("R2020a", "/fakePath/matlab/R2020a");
        ArrayList<MatlabInstallation> instArr = new ArrayList<>(Arrays.asList(matlabInst, matlabInst2));

        jenkins.configRoundtrip();

        MatlabInstallation[] configuredMatlab = getMatlabInstallation();
        MatlabInstallation[] expectedMatlab = new MatlabInstallation[instArr.size()];
        assertEquals(2, configuredMatlab.length);
        assertArrayEquals(configuredMatlab, instArr.toArray(expectedMatlab));
    }

    /*
     * Test to verify usage of MATLAB tool installation in pipeline project.
     */
    @Test
    public void verifyInstallationInPipeline() throws Exception {
        URL url = MatlabInstallationTest.class.getClassLoader().getResource("versioninfo/R2018b");
        setMatlabInstallation("R2018b", new File(url.toURI()).getAbsolutePath());
        jenkins.configRoundtrip();
        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition("node { \n"
                + " def matlabroot \n"
                + " matlabroot = tool 'R2018b' \n"
                + " withEnv([\"PATH+MATLAB=$matlabroot/bin\"]) { \n"
                + " echo env.PATH \n"
                + " runMATLABTests(testResultsPDF:'myresult/result.pdf')}}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("versioninfo", build);
        jenkins.assertLogContains("2018b", build);
        jenkins.assertLogContains("bin", build);
    }

    /*
     * Test to verify usage of MATLAB tool installation in freestyle project.
     */
    @Test
    public void verifyInstallationInFreeStyle() throws Exception {
        URL url = MatlabInstallationTest.class.getClassLoader().getResource("versioninfo" + FileSeperator + "R2018a");
        setMatlabInstallation("R2018b", new File(url.toURI()).getAbsolutePath());
        jenkins.configRoundtrip();

        FreeStyleProject fsPrj = jenkins.createFreeStyleProject();
        MatlabInstallation _inst = MatlabInstallation.getInstallation("R2018b");
        MatlabBuildWrapperContent content = new MatlabBuildWrapperContent(_inst.getName(), null);
        UseMatlabVersionBuildWrapper buildWrapper = new UseMatlabVersionBuildWrapper();
        buildWrapper.setMatlabBuildWrapperContent(content);
        fsPrj.getBuildWrappersList().add(buildWrapper);

        FreeStyleBuild build = fsPrj.scheduleBuild2(0).get();
        // Verify correct MATLAB is invoked
        jenkins.assertLogContains(_inst.getHome(), build);
    }

    /*
     * Test to verify if Matrix build passes with MATLAB installation(mock MATLAB).
     */
    @Test
    public void verifyInstallationInMatrixBuild() throws Exception {
        // configure MATLAB installation
        setMatlabInstallation("R2018a", matlabExecutorAbsolutePath);
        setMatlabInstallation("R2018b", matlabExecutorAbsolutePath);
        jenkins.configRoundtrip();

        // configure multi-config project
        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class);
        List<String> val = new ArrayList<>(Arrays.asList("R2018a", "R2018b"));
        MatlabInstallationAxis axes = new MatlabInstallationAxis(val);
        matrixProject.setAxes(new AxisList(axes));

        RunMatlabTestsBuilderTester tester = new RunMatlabTestsBuilderTester("-positive");
        matrixProject.getBuildersList().add(tester);

        MatrixBuild build = matrixProject.scheduleBuild2(0).get();
        List<MatrixRun> runs = build.getRuns();
        for (MatrixRun run : runs) {
            String matlabName = run.getBuildVariables().get("MATLAB");
            Assert.assertTrue(matlabName.equalsIgnoreCase("R2018a") || matlabName.equalsIgnoreCase("R2018b"));
        }

        jenkins.assertLogContains("R2018a completed", build);
        jenkins.assertLogContains("R2018b completed", build);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    /*
     * @Integ Test
     * Paths should point to MATLAB executable
     * Test to verify correct MATLAB installation is added to PATH environment
     * variable
     */

    public void verifyInstallationPathVarInMatrixBuild() throws Exception {
        // configure MATLAB installation
        setMatlabInstallation("R2018a", "<Path to MATLAB ver R2018a>");
        setMatlabInstallation("R2018b", "<Path to MATLAB ver R2018b>");
        jenkins.configRoundtrip();

        // configure multi-config project
        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class);
        List<String> val = new ArrayList<>(Arrays.asList("R2018a", "R2018b"));
        MatlabInstallationAxis axes = new MatlabInstallationAxis(val);
        matrixProject.setAxes(new AxisList(axes));

        RunMatlabTestsBuilder tester = new RunMatlabTestsBuilder();
        matrixProject.getBuildersList().add(tester);

        MatrixBuild build = matrixProject.scheduleBuild2(0).get();
        List<MatrixRun> runs = build.getRuns();
        for (MatrixRun run : runs) {
            String matlabName = run.getBuildVariables().get("MATLAB");
            Assert.assertTrue(matlabName.equalsIgnoreCase("R2018a") || matlabName.equalsIgnoreCase("R2018b"));
            // Verify correct MATLAB is added to path and is printed in logs.
            jenkins.assertLogContains(MatlabInstallation.getInstallation(matlabName).getHome(), run);
        }

        jenkins.assertLogContains("R2018a completed", build);
        jenkins.assertLogContains("R2018b completed", build);
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }
}
