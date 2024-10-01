package com.mathworks.ci.tools;


import hudson.FilePath;
import hudson.tools.DownloadFromUrlInstaller.Installable;

public class MatlabInstallable extends Installable {
    public String getBatchURL() {
        return this.batchURL;
    }

    public String batchURL;
    private String osName;
    public MatlabInstallable(String osName) throws InstallationFailedException {
        this.osName = osName;
        switch (osName) {
            case "win64" :
                this.url =  "https://www.mathworks.com/mpm/win64/mpm";
                this.batchURL = "https://ssd.mathworks.com/supportfiles/ci/matlab-batch/v1/win64/matlab-batch.exe";
                break;
            case "glnxa64" :
                this.url =  "https://www.mathworks.com/mpm/glnxa64/mpm";
                this.batchURL = "https://ssd.mathworks.com/supportfiles/ci/matlab-batch/v1/glnxa64/matlab-batch";
                break;
            case "maci64" :
                this.url =  "https://www.mathworks.com/mpm/maci64/mpm";
                this.batchURL = "https://ssd.mathworks.com/supportfiles/ci/matlab-batch/v1/maci64/matlab-batch";
                break;
            default :
                throw new InstallationFailedException("Unsupported OS");
        }
    }

    public FilePath getBatchInstallable(FilePath expectedPath)  {
        if(this.osName == "win64"){
            return new FilePath(expectedPath,"matlab-batch.exe");
        }
        return new FilePath(expectedPath, "matlab-batch");
    }

    public FilePath getMpmInstallable(FilePath expectedPath)  {
        if(this.osName == "win64"){
            return new FilePath(expectedPath,"mpm.exe");
        }
        return new FilePath(expectedPath, "mpm");
    }

}
