package com.mathworks.ci;

/**
 * Copyright 2020-2024 The MathWorks, Inc.
 *
 * Test class for RunMatlabTestsBuilder Persistence
 */

import hudson.model.FreeStyleProject;
import hudson.model.Item;
import org.junit.*;
import org.jvnet.hudson.test.RestartableJenkinsRule;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

import com.mathworks.ci.freestyle.RunMatlabTestsBuilder;
import com.mathworks.ci.freestyle.options.*;

public class RunMatlabTestBuilderPersistenceTest {
    private final String tapFilePath = "mytap/report.tap";
    private final String pdfFilePath = "mypdf/report.pdf";
    private final String jUnitFilePath = "myjunit/report.xml";
    private final String coberturaFilePath = "mycobertura/report.xml";
    private final String modelCovFilePath = "mymodel/report.xml";
    private final String stmFilePath = "mystm/results.mldatx";
    private final List<SourceFolderPaths> paths = new ArrayList<>();

    @Rule
    public RestartableJenkinsRule jenkins = new RestartableJenkinsRule();

    private boolean areSourcePathsEqual(List<SourceFolderPaths> listA, List<SourceFolderPaths> listB) {
        return listA.stream()
                .map(SourceFolderPaths::getSrcFolderPath)
                .collect(Collectors.toList())
                .equals(listB.stream()
                        .map(SourceFolderPaths::getSrcFolderPath)
                        .collect(Collectors.toList()));
    }

    /*
     * Test to verify artifacts are correctly restored after Jenkins restarts.
     */

    @Test
    public void verifyArtifactSpecPersistAfterRestart() {
        jenkins.then(r -> {
            FreeStyleProject project = r.createFreeStyleProject();
            RunMatlabTestsBuilder testBuilder = new RunMatlabTestsBuilder();

            RunMatlabTestsBuilder.TapArtifact tap = new RunMatlabTestsBuilder.TapArtifact(tapFilePath);
            RunMatlabTestsBuilder.PdfArtifact pdf = new RunMatlabTestsBuilder.PdfArtifact(pdfFilePath);
            RunMatlabTestsBuilder.JunitArtifact junit = new RunMatlabTestsBuilder.JunitArtifact(jUnitFilePath);
            RunMatlabTestsBuilder.CoberturaArtifact cobertura = new RunMatlabTestsBuilder.CoberturaArtifact(
                    coberturaFilePath);
            RunMatlabTestsBuilder.ModelCovArtifact modelCov = new RunMatlabTestsBuilder.ModelCovArtifact(
                    modelCovFilePath);
            RunMatlabTestsBuilder.StmResultsArtifact stmResults = new RunMatlabTestsBuilder.StmResultsArtifact(
                    stmFilePath);

            testBuilder.setTapArtifact(tap);
            testBuilder.setPdfReportArtifact(pdf);
            testBuilder.setJunitArtifact(junit);
            testBuilder.setCoberturaArtifact(cobertura);
            testBuilder.setModelCoverageArtifact(modelCov);
            testBuilder.setStmResultsArtifact(stmResults);

            project.getBuildersList().add(testBuilder);
            project.save();
        });

        jenkins.then(r -> {
            // Make sure there's only one project
            List<Item> items = r.getInstance().getAllItems();
            assertEquals(items.size(), 1);

            FreeStyleProject p = (FreeStyleProject) items.get(0);
            RunMatlabTestsBuilder savedInstance = p.getBuildersList().get(RunMatlabTestsBuilder.class);

            // Verify artifacts are not NullArtifact instances and verify saved path values
            assertTrue(savedInstance.getTapArtifact() instanceof RunMatlabTestsBuilder.TapArtifact);
            assertTrue(savedInstance.getPdfReportArtifact() instanceof RunMatlabTestsBuilder.PdfArtifact);
            assertTrue(savedInstance.getJunitArtifact() instanceof RunMatlabTestsBuilder.JunitArtifact);
            assertTrue(savedInstance.getCoberturaArtifact() instanceof RunMatlabTestsBuilder.CoberturaArtifact);
            assertTrue(savedInstance.getModelCoverageArtifact() instanceof RunMatlabTestsBuilder.ModelCovArtifact);
            assertTrue(savedInstance.getStmResultsArtifact() instanceof RunMatlabTestsBuilder.StmResultsArtifact);

            assertEquals(savedInstance.getTapReportFilePath(), tapFilePath);
            assertEquals(savedInstance.getPdfReportFilePath(), pdfFilePath);
            assertEquals(savedInstance.getJunitReportFilePath(), jUnitFilePath);
            assertEquals(savedInstance.getCoberturaReportFilePath(), coberturaFilePath);
            assertEquals(savedInstance.getModelCoverageFilePath(), modelCovFilePath);
            assertEquals(savedInstance.getStmResultsFilePath(), stmFilePath);
        });
    }

    /*
     * Test to verify Source Folder specification is correctly restored after
     * Jenkins restarts
     */

    @Test
    public void verifySourceFolderSpecPersistence() {
        jenkins.then(r -> {
            paths.add(new SourceFolderPaths("src/A"));
            paths.add(new SourceFolderPaths("src/B"));

            FreeStyleProject project = r.createFreeStyleProject();
            RunMatlabTestsBuilder testBuilder = new RunMatlabTestsBuilder();
            SourceFolder sf = new SourceFolder(paths);
            testBuilder.setSourceFolder(sf);
            project.getBuildersList().add(testBuilder);
            project.save();
        });

        jenkins.then(r -> {
            // Make sure there's only one project
            List<Item> items = r.getInstance().getAllItems();
            assertEquals(items.size(), 1);

            FreeStyleProject project = (FreeStyleProject) items.get(0);
            // Compare sourceFolder values
            RunMatlabTestsBuilder saveInstance = project.getBuildersList().get(RunMatlabTestsBuilder.class);
            assertNotNull(saveInstance.getSourceFolder());
            List<SourceFolderPaths> savedList = saveInstance.getSourceFolder().getSourceFolderPaths();

            assertTrue(areSourcePathsEqual(paths, savedList));
        });
    }
}
