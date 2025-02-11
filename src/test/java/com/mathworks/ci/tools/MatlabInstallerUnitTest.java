package com.mathworks.ci.tools;

/**
 * Copyright 2024, The MathWorks, Inc.
 */

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.mathworks.ci.tools.InstallationFailedException;
import com.mathworks.ci.tools.MatlabInstaller;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;

import org.junit.Before;
import org.junit.Test;

public class MatlabInstallerUnitTest {

    private MatlabInstaller installer;

    @Mock
    private Node mockNode;

    @Mock
    private TaskListener mockListener;

    @Mock
    private ToolInstallation mockTool;

    @Mock
    private FilePath mockFilePath;

    @Mock
    private Launcher mockLauncher;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        installer = spy(new MatlabInstaller("test-id"));
        installer.setRelease("R2021a");
        installer.setProducts("MATLAB");
    }

    @Test
    public void testGetRelease() {
        assertEquals("R2021a", installer.getRelease());
    }

    @Test
    public void testGetProducts() {
        assertEquals("MATLAB", installer.getProducts());
    }

    @Test
    public void testPerformInstallation() throws Exception {
        doReturn(mockFilePath).when(installer)
                .performInstallation(mockTool, mockNode, mockListener);

        FilePath result = installer.performInstallation(mockTool, mockNode, mockListener);
        assertNotNull(result);
    }

    @Test(expected = InstallationFailedException.class)
    public void testUnsupportedOS() throws Exception {
        installer.getPlatform("unsupportedOS", "unsupportedArch");
    }

    @Test
    public void testGetPlatform() throws InstallationFailedException {
        assertEquals("glnxa64", installer.getPlatform("Linux", "i686"));
        assertEquals("maci64", installer.getPlatform("Mac OS X", "amd64"));
        assertEquals("maca64", installer.getPlatform("Mac OS X", "arm64"));
    }
}
