package com.mathworks.ci;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import jenkins.tasks.SimpleBuildWrapper;

public class MatlabBuildWrapper extends SimpleBuildWrapper  {
    
    private String matlabRootFolder;
    private EnvVars env;
    
    @DataBoundConstructor
     public MatlabBuildWrapper() {
    }
    
    public String getMatlabRootFolder() {
        return this.matlabRootFolder;
    }

    @DataBoundSetter
    public void setMatlabRootFolder(String matlabRootFolder) {
        this.matlabRootFolder = matlabRootFolder;
    }
    
    private String getLocalMatlab() {
        return this.env == null ? getMatlabRootFolder(): this.env.expand(getMatlabRootFolder());
    }
    
    private void setEnv(EnvVars env) {
       this.env = env;
    }

    
    @Extension
    public static final class MatabBuildWrapperDescriptor extends BuildWrapperDescriptor {
        
        MatlabReleaseInfo rel;
        String matlabRootFolder;

        public String getMatlabRootFolder() {
            return matlabRootFolder;
        }

        public void setMatlabRootFolder(String matlabRootFolder) {
            this.matlabRootFolder = matlabRootFolder;
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {       
            return true;
        }
        
        @Override
        public String getDisplayName() {
            return "With MATLAB";
        }
        
        
         /*
         * Below methods with 'doCheck' prefix gets called by jenkins when this builder is loaded.
         * these methods are used to perform basic validation on UI elements associated with this
         * descriptor class.
         */


        public FormValidation doCheckMatlabRootFolder(@QueryParameter String matlabRootFolder) {
            setMatlabRootFolder(matlabRootFolder);
            List<Function<String, FormValidation>> listOfCheckMethods =
                    new ArrayList<Function<String, FormValidation>>();
            listOfCheckMethods.add(chkMatlabEmpty);
            listOfCheckMethods.add(chkMatlabSupportsRunTests);

            return getFirstErrorOrWarning(listOfCheckMethods,matlabRootFolder);
        }

        public FormValidation getFirstErrorOrWarning(
                List<Function<String, FormValidation>> validations, String matlabRootFolder) {
            if (validations == null || validations.isEmpty())
                return FormValidation.ok();
            for (Function<String, FormValidation> val : validations) {
                FormValidation validationResult = val.apply(matlabRootFolder);
                if (validationResult.kind.compareTo(Kind.ERROR) == 0
                        || validationResult.kind.compareTo(Kind.WARNING) == 0) {
                    return validationResult;
                }
            }
            return FormValidation.ok();
        }

        Function<String, FormValidation> chkMatlabEmpty = (String matlabRootFolder) -> {
            if (matlabRootFolder.isEmpty()) {
                return FormValidation.error(Message.getValue("Builder.matlab.root.empty.error"));
            }
            return FormValidation.ok();
        };
        
        Function<String, FormValidation> chkMatlabSupportsRunTests = (String matlabRootFolder) -> {
            final MatrixPatternResolver resolver = new MatrixPatternResolver(matlabRootFolder);
            if (!resolver.hasVariablePattern()) {
                try {
                    FilePath matlabRootPath = new FilePath(new File(matlabRootFolder));
                    rel = new MatlabReleaseInfo(matlabRootPath);
                    if (rel.verLessThan(MatlabBuilderConstants.BASE_MATLAB_VERSION_RUNTESTS_SUPPORT)) {
                        return FormValidation
                                .error(Message.getValue("Builder.matlab.test.support.error"));
                    }
                } catch (MatlabVersionNotFoundException e) {
                    return FormValidation
                            .warning(Message.getValue("Builder.invalid.matlab.root.warning"));
                }
            }
            return FormValidation.ok();
        };
    }

    @Override
    public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener,
            EnvVars initialEnvironment) throws IOException, InterruptedException {
        CommandConstructUtil utils = new CommandConstructUtil(launcher, getLocalMatlab());
        //Set Environment variable
        
        setEnv(initialEnvironment);
        String nodeSpecificFileSep = utils.getNodeSpecificFileSeperator();
        context.env("matlabroot", getLocalMatlab());
        context.env("PATH+matlabroot", getLocalMatlab() + nodeSpecificFileSep + "bin");
    }    
}
