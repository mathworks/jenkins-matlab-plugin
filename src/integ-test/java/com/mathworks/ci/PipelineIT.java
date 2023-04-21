package com.mathworks.ci;

import hudson.model.Result;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.*;
import org.junit.rules.Timeout;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.jvnet.hudson.test.JenkinsRule.getLog;

public class PipelineIT {
    private WorkflowJob project;
    private String envScripted;
    private String envDSL;
    // Test data to run MATLAB Tests
    private URL zipFile;

    @Rule
    public Timeout timeout = Timeout.seconds(0);

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void testSetup() throws IOException {
        this.project = jenkins.createProject(WorkflowJob.class);
        this.envDSL = MatlabRootSetup.getEnvironmentDSL();
        this.envScripted = MatlabRootSetup.getEnvironmentScriptedPipeline();
        zipFile = MatlabRootSetup.getRunMATLABTestsData();
    }

    @After
    public void testTearDown() {
        MatlabRootSetup.matlabInstDescriptor = null;
    }

    /*
     * Utility function which returns the build of the project
     */
    private WorkflowRun getPipelineBuild(String script) throws Exception{
        project.setDefinition(new CpsFlowDefinition(script,true));
        return project.scheduleBuild2(0).get();
    }

//     @Test
//     public void verifyEmptyRootError() throws Exception {
//         String script = "pipeline {\n" +
//                         "  agent any\n" +
//                         "    stages{\n" +
//                         "        stage('Run MATLAB Command') {\n" +
//                         "            steps\n" +
//                         "            {\n" +
//                         "              runMATLABCommand 'version' \n" +
//                         "            }\n" +
//                         "        }\n" +
//                         "    }\n" +
//                         "}";
//         WorkflowRun build = getPipelineBuild(script);
//         jenkins.assertLogContains("MATLAB_ROOT",build);
//         jenkins.assertBuildStatus(Result.FAILURE,build);
//     }

//    @Test
//    public void verifyWrongMatlabVersion() throws Exception {
//        project.setDefinition(new CpsFlowDefinition(
//                "node {env.PATH = \"C:\\\\Program Files\\\\MATLAB\\\\R2009a\\\\bin;${env.PATH}\" \n" +
//                        "runMATLABCommand \"version\"}",true));
//        WorkflowRun build = project.scheduleBuild2(0).get();
//        String build_log = jenkins.getLog(build);
//        jenkins.assertLogContains("No such DSL method",build);
//        jenkins.assertBuildStatus(Result.FAILURE,build);
//    }

    @Test
    public void verifyBuildPassesWhenMatlabCommandPasses() throws Exception {
        String script = "pipeline {\n" +
                "  agent any\n" +
                envDSL + "\n" +
                "    stages{\n" +
                "        stage('Run MATLAB Command') {\n" +
                "            steps\n" +
                "            {\n" +
                "              runMATLABCommand 'version' \n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    @Test
    public void verifyBuildFailsWhenMatlabCommandFails() throws Exception {
        String script = "pipeline {\n" +
                        "  agent any\n" +
                        envDSL + "\n" +
                        "    stages{\n" +
                        "        stage('Run MATLAB Command') {\n" +
                        "            steps\n" +
                        "            {\n" +
                        "              runMATLABCommand 'apple' \n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertLogContains("apple",build);
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    @Test
    public void verifyBuildFailsWhenDSLNameFails() throws Exception {
        String script = "pipeline {\n" +
                        "  agent any\n" +
                        envDSL + "\n" +
                        "    stages{\n" +
                        "        stage('Run MATLAB Command') {\n" +
                        "            steps\n" +
                        "            {\n" +
                        "              runMatlabCommand 'version' \n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertLogContains("No such DSL method",build);
        jenkins.assertBuildStatus(Result.FAILURE,build);
    }

    @Test
    public void verifyCustomeFilenamesForArtifacts() throws Exception {
        String script = "pipeline {\n" +
                            "  agent any\n" +
                            envDSL + "\n" +
                            "    stages{\n" +
                            "        stage('Run MATLAB Command') {\n" +
                            "            steps\n" +
                            "            {\n" +
                            "                unzip '" + MatlabRootSetup.getRunMATLABTestsData().getPath() + "'" + "\n" +
                            "              runMATLABTests(sourceFolder:['src'], testResultsTAP: 'test-results/results.tap',\n" +
                            "                             testResultsJUnit: 'test-results/results.xml',\n" +
                            "                             testResultsSimulinkTest: 'test-results/results.mldatx',\n" +
                            "                             codeCoverageCobertura: 'code-coverage/coverage.xml',\n" +
                            "                             modelCoverageCobertura: 'model-coverage/coverage.xml')\n" +
                            "            }\n" +
                            "        }\n" +
                            "    }\n" +
                            "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    // .pdf extension
    @Test
    public void verifyExtForPDF() throws Exception {
        Assume.assumeFalse
                (System.getProperty("os.name").toLowerCase().startsWith("mac"));
        String script = "pipeline {\n" +
                        "  agent any\n" +
                        envDSL + "\n" +
                        "    stages{\n" +
                        "        stage('Run MATLAB Command') {\n" +
                        "            steps\n" +
                        "            {\n" +
                        "              runMATLABTests(testResultsPDF:'test-results/results')\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertLogContains("File extension missing.  Expected '.pdf'", build);
        jenkins.assertBuildStatus(Result.FAILURE,build);
    }

    // invalid filename
    @Test
    public void verifyInvalidFilename() throws Exception {
        Assume.assumeTrue
                (System.getProperty("os.name").toLowerCase().startsWith("windows"));
        String script = "pipeline {\n" +
                "  agent any\n" +
                envDSL + "\n" +
                "    stages{\n" +
                "        stage('Run MATLAB Command') {\n" +
                "            steps\n" +
                "            {\n" +
                "              runMATLABTests(testResultsPDF:'abc/x?.pdf')\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertLogContains("Unable to write to file", build);
        jenkins.assertBuildStatus(Result.FAILURE,build);
    }

    // Tool config
    @Test
    public void verifyGlobalToolDSLPipeline() throws Exception {
        MatlabRootSetup.setMatlabInstallation("MATLAB_PATH_1", MatlabRootSetup.getMatlabRoot(), jenkins);
        MatlabRootSetup.setMatlabInstallation("MATLAB_PATH_2", MatlabRootSetup.getMatlabRoot().replace("R2020b", "R2020a"), jenkins);
        String script = "pipeline {\n" +
                "   agent any\n" +
                envDSL + "\n" +
                "   tools {\n" +
                "       matlab 'MATLAB_PATH_1'\n" +
                "   }\n" +
                "    stages{\n" +
                "        stage('Run MATLAB Command') {\n" +
                "            steps\n" +
                "            {\n" +
                "               runMATLABCommand 'version'\n" +
                "            }       \n" +
                "        }                \n" +
                "    } \n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }
    @Test
    public void verifyGlobalToolScriptedPipeline() throws Exception {
        MatlabRootSetup.setMatlabInstallation("MATLAB_PATH_1", MatlabRootSetup.getMatlabRoot(), jenkins);
        MatlabRootSetup.setMatlabInstallation("MATLAB_PATH_2", MatlabRootSetup.getMatlabRoot().replace("R2020b", "R2020a"), jenkins);
        String script = "node {\n" +
                "    def matlabver\n" +
                "    stage('Run MATLAB Command') {\n" +
                "        matlabver = tool 'MATLAB_PATH_1'\n" +
                "        if (isUnix()){\n" +
                "            env.PATH = \"${matlabver}/bin:${env.PATH}\"   // Linux or macOS agent\n" +
                "        }else{\n" +
                "            env.PATH = \"${matlabver}\\\\bin;${env.PATH}\"   // Windows agent\n" +
                "        }     \n" +
                "        runMATLABCommand 'version'\n" +
                "    }\n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    @Test
    public void verifyRunInParallel() throws Exception {
        String script = "pipeline {\n" +
                "  agent any\n" +
                envDSL + "\n" +
                "    stages{\n" +
                "        stage('Run MATLAB Command') {\n" +
                "            steps\n" +
                "            {\n" +
                "              runMATLABTests(useParallel:true)\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertLogContains("runInParallel", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    @Test
    public void verifyStrictSet() throws Exception {
        String script = "pipeline {\n" +
                "  agent any\n" +
                envDSL + "\n" +
                "    stages{\n" +
                "        stage('Run MATLAB Command') {\n" +
                "            steps\n" +
                "            {\n" +
                "              runMATLABTests(strict:true)\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertLogContains("FailOnWarningsPlugin", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    @Test
    public void verifyLoggingLevelSet() throws Exception {
        String script = "pipeline {\n" +
                "  agent any\n" +
                envDSL + "\n" +
                "    stages{\n" +
                "        stage('Run MATLAB Command') {\n" +
                "            steps\n" +
                "            {\n" +
                "              runMATLABTests(sourceFolder:['src'], loggingLevel:'None')\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertLogContains("'LoggingLevel', 0", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    @Test
    public void verifyOutoutDetailSet() throws Exception {
        String script = "pipeline {\n" +
                "  agent any\n" +
                envDSL + "\n" +
                "    stages{\n" +
                "        stage('Run MATLAB Command') {\n" +
                "            steps\n" +
                "            {\n" +
                "              runMATLABTests(outputDetail:'None')\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertLogContains("'OutputDetail', 0", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }
    @Test
    public void verifyBuildPassesWhenRunMatlabBuildPassesTaskProvided() throws Exception {
        String script = "pipeline {\n" +
                "  agent any\n" +
                "    stages{\n" +
                "        stage('Run MATLAB Build') {\n" +
                "            steps\n" +
                "            {\n" +
                "                unzip '" + MatlabRootSetup.getRunMATLABTestsData().getPath() + "'" + "\n" +
                "              runMATLABBuild tasks: 'test' \n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
        jenkins.assertLogContains("buildtool test",build);
    }
    @Test
    public void verifyBuildPassesWhenRunMatlabBuildPassesTaskNotProvided() throws Exception {
        String script = "pipeline {\n" +
                "  agent any\n" +
                "    stages{\n" +
                "        stage('Run MATLAB Build') {\n" +
                "            steps\n" +
                "            {\n" +
                "                unzip '" + MatlabRootSetup.getRunMATLABTestsData().getPath() + "'" + "\n" +
                "              runMATLABBuild tasks: '' \n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
        jenkins.assertLogContains("buildtool",build);
    }
}
