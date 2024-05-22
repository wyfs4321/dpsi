package edu.alibaba.mpc4j.work.dpsi;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.dpsi.DpsiFactory.DpPsiType;


public interface DpsiConfig extends MultiPartyPtoConfig {
    /**
     * Gets the type.
     *
     * @return tye type.
     */
    DpPsiType getPtoType();

    /**
     * Gets ε.
     * @return ε.
     */
    double getEpsilon();
}
