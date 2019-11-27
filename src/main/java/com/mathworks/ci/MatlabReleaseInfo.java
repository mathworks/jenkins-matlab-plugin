package com.mathworks.ci;

/*
 * Copyright 2019 The MathWorks, Inc. This Class provides MATLAB release information in the form of
 * Version numbers. Class constructor requires MATLAB root as input parameter
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.collections.MapUtils;
import org.jenkinsci.remoting.RoleChecker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

public class MatlabReleaseInfo {
    private FilePath matlabRoot;
    private static final String VERSION_INFO_FILE = "VersionInfo.xml";
    private static final String VERSION_INFO_ROOT_TAG = "MathWorks_version_info";
    private static final String RELEASE_TAG = "release";
    private static final String VERSION_TAG = "version";
    private static final String DESCRIPTION_TAG = "description";
    private static final String DATE_TAG = "date";
    private static final String VERSION_16B = "9.1.0.888888";
    private static final Map<String, String> VERSION_OLDER_THAN_17A = new HashMap<String, String>(){
        {
            put(VERSION_TAG,VERSION_16B);
        }
    };
    
    private Map<String, String> versionInfoCache = new HashMap<String, String>();
    
    public MatlabReleaseInfo(FilePath matlabRoot) {
        this.matlabRoot = matlabRoot;
    }

    public String getFullMatlabVersionNumber() throws MatlabVersionNotFoundException {
        Map<String, String> fullVersionNumber = getVersionInfoFromFile();
        return fullVersionNumber.get(VERSION_TAG);
    }

    private int getMatlabMajorVersionNumber() throws MatlabVersionNotFoundException {
        String[] version = getFullMatlabVersionNumber().split("[.]");
        return Integer.parseInt(version[0].trim());
    }

    private int getMatlabMinorVersionNumber() throws MatlabVersionNotFoundException {
        String[] version = getFullMatlabVersionNumber().split("[.]");
        if (version.length > 1) {
            return Integer.parseInt(version[1].trim());
        } else {
            return 0;
        }
    }

    public boolean verLessThan(double version) throws MatlabVersionNotFoundException {
        double matlabVersion = Double
                .parseDouble(getMatlabMajorVersionNumber() + "." + getMatlabMinorVersionNumber());
        if (Double.compare(matlabVersion, version) < 0) {
            return true;
        } else {
            return false;
        }
    }
    
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION",
            justification = "Irrespective of exception type, intention is to handle it in same way. Also, there is no intention to propagate any runtime exception up in the hierarchy.")
    private Map<String, String> getVersionInfoFromFile() throws MatlabVersionNotFoundException {
        if (MapUtils.isEmpty(versionInfoCache)) {
            try {
                FilePath versionFile = new FilePath(this.matlabRoot, VERSION_INFO_FILE);
                if(versionFile.exists()) {
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(versionFile.read());
                    
                    doc.getDocumentElement().normalize();
                    NodeList nList = doc.getElementsByTagName(VERSION_INFO_ROOT_TAG);

                    for (int temp = 0; temp < nList.getLength(); temp++) {
                        Node nNode = nList.item(temp);
                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                            Element eElement = (Element) nNode;

                            versionInfoCache.put(RELEASE_TAG, eElement.getElementsByTagName(RELEASE_TAG)
                                    .item(0).getTextContent());
                            versionInfoCache.put(VERSION_TAG, eElement.getElementsByTagName(VERSION_TAG)
                                    .item(0).getTextContent());
                            versionInfoCache.put(DESCRIPTION_TAG, eElement
                                    .getElementsByTagName(DESCRIPTION_TAG).item(0).getTextContent());
                            versionInfoCache.put(DATE_TAG,
                                    eElement.getElementsByTagName(DATE_TAG).item(0).getTextContent());
                        }
                    }
                }
                else if(!this.matlabRoot.exists()){
                    throw new NotDirectoryException("Invalid matlabroot path");
                }else {
                    versionInfoCache.putAll(VERSION_OLDER_THAN_17A);
                }
                
            } catch (Exception e) {
                throw new MatlabVersionNotFoundException(
                        Message.getValue("Releaseinfo.matlab.version.not.found.error"), e);
            }
        }
        return versionInfoCache;
    }
 }
