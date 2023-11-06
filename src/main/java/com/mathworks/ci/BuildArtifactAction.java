package com.mathworks.ci;

import hudson.FilePath;
import hudson.model.Action;
import hudson.model.Run;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.CheckForNull;
import jenkins.model.Jenkins;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class BuildArtifactAction implements Action {
    private Run<?, ?> build;
    private FilePath workspace;
    private int totalcount;
    private int skipcount;
    private int failCount;

    public BuildArtifactAction(Run<?, ?> build, FilePath workspace) {
        this.build = build;
        this.workspace = workspace;
    }

    public BuildArtifactAction() {
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
        return "buildresults";
    }

    public List<BuildArtifactData> getBuildArtifact() throws ParseException, InterruptedException {
        List<BuildArtifactData> artifactData = new ArrayList<BuildArtifactData>();
        FilePath fl = new FilePath(new File(build.getRootDir().getAbsolutePath() + "/buildArtifact.json"));
        try(InputStreamReader reader = new InputStreamReader(new FileInputStream(new File(fl.toURI())),"UTF-8")){
            Object obj = new JSONParser().parse(reader);
            JSONObject jo = (JSONObject) obj;
            if (jo.get("taskDetails") instanceof JSONArray) {
                JSONArray ja = (JSONArray) jo.get("taskDetails");
                Iterator itr2 = ja.iterator();
                Iterator<Entry> itr1;
                while (itr2.hasNext()) {
                    BuildArtifactData data = new BuildArtifactData();
                    itr1 = ((Map) itr2.next()).entrySet().iterator();
                    while (itr1.hasNext()) {
                        Entry pair = itr1.next();
                        if (pair.getKey().toString().equalsIgnoreCase("duration")) {
                            data.setTaskDuration(pair.getValue().toString());
                        } else if (pair.getKey().toString().equalsIgnoreCase("name")) {
                            data.setTaskName(pair.getValue().toString());
                        } else if (pair.getKey().toString().equalsIgnoreCase("description")) {
                            data.setTaskDescription(pair.getValue().toString());
                        } else if (pair.getKey().toString().equalsIgnoreCase("failed")) {
                            data.setTaskStatus(pair.getValue().toString());
                        } else if (pair.getKey().toString().equalsIgnoreCase("skipped")) {
                            data.setTaskSkipped(pair.getValue().toString());
                        }
                    }
                    artifactData.add(data);
                }
            } else {
                Map ja = ((Map) jo.get("taskDetails"));
                Iterator<Entry> itr1 = ja.entrySet().iterator();
                BuildArtifactData data = new BuildArtifactData();
                while (itr1.hasNext()) {
                    Entry pair = itr1.next();
                    if (pair.getKey().toString().equalsIgnoreCase("duration")) {
                        data.setTaskDuration(pair.getValue().toString());
                    } else if (pair.getKey().toString().equalsIgnoreCase("name")) {
                        data.setTaskName(pair.getValue().toString());
                    } else if (pair.getKey().toString().equalsIgnoreCase("description")) {
                        data.setTaskDescription(pair.getValue().toString());
                    } else if (pair.getKey().toString().equalsIgnoreCase("failed")) {
                        data.setTaskStatus(pair.getValue().toString());
                    } else if (pair.getKey().toString().equalsIgnoreCase("skipped")) {
                        data.setTaskSkipped(pair.getValue().toString());
                    }
                }
                artifactData.add(data);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
     /*   //Object obj = new JSONParser().parse(new FileReader(new File(fl.toURI())));
        Object obj = new JSONParser().parse(reader);
        JSONObject jo = (JSONObject) obj;

        if (jo.get("taskDetails") instanceof JSONArray) {
            JSONArray ja = (JSONArray) jo.get("taskDetails");
            Iterator itr2 = ja.iterator();
            Iterator<Entry> itr1;
            while (itr2.hasNext()) {
                BuildArtifactData data = new BuildArtifactData();
                itr1 = ((Map) itr2.next()).entrySet().iterator();
                while (itr1.hasNext()) {
                    Entry pair = itr1.next();
                    if (pair.getKey().toString().equalsIgnoreCase("duration")) {
                        data.setTaskDuration(pair.getValue().toString());
                    } else if (pair.getKey().toString().equalsIgnoreCase("name")) {
                        data.setTaskName(pair.getValue().toString());
                    } else if (pair.getKey().toString().equalsIgnoreCase("description")) {
                        data.setTaskDescription(pair.getValue().toString());
                    } else if (pair.getKey().toString().equalsIgnoreCase("failed")) {
                        data.setTaskStatus(pair.getValue().toString());
                    } else if (pair.getKey().toString().equalsIgnoreCase("skipped")) {
                        data.setTaskSkipped(pair.getValue().toString());
                    }
                }
                artifactData.add(data);
            }
        } else {
            Map ja = ((Map) jo.get("taskDetails"));
            Iterator<Entry> itr1 = ja.entrySet().iterator();
            BuildArtifactData data = new BuildArtifactData();
            while (itr1.hasNext()) {
                Entry pair = itr1.next();
                if (pair.getKey().toString().equalsIgnoreCase("duration")) {
                    data.setTaskDuration(pair.getValue().toString());
                } else if (pair.getKey().toString().equalsIgnoreCase("name")) {
                    data.setTaskName(pair.getValue().toString());
                } else if (pair.getKey().toString().equalsIgnoreCase("description")) {
                    data.setTaskDescription(pair.getValue().toString());
                } else if (pair.getKey().toString().equalsIgnoreCase("failed")) {
                    data.setTaskStatus(pair.getValue().toString());
                } else if (pair.getKey().toString().equalsIgnoreCase("skipped")) {
                    data.setTaskSkipped(pair.getValue().toString());
                }
            }
            artifactData.add(data);
        }*/
        return artifactData;
    }

    public void setTotalcount(int totalcount) {
        this.totalcount = totalcount;
    }

    public void setSkipcount(int skipcount) {
        this.skipcount = skipcount;
    }

    public int getTotalCount() throws IOException, ParseException, InterruptedException {
        //calling setCount as this is the first method which gets invoked in index.jelly
        setCounts();
        return this.totalcount;
    }

    public int getFailCount() {
        return this.failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    public int getSkipCount() {
        return this.skipcount;
    }

    public Run getOwner() {
        return this.build;
    }

    public FilePath getWorkspace(){
        return this.workspace;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(Run owner) {
        this.build = owner;
    }

    private void setCounts() throws IOException, InterruptedException, ParseException {
        List<BuildArtifactData> artifactData = new ArrayList<BuildArtifactData>();
        FilePath fl = new FilePath(new File(build.getRootDir().getAbsolutePath() + "/buildArtifact.json"));
        try(InputStreamReader reader = new InputStreamReader(new FileInputStream(new File(fl.toURI())),"UTF-8")){
            Object obj = new JSONParser().parse(reader);
            //Object obj = new JSONParser().parse(new FileReader(new File(fl.toURI())));
            JSONObject jo = (JSONObject) obj;

            // getting taskDetails
            if (jo.get("taskDetails") instanceof JSONArray) {
                JSONArray ja = (JSONArray) jo.get("taskDetails");
                Iterator itr2 = ja.iterator();
                Iterator<Entry> itr1;
                while (itr2.hasNext()) {
                    BuildArtifactData data = new BuildArtifactData();
                    itr1 = ((Map) itr2.next()).entrySet().iterator();
                    while (itr1.hasNext()) {
                        Entry pair = itr1.next();
                        if (pair.getKey().toString().equalsIgnoreCase("failed")) {
                            data.setTaskStatus(pair.getValue().toString());
                        } else if (pair.getKey().toString().equalsIgnoreCase("skipped")) {
                            data.setTaskSkipped(pair.getValue().toString());
                        }
                    }
                    artifactData.add(data);
                    setTotalcount(artifactData.size());
                }
            } else {
                Map ja = ((Map) jo.get("taskDetails"));
                Iterator<Entry> itr1 = ja.entrySet().iterator();
                BuildArtifactData data = new BuildArtifactData();
                while (itr1.hasNext()) {
                    Entry pair = itr1.next();
                    if (pair.getKey().toString().equalsIgnoreCase("failed")) {
                        data.setTaskStatus(pair.getValue().toString());
                    } else if (pair.getKey().toString().equalsIgnoreCase("skipped")) {
                        data.setTaskSkipped(pair.getValue().toString());
                    }
                }
                artifactData.add(data);
                setTotalcount(artifactData.size());
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        /* Object obj = new JSONParser().parse(reader);
        //Object obj = new JSONParser().parse(new FileReader(new File(fl.toURI())));
        JSONObject jo = (JSONObject) obj;

        // getting taskDetails
        if (jo.get("taskDetails") instanceof JSONArray) {
            JSONArray ja = (JSONArray) jo.get("taskDetails");
            Iterator itr2 = ja.iterator();
            Iterator<Entry> itr1;
            while (itr2.hasNext()) {
                BuildArtifactData data = new BuildArtifactData();
                itr1 = ((Map) itr2.next()).entrySet().iterator();
                while (itr1.hasNext()) {
                    Entry pair = itr1.next();
                    if (pair.getKey().toString().equalsIgnoreCase("failed")) {
                        data.setTaskStatus(pair.getValue().toString());
                    } else if (pair.getKey().toString().equalsIgnoreCase("skipped")) {
                        data.setTaskSkipped(pair.getValue().toString());
                    }
                }
                artifactData.add(data);
                setTotalcount(artifactData.size());
            }
        } else {
            Map ja = ((Map) jo.get("taskDetails"));
            Iterator<Entry> itr1 = ja.entrySet().iterator();
            BuildArtifactData data = new BuildArtifactData();
            while (itr1.hasNext()) {
                Entry pair = itr1.next();
                if (pair.getKey().toString().equalsIgnoreCase("failed")) {
                    data.setTaskStatus(pair.getValue().toString());
                } else if (pair.getKey().toString().equalsIgnoreCase("skipped")) {
                    data.setTaskSkipped(pair.getValue().toString());
                }
            }
            artifactData.add(data);
            setTotalcount(artifactData.size());
        } */

        // Update the FAILED and SKIPPED task count
        int failCount = 0;
        int skipCount = 0;
        for (BuildArtifactData data : artifactData) {
            if (data.getTaskStatus().equalsIgnoreCase("true")) {
                failCount = failCount + 1;
            } else if (data.getTaskStatus().equalsIgnoreCase("false") && data.getTaskSkipped().equalsIgnoreCase("true")) {
                skipCount = skipCount + 1;
            }
        }
        // Set count for each failed and skipped tasks
        setFailCount(failCount);
        setSkipcount(skipCount);
    }
}
