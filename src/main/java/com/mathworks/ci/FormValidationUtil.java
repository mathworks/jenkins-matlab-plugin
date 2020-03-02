package com.mathworks.ci;

/**
 * Copyright 2019-2020 The MathWorks, Inc.
 * 
 * This is Utility class which provides commonly used methods for form validations across builders
 * 
 */
import java.util.List;
import java.util.function.Function;
import com.mathworks.ci.UseMatlabVersionBuildWrapper.UseMatlabVersionDescriptor;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import jenkins.model.Jenkins;

public class FormValidationUtil {

    public static FormValidation getFirstErrorOrWarning(
            List<Function<String, FormValidation>> validations, String validationArg) {
        if (validations == null || validations.isEmpty())
            return FormValidation.ok();
        try {
            for (Function<String, FormValidation> val : validations) {
                FormValidation validationResult = val.apply(validationArg);
                if (validationResult.kind.compareTo(Kind.ERROR) == 0
                        || validationResult.kind.compareTo(Kind.WARNING) == 0) {
                    return validationResult;
                }
            }
        } catch (Exception e) {
            return FormValidation.warning(Message.getValue("Builder.invalid.matlab.root.warning"));
        }
        return FormValidation.ok();
    }
    
    //Method to get the MATLAB root from build wrapper class.
    
    public static String getMatlabRoot() {
        try {
            return Jenkins.getInstance().getDescriptorByType(UseMatlabVersionDescriptor.class)
                    .getMatlabRootFolder();
        } catch (Exception e) {
            // For any exception during getMatlabRootFolder() operation, return matlabRoot as NULL.
            return null;
        }
    }
}
