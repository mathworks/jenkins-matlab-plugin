package com.mathworks.ci;

import hudson.matrix.AxisList;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.model.Computer;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.jvnet.hudson.test.JenkinsRule.NO_PROPERTIES;


public class MatlabInstallationTest {

    private MatlabInstallation.DescriptorImpl matlabInstDescriptor;

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

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
        return  newMatlabInstallation;
    }

    private MatlabInstallation[] getMatlabInstallation(){
        return matlabInstDescriptor.getInstallations();
    }

    /*
    * Test to verify global tool configuration for MATLAB by doing a configuration round trip.
    * */
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
     * */
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
                        + " testMATLABTests(testResultsPDF:'myresult/result.pdf')}}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatusSuccess(build);
        jenkins.assertLogContains("versioninfo", build);
    }

    /*
     * Test to verify usage of MATLAB tool installation in freestyle project.
     * */
    @Test
    public void verifyInstallationInFreeStyle() throws Exception {
        URL url = MatlabInstallationTest.class.getClassLoader().getResource("versioninfo/R2018b");
        setMatlabInstallation("R2018b", new File(url.toURI()).getAbsolutePath());
        jenkins.configRoundtrip();

        FreeStyleProject fsPrj = jenkins.createFreeStyleProject();
        MatlabInstallation _inst = MatlabInstallation.getInstallation("R2018b");
        MatlabBuildWrapperContent content = new MatlabBuildWrapperContent(_inst.getName(), null);
        UseMatlabVersionBuildWrapper buildWrapper = new UseMatlabVersionBuildWrapper();
        buildWrapper.setMatlabBuildWrapperContent(content);
        fsPrj.getBuildWrappersList().add(buildWrapper);

        RunMatlabTestsBuilderTester tester = new RunMatlabTestsBuilderTester("-positive");
        fsPrj.getBuildersList().add(tester);

        FreeStyleBuild build = fsPrj.scheduleBuild2(0).get();
        // Verify correct MATLAB is invoked
        jenkins.assertLogContains(_inst.getHome(), build);
        jenkins.assertBuildStatusSuccess(build);
    }

    /*
     * Test to verify if Matrix build passes with MATLAB installation(mock MATLAB).
     */
    @Test
    public void verifyInstallationInMatrixBuild() throws Exception {
        // configure MATLAB installation
        URL url = MatlabInstallationTest.class.getClassLoader().getResource("versioninfo/R2018a");
        setMatlabInstallation("R2018a", new File(url.toURI()).getAbsolutePath());
        url = MatlabInstallationTest.class.getClassLoader().getResource("versioninfo/R2018b");
        setMatlabInstallation("R2018b", new File(url.toURI()).getAbsolutePath());
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
            jenkins.assertLogContains("versioninfo", run);
        }

        jenkins.assertLogContains("R2018a completed", build);
        jenkins.assertLogContains("R2018b completed", build);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

}
