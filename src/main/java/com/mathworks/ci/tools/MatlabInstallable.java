package com.mathworks.ci.tools;
/**
 * Copyright 2024, The MathWorks, Inc.
 *
 */


import com.mathworks.ci.Message;
import hudson.FilePath;
import hudson.tools.DownloadFromUrlInstaller.Installable;

public class MatlabInstallable extends Installable {

    public String batchURL;
    private String osName;
    public MatlabInstallable (String osName) throws InstallationFailedException {
        this.osName = osName;
        switch (osName) {
            case "win64":
                this.url = Message.getValue ("tools.matlab.mpm.installer.win");
                this.batchURL = Message.getValue ("tools.matlab.batch.executable.win");
                break;
            case "glnxa64":
                this.url = Message.getValue ("tools.matlab.mpm.installer.linux");
                this.batchURL = Message.getValue ("tools.matlab.batch.executable.linux");
                break;
            case "maci64":
                this.url = Message.getValue ("tools.matlab.mpm.installer.maci64");
                this.batchURL = Message.getValue ("tools.matlab.batch.executable.maci64");
                break;
            case "maca64":
                this.url = Message.getValue ("tools.matlab.mpm.installer.maca64");
                this.batchURL = Message.getValue ("tools.matlab.batch.executable.maca64");
                break;
            default:
                throw new InstallationFailedException ("Unsupported OS");
        }
    }

    public String getBatchURL () {
        return this.batchURL;
    }

    public FilePath getBatchInstallable (FilePath expectedPath) {
        if (this.osName == "win64") {
            return new FilePath (expectedPath, "matlab-batch.exe");
        }
        return new FilePath (expectedPath, "matlab-batch");
    }

    public FilePath getMpmInstallable (FilePath expectedPath) {
        if (this.osName == "win64") {
            return new FilePath (expectedPath, "mpm.exe");
        }
        return new FilePath (expectedPath, "mpm");
    }
}
