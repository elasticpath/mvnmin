package com.elasticpath.tools.mavenminimal.reactor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class MavenDriverTest {


	@Test
	public void testRemoveThreadingFlags() {
		List<String> mvnParams = new ArrayList<>();

		mvnParams.add("-T10");

		mvnParams.add("-T");
		mvnParams.add("4");

		mvnParams.add("--threads");
		mvnParams.add("4");

		mvnParams.add("--threads=10");

		MavenDriver.removeThreadingFlags(mvnParams);

		assertEquals(Collections.emptyList(), mvnParams, "Threading flags not removed");
	}


}