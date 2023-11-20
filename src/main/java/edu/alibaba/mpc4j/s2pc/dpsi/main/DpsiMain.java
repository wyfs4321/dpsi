package edu.alibaba.mpc4j.s2pc.dpsi.main;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.dpsi.main.circuit.CircuitDpsiMain;
import edu.alibaba.mpc4j.s2pc.dpsi.main.mqrpmt.MqRpmtDpsiMain;
import edu.alibaba.mpc4j.s2pc.pso.main.PsoMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * DPSI协议主函数。
 *
 * @author Yufei Wang
 * @date 2023/10/09
 */
public class DpsiMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsoMain.class);
    /**
     * 主函数。
     *
     * @param args 只有一个输入：配置文件。
     */
    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        // 读取配置文件
        LOGGER.info("read PTO config");
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        // 读取协议类型
        String ptoType = PropertiesUtils.readString(properties, "pto_type");
        LOGGER.info("pto_type = " + ptoType);
        switch (ptoType) {
            case CircuitDpsiMain.PTO_TYPE_NAME:
                CircuitDpsiMain circuitDpsiMain = new CircuitDpsiMain(properties);
                circuitDpsiMain.runNetty();
                break;
            case MqRpmtDpsiMain.PTO_TYPE_NAME:
                MqRpmtDpsiMain mqRpmtDpsiMain = new MqRpmtDpsiMain(properties);
                mqRpmtDpsiMain.runNetty();
                break;
            default:
                throw new IllegalArgumentException("Invalid pto_type: " + ptoType);
        }
        System.exit(0);
    }
}
