package com.mathworks.ci.systemTests;

import com.mathworks.ci.MatlabInstallation;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import static org.jvnet.hudson.test.JenkinsRule.NO_PROPERTIES;

public class Utilities {
    public static MatlabInstallation.DescriptorImpl matlabInstDescriptor;

    /*
     * This method returns the environment path needed to be set for DSL pipeline scripts
     */
    public static String getEnvironmentDSL() {
        String environment = "environment { \n" +
                "PATH = " + "\""+  getBinPath() + "${PATH}"+ "\"" + "\n" +
                "}";
        return environment;
    }

    /*
     * This method returns the environment path needed to be set for Scripted pipeline
     */
    public static String getEnvironmentScriptedPipeline() {
        String environment = "env.PATH =" + '"' + getBinPath() + "${env.PATH}" + '"';
        return environment;
    }

    /*
     * This method returns the MATLAB Root needed for Free Style or Multi Config projects
     */
    public static String getMatlabRoot() {
        return System.getenv("MATLAB_ROOT");
    }

    /*
     * This method returns the bin path needed for scripted pipelines based on the testing platform -- Windows or Linux
     * or Mac
     */
    public static String getBinPath() {
        String installedPath = System.getenv("MATLAB_ROOT");
        String binPath = installedPath + "/bin:";

        if (System.getProperty("os.name").startsWith("Win")) {
            binPath = installedPath.replace("\\", "\\\\")+ "\\\\bin;";
        }
        return binPath;
    }

    public static void setMatlabInstallation(String name, String home, JenkinsRule jenkins) {
        if(matlabInstDescriptor == null){
            Utilities.matlabInstDescriptor = jenkins.getInstance().getDescriptorByType(MatlabInstallation.DescriptorImpl.class);
        }
        MatlabInstallation[] prevInst = getMatlabInstallation();
        ArrayList<MatlabInstallation> newInst = new ArrayList<>(Arrays.asList(prevInst));
        MatlabInstallation newMatlabInstallation = new MatlabInstallation(name, home, NO_PROPERTIES);
        newInst.add(newMatlabInstallation);
        MatlabInstallation[] setInst = new MatlabInstallation[newInst.size()];
        matlabInstDescriptor.setInstallations(newInst.toArray(setInst));
    }

    public static MatlabInstallation[] getMatlabInstallation(){
        // static method to return all installations
        return MatlabInstallation.getAll();
    }

    public static URL getRunMATLABTestsData() throws MalformedURLException {
        File file = new File(System.getProperty("user.dir") + File.separator +"src" + File.separator + "test" + File.separator + "resources" + File.separator + "TestData" + File.separator + "FilterTestData.zip");
        return file.toURI().toURL();
    }
}
