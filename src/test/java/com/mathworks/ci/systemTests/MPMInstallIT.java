package com.mathworks.ci.systemTests;

import com.mathworks.ci.MatlabBuildWrapperContent;
import com.mathworks.ci.MatlabInstallation;
import com.mathworks.ci.UseMatlabVersionBuildWrapper;
import com.mathworks.ci.freestyle.RunMatlabCommandBuilder;
import com.mathworks.ci.freestyle.RunMatlabTestsBuilder;
import com.mathworks.ci.tools.MatlabInstaller;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import org.junit.*;
import org.junit.rules.Timeout;
import org.jvnet.hudson.test.JenkinsRule;
import com.mathworks.ci.MatlabInstallation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jvnet.hudson.test.JenkinsRule.NO_PROPERTIES;

public class MPMInstallIT {
    private FreeStyleProject project;
    private RunMatlabCommandBuilder scriptBuilder;
    private UseMatlabVersionBuildWrapper buildWrapper;

    @Rule
    public Timeout timeout = Timeout.seconds(0);

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

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
        MatlabRootSetup.matlabInstDescriptor = null;
        this.buildWrapper = null;
    }

    @Test
    public void verifyWindowsError() throws Exception {
        List<ToolInstaller> toolInstallers = new ArrayList<>();
        MatlabInstaller mi = new MatlabInstaller("matlab");
        mi.setRelease("R2023a");
        toolInstallers.add(mi);


//        List<? extends ToolProperty<?>> properties =

        List<ToolProperty<ToolInstallation>> gitToolProperties = new ArrayList<>();
        InstallSourceProperty installSourceProperty = new InstallSourceProperty(toolInstallers);
        gitToolProperties.add(installSourceProperty);


        MatlabInstallation.DescriptorImpl matlabInstDescriptor = jenkins.getInstance().getDescriptorByType(MatlabInstallation.DescriptorImpl.class);
        MatlabInstallation[] prevInst = getMatlabInstallation();
        ArrayList<MatlabInstallation> newInst = new ArrayList<>(Arrays.asList(prevInst));

        MatlabInstallation newMatlabInstallation = new MatlabInstallation("MATLAB1", null,gitToolProperties );
        newInst.add(newMatlabInstallation);

        MatlabInstallation[] setInst = new MatlabInstallation[newInst.size()];
        matlabInstDescriptor.setInstallations(newInst.toArray(setInst));

        MatlabBuildWrapperContent content = new MatlabBuildWrapperContent(newMatlabInstallation.getName(), null);
        buildWrapper.setMatlabBuildWrapperContent(content);
        project.getBuildWrappersList().add(buildWrapper);
        useCommandFreeStyle("version");

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        System.out.println(build.getLog());
        jenkins.assertLogContains(newMatlabInstallation.getHome(), build);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    @Test
    public void verifyMPMInstall(){

    }

    @Test
    public void verifyAddingProducts(){

    }

    @Test
    public void verifyUsingLatest(){

    }

    @Test
    public void verifyMPMInPipeline(){

    }

    @Test
    public void verifyMPMInScriptedPipeline(){

    }

    @Test
    public void verifyMPMInMatrix(){

    }

    @Test
    public void verifyMultipleMPMInsatallation(){

    }


    public static MatlabInstallation[] getMatlabInstallation(){
        // static method to return all installations
        return MatlabInstallation.getAll();
    }

    public void useCommandFreeStyle(String command) {
        scriptBuilder.setMatlabCommand(TestData.getPropValues("matlab.command"));
        project.getBuildersList().add(scriptBuilder);
    }


}
