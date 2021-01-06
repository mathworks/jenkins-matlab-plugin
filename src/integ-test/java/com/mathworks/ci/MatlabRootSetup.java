package com.mathworks.ci;

public class MatlabRootSetup {
    static String installedPath = "", binPath = "", MATLAB_ROOT="";
    public MatlabRootSetup(){
        getBinPath();
    }
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

    /*
     * This method returns the environment path needed to be set for pipeline scripts
     */
    public static String getEnvironmentDSL() {
        String environment = "environment { \n" +
                "PATH = " + "\""+  binPath + "${PATH}"+ "\"" + "\n" +
                "}";
        return environment;
    }

    public static String getEnvironmentScriptedPipeline() {
        String environment = "";
        environment = "env.PATH =" + '"' + binPath + "${env.PATH}" + '"';
        return environment;
    }

    public static String getMatlabRoot() {
        MATLAB_ROOT = installedPath;
        System.out.println(MATLAB_ROOT);
        return MATLAB_ROOT;
    }


}
