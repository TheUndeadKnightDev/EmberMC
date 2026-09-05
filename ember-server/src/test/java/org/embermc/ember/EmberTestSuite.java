package org.embermc.ember;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * EmberMC's own tests. Paper's test task only runs classes named {@code *TestSuite},
 * so this is the door; everything under {@code org.embermc} is behind it. These
 * tests need no server, no registry and no world: they cover the pure logic -
 * the profiler's ring statistics, the adaptive engine's state machine, the
 * tuner's preset table and value handling.
 */
@Suite(failIfNoTests = false)
@SuiteDisplayName("EmberMC")
@SelectPackages("org.embermc")
public class EmberTestSuite {
}
