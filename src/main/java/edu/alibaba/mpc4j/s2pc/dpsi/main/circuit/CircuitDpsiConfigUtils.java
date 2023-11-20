package edu.alibaba.mpc4j.s2pc.dpsi.main.circuit;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.dpsi.circuit.CcpsiDpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;

import java.util.Properties;

/**
 * Circuit-DPSI config utilities.
 *
 * @author Yufei Wang
 * @date 2023/10/09
 */
public class CircuitDpsiConfigUtils {
    /**
     * private constructor.
     */
    private CircuitDpsiConfigUtils() {
        // empty
    }

    public static CcpsiDpsiConfig createCircuitDpsiConfig(Properties properties) {
        // read CircuitPSI type
        String circuitPsiTypeString= PropertiesUtils.readString(properties, "circuit_psi_pto_name");
        CcpsiFactory.CcpsiType circuitDpsiType = CcpsiFactory.CcpsiType.valueOf(circuitPsiTypeString);
        switch (circuitDpsiType) {
            case PSTY19:
                return createPsty19Config(properties);
            case RS21:
                return createRs21Config(properties);
            case CGS22:
                return createCgs22Config(properties);
            default:
                throw new IllegalArgumentException("Invalid " + PsiFactory.PsiType.class.getSimpleName() + ": " + circuitDpsiType.name());
        }
    }

    private  static CcpsiDpsiConfig createPsty19Config(Properties properties) {
        double epsilon = PropertiesUtils.readDouble(properties, "epsilon");
        return new CcpsiDpsiConfig.Builder(CcpsiFactory.CcpsiType.PSTY19,epsilon).build();
    }

    private  static CcpsiDpsiConfig createRs21Config(Properties properties) {
        double epsilon = PropertiesUtils.readDouble(properties, "epsilon");
        return new CcpsiDpsiConfig.Builder(CcpsiFactory.CcpsiType.RS21,epsilon).build();
    }

    private  static CcpsiDpsiConfig createCgs22Config(Properties properties) {
        double epsilon = PropertiesUtils.readDouble(properties, "epsilon");
        return new CcpsiDpsiConfig.Builder(CcpsiFactory.CcpsiType.CGS22,epsilon).build();
    }
}
