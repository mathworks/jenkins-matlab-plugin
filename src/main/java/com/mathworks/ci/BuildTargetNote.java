package com.mathworks.ci;

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.MarkupText;
import hudson.console.ConsoleAnnotationDescriptor;
import hudson.console.ConsoleAnnotator;
import hudson.console.ConsoleNote;
import java.util.regex.Pattern;
import org.jenkinsci.Symbol;

public class BuildTargetNote extends ConsoleNote {
    @VisibleForTesting
    @SuppressFBWarnings(value = "MS_SHOULD_BE_FINAL", justification = "Visible for testing")
    public static boolean ENABLED = !Boolean.getBoolean(BuildTargetNote.class.getName() + ".disabled");

    public BuildTargetNote() {
    }

    @Override
    public ConsoleAnnotator annotate(Object context, MarkupText text, int charPos) {
        MarkupText.SubText t = text.findToken(Pattern.compile("Starting"));
        String taskName = text.subText(12, text.length()).getText();
        if (t != null)
            t.addMarkup(0, t.length(), "<a id= matlab_" + taskName + " name=matlab_" + taskName + ">", "</a>");
        return null;
    }

    @Extension
    @Symbol("mbuildTarget")
    public static final class DescriptorImpl extends ConsoleAnnotationDescriptor {
        public String getDisplayName() {
            return "Build targets";
        }
    }

}
