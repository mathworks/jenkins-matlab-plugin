package com.mathworks.ci;
import java.io.*;
import java.util.*;
import java.util.ResourceBundle;

public class TestData {

    String value="";
    InputStream inputStream;

    public String getPropValues(String key) throws IOException{
        try{
            Properties prop=new Properties();
            String propFileName="resources/testdataconfig.properties";
            inputStream=getClass().getClassLoader().getResourceAsStream(propFileName);
            if(inputStream!=null){
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("PROPERTY FILE '"+propFileName+"' not found in classpath");
            }
            value=prop.getProperty(key);
        }
        catch(Exception e){
            System.out.println(e);
        }
        finally {
           inputStream.close();
        }
        return value;
    }

}


