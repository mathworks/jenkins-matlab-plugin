package unit.com.mathworks.ci.tools;

import static org.junit.Assert.assertEquals;

import com.mathworks.ci.TestMessage;
import com.mathworks.ci.tools.InstallationFailedException;
import com.mathworks.ci.tools.MatlabInstallable;
import hudson.FilePath;
import java.io.File;
import org.junit.Test;

public class MatlabInstallableUnitTest {
    @Test
    public void testValidWin64OS() throws InstallationFailedException {
        MatlabInstallable installable = new MatlabInstallable("win64");
        assertEquals(TestMessage.getValue("tools.matlab.mpm.installer.win"), installable.url);
        assertEquals(TestMessage.getValue("tools.matlab.batch.executable.win"), installable.getBatchURL());

        FilePath expectedPath = new FilePath(new File("C:/install"));
        assertEquals(new FilePath(expectedPath, "matlab-batch.exe").getRemote(), installable.getBatchInstallable(expectedPath).getRemote());
        assertEquals(new FilePath(expectedPath, "mpm.exe").getRemote(), installable.getMpmInstallable(expectedPath).getRemote());
    }

    @Test
    public void testValidGlnxa64OS() throws InstallationFailedException {
        MatlabInstallable installable = new MatlabInstallable("glnxa64");
        assertEquals(TestMessage.getValue("tools.matlab.mpm.installer.linux"), installable.url);
        assertEquals(TestMessage.getValue("tools.matlab.batch.executable.linux"), installable.getBatchURL());

        FilePath expectedPath = new FilePath(new File("/usr/local/install"));
        assertEquals(new FilePath(expectedPath, "matlab-batch").getRemote(), installable.getBatchInstallable(expectedPath).getRemote());
        assertEquals(new FilePath(expectedPath, "mpm").getRemote(), installable.getMpmInstallable(expectedPath).getRemote());
    }

    @Test
    public void testValidMaci64OS() throws InstallationFailedException {
        MatlabInstallable installable = new MatlabInstallable("maci64");
        assertEquals(TestMessage.getValue("tools.matlab.mpm.installer.mac"), installable.url);
        assertEquals(TestMessage.getValue("tools.matlab.batch.executable.mac"), installable.getBatchURL());

        FilePath expectedPath = new FilePath(new File("/Applications/install"));
        assertEquals(new FilePath(expectedPath, "matlab-batch").getRemote(), installable.getBatchInstallable(expectedPath).getRemote());
        assertEquals(new FilePath(expectedPath, "mpm").getRemote(), installable.getMpmInstallable(expectedPath).getRemote());
    }
}
