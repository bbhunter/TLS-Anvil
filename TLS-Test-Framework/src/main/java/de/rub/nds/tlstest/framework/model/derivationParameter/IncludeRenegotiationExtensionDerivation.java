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

public class IncludeRenegotiationExtensionDerivation extends TlsDerivationParameter<Boolean> {

    public IncludeRenegotiationExtensionDerivation() {
        super(TlsParameterType.INCLUDE_RENEGOTIATION_EXTENSION, Boolean.class);
    }

    public IncludeRenegotiationExtensionDerivation(Boolean selectedValue) {
        this();
        setSelectedValue(selectedValue);
    }

    @Override
    public List<DerivationParameter<Config, Boolean>> getParameterValues(
            DerivationScope derivationScope) {
        List<DerivationParameter<Config, Boolean>> parameterValues = new LinkedList<>();
        parameterValues.add(new IncludeRenegotiationExtensionDerivation(true));
        parameterValues.add(new IncludeRenegotiationExtensionDerivation(false));
        return parameterValues;
    }

    @Override
    public void applyToConfig(Config config, DerivationScope derivationScope) {
        config.setAddRenegotiationInfoExtension(getSelectedValue());
    }

    @Override
    protected TlsDerivationParameter<Boolean> generateValue(Boolean selectedValue) {
        return new IncludeRenegotiationExtensionDerivation(selectedValue);
    }
}
