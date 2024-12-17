package com.mathworks.ci;

/**
 * Copyright 2024 The MathWorks, Inc.
 */

import com.google.common.annotations.VisibleForTesting;
import hudson.Extension;
import hudson.MarkupText;
import hudson.console.ConsoleAnnotationDescriptor;
import hudson.console.ConsoleAnnotator;
import hudson.console.ConsoleNote;
import java.util.regex.Pattern;

public class BuildTargetNote extends ConsoleNote {
    @VisibleForTesting
    public static boolean ENABLED = !Boolean.getBoolean(BuildTargetNote.class.getName() + ".disabled");

    public BuildTargetNote() {
    }

    @Override
    public ConsoleAnnotator annotate(Object context, MarkupText text, int charPos) {
        MarkupText.SubText t = text.findToken(Pattern.compile("MATLAB-Build-"));
        String taskName = text.subText(13, text.length() - 2).getText();
        taskName = taskName.replace("]", "").trim();
        if (t != null)
            t.addMarkup(0, t.length() - 1, "<a id= matlab" + taskName + " name=matlab" + taskName + ">", "</a>");
        return null;
    }

    @Extension
    public static final class DescriptorImpl extends ConsoleAnnotationDescriptor {
        public String getDisplayName() {
            return "Build targets";
        }
    }

}
