package com.mathworks.ci;
import java.io.*;
import java.util.*;
import java.util.ResourceBundle;

public class TestData {

    static String value="";
    static InputStream inputStream;

    public static String getPropValues(String key){
        try{
            Properties prop=new Properties();

            inputStream=TestData.class.getClassLoader().getResourceAsStream("testdataconfig.properties");
            if(inputStream!=null){
                prop.load(inputStream);
            } else {
                System.out.println("NOT ABLE TO FIND FILE");
            }
            value=prop.getProperty(key);
        }
        catch(Exception e){
            System.out.println(e);
        }

        return value;
    }

}
