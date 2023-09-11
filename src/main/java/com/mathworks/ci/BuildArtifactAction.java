package com.mathworks.ci;

import hudson.FilePath;
import hudson.Functions;
import hudson.model.Action;
import hudson.model.Run;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import org.acegisecurity.providers.dao.salt.SystemWideSaltSource;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class BuildArtifactAction implements Action {
  //private List<BuildArtifactData> artifactData = new ArrayList<BuildArtifactData>();
  private Run<?, ?> build;

  public BuildArtifactAction(Run<?, ?> build){
    this.build = build;
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

  public String getRootUrl() {
    return Jenkins.get().getRootUrl();
  }

  public List<BuildArtifactData> getBuildArtifact() throws IOException, ParseException, URISyntaxException {
    List<BuildArtifactData> artifactData = new ArrayList<BuildArtifactData>();
    ClassLoader loader = this.getClass().getClassLoader();
    File fl = new File(loader.getResource("buildArtifact.json").toURI());
    Object obj = new JSONParser().parse(new FileReader(fl));
    JSONObject jo = (JSONObject) obj;
    // getting task
    JSONArray ja = (JSONArray) jo.get("taskDetails");

    // iterating phoneNumbers
    Iterator itr2 = ja.iterator();
    Iterator<Map.Entry> itr1;

    while (itr2.hasNext())
    {
      BuildArtifactData data = new BuildArtifactData();
      itr1 = ((Map) itr2.next()).entrySet().iterator();
      while (itr1.hasNext()) {
        Map.Entry pair = itr1.next();
        if(pair.getKey().toString().equalsIgnoreCase("duration")){
          data.setTaskDuration(pair.getValue().toString());
        } else if (pair.getKey().toString().equalsIgnoreCase( "name")) {
          data.setTaskName(pair.getValue().toString());
        } else if (pair.getKey().toString().equalsIgnoreCase("description")){
          data.setTaskDescription(pair.getValue().toString());
        } else if (pair.getKey().toString().equalsIgnoreCase( "failed")) {
          data.setTaskStatus(pair.getValue().toString());
        }

        //System.out.println(pair.getKey() + " : " + pair.getValue());
      }
      artifactData.add(data);
    }
    artifactData.forEach(artifact -> System.out.println(artifact.getTaskName()));

    return artifactData;
  }


  public int getTotalCount(){
    return 2;
  }

  public int getFailCount(){
    return 1;
  }

  public int getSkipCount(){
    return 0;
  }

   /**public void doSummary(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
    rsp.setContentType("text/html;charset=UTF-8");
    req.getView(this.getClass(),"summary.jelly").forward(req,rsp);
  }**/

}
