package com.mathworks.ci;

/**
 * Copyright 2024 The MathWorks, Inc.
 */

import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.mathworks.ci.freestyle.RunMatlabBuildBuilder;

public class BuildArtifactActionTest {
    private FreeStyleProject project;
    private UseMatlabVersionBuildWrapper buildWrapper;
    private RunMatlabBuildBuilder scriptBuilder;

    private static String VERSION_INFO_XML_FILE = "VersionInfo.xml";

    public BuildArtifactActionTest() {
    }

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void testSetup() throws IOException {
        this.project = jenkins.createFreeStyleProject();
        this.scriptBuilder = new RunMatlabBuildBuilder();
        this.buildWrapper = new UseMatlabVersionBuildWrapper();
    }

    @After
    public void testTearDown() {
        this.project = null;
        this.scriptBuilder = null;
    }

    private String getMatlabroot(String version) throws URISyntaxException {
        String defaultVersionInfo = "versioninfo/R2017a/" + VERSION_INFO_XML_FILE;
        String userVersionInfo = "versioninfo/" + version + "/" + VERSION_INFO_XML_FILE;
        URL matlabRootURL = Optional.ofNullable(getResource(userVersionInfo))
                .orElseGet(() -> getResource(defaultVersionInfo));
        File matlabRoot = new File(matlabRootURL.toURI());
        return matlabRoot.getAbsolutePath().replace(File.separator + VERSION_INFO_XML_FILE, "")
                .replace("R2017a", version);
    }

    private URL getResource(String resource) {
        return BuildArtifactAction.class.getClassLoader().getResource(resource);
    }

    /**
     * Verify if total BuildArtifacts returned from artifact file.
     * 5
     */

    @Test
    public void verifyBuildArtifactsReturned()
            throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final String actionID = "abc123";
        final String targetFile = "buildArtifact" + actionID + ".json";
        BuildArtifactAction ac = new BuildArtifactAction(build, actionID);
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("buildArtifacts/t1/buildArtifact.json", targetFile, artifactRoot);
        List<BuildArtifactData> ba = ac.getBuildArtifact();
        int expectedSize = ba.size();
        Assert.assertEquals("Incorrect build artifact", 3, expectedSize);
    }

    /**
     * Verify if total Failed count returned from artifact file.
     *
     */

    @Test
    public void verifyFailedCount()
            throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final String actionID = "abc123";
        final String targetFile = "buildArtifact" + actionID + ".json";
        BuildArtifactAction ac = new BuildArtifactAction(build, actionID);
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("buildArtifacts/t1/buildArtifact.json", targetFile, artifactRoot);
        List<BuildArtifactData> ba = ac.getBuildArtifact();
        boolean expectedStatus = ba.get(0).getTaskFailed();
        Assert.assertEquals("The task succeeded", false, expectedStatus);
    }

    /**
     * Verify if total skipped count returned from artifact file.
     *
     */

    @Test
    public void verifySkipCount()
            throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final String actionID = "abc123";
        final String targetFile = "buildArtifact" + actionID + ".json";
        BuildArtifactAction ac = new BuildArtifactAction(build, actionID);
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("buildArtifacts/t2/buildArtifact.json", targetFile, artifactRoot);
        List<BuildArtifactData> ba = ac.getBuildArtifact();
        Assert.assertEquals("The task is not skipped", true, ba.get(0).getTaskSkipped());
    }

    /**
     * Verify if skip reason is returned from artifact file.
     *
     */

    @Test
    public void verifySkipReasonIsAccurate()
            throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final String actionID = "abc123";
        final String targetFile = "buildArtifact" + actionID + ".json";
        BuildArtifactAction ac = new BuildArtifactAction(build, actionID);
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("buildArtifacts/t2/buildArtifact.json", targetFile, artifactRoot);
        List<BuildArtifactData> ba = ac.getBuildArtifact();
        Assert.assertEquals("The task is not skipped", true, ba.get(0).getTaskSkipped());
        Assert.assertEquals("The skip reason for skipped task is inaccurate", "user requested",
                ba.get(0).getSkipReason());
    }

    /**
     * Verify if duration returned from artifact file.
     *
     */

    @Test
    public void verifyDurationIsAccurate()
            throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final String actionID = "abc123";
        final String targetFile = "buildArtifact" + actionID + ".json";
        BuildArtifactAction ac = new BuildArtifactAction(build, actionID);
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("buildArtifacts/t2/buildArtifact.json", targetFile, artifactRoot);
        List<BuildArtifactData> ba = ac.getBuildArtifact();
        Assert.assertEquals("The task duration is not matching", "00:02:53", ba.get(0).getTaskDuration());
    }

    /**
     * Verify if Task description returned from artifact file.
     *
     */

    @Test
    public void verifyTaskDescriptionIsAccurate()
            throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final String actionID = "abc123";
        final String targetFile = "buildArtifact" + actionID + ".json";
        BuildArtifactAction ac = new BuildArtifactAction(build, actionID);
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("buildArtifacts/t2/buildArtifact.json", targetFile, artifactRoot);
        List<BuildArtifactData> ba = ac.getBuildArtifact();
        Assert.assertEquals("The task description is not matching", "Test show", ba.get(0).getTaskDescription());
    }

    /**
     * Verify if Task name returned from artifact file.
     *
     */

    @Test
    public void verifyTaskNameIsAccurate()
            throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final String actionID = "abc123";
        final String targetFile = "buildArtifact" + actionID + ".json";
        BuildArtifactAction ac = new BuildArtifactAction(build, actionID);
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("buildArtifacts/t2/buildArtifact.json", targetFile, artifactRoot);
        List<BuildArtifactData> ba = ac.getBuildArtifact();
        Assert.assertEquals("The task name is not matching", "show", ba.get(0).getTaskName());
    }

    /**
     * Verify if total count returned from artifact file.
     *
     */

    @Test
    public void verifyTotalTaskCountIsAccurate()
            throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        FilePath artifactRoot = new FilePath(build.getRootDir());
        final String actionID = "abc123";
        final String targetFile = "buildArtifact" + actionID + ".json";
        copyFileInWorkspace("buildArtifacts/t2/buildArtifact.json", targetFile, artifactRoot);
        BuildArtifactAction ac = new BuildArtifactAction(build, actionID);
        Assert.assertEquals("Total task count is not correct", 1, ac.getTotalCount());
    }

    /**
     * Verify if total count returned from artifact file.
     *
     */

    @Test
    public void verifyTotalTaskCountIsAccurate2()
            throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        FilePath artifactRoot = new FilePath(build.getRootDir());
        final String actionID = "abc123";
        final String targetFile = "buildArtifact" + actionID + ".json";
        copyFileInWorkspace("buildArtifacts/t1/buildArtifact.json", targetFile, artifactRoot);
        BuildArtifactAction ac = new BuildArtifactAction(build, actionID);
        Assert.assertEquals("Total task count is not correct", 3, ac.getTotalCount());
    }

    /**
     * Verify if total failed count returned from artifact file.
     *
     */

    @Test
    public void verifyTotalFailedTaskCountIsAccurate()
            throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        FilePath artifactRoot = new FilePath(build.getRootDir());
        final String actionID = "abc123";
        final String targetFile = "buildArtifact" + actionID + ".json";
        copyFileInWorkspace("buildArtifacts/t1/buildArtifact.json", targetFile, artifactRoot);
        BuildArtifactAction ac = new BuildArtifactAction(build, actionID);
        Assert.assertEquals("Total task count is not correct", 3, ac.getTotalCount());
        Assert.assertEquals("Total task failed count is not correct", 1, ac.getFailCount());
    }

    /**
     * Verify if total skipped count returned from artifact file.
     *
     */

    @Test
    public void verifyTotalSkipTaskCountIsAccurate()
            throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        FilePath artifactRoot = new FilePath(build.getRootDir());
        final String actionID = "abc123";
        final String targetFile = "buildArtifact" + actionID + ".json";
        copyFileInWorkspace("buildArtifacts/t1/buildArtifact.json", targetFile, artifactRoot);
        BuildArtifactAction ac = new BuildArtifactAction(build, actionID);
        Assert.assertEquals("Total task count is not correct", 3, ac.getTotalCount());
        Assert.assertEquals("Total task skip count is not correct", 1, ac.getSkipCount());
    }

    /**
     * Verify if ActionID is set correctly.
     *
     */

    @Test
    public void verifyActionIDisAppropriate()
            throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        FilePath artifactRoot = new FilePath(build.getRootDir());
        final String actionID = "abc123";
        final String targetFile = "buildArtifact" + actionID + ".json";
        copyFileInWorkspace("buildArtifacts/t1/buildArtifact.json", targetFile, artifactRoot);
        BuildArtifactAction ac = new BuildArtifactAction(build, actionID);
        Assert.assertEquals("Incorrect ActionID", actionID, ac.getActionID());
    }

    private void copyFileInWorkspace(String sourceFile, String targetFile, FilePath targetWorkspace)
            throws IOException, InterruptedException {
        final ClassLoader classLoader = getClass().getClassLoader();
        FilePath targetFilePath = new FilePath(targetWorkspace, targetFile);
        InputStream in = classLoader.getResourceAsStream(sourceFile);
        targetFilePath.copyFrom(in);
        // set executable permission
        targetFilePath.chmod(0777);
    }

    private FreeStyleBuild getFreestyleBuild() throws ExecutionException, InterruptedException, URISyntaxException {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        return build;
    }
}
