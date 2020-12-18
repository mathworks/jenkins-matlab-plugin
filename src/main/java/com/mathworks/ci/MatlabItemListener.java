package com.mathworks.ci;

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
        checkElements(Jenkins.get().getItems());
    }

    @Override
    public void onUpdated(Item item) {
        if(!(item instanceof MatrixProject)){
            return;
        }
        checkElementsSingle(item);
    }


    void checkElements(List<TopLevelItem> items) {
        for(TopLevelItem item : items){
            if(item instanceof MatrixProject){
                check((MatrixProject) item);
            }
        }
    }

    void checkElementsSingle(Item item){
        check((MatrixProject) item);
    }

    void check(MatrixProject _prj){
        boolean checkForAxis = false;
        boolean checkForBuildWrapper = false;
        Collection<MatrixConfiguration> conf = _prj.getActiveConfigurations();
        for(MatrixConfiguration _conf : conf){
            String a = _conf.getCombination().get(Message.getValue("Axis.matlab.key"));
            if (a != null) {
                checkForAxis = true;
                break;
            }
        }

        for(Object _bWrapper : _prj.getBuildWrappersList().toArray()) {
            if(_bWrapper instanceof UseMatlabVersionBuildWrapper){
                checkForBuildWrapper = ((UseMatlabVersionBuildWrapper) _bWrapper).getMatlabInstName() != null || ((UseMatlabVersionBuildWrapper) _bWrapper).getMatlabRootFolder() != null;
                break;
            }
        }
        prjCheckMATLABAxis.put(_prj.getFullName(), checkForAxis);
        prjCheckMATLABBuildWrapper.put(_prj.getFullName(), checkForBuildWrapper);
    }

    public static boolean getMATLABAxisCheckForPrj(String prjName) {
        return prjCheckMATLABAxis.get(prjName) != null && prjCheckMATLABAxis.get(prjName);
    }

    public static boolean getMATLABBuildWrapperCheckForPrj(String prjName) {
        return prjCheckMATLABBuildWrapper.get(prjName) != null && prjCheckMATLABBuildWrapper.get(prjName);
    }
}
