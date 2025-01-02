package com.mathworks.ci;

/**
 * Copyright 2020-2024 The MathWorks, Inc.
 *
 * Utility class for common methods.
 */

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Utilities {

    public static String getCellArrayFromList(List<String> listOfStr) {
        // Ignore empty string values in the list
        Predicate<String> isEmpty = String::isEmpty;
        Predicate<String> isNotEmpty = isEmpty.negate();
        List<String> filteredListOfStr = listOfStr.stream().filter(isNotEmpty).collect(Collectors.toList());

        // Escape apostrophe for MATLAB
        filteredListOfStr.replaceAll(val -> "'" + val.replaceAll("'", "''") + "'");
        return "{" + String.join(",", filteredListOfStr) + "}";
    }

    public static void addMatlabToEnvPathFromAxis(Computer cmp, TaskListener listener, EnvVars env)
            throws IOException, InterruptedException {
        String name = env.get(Message.getValue("Axis.matlab.key"));

        // If no MATLAB axis is set or if 'Use MATLAB version' is selected, return
        if (name == null || name.isEmpty() || env.get("matlabroot") != null) {
            return;
        }

        FilePath matlabRoot = getNodeSpecificHome(name, cmp.getNode(), listener, env);

        FilePath matlabBin = new FilePath(matlabRoot, "bin");
        env.put("PATH+matlabroot", matlabBin.getRemote());

        // Specify which MATLAB was added to path.
        listener.getLogger().println(
                "\n" + String.format(Message.getValue("matlab.added.to.path.from"), matlabBin.getRemote()) + "\n");
    }

    public static FilePath getNodeSpecificHome(String instName, Node node, TaskListener listener, EnvVars env)
            throws IOException, InterruptedException {
        MatlabInstallation inst = MatlabInstallation.getInstallation(instName);
        if (inst == null || node == null) {
            // Following will error out in BuildWrapper
            throw new MatlabNotFoundError("MATLAB installations could not be found");
        }

        // get installation for node and environment.
        inst = inst.forNode(node, listener).forEnvironment(env);

        FilePath matlabExecutablePath = node.createPath(inst.getHome());
        // If no MATLAB version is configured for current node, throw error.
        if (matlabExecutablePath == null || !matlabExecutablePath.exists()) {
            throw new MatlabNotFoundError(
                    String.format(Message.getValue("matlab.not.found.error.for.node"), instName, Objects
                            .requireNonNull(node).getDisplayName()));
        }
        return matlabExecutablePath;
    }
}
