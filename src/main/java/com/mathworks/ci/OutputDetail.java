package com.mathworks.ci;

import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OutputDetail extends AbstractDescribableImpl<OutputDetail> 
{
    private boolean check ;
    private static final String OUTPUT_DETAIL = "OutputDetail";
    
	private Verbosity.verbosityTypes outputDetail;

    @DataBoundConstructor
    public OutputDetail() {
    }
    
    public Verbosity.verbosityTypes getOutputDetail() {
		return outputDetail;
	}
    
    @DataBoundSetter
    public void setOutputDetail(String outputDetail) {
    	this.outputDetail = Verbosity.verbosityTypes.valueOf(outputDetail.toUpperCase()) ;
    }

    public boolean getCheck(String val)
    {
    	check = getOutputDetail().toString().equalsIgnoreCase(val) ;
    	return check ;
    }
    
    public void addOutputDetailToInputArgs(List<String> inputArgsList, Verbosity.verbosityTypes outputDetail) {
    	
    	String outputDetailLevel = Verbosity.getverbosityValue().get(outputDetail);
    	inputArgsList.add("'" + OUTPUT_DETAIL + "'" + ","+ "'"+outputDetailLevel+"'");
    }

    @Extension 
    public static class DescriptorImpl extends Descriptor<OutputDetail>
    {	
    	public String getDisplayName() { return ""; }
    
    	public final String outputvalue = Message.getValue("matlab.outputdetail.default") ;
    	
        public Verbosity.verbosityTypes[] getArr()
        {
        	return Verbosity.verbosityTypes.values();
        } 
        
        public boolean getlevel(String str)
        {
        	return str.equalsIgnoreCase(outputvalue) ;
        }
        
        public String capitalize(String str) {
            if(str == null || str.isEmpty()) {
                return str;
            }

            return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
        }
    }
}
