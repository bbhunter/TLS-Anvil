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

/** Defines values for the optional TLS 1.3 padding lengths */
public class AdditionalPaddingLengthDerivation extends TlsDerivationParameter<Integer> {

    public AdditionalPaddingLengthDerivation() {
        super(TlsParameterType.ADDITIONAL_PADDING_LENGTH, Integer.class);
    }

    public AdditionalPaddingLengthDerivation(Integer selectedValue) {
        this();
        setSelectedValue(selectedValue);
    }

    @Override
    public void applyToConfig(Config config, DerivationScope derivationScope) {
        config.setDefaultAdditionalPadding(getSelectedValue());
    }

    @Override
    public List<DerivationParameter<Config, Integer>> getParameterValues(
            DerivationScope derivationScope) {
        List<DerivationParameter<Config, Integer>> parameterValues = new LinkedList<>();
        parameterValues.add(new AdditionalPaddingLengthDerivation(5));
        parameterValues.add(new AdditionalPaddingLengthDerivation(100));
        parameterValues.add(new AdditionalPaddingLengthDerivation(1000));
        return parameterValues;
    }

    @Override
    protected TlsDerivationParameter<Integer> generateValue(Integer selectedValue) {
        return new AdditionalPaddingLengthDerivation(selectedValue);
    }
}
