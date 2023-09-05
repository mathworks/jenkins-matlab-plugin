package com.mathworks.ci;

import hudson.model.Action;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.servlet.ServletException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class BuildArtifactAction implements Action {

  public BuildArtifactAction(){
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

  public void doSummary(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
    rsp.setContentType("text/html;charset=UTF-8");
    req.getView(this.getClass(),"summary.jelly").forward(req,rsp);
  }

}
