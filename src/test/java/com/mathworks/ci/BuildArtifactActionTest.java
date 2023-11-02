package com.mathworks.ci;

import static java.nio.file.Files.deleteIfExists;

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
import org.apache.commons.io.FileUtils;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TemporaryDirectoryAllocator;

public class BuildArtifactActionTest {
    private FreeStyleProject project;
    private BuildArtifactAction buildAction;
    private UseMatlabVersionBuildWrapper buildWrapper;
    private RunMatlabBuildBuilder scriptBuilder;
    private static URL url;
    private static String FileSeperator;
    private static String VERSION_INFO_XML_FILE = "VersionInfo.xml";
    private TemporaryDirectoryAllocator tempd;

    public BuildArtifactActionTest(){
    }

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();


    @Before
    public void testSetup() throws IOException {
        this.project = jenkins.createFreeStyleProject();
        this.buildAction = new BuildArtifactAction();
        this.scriptBuilder = new RunMatlabBuildBuilder();
        this.buildWrapper = new UseMatlabVersionBuildWrapper();
        this.tempd = new TemporaryDirectoryAllocator();
    }

    @After
    public void testTearDown() {
        this.project = null;
        this.buildAction = null;
        this.scriptBuilder = null;
        this.tempd.disposeAsync();
    }

    private String getMatlabroot(String version) throws URISyntaxException {
        String defaultVersionInfo = "versioninfo/R2017a/" + VERSION_INFO_XML_FILE;
        String userVersionInfo = "versioninfo/" + version + "/" + VERSION_INFO_XML_FILE;
        URL matlabRootURL = Optional.ofNullable(getResource(userVersionInfo))
                .orElseGet(() -> getResource(defaultVersionInfo));
        File matlabRoot = new File(matlabRootURL.toURI());
        return matlabRoot.getAbsolutePath().replace(FileSeperator + VERSION_INFO_XML_FILE, "")
                .replace("R2017a", version);
    }

    private URL getResource(String resource) {
        return BuildArtifactAction.class.getClassLoader().getResource(resource);
    }

    /**
     *  Verify if total BuildArtifacts returned from artifact file.
     *5
     */

    @Test
    public void verifyBuildArtifactsReturned() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.setDisplayName("mutest");
        scriptBuilder.setTasks("");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        BuildArtifactAction ac = new BuildArtifactAction(build,build.getWorkspace());
        FilePath artifactRoot = new FilePath(build.getWorkspace(), "/.matlab");
        copyFileInWorkspace("buildArtifacts/t1/buildArtifact.json","buildArtifact.json",artifactRoot);
        List<BuildArtifactData> ba = ac.getBuildArtifact();
        int expectedSize = ba.size();
        Assert.assertEquals("The build names are not matching",3,expectedSize);
        delfile(build.getRootDir());
    }

    /**
     *  Verify if total Failed count returned from artifact file.
     *
     */

    @Test
    public void verifyFailedCount() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.setDisplayName("mutest");
        scriptBuilder.setTasks("");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        BuildArtifactAction ac = new BuildArtifactAction(build,build.getWorkspace());
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("buildArtifacts/t1/buildArtifact.json","buildArtifact.json",artifactRoot);
        List<BuildArtifactData> ba = ac.getBuildArtifact();
        String expectedStatus = ba.get(0).getTaskStatus();
        Assert.assertEquals("The task is passed","false",expectedStatus);
        delfile(build.getRootDir());
    }

    /**
     *  Verify if total skipped count returned from artifact file.
     *
     */

    @Test
    public void verifySkipCount() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        BuildArtifactAction ac = new BuildArtifactAction(build,build.getWorkspace());
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("buildArtifacts.t2/buildArtifact.json","buildArtifact.json",artifactRoot);
        List<BuildArtifactData> ba = ac.getBuildArtifact();
        Assert.assertEquals("The task is not skipped","true",ba.get(0).getTaskSkipped());
        delfile(build.getRootDir());
    }

    /**
     *  Verify if duration returned from artifact file.
     *
     */

    @Test
    public void verifyDurationIsAccurate() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        BuildArtifactAction ac = new BuildArtifactAction(build,build.getWorkspace());
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("buildArtifacts.t2/buildArtifact.json","buildArtifact.json",artifactRoot);
        List<BuildArtifactData> ba = ac.getBuildArtifact();
        Assert.assertEquals("The task duration is not matching","00:02:53",ba.get(0).getTaskDuration());
        delfile(build.getRootDir());
    }

    /**
     *  Verify if Task description returned from artifact file.
     *
     */

    @Test
    public void verifyTaskDescriptionIsAccurate() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        BuildArtifactAction ac = new BuildArtifactAction(build,build.getWorkspace());
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("buildArtifacts.t2/buildArtifact.json","buildArtifact.json",artifactRoot);
        List<BuildArtifactData> ba = ac.getBuildArtifact();
        Assert.assertEquals("The task description is not matching","Test show",ba.get(0).getDescription());
        delfile(build.getRootDir());
    }

    /**
     *  Verify if Task name returned from artifact file.
     *
     */

    @Test
    public void verifyTaskNameIsAccurate() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        BuildArtifactAction ac = new BuildArtifactAction(build,build.getWorkspace());
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("buildArtifacts.t2/buildArtifact.json","buildArtifact.json",artifactRoot);
        List<BuildArtifactData> ba = ac.getBuildArtifact();
        Assert.assertEquals("The task name is not matching","show",ba.get(0).getTaskName());
        delfile(build.getRootDir());
    }

    /**
     *  Verify if total count returned from artifact file.
     *
     */

    @Test
    public void verifyTotalTaskCountIsAccurate() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        BuildArtifactAction ac = new BuildArtifactAction(build,build.getWorkspace());
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("buildArtifacts.t2/buildArtifact.json","buildArtifact.json",artifactRoot);
        Assert.assertEquals("Total task count is not correct",1,ac.getTotalCount());
        delfile(build.getRootDir());
    }

    /**
     *  Verify if total count returned from artifact file.
     *
     */

    @Test
    public void verifyTotalTaskCountIsAccurate2() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        BuildArtifactAction ac = new BuildArtifactAction(build,build.getWorkspace());
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("buildArtifacts/t1/buildArtifact.json","buildArtifact.json",artifactRoot);
        Assert.assertEquals("Total task count is not correct",3,ac.getTotalCount());
        delfile(build.getRootDir());
    }

    /**
     *  Verify if total failed count returned from artifact file.
     *
     */

    @Test
    public void verifyTotalFailedTaskCountIsAccurate() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        BuildArtifactAction ac = new BuildArtifactAction(build,build.getWorkspace());
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("buildArtifacts/t1/buildArtifact.json","buildArtifact.json",artifactRoot);
        Assert.assertEquals("Total task count is not correct",3,ac.getTotalCount());
        Assert.assertEquals("Total task failed count is not correct",1,ac.getFailCount());
        deleteArtifact(artifactRoot);
        delfile(build.getRootDir());
    }
    /**
     *  Verify if total skipped count returned from artifact file.
     *
     */

    @Test
    public void verifyTotalSkipTaskCountIsAccurate() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        BuildArtifactAction ac = new BuildArtifactAction(build,build.getWorkspace());
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("buildArtifacts/t1/buildArtifact.json","buildArtifact.json",artifactRoot);
        Assert.assertEquals("Total task count is not correct",3,ac.getTotalCount());
        Assert.assertEquals("Total task skip count is not correct",1,ac.getSkipCount());
        delfile(build.getRootDir());
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

    private void deleteArtifact(FilePath path) throws IOException, InterruptedException {
        FilePath af = new FilePath(path,"buildArtifact.json");
        if(af.exists()){
            System.out.println("***** IN ****");
            if(af.delete()){
                System.out.println("$$$$$ FILE DELETED $$$$$");
            }
        }
    }
    private void delfile(File path) throws IOException {
        File fl = new File(path.getAbsolutePath(),"buildArtifact.json");
        if(fl.exists()){
            FileUtils.forceDelete(fl);
            System.out.println("***** IN ****");
            //fl.deleteOnExit();
        }
    }
}
