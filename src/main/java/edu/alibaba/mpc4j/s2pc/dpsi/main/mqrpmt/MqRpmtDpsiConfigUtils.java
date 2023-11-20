package edu.alibaba.mpc4j.s2pc.dpsi.main.mqrpmt;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.dpsi.mqrpmt.MqRpmtDpsiConfig;
import edu.alibaba.mpc4j.s2pc.dpsi.mqrpmt.MqRpmtDpsiFactory;
import edu.alibaba.mpc4j.s2pc.dpsi.mqrpmt.czz22.Czz22MqRpmtDpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;

import java.util.Properties;

/**
 * MqRPMT-DPSI config utilities.
 *
 * @author Yufei Wang
 * @date 2023/10/09
 */
public class MqRpmtDpsiConfigUtils {
    /**
     * private constructor.
     */
    private MqRpmtDpsiConfigUtils() {
        // empty
    }

    public static MqRpmtDpsiConfig createMqRpmtDpsiConfig(Properties properties) {
        // read CircuitPSI type
        String mqRpmtDpsiTypeString= PropertiesUtils.readString(properties, "mqrpmt_dpsi_pto_name");
        MqRpmtDpsiFactory.MqRpmtDpsiType mqRpmtDpsiType = MqRpmtDpsiFactory.MqRpmtDpsiType.valueOf(mqRpmtDpsiTypeString);
        switch (mqRpmtDpsiType) {
            case CZZ22:
                return createCzz22Config(properties);
            default:
                throw new IllegalArgumentException("Invalid " + PsiFactory.PsiType.class.getSimpleName() + ": " + mqRpmtDpsiType.name());
        }
    }

    private static MqRpmtDpsiConfig createCzz22Config(Properties properties) {
        double epsilon = PropertiesUtils.readDouble(properties, "epsilon");
        return new Czz22MqRpmtDpsiConfig.Builder(epsilon).build();
    }
}
