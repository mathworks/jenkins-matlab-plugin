package com.mathworks.ci;

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

public class BuildArtifactActionTest {
    private FreeStyleProject project;
    private BuildArtifactAction buildAction;
    private UseMatlabVersionBuildWrapper buildWrapper;
    private RunMatlabBuildBuilder scriptBuilder;
    private static URL url;
    private static String FileSeperator;
    private static String VERSION_INFO_XML_FILE = "VersionInfo.xml";

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
    }

    @After
    public void testTearDown() {
        this.project = null;
        this.buildAction = null;
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
     *
     */

    @Test
    public void verifyBuildArtifactsReturned() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        BuildArtifactAction ac = new BuildArtifactAction(build,build.getWorkspace());
        copyFileInWorkspace("buildArtifacts/t1/buildArtifact.json","buildArtifact.json",new FilePath(build.getRootDir()));
        List<BuildArtifactData> ba = ac.getBuildArtifact();
        Assert.assertEquals("The build names are not matching",3,ba.size());
    }

    private void copyFileInWorkspace(String sourceFile, String targetFile, FilePath targetWorkspace)
            throws IOException, InterruptedException {
        final ClassLoader classLoader = getClass().getClassLoader();
        FilePath targetFilePath = new FilePath(targetWorkspace, targetFile);
        InputStream in = classLoader.getResourceAsStream(sourceFile);
        targetFilePath.copyFrom(in);
        // set executable permission
        targetFilePath.chmod(0755);
    }
}
