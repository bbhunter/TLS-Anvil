/**
 * TLS-Test-Framework - A framework for modeling TLS tests
 *
 * Copyright 2020 Ruhr University Bochum and
 * TÜV Informationstechnik GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.framework.model.derivationParameter;

import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.transport.TransportHandlerType;
import de.rub.nds.tlstest.framework.TestContext;
import de.rub.nds.tlstest.framework.model.DerivationScope;
import de.rub.nds.tlstest.framework.model.DerivationType;
import java.util.LinkedList;
import java.util.List;

public class TcpFragmentationDerivation extends DerivationParameter<Boolean> {

    public TcpFragmentationDerivation() {
        super(BasicDerivationType.TCP_FRAGMENTATION, Boolean.class);
    }
    
    public TcpFragmentationDerivation(Boolean selectedValue) {
        this();
        setSelectedValue(selectedValue);
    }
    
    
    @Override
    public List<DerivationParameter> getParameterValues(TestContext context, DerivationScope scope) {
        List<DerivationParameter> parameterValues = new LinkedList<>();
        parameterValues.add(new TcpFragmentationDerivation(false));
        parameterValues.add(new TcpFragmentationDerivation(true));
        return parameterValues;
    }

    @Override
    public void applyToConfig(Config config, TestContext context) {
        if(getSelectedValue() == true) {
            config.getDefaultClientConnection().setTransportHandlerType(TransportHandlerType.TCP_FRAGMENTATION);
            config.getDefaultServerConnection().setTransportHandlerType(TransportHandlerType.TCP_FRAGMENTATION);
        }
    }
    
}
