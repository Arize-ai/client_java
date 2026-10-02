package com.arize;

import com.arize.surrogate.SurrogateExplainerTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({RecordUtilTest.class, ArizeClientTest.class, SurrogateExplainerTest.class})
public class TestSuite {}
