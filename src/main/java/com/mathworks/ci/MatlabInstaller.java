package com.mathworks.ci;

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.AbstractCommandInstaller;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.Messages;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallerDescriptor;
import hudson.util.ArgumentListBuilder;
import hudson.util.VersionNumber;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import jenkins.security.MasterToSlaveCallable;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class MatlabInstaller extends DownloadFromUrlInstaller {
    private String version;
    private String products;

    @DataBoundConstructor
    public MatlabInstaller(String id) {
        super(id);
    }


    public String getVersion(){
        return this.version;
    }
    public String getProducts() { return this.products; }

    @DataBoundSetter
    public void setVersion(String version) {
        this.version = version;
    }

    @DataBoundSetter
    public void setProducts(String products) {
        this.products = products;
    }

    @Extension
    public static final class DescriptorImpl extends ToolInstallerDescriptor<MatlabInstaller> {
        public String getDisplayName() {
            return "Install MATLAB";
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType == MatlabInstallation.class;
        }
    }


    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        FilePath expectedPath = preferredLocation(tool, node);

        MatlabInstallable installable;
        try {
            installable = (MatlabInstallable) getInstallable(node);
            log.getLogger().println("CAME HERE");
        } catch (InstallationFailedException e) {
            throw new InstallationFailedException(e.getMessage());
        }

        if (installable == null) {
            log.getLogger().println("Unrecognized");
            return expectedPath;
        }

        if (isUpToDate(expectedPath, installable)) {
            return expectedPath;
        }

        String message = "mpmurl";
        log.getLogger().println("URL ISSS" + new URL(installable.url).toString());

        if (expectedPath.exists()) {
            log.getLogger().println("PATH EXISTS NOT installing " + expectedPath.getRemote());
        } else {

            log.getLogger().println("INSIDE else");

            FilePath mpmPath = installable.getMpmInstallable(expectedPath);
            FilePath mbatchPath = installable.getBatchInstallable(expectedPath);
            FilePath destinatioFolder = new FilePath(node.getChannel(), this.getVersion());
            mpmPath.copyFrom(new URL(installable.url));
            mpmPath.chmod(0777);
            mbatchPath.copyFrom(new URL(installable.batchURL));
            mbatchPath.chmod(0777);

            if(!destinatioFolder.exists()){
                destinatioFolder.mkdirs();
                destinatioFolder.chmod(0777);
            }

            //Add matlab-batch into the PATH varible
            EnvVars env = node.toComputer().getEnvironment();
            env.put("PATH+MATLAB_BATCH",expectedPath.getRemote());
            log.getLogger().println(env.get("PATH"));


            Launcher matlabInstaller = node.createLauncher(log);
            ProcStarter installerProc = matlabInstaller.launch();
            ArgumentListBuilder args = new ArgumentListBuilder();
            //args.add("cmd.exe");
            //args.add("dir");
            args.add(expectedPath.getRemote() + "\\mpm.exe");
            args.add("install");
            args.add("--release=" + this.id +"");
            args.add("--destination="+ this.getVersion() +"");
            args.add("--products=MATLAB");
            installerProc.pwd(expectedPath)
                    //.cmds(expectedPath.getRemote() + "\\mpm.exe","install","--release=" + this.id +"","--destination="+ this.getHome() +"","--products=MATLAB")//cmds(".\\mpm.exe install --release=" + this.id + " --destination="+ this.getHome() +"")
                    .cmds(args)
                    .envs(env).stdout(log);
            int i = installerProc.join();

            log.getLogger().println("Result ===== " + i);
            if (i != 0) {
                log.getLogger().println("NON zero" + i);
            } else {
                log.getLogger().println("Zero" + i);
            }
        }
        return new FilePath(node.getChannel(),this.getVersion());

    }

    private Installable getInstallable(Node node) throws IOException, InterruptedException {
        // Get the Go release that we want to install
        String release = this.id;
        if (release == null) {
            return null;
        }

        // Gather properties for the node to install on
        String[] properties = node.getChannel().call(new GetSystemProperties("os.name", "os.arch", "os.version"));

        // Get the best matching install candidate for this node
        return getInstallCandidate(properties[0]);
    }

    public MatlabInstallable getInstallCandidate(String osName)
            throws InstallationFailedException {
        String platform = getPlatform(osName);

        return new MatlabInstallable(platform);
    }

    private class GetSystemProperties extends MasterToSlaveCallable<String[], InterruptedException> {
        private static final long serialVersionUID = 1L;

        private final String[] properties;

        GetSystemProperties(String... properties) {
            this.properties = properties;
        }

        public String[] call() {
            String[] values = new String[properties.length];
            for (int i = 0; i < properties.length; i++) {
                values[i] = System.getProperty(properties[i]);
            }
            return values;
        }
    }

    private String getPlatform(String os) throws InstallationFailedException {
        String value = os.toLowerCase(Locale.ENGLISH);
        if (value.contains("linux")) {
            return "glnxa64";
        }
        if (value.contains("os x")) {
            return "maci64";
        }
        if (value.contains("windows")) {
            return "win64";
        }
        throw new InstallationFailedException("Unsupported OS");
    }


    // Extend IOException so we can throw and stop the build if installation fails
    public class InstallationFailedException extends IOException {
        InstallationFailedException(String message) {
            super(message);
        }
    }


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

}
