package com.mathworks.ci;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import groovy.util.GroovyTestCase;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PersistentIT {
    private final String tapFilePath = "mytap/report.tap";
    private final String pdfFilePath = "mypdf/report.pdf";
    private final String jUnitFilePath = "myjunit/report.xml";
    private final String coberturaFilePath = "mycobertura/report.xml";
    private final String modelCovFilePath = "mymodel/report.xml";
    private final String stmFilePath = "mystm/results.mldatx";
    private final List<SourceFolderPaths> paths = new ArrayList<>();

    @Rule
    public Timeout timeout = new Timeout(0, TimeUnit.MILLISECONDS);

    @Rule
    public RestartableJenkinsRule jenkins = new RestartableJenkinsRule();

    @Test
    public void verifyArtifactsPathArePersistent() throws Exception {
        jenkins.then(r->{
            FreeStyleProject project1 = r.createFreeStyleProject();
            RunMatlabTestsBuilder testBuilder1 = new RunMatlabTestsBuilder();
            UseMatlabVersionBuildWrapper buildWrapper1 = new UseMatlabVersionBuildWrapper();
            buildWrapper1.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
            project1.getBuildWrappersList().add(buildWrapper1);
            testBuilder1.setModelCoverageArtifact(new RunMatlabTestsBuilder.ModelCovArtifact("model/coverage.xml"));
            project1.getBuildersList().add(testBuilder1);

            project1.save();
            HtmlPage page = r.createWebClient().goTo("job/test0/configure");
            HtmlTextInput coberturaModelCoverageFileInput=(HtmlTextInput) page.getElementByName("_.modelCoverageFilePath");
            Assert.assertEquals("model/coverage.xml",coberturaModelCoverageFileInput.getValueAttribute());
        });

        jenkins.then(r->{
            HtmlPage page = r.createWebClient().goTo("job/test0/configure");
            HtmlTextInput coberturaModelCoverageFileInput=(HtmlTextInput) page.getElementByName("_.modelCoverageFilePath");
            Assert.assertEquals("model/coverage.xml",coberturaModelCoverageFileInput.getValueAttribute());
        });
    }
}
