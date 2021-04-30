package com.arize;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ RecordUtilTest.class, ArizeClientTest.class })

public class TestSuite {
}
