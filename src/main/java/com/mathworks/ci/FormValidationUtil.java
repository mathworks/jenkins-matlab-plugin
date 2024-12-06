package com.mathworks.ci;

/**
 * Copyright 2019-2024 The MathWorks, Inc.
 * 
 * This is Utility class which provides commonly used methods for form validations across builders
 */

import java.util.List;
import java.util.function.Function;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;

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
}
