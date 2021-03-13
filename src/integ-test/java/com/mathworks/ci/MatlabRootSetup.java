package com.mathworks.ci;

import org.jvnet.hudson.test.JenkinsRule;

import java.util.ArrayList;
import java.util.Arrays;

import static org.jvnet.hudson.test.JenkinsRule.NO_PROPERTIES;

public class MatlabRootSetup {
    static String installedPath = "", binPath = "", MATLAB_ROOT="";
    static MatlabInstallation.DescriptorImpl matlabInstDescriptor;

    public MatlabRootSetup(){
        getBinPath();
    }

    /*
     * This method returns the environment path needed to be set for DSL pipeline scripts
     */
    public static String getEnvironmentDSL() {
        getBinPath();
        String environment = "environment { \n" +
                "PATH = " + "\""+  binPath + "${PATH}"+ "\"" + "\n" +
                "}";
        return environment;
    }

    /*
     * This method returns the environment path needed to be set for Scripted pipeline
     */
    public static String getEnvironmentScriptedPipeline() {
        getBinPath();
        String environment = "";
        environment = "env.PATH =" + '"' + binPath + "${env.PATH}" + '"';
        return environment;
    }

    /*
     * This method returns the MATLAB Root needed for Free Style or Multi Config projects
     */
    public static String getMatlabRoot() {
        getBinPath();
        MATLAB_ROOT = installedPath;

        System.out.println(MATLAB_ROOT);
        return MATLAB_ROOT;
    }

    /*
     * This method returns the bin path needed for scripted pipelines based on the testing platform -- Windows or Linux
     * or Mac
     */
    public static void getBinPath(){
        if (System.getProperty("os.name").startsWith("Win")) {
            installedPath = TestData.getPropValues("matlab.windows.installed.path");
            binPath  = installedPath.replace("\\", "\\\\")  + "\\\\bin;";
        }
        else if(System.getProperty("os.name").startsWith("Linux")){
            installedPath = TestData.getPropValues("matlab.linux.installed.path");
            binPath = installedPath + "/bin:";
        }
        else {
            installedPath = TestData.getPropValues("matlab.mac.installed.path");
            binPath = installedPath + "/bin:";
        }
    }

    public static MatlabInstallation setMatlabInstallation(String name, String home, JenkinsRule jenkins) {
        if(matlabInstDescriptor == null){
            MatlabRootSetup.matlabInstDescriptor = jenkins.getInstance().getDescriptorByType(MatlabInstallation.DescriptorImpl.class);
        }
        MatlabInstallation[] prevInst = getMatlabInstallation();
        ArrayList<MatlabInstallation> newInst = new ArrayList<>(Arrays.asList(prevInst));
        MatlabInstallation newMatlabInstallation = new MatlabInstallation(name, home, NO_PROPERTIES);
        newInst.add(newMatlabInstallation);
        MatlabInstallation[] setInst = new MatlabInstallation[newInst.size()];
        matlabInstDescriptor.setInstallations(newInst.toArray(setInst));
        return  newMatlabInstallation;
    }

    public static MatlabInstallation[] getMatlabInstallation(){
        // static method to return all installations
        return MatlabInstallation.getAll();
    }
}
