package com.mathworks.ci.utilities;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.mathworks.ci.utilities.GetSystemProperties;

public class GetSystemPropertiesUnitTest {

	@Mock
	private GetSystemProperties getSystemProperties;

	@Before
	public void setUp() {
		// Initialize with the properties
		getSystemProperties = spy(new GetSystemProperties("os.name", "os.arch", "os.version"));
	}

	@Test
	public void testCall() {
		doReturn(new String[] { "MockOS", "MockArch", "MockVersion" }).when(getSystemProperties).call();

		// Call the method under test
		String[] result = getSystemProperties.call();

		// Define the expected result
		String[] expected = { "MockOS", "MockArch", "MockVersion" };
		assertArrayEquals(expected, result);
	}
}
