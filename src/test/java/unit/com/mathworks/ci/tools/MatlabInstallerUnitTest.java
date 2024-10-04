package com.mathworks.ci.tools;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
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
		installer.setVersion("R2021a");
		installer.setProducts("MATLAB");
	}

	@Test
	public void testGetVersion() {
		assertEquals("R2021a", installer.getVersion());
	}

	@Test
	public void testGetProducts() {
		assertEquals("MATLAB", installer.getProducts());
	}

	@Test
	public void testPerformInstallation() throws Exception {
		doReturn(mockFilePath).when(installer).performInstallation(mockTool, mockNode, mockListener);

		FilePath result = installer.performInstallation(mockTool, mockNode, mockListener);
		assertNotNull(result);
	}

	@Test(expected = InstallationFailedException.class)
	public void testUnsupportedOS() throws Exception {
		installer.getPlatform("unsupportedOS");
	}

	@Test
	public void testGetPlatform() throws InstallationFailedException {
		assertEquals("glnxa64", installer.getPlatform("Linux"));
		assertEquals("maci64", installer.getPlatform("Mac OS X"));
		assertEquals("win64", installer.getPlatform("Windows 10"));
	}
}
