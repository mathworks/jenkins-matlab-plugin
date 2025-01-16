package com.mathworks.ci;

/*
 * Copyright 2019-2024 The MathWorks, Inc.
 *
 * This Class provides MATLAB release information in the form of Version numbers. Class constructor
 * requires MATLAB root as input parameter.
 */

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.lang.InterruptedException;
import java.nio.charset.StandardCharsets;
import java.nio.file.NotDirectoryException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.collections.MapUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import hudson.FilePath;

public class MatlabReleaseInfo {
    private FilePath matlabRoot;
    private static final String VERSION_INFO_FILE = "VersionInfo.xml";
    private static final String CONTENTS_FILE = "toolbox/matlab/general/Contents.m";
    private static final String VERSION_PATTERN = "(\\d+)\\.(\\d+)";
    private static final String VERSION_INFO_ROOT_TAG = "MathWorks_version_info";
    private static final String RELEASE_TAG = "release";
    private static final String VERSION_TAG = "version";
    private static final String DESCRIPTION_TAG = "description";
    private static final String DATE_TAG = "date";

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

    private Map<String, String> getVersionInfoFromFile() throws MatlabVersionNotFoundException {
        if (MapUtils.isEmpty(versionInfoCache)) {
            try {
                FilePath versionFile = new FilePath(this.matlabRoot, VERSION_INFO_FILE);
                if (versionFile.exists()) {
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    String FEATURE = null;
                    try {
                        FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";
                        dbFactory.setFeature(FEATURE, true);
                        dbFactory.setXIncludeAware(false);

                    } catch (ParserConfigurationException e) {
                        throw new MatlabVersionNotFoundException("Error parsing verify if XML is valid", e);
                    }
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
                } else if (!this.matlabRoot.exists()) {
                    throw new NotDirectoryException("Invalid matlabroot path");
                } else {
                    // Get the version information from Contents.m file when VersionInfo.xml is not
                    // present.
                    FilePath contentFile = new FilePath(this.matlabRoot, CONTENTS_FILE);
                    String actualVersion = null;
                    try (InputStream in = contentFile.read();
                            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

                        // Skip first line and capture the second line.
                        br.readLine();
                        String versionLine = br.readLine();

                        Pattern p = Pattern.compile(VERSION_PATTERN);
                        Matcher m = p.matcher(versionLine);
                        if (m.find()) {
                            actualVersion = m.group();
                        }
                    }
                    // Update the versionInfoCache with actual version extracted from Contents.m
                    versionInfoCache.put(VERSION_TAG, actualVersion);
                }
            } catch (InterruptedException | IOException | ParserConfigurationException | SAXException e) {
                throw new MatlabVersionNotFoundException(
                        Message.getValue("Releaseinfo.matlab.version.not.found.error"), e);
            }
        }
        return versionInfoCache;
    }
}
