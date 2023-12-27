package com.mathworks.ci;

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.MarkupText;
import hudson.console.ConsoleAnnotationDescriptor;
import hudson.console.ConsoleAnnotator;
import hudson.console.ConsoleNote;
import java.util.regex.Pattern;


public class BuildTargetNote extends ConsoleNote {
    @VisibleForTesting
    @SuppressFBWarnings(value = "MS_SHOULD_BE_FINAL", justification = "Visible for testing")
    public static boolean ENABLED = !Boolean.getBoolean(BuildTargetNote.class.getName() + ".disabled");

    public BuildTargetNote() {
    }

    @Override
    public ConsoleAnnotator annotate(Object context, MarkupText text, int charPos) {
        MarkupText.SubText t = text.findToken(Pattern.compile("MATLAB-Build-"));
        String taskName = text.subText(13, text.length()-2).getText();
        if (t != null)
            t.addMarkup(0, t.length()-1, "<a id= matlab" + taskName + " name=matlab" + taskName + ">", "</a>");
        return null;
    }

    @Extension
    public static final class DescriptorImpl extends ConsoleAnnotationDescriptor {
        public String getDisplayName() {
            return "Build targets";
        }
    }

}
