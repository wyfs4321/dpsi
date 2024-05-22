package edu.alibaba.mpc4j.work.dpsi;

import edu.alibaba.mpc4j.work.dpsi.mqrpmt.MqRpmtDpsiConfig;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DP-PSI based on mqRPMT efficiency test.
 *
 * @author Weiran Liu
 * @date 2024/5/5
 */
@Ignore
public class DpPsiParameterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DpPsiParameterTest.class);

    @Test
    public void testMqRpmtMaxDummySize() {
        for (int exp = -10; exp <= 10; exp++) {
            double epsilon = Math.pow(2, exp);
            MqRpmtDpsiConfig config = new MqRpmtDpsiConfig.Builder(epsilon, epsilon / 2, epsilon / 2).build();
            LOGGER.info(
                "ε = 2^{}, PSI-CA-R = {}, PSD-CA-R = {}",
                exp - 1, config.getMaxPsicaDummySize(), config.getMaxPsdcaDummySize()
            );

        }
    }
}
