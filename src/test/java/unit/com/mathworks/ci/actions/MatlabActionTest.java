package unit.com.mathworks.ci.actions;

/**
 * Copyright 2024, The MathWorks Inc.
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

import com.mathworks.ci.MatlabBuilderConstants;
import com.mathworks.ci.BuildArtifactAction;
import com.mathworks.ci.BuildConsoleAnnotator;
import com.mathworks.ci.MatlabExecutionException;
import com.mathworks.ci.actions.RunMatlabCommandAction;
import com.mathworks.ci.utilities.MatlabCommandRunner;
import com.mathworks.ci.parameters.CommandActionParameters;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MatlabActionTest {
    @Mock
    CommandActionParameters params;
    @Mock
    BuildConsoleAnnotator annotator;
    @Mock
    MatlabCommandRunner runner;
    @Mock
    PrintStream out;
    @Mock
    TaskListener listener;
    @Mock
    Run build;

    @Mock
    FilePath tempFolder;

    private boolean setup = false;
    private RunMatlabCommandAction action;

    // Not using @BeforeClass to avoid static fields.
    @Before
    public void init() {
        if (!setup) {
            setup = true;
            action = new RunMatlabCommandAction(runner, annotator, params);

            when(runner.getTempFolder()).thenReturn(tempFolder);
            when(tempFolder.getRemote()).thenReturn("/path/less/traveled");

            when(params.getTaskListener()).thenReturn(listener);
            when(listener.getLogger()).thenReturn(out);

            when(params.getBuild()).thenReturn(build);
        }
    }

    @Test
    public void shouldCopyPluginsToTempDirectory() throws IOException, InterruptedException, MatlabExecutionException {
        action.run();

        InOrder inOrder = inOrder(runner);

        inOrder.verify(runner)
                .copyFileToTempFolder(MatlabBuilderConstants.DEFAULT_PLUGIN, MatlabBuilderConstants.DEFAULT_PLUGIN);
        inOrder.verify(runner)
                .copyFileToTempFolder(MatlabBuilderConstants.BUILD_REPORT_PLUGIN,
                        MatlabBuilderConstants.BUILD_REPORT_PLUGIN);
        inOrder.verify(runner)
                .copyFileToTempFolder(MatlabBuilderConstants.TASK_RUN_PROGRESS_PLUGIN,
                        MatlabBuilderConstants.TASK_RUN_PROGRESS_PLUGIN);
    }

    @Test
    public void shouldOverrideDefaultBuildtoolPlugin()
            throws IOException, InterruptedException, MatlabExecutionException {
        action.run();

        verify(runner).addEnvironmentVariable(
                "MW_MATLAB_BUILDTOOL_DEFAULT_PLUGINS_FCN_OVERRIDE",
                "ciplugins.jenkins.getDefaultPlugins");
    }

    @Test
    public void shouldCopyBuildResultsToRootAndAddAction()
            throws IOException, InterruptedException, MatlabExecutionException {
        File tmp = Files.createTempDirectory("temp").toFile();
        tmp.deleteOnExit();

        File dest = Files.createTempDirectory("dest").toFile();
        dest.deleteOnExit();

        File json = new File(tmp, "buildArtifact.json");
        json.createNewFile();

        doReturn(new FilePath(tmp)).when(runner).getTempFolder();
        doReturn(dest).when(build).getRootDir();

        action.run();

        // Should have deleted original file
        assertFalse(json.exists());
        // Should have copied file to root dir
        assertTrue(new File(dest, "buildArtifact" + action.getActionID() + ".json").exists());
    }

    @Test
    public void shouldNotAddActionIfNoBuildResult() throws IOException, InterruptedException, MatlabExecutionException {
        action.run();

        verify(build, never()).addAction(any(BuildArtifactAction.class));
    }

    @Test
    public void shouldRemoveTempFolder() throws IOException, InterruptedException, MatlabExecutionException {
        action.run();

        verify(runner).removeTempFolder();
    }
}
