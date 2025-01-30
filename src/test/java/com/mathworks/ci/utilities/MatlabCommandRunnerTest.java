package com.mathworks.ci.utilities;

/**
 * Copyright 2024, The MathWorks Inc.
 */

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.util.List;

import hudson.FilePath;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.TaskListener;
import hudson.slaves.WorkspaceList;
import hudson.util.ArgumentListBuilder;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.Assert;
import org.junit.runner.RunWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.CoreMatchers.containsString;

import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;
import static org.mockito.Mockito.*;

import com.mathworks.ci.MatlabExecutionException;
import com.mathworks.ci.parameters.MatlabActionParameters;

@RunWith(MockitoJUnitRunner.class)
public class MatlabCommandRunnerTest {

    @Mock
    private Launcher launcher;
    @Mock
    private ProcStarter procStarter;
    private EnvVars env;
    @Mock
    private TaskListener listener;
    @Mock
    private PrintStream logger;
    @Mock
    private MatlabActionParameters params;

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private MatlabCommandRunner runner;

    @Before
    public void initialize() throws IOException, InterruptedException {
        env = new EnvVars();

        doReturn(new FilePath(tempDir.getRoot())).when(params).getWorkspace();
        when(params.getLauncher()).thenReturn(launcher);
        when(params.getEnvVars()).thenReturn(env);
        when(params.getTaskListener()).thenReturn(listener);
        when(params.getStartupOptions()).thenReturn("");

        when(listener.getLogger()).thenReturn(logger);

        doReturn(false).when(launcher).isUnix();
        when(launcher.launch()).thenReturn(procStarter);
        when(procStarter.cmds(any(ArgumentListBuilder.class))).thenReturn(procStarter);
        when(procStarter.masks(anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(procStarter);
        when(procStarter.envs(any(EnvVars.class))).thenReturn(procStarter);
        doReturn(procStarter).when(procStarter)
                .stdout(any(OutputStream.class));
        when(procStarter.join()).thenReturn(0);
    }

    @Test
    public void validConstructorDoesNotThrow() throws IOException, InterruptedException {
        MatlabCommandRunner runner = new MatlabCommandRunner(params);
        Assert.assertNotNull(runner);
    }

    @Test
    public void constructorUsesParamsForTempFolder() throws IOException, InterruptedException {
        MatlabCommandRunner runner = new MatlabCommandRunner(params);
        verify(params, times(1)).getWorkspace();
    }

    @Test
    public void correctTempFolderLocation() throws IOException, InterruptedException {
        runner = new MatlabCommandRunner(params);
        FilePath tmp = runner.getTempFolder();

        FilePath expected = WorkspaceList.tempDir(new FilePath(tempDir.getRoot()));

        Assert.assertTrue(tmp.exists());
        Assert.assertThat(
                tmp.getRemote(),
                startsWith(expected.getRemote()));
    }

    @Test
    public void removeTempFolderDeletesContents() throws IOException, InterruptedException {
        runner = new MatlabCommandRunner(params);

        FilePath t = runner.getTempFolder();
        FilePath f = runner.copyFileToTempFolder("testcontent.txt", "target.txt");

        Assert.assertTrue(t.exists());
        Assert.assertTrue(f.exists());

        runner.removeTempFolder();

        Assert.assertFalse(t.exists());
        Assert.assertFalse(f.exists());
    }

    @Test
    public void prepareRunnerExecutableMaci() throws IOException, InterruptedException {
        runner = new MatlabCommandRunner(params);

        doReturn(true).when(launcher).isUnix();

        FilePath f = runner.prepareRunnerExecutable();

        Assert.assertTrue(f.exists());
        Assert.assertEquals(
                runner.getTempFolder().getRemote()
                        + File.separator
                        + "run-matlab-command",
                f.getRemote());
    }

    @Test
    public void prepareRunnerExecutableMaca() throws IOException, InterruptedException {
        runner = new MatlabCommandRunner(params);

        doReturn(true).when(launcher).isUnix();
        when(procStarter.stdout(any(OutputStream.class))).thenAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocation) throws IOException {
                        Object[] args = invocation.getArguments();
                        OutputStream s = (OutputStream) args[0];

                        String tag = "arm64";
                        s.write(tag.getBytes());
                        return procStarter;
                    }
                });

        FilePath f = runner.prepareRunnerExecutable();

        Assert.assertTrue(f.exists());
        Assert.assertEquals(
                runner.getTempFolder().getRemote()
                        + File.separator
                        + "run-matlab-command",
                f.getRemote());
    }

    @Test
    public void prepareRunnerExecutableLinux() throws IOException, InterruptedException {
        runner = new MatlabCommandRunner(params);

        doReturn(true).when(launcher).isUnix();
        when(procStarter.stdout(any(OutputStream.class))).thenAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocation) throws IOException {
                        Object[] args = invocation.getArguments();
                        OutputStream s = (OutputStream) args[0];

                        String tag = "Linux";
                        s.write(tag.getBytes());
                        return procStarter;
                    }
                });

        FilePath f = runner.prepareRunnerExecutable();

        Assert.assertTrue(f.exists());
        Assert.assertEquals(
                runner.getTempFolder().getRemote()
                        + File.separator
                        + "run-matlab-command",
                f.getRemote());
    }

    @Test
    public void prepareRunnerExecutableWindows() throws IOException, InterruptedException {
        runner = new MatlabCommandRunner(params);

        FilePath f = runner.prepareRunnerExecutable();

        Assert.assertTrue(f.exists());
        Assert.assertEquals(
                runner.getTempFolder().getRemote()
                        + File.separator
                        + "run-matlab-command.exe",
                f.getRemote());
    }

    @Test
    public void createFileWithContentWorks() throws IOException, InterruptedException {
        runner = new MatlabCommandRunner(params);

        String content = "I'm a $pecia1 $tri^g";
        FilePath f = runner.createFileWithContent(content);

        String expected = "cd(getenv('MW_ORIG_WORKING_FOLDER'));\n"
                + content;

        Assert.assertTrue(f.exists());
        Assert.assertThat(f.getRemote(),
                startsWith(runner.getTempFolder().getRemote()));
        Assert.assertEquals(f.readToString(), expected);
    }

    @Test
    public void copyFileFromResourcePathWorks() throws IOException, InterruptedException {
        runner = new MatlabCommandRunner(params);

        FilePath f = runner.copyFileToTempFolder("testcontent.txt", "target.txt");

        Assert.assertTrue(f.exists());
        Assert.assertThat(f.readToString(), startsWith("This has text!"));
    }

    @Test
    public void runWorksInBasicCase() throws IOException, InterruptedException, MatlabExecutionException {
        runner = new MatlabCommandRunner(params);

        String myCommand = "OBEY";
        runner.runMatlabCommand(myCommand);

        String exe = runner.getTempFolder().getRemote()
                + File.separator
                + "run-matlab-command.exe";
        String cmd = "setenv('MW_ORIG_WORKING_FOLDER', cd('"
                + runner.getTempFolder().getRemote()
                + "'));script_";

        ArgumentCaptor<ArgumentListBuilder> captor = ArgumentCaptor.forClass(ArgumentListBuilder.class);
        verify(procStarter).cmds(captor.capture());

        List<String> cmds = captor.getValue().toList();
        Assert.assertEquals(3, cmds.size());
        Assert.assertEquals(exe, cmds.get(0));
        Assert.assertThat(cmds.get(1), startsWith(cmd));
    }

    @Test
    public void runUsesWorkspaceLocationAsWD() throws IOException, InterruptedException, MatlabExecutionException {
        runner = new MatlabCommandRunner(params);

        runner.runMatlabCommand("COMMAND");

        verify(procStarter).pwd(new FilePath(tempDir.getRoot()));
    }

    @Test
    public void runWorksWithAddedEnvVars() throws IOException, InterruptedException, MatlabExecutionException {
        runner = new MatlabCommandRunner(params);

        String myCommand = "OBEY";
        runner.addEnvironmentVariable("MYVAR", "MYVALUE");
        runner.runMatlabCommand(myCommand);

        ArgumentCaptor<EnvVars> captor = ArgumentCaptor.forClass(EnvVars.class);
        verify(procStarter).envs(captor.capture());

        EnvVars cmds = captor.getValue();
        Assert.assertEquals("MYVALUE", cmds.get("MYVAR"));
    }

    @Test
    public void runShouldExpandAddedEnvVars() throws IOException, InterruptedException, MatlabExecutionException {
        runner = new MatlabCommandRunnerTester(params);

        String myCommand = "STAY";
        runner.addEnvironmentVariable("COMMAND", myCommand);
        FilePath f = runner.createFileWithContent("$COMMAND");

        Assert.assertThat(f.readToString(), containsString(myCommand));
    }

    @Test
    public void runWorksWithStartupOptions() throws IOException, InterruptedException, MatlabExecutionException {
        runner = new MatlabCommandRunner(params);

        doReturn("-nojvm -logfile mylog.log")
                .when(params).getStartupOptions();

        String myCommand = "OBEY";
        runner.runMatlabCommand(myCommand);

        ArgumentCaptor<ArgumentListBuilder> captor = ArgumentCaptor.forClass(ArgumentListBuilder.class);
        verify(procStarter).cmds(captor.capture());

        List<String> cmds = captor.getValue().toList();
        Assert.assertEquals(5, cmds.size());
        Assert.assertEquals("-nojvm", cmds.get(2));
        Assert.assertEquals("-logfile", cmds.get(3));
        Assert.assertEquals("mylog.log", cmds.get(4));
    }

    @Test
    public void runWorksWithRedirectedOutput() throws IOException, InterruptedException, MatlabExecutionException {
        OutputStream out = mock(OutputStream.class);

        runner = new MatlabCommandRunner(params);
        runner.redirectStdOut(out);

        runner.runMatlabCommand("some pig");

        ArgumentCaptor<OutputStream> captor = ArgumentCaptor.forClass(OutputStream.class);
        verify(procStarter).stdout(captor.capture());

        Assert.assertEquals(out, captor.getValue());
    }

    @Test
    public void runThrowsCorrectCodeOnError() throws IOException, InterruptedException {
        runner = new MatlabCommandRunner(params);

        doReturn(8).when(procStarter).join();

        try {
            runner.runMatlabCommand("Open sesame");
        } catch (MatlabExecutionException e) {
            Assert.assertEquals(8, e.getExitCode());
        }
    }
}
