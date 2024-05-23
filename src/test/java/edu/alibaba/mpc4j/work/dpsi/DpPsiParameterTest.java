package edu.alibaba.mpc4j.work.dpsi;

import edu.alibaba.mpc4j.work.dpsi.mqrpmt.MqRpmtDpsiConfig;
import edu.alibaba.mpc4j.work.dpsi.mqrpmt.MqRpmtDpUtils;

import java.io.*;
import java.util.Arrays;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
        LOGGER.info("------ change ε ----------");
        for (double epsilon = 0.001; epsilon <= 1000; epsilon *= 10) {
            MqRpmtDpsiConfig config = new MqRpmtDpsiConfig.Builder(epsilon, epsilon / 2, epsilon / 2).build();
            LOGGER.info(
                "ε = {}, delta = {}, PSI-CA-R = {}, PSD-CA-R = {}",
                epsilon, config.getDelta(), config.getMaxPsicaDummySize(), config.getMaxPsdcaDummySize()
            );
        }
        LOGGER.info("------ change delta ----------");
        for (double delta = 0.0000001; delta <= 0.1; delta *= 10) {
            MqRpmtDpsiConfig config = new MqRpmtDpsiConfig.Builder(1.0, 1.0 / 2, 1.0 / 2).setDelta(delta).build();
            LOGGER.info(
                "ε = {}, delta = {}, PSI-CA-R = {}, PSD-CA-R = {}",
                1.0, config.getDelta(), config.getMaxPsicaDummySize(), config.getMaxPsdcaDummySize()
            );
        }
    }
    @Test
    public void testMqRpmtReceiverDummySize() {
        double[] epsilonSet={1.0, 2.0, 3.0, 4.0, 5.0};
        double[] deltaSet={0.1, 0.0001};
        for (double epsilon: epsilonSet){
            for (double delta: deltaSet){
                int[] count = new int[30];
                Arrays.fill(count, 0);
                for (int i=0;i<100000;i++){
                    count[MqRpmtDpUtils.randomize(1, epsilon, delta, 0)]++;
                }
                LOGGER.info("count[0]={}",count[0]);
                String fileName = Double.toString(epsilon)+'_'+ delta +"_dummy_point.txt";
                try (FileOutputStream fos = new FileOutputStream(fileName);
                     OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
                     BufferedWriter writer = new BufferedWriter(osw)) {
                    for (int c: count) {
                        writer.write(Integer.toString(c));
                        writer.newLine();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
