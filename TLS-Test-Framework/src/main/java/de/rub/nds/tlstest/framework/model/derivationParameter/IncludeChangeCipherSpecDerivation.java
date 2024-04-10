/**
 * TLS-Test-Framework - A framework for modeling TLS tests
 *
 * <p>Copyright 2022 Ruhr University Bochum
 *
 * <p>Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.framework.model.derivationParameter;

import de.rub.nds.anvilcore.model.DerivationScope;
import de.rub.nds.anvilcore.model.parameter.DerivationParameter;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlstest.framework.anvil.TlsDerivationParameter;
import de.rub.nds.tlstest.framework.model.TlsParameterType;
import java.util.LinkedList;
import java.util.List;

/**
 * This is only applied to legay Change Cipher Specs used for backwardscompatibility in a TLS 1.3
 * handshake
 */
public class IncludeChangeCipherSpecDerivation extends TlsDerivationParameter<Boolean> {

    public IncludeChangeCipherSpecDerivation() {
        super(TlsParameterType.INCLUDE_CHANGE_CIPHER_SPEC, Boolean.class);
    }

    public IncludeChangeCipherSpecDerivation(Boolean selectedValue) {
        this();
        setSelectedValue(selectedValue);
    }

    @Override
    public void applyToConfig(Config config, DerivationScope derivationScope) {
        config.setTls13BackwardsCompatibilityMode(getSelectedValue());
    }

    @Override
    protected TlsDerivationParameter<Boolean> generateValue(Boolean selectedValue) {
        return new IncludeChangeCipherSpecDerivation(selectedValue);
    }

    @Override
    public List<DerivationParameter<Config, Boolean>> getParameterValues(
            DerivationScope derivationScope) {
        List<DerivationParameter<Config, Boolean>> parameterValues = new LinkedList<>();
        parameterValues.add(new IncludeChangeCipherSpecDerivation(true));
        parameterValues.add(new IncludeChangeCipherSpecDerivation(false));
        return parameterValues;
    }
}
