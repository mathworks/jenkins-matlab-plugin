package com.mathworks.ci;

import com.google.common.base.Charsets;
import hudson.console.ConsoleLogFilter;
import hudson.console.LineTransformationOutputStream;
import hudson.model.Run;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import jenkins.util.JenkinsJVM;

public class BuildConsoleAnnotator extends LineTransformationOutputStream {
  private final OutputStream out;
  private final Charset charset;

  private final byte[][] antNotes;

  private boolean seenEmptyLine;

    public BuildConsoleAnnotator(OutputStream out, Charset charset) {
    this(out, charset, createBuildNotes());
  }

    private BuildConsoleAnnotator(OutputStream out, Charset charset, byte[][] antNotes) {
    this.out = out;
    this.charset = charset;
    this.antNotes = antNotes;
  }

  private static byte[][] createBuildNotes() {
    JenkinsJVM.checkJenkinsJVM();
    try {
      ByteArrayOutputStream targetNote = new ByteArrayOutputStream();
      new BuildTargetNote().encodeTo(targetNote);
      ByteArrayOutputStream outcomeNote = new ByteArrayOutputStream();
      //new AntOutcomeNote().encodeTo(outcomeNote);
      return new byte[][] {targetNote.toByteArray(), outcomeNote.toByteArray()};

    }catch (IOException e){
      throw new RuntimeException(e);
    }

  }

  @Override
  protected void eol(byte[] b, int len) throws IOException {
    String line = charset.decode(ByteBuffer.wrap(b, 0, len)).toString();

    // trim off CR/LF from the end
    line = trimEOL(line);
    System.out.println(line); // this shows only log for Build runner

    if (line.contains("Error"))
      out.write(antNotes[0]);



    //if (line.equals("ERROR: MATLAB error Exit Status: 0x00000001"))
      // put the annotation
      //out.write(antNotes[0]);

    //if (line.equals("BUILD SUCCESSFUL") || line.equals("BUILD FAILED"))
    //  out.write(antNotes[1]);

    seenEmptyLine = line.length()==0;
    out.write(b,0,len);
  }

  private boolean startsWith(String line, char c) {
    int len = line.length();
    return len>0 && line.charAt(0)==c;
  }

  @Override
  public void flush() throws IOException {
    out.flush();
  }

  @Override
  public void close() throws IOException {
    super.close();
    out.close();
  }

  public static ConsoleLogFilter asConsoleLogFilter() {
    return new ConsoleLogFilterImpl();
  }
  private static class ConsoleLogFilterImpl extends ConsoleLogFilter implements Serializable {
    private static final long serialVersionUID = 1;
    private byte[][] buildNotes = createBuildNotes();

    //Taking care of old MATLAB build actions.
    private Object readResolve() {
      if (buildNotes == null) {
        buildNotes = createBuildNotes();
      }
      return this;
    }
    @Override public OutputStream decorateLogger(Run build, OutputStream logger) throws IOException, InterruptedException {
      return new BuildConsoleAnnotator(logger, Charsets.UTF_8, buildNotes);
    }
  }
}
