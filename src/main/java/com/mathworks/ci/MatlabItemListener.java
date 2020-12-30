package com.mathworks.ci;

/**
 * Copyright 2020 The MathWorks, Inc.
 *
 * Item listener class to provide functionality to check UI element states for a
 * Multi-configuration project.
 *
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
    private static final Map<String, Boolean> prjCheckMATLABAxis = new HashMap<>();
    private static final Map<String, Boolean> prjCheckMATLABBuildWrapper = new HashMap<>();

    @Override
    public void onLoaded(){
        checkItems(Jenkins.get().getItems());
    }

    @Override
    public void onUpdated(Item item) {
        if(!(item instanceof MatrixProject)){
            return;
        }
        checkSingleItem(item);
    }


    private void checkItems(List<TopLevelItem> items) {
        for(TopLevelItem item : items){
            if(item instanceof MatrixProject){
                check((MatrixProject) item);
            }
        }
    }

    private void checkSingleItem(Item item){
        check((MatrixProject) item);
    }

    private void check(MatrixProject _prj) {
        checkForAxis(_prj);
        checkForBuildWrapper(_prj);
    }

    private void checkForAxis(MatrixProject _prj) {
        boolean checkForAxis = false;
        Collection<MatrixConfiguration> conf = _prj.getActiveConfigurations();
        for(MatrixConfiguration _conf : conf){
            String a = _conf.getCombination().get(Message.getValue("Axis.matlab.key"));
            if (a != null) {
                checkForAxis = true;
                break;
            }
        }
        prjCheckMATLABAxis.put(_prj.getFullName(), checkForAxis);
    }

    private void checkForBuildWrapper(MatrixProject _prj) {
        boolean checkForBuildWrapper = false;
        for(Object _bWrapper : _prj.getBuildWrappersList().toArray()) {
            if(_bWrapper instanceof UseMatlabVersionBuildWrapper){
                checkForBuildWrapper = ((UseMatlabVersionBuildWrapper) _bWrapper).getMatlabInstallationName() != null || ((UseMatlabVersionBuildWrapper) _bWrapper).getMatlabRootFolder() != null;
                break;
            }
        }
        prjCheckMATLABBuildWrapper.put(_prj.getFullName(), checkForBuildWrapper);
    }

    public static boolean getMATLABAxisCheckForPrj(String prjName) {
        return prjCheckMATLABAxis.get(prjName) != null && prjCheckMATLABAxis.get(prjName);
    }

    public static boolean getMATLABBuildWrapperCheckForPrj(String prjName) {
        return prjCheckMATLABBuildWrapper.get(prjName) != null && prjCheckMATLABBuildWrapper.get(prjName);
    }
}
