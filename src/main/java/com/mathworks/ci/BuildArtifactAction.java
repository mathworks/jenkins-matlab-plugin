package com.mathworks.ci;

/**
 * Copyright 2024 The MathWorks, Inc.
 */

import hudson.FilePath;
import hudson.model.Action;
import hudson.model.Run;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.CheckForNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class BuildArtifactAction implements Action {
    private Run<?, ?> build;
    private int totalCount;
    private int skipCount;
    private int failCount;
    private String actionID;
    private static final String ROOT_ELEMENT = "taskDetails";

    public BuildArtifactAction(Run<?, ?> build, String actionID) {
        this.build = build;
        this.actionID = actionID;

        // Setting the counts of task when Action is created.
        try {
            setCounts();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String getActionID() {
        return (this.actionID == null) ? "" : this.actionID;
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return "document.png";
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "MATLAB Build Results";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return (this.actionID == null) ? "buildresults" : "buildresults" + this.actionID;
    }

    public List<BuildArtifactData> getBuildArtifact() throws ParseException, InterruptedException, IOException {
        List<BuildArtifactData> artifactData = new ArrayList<BuildArtifactData>();
        FilePath fl;
        if (this.actionID == null) {
            fl = new FilePath(new File(build.getRootDir().getAbsolutePath() + "/" +
                    MatlabBuilderConstants.BUILD_ARTIFACT + ".json"));
        } else {
            fl = new FilePath(new File(build.getRootDir().getAbsolutePath() + "/" +
                    MatlabBuilderConstants.BUILD_ARTIFACT + this.actionID + ".json"));
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(new File(fl.toURI())), "UTF-8")) {
            Object obj = new JSONParser().parse(reader);
            JSONObject jo = (JSONObject) obj;
            if (jo.get(ROOT_ELEMENT) instanceof JSONArray) {
                JSONArray ja = (JSONArray) jo.get(ROOT_ELEMENT);
                Iterator itr2 = ja.iterator();
                Iterator<Entry> itr1;
                while (itr2.hasNext()) {
                    BuildArtifactData data = new BuildArtifactData();
                    itr1 = ((Map) itr2.next()).entrySet().iterator();
                    while (itr1.hasNext()) {
                        Entry pair = itr1.next();
                        iterateAllTaskAttributes(pair, data);
                    }
                    artifactData.add(data);
                }
            } else {
                Map ja = ((Map) jo.get(ROOT_ELEMENT));
                Iterator<Entry> itr1 = ja.entrySet().iterator();
                BuildArtifactData data = new BuildArtifactData();
                while (itr1.hasNext()) {
                    Entry pair = itr1.next();
                    iterateAllTaskAttributes(pair, data);
                }
                artifactData.add(data);
            }
        } catch (IOException e) {
            throw new IOException(e.getLocalizedMessage());
        }
        return artifactData;
    }

    public void setTotalcount(int totalCount) {
        this.totalCount = totalCount;
    }

    public void setSkipCount(int skipCount) {
        this.skipCount = skipCount;
    }

    public int getTotalCount() {
        return this.totalCount;
    }

    public int getFailCount() {
        return this.failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    public int getSkipCount() {
        return this.skipCount;
    }

    public Run getOwner() {
        return this.build;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(Run owner) {
        this.build = owner;
    }

    private void setCounts() throws InterruptedException, ParseException {
        List<BuildArtifactData> artifactData = new ArrayList<BuildArtifactData>();
        FilePath fl;
        if (this.actionID == null) {
            fl = new FilePath(new File(build.getRootDir().getAbsolutePath() + "/" +
                    MatlabBuilderConstants.BUILD_ARTIFACT + ".json"));
        } else {
            fl = new FilePath(new File(build.getRootDir().getAbsolutePath() + "/" +
                    MatlabBuilderConstants.BUILD_ARTIFACT + this.actionID + ".json"));
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(new File(fl.toURI())), "UTF-8")) {
            Object obj = new JSONParser().parse(reader);
            JSONObject jo = (JSONObject) obj;

            // getting taskDetails
            if (jo.get(ROOT_ELEMENT) instanceof JSONArray) {
                JSONArray ja = (JSONArray) jo.get(ROOT_ELEMENT);
                Iterator itr2 = ja.iterator();
                Iterator<Entry> itr1;
                while (itr2.hasNext()) {
                    BuildArtifactData data = new BuildArtifactData();
                    itr1 = ((Map) itr2.next()).entrySet().iterator();
                    while (itr1.hasNext()) {
                        Entry pair = itr1.next();
                        iterateFailedSkipped(pair, data);
                    }
                    artifactData.add(data);
                    setTotalcount(artifactData.size());
                }
            } else {
                Map ja = ((Map) jo.get(ROOT_ELEMENT));
                Iterator<Entry> itr1 = ja.entrySet().iterator();
                BuildArtifactData data = new BuildArtifactData();
                while (itr1.hasNext()) {
                    Entry pair = itr1.next();
                    iterateFailedSkipped(pair, data);
                }
                artifactData.add(data);
                setTotalcount(artifactData.size());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Update the FAILED and SKIPPED task count
        int failCount = 0;
        int skipCount = 0;
        for (BuildArtifactData data : artifactData) {
            if (data.getTaskFailed()) {
                failCount = failCount + 1;
            } else if (data.getTaskSkipped()) {
                skipCount = skipCount + 1;
            }
        }
        // Set count for each failed and skipped tasks
        setFailCount(failCount);
        setSkipCount(skipCount);
    }

    private void iterateAllTaskAttributes(Entry pair, BuildArtifactData data) {
        // Iterates across all task attributes and updates
        String key = pair.getKey().toString();
        switch (key) {
            case "duration":
                data.setTaskDuration(pair.getValue().toString());
                break;
            case "name":
                data.setTaskName(pair.getValue().toString());
                break;
            case "description":
                data.setTaskDescription(pair.getValue().toString());
                break;
            case "failed":
                data.setTaskFailed((Boolean) pair.getValue());
                break;
            case "skipped":
                data.setTaskSkipped((Boolean) pair.getValue());
                break;
            case "skipReason":
                String skipReasonKey = pair.getValue().toString();
                String skipReason;
                switch (skipReasonKey) {
                    case "UpToDate":
                        skipReason = "up-to-date";
                        break;
                    case "UserSpecified":
                    case "UserRequested":
                        skipReason = "user requested";
                        break;
                    case "DependencyFailed":
                        skipReason = "dependency failed";
                        break;
                    default:
                        skipReason = skipReasonKey;
                        break;
                }
                data.setSkipReason(skipReason);
                break;
            default:
                break;
        }
    }

    private void iterateFailedSkipped(Entry pair, BuildArtifactData data) {
        if (pair.getKey().toString().equalsIgnoreCase("failed")) {
            data.setTaskFailed((Boolean) pair.getValue());
        } else if (pair.getKey().toString().equalsIgnoreCase("skipped")) {
            data.setTaskSkipped((Boolean) pair.getValue());
        }
    }
}
