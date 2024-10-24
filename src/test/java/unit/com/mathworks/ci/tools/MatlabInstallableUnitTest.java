package com.mathworks.ci.tools;

import static org.junit.Assert.assertEquals;

import java.io.File;
/**
 * Copyright 2024, The MathWorks, Inc.
 *
 */

import org.junit.Rule;
import org.junit.Test;

import hudson.FilePath;
import com.mathworks.ci.TestMessage;
import org.junit.rules.ExpectedException;

public class MatlabInstallableUnitTest {

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none ();

    @Test
    public void testValidWin64OS () throws InstallationFailedException {
        exceptionRule.expect(InstallationFailedException.class);
        exceptionRule.expectMessage("Unsupported OS");
        MatlabInstallable installable = new MatlabInstallable ("win64");

    }

    @Test
    public void testValidGlnxa64OS () throws InstallationFailedException {
        MatlabInstallable installable = new MatlabInstallable ("glnxa64");
        assertEquals (TestMessage.getValue ("tools.matlab.mpm.installer.linux"), installable.url);
        assertEquals (TestMessage.getValue ("tools.matlab.batch.executable.linux"),
            installable.getBatchURL ());

        FilePath expectedPath = new FilePath (new File ("/usr/local/install"));
        assertEquals (new FilePath (expectedPath, "matlab-batch").getRemote (),
            installable.getBatchInstallable (expectedPath).getRemote ());
        assertEquals (new FilePath (expectedPath, "mpm").getRemote (),
            installable.getMpmInstallable (expectedPath).getRemote ());
    }

    @Test
    public void testValidMaci64OS () throws InstallationFailedException {
        MatlabInstallable installable = new MatlabInstallable ("maci64");
        assertEquals (TestMessage.getValue ("tools.matlab.mpm.installer.mac"), installable.url);
        assertEquals (TestMessage.getValue ("tools.matlab.batch.executable.mac"),
            installable.getBatchURL ());

        FilePath expectedPath = new FilePath (new File ("/Applications/install"));
        assertEquals (new FilePath (expectedPath, "matlab-batch").getRemote (),
            installable.getBatchInstallable (expectedPath).getRemote ());
        assertEquals (new FilePath (expectedPath, "mpm").getRemote (),
            installable.getMpmInstallable (expectedPath).getRemote ());
    }
}
