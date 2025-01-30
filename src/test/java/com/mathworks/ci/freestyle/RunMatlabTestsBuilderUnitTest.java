package com.mathworks.ci.freestyle;

/**
 * Copyright 2024, The MathWorks Inc.
 */

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;
import static org.mockito.Mockito.*;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Result;

import com.mathworks.ci.freestyle.options.*;
import com.mathworks.ci.MatlabExecutionException;
import com.mathworks.ci.actions.MatlabActionFactory;
import com.mathworks.ci.actions.RunMatlabTestsAction;
import com.mathworks.ci.parameters.TestActionParameters;

@RunWith(MockitoJUnitRunner.class)
public class RunMatlabTestsBuilderUnitTest {
        @Mock
        MatlabActionFactory factory;

        @Mock
        RunMatlabTestsAction action;

        @Mock
        Run build;

        @Mock
        Launcher launcher;

        @Mock
        TaskListener listener;

        @Mock
        FilePath workspace;

        @Before
        public void setup() throws IOException, InterruptedException {
                doReturn(action).when(factory).createAction(any(TestActionParameters.class));
        }

        @Test
        public void shouldHandleNullCases() throws IOException, InterruptedException, MatlabExecutionException {
                RunMatlabTestsBuilder builder = new RunMatlabTestsBuilder(factory);

                builder.perform(build, workspace, launcher, listener);

                ArgumentCaptor<TestActionParameters> captor = ArgumentCaptor.forClass(TestActionParameters.class);
                verify(factory).createAction(captor.capture());

                TestActionParameters actual = captor.getValue();

                assertEquals("", actual.getStartupOptions());
                assertEquals(null, actual.getTestResultsPDF());
                assertEquals(null, actual.getTestResultsTAP());
                assertEquals(null, actual.getTestResultsJUnit());
                assertEquals(null, actual.getCodeCoverageCobertura());
                assertEquals(null, actual.getTestResultsSimulinkTest());
                assertEquals(null, actual.getModelCoverageCobertura());
                assertEquals(null, actual.getSelectByTag());
                assertEquals(null, actual.getLoggingLevel());
                assertEquals(null, actual.getOutputDetail());
                assertEquals("false", actual.getUseParallel());
                assertEquals("false", actual.getStrict());
                assertEquals(null, actual.getSourceFolder());
                assertEquals(null, actual.getSelectByFolder());
                verify(action).run();
        }

        @Test
        public void shouldHandleMaximalCases() throws IOException, InterruptedException, MatlabExecutionException {
                RunMatlabTestsBuilder builder = new RunMatlabTestsBuilder(factory);

                ArrayList<SourceFolderPaths> source = new ArrayList<SourceFolderPaths>();
                source.add(new SourceFolderPaths("toolbox"));
                source.add(new SourceFolderPaths("src"));

                ArrayList<TestFolders> select = new ArrayList<TestFolders>();
                select.add(new TestFolders("toolbox"));
                select.add(new TestFolders("src"));

                builder.setStartupOptions(new StartupOptions("-nojvm -logfile mylog"));
                builder.setPdfReportArtifact(
                                new RunMatlabTestsBuilder.PdfArtifact("pdf.pdf"));
                builder.setTapArtifact(
                                new RunMatlabTestsBuilder.TapArtifact("tap.tap"));
                builder.setJunitArtifact(
                                new RunMatlabTestsBuilder.JunitArtifact("results.xml"));
                builder.setCoberturaArtifact(
                                new RunMatlabTestsBuilder.CoberturaArtifact("cov.xml"));
                builder.setStmResultsArtifact(
                                new RunMatlabTestsBuilder.StmResultsArtifact("res.sltest"));
                builder.setModelCoverageArtifact(
                                new RunMatlabTestsBuilder.ModelCovArtifact("cov.model"));
                builder.setSelectByTag(
                                new RunMatlabTestsBuilder.SelectByTag("MyTag"));
                builder.setSourceFolder(
                                new SourceFolder(source));
                builder.setSelectByFolder(
                                new SelectByFolder(select));
                builder.setLoggingLevel("Concise");
                builder.setOutputDetail("Concise");
                builder.setUseParallel(true);
                builder.setStrict(true);

                builder.perform(build, workspace, launcher, listener);

                ArgumentCaptor<TestActionParameters> captor = ArgumentCaptor.forClass(TestActionParameters.class);
                verify(factory).createAction(captor.capture());

                TestActionParameters actual = captor.getValue();

                assertEquals("-nojvm -logfile mylog", actual.getStartupOptions());
                assertEquals("pdf.pdf", actual.getTestResultsPDF());
                assertEquals("tap.tap", actual.getTestResultsTAP());
                assertEquals("results.xml", actual.getTestResultsJUnit());
                assertEquals("cov.xml", actual.getCodeCoverageCobertura());
                assertEquals("res.sltest", actual.getTestResultsSimulinkTest());
                assertEquals("cov.model", actual.getModelCoverageCobertura());
                assertEquals("MyTag", actual.getSelectByTag());
                assertEquals("Concise", actual.getLoggingLevel());
                assertEquals("Concise", actual.getOutputDetail());
                assertEquals("true", actual.getUseParallel());
                assertEquals("true", actual.getStrict());
                assertEquals(2, actual.getSourceFolder().size());
                assertEquals(2, actual.getSelectByFolder().size());
                verify(action).run();
        }

        @Test
        public void shouldMarkFailureWhenActionFails()
                        throws IOException, InterruptedException, MatlabExecutionException {
                RunMatlabTestsBuilder builder = new RunMatlabTestsBuilder(factory);

                doThrow(new MatlabExecutionException(12)).when(action).run();

                builder.perform(build, workspace, launcher, listener);

                verify(build).setResult(Result.FAILURE);
        }
}
