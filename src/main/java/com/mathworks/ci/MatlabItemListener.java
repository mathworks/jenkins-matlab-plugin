package com.mathworks.ci;

/**
 * Copyright 2020-2024 The MathWorks, Inc.
 *
 * Item listener class to provide functionality to check UI element states for a
 * Multi-configuration project.
 */

import hudson.Extension;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.model.Item;
import hudson.model.TopLevelItem;
import hudson.model.listeners.ItemListener;
import jenkins.model.Jenkins;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Extension
public final class MatlabItemListener extends ItemListener {
    private static final Map<String, Boolean> prjCheckMatlabAxis = new HashMap<>();
    private static final Map<String, Boolean> prjCheckMatlabBuildWrapper = new HashMap<>();

    @Override
    public void onLoaded() {
        checkItems(Jenkins.get().getItems());
    }

    @Override
    public void onUpdated(Item item) {
        if (!(item instanceof MatrixProject)) {
            return;
        }
        checkSingleItem(item);
    }

    private void checkItems(List<TopLevelItem> items) {
        for (TopLevelItem item : items) {
            if (item instanceof MatrixProject) {
                check((MatrixProject) item);
            }
        }
    }

    private void checkSingleItem(Item item) {
        check((MatrixProject) item);
    }

    private void check(MatrixProject prj) {
        checkForAxis(prj);
        checkForBuildWrapper(prj);
    }

    private void checkForAxis(MatrixProject prj) {
        boolean checkForAxis = false;
        Collection<MatrixConfiguration> configurations = prj.getActiveConfigurations();
        for (MatrixConfiguration conf : configurations) {
            String matlabAxisValue = conf.getCombination().get(Message.getValue("Axis.matlab.key"));
            if (matlabAxisValue != null) {
                checkForAxis = true;
                break;
            }
        }
        prjCheckMatlabAxis.put(prj.getFullName(), checkForAxis);
    }

    private void checkForBuildWrapper(MatrixProject prj) {
        boolean checkForBuildWrapper = false;
        for (Object bWrapper : prj.getBuildWrappersList().toArray()) {
            if (bWrapper instanceof UseMatlabVersionBuildWrapper) {
                checkForBuildWrapper = ((UseMatlabVersionBuildWrapper) bWrapper).getMatlabInstallationName() != null;
                break;
            }
        }
        prjCheckMatlabBuildWrapper.put(prj.getFullName(), checkForBuildWrapper);
    }

    public static boolean getMatlabAxisCheckForPrj(String prjName) {
        return prjCheckMatlabAxis.get(prjName) != null && prjCheckMatlabAxis.get(prjName);
    }

    public static boolean getMatlabBuildWrapperCheckForPrj(String prjName) {
        return prjCheckMatlabBuildWrapper.get(prjName) != null && prjCheckMatlabBuildWrapper.get(prjName);
    }
}
