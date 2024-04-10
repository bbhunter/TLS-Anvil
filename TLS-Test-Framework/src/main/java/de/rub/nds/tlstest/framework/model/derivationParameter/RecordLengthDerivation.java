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
import de.rub.nds.scanner.core.probe.result.TestResults;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsscanner.core.constants.TlsAnalyzedProperty;
import de.rub.nds.tlstest.framework.anvil.TlsDerivationParameter;
import de.rub.nds.tlstest.framework.model.TlsParameterType;
import java.util.LinkedList;
import java.util.List;

public class RecordLengthDerivation extends TlsDerivationParameter<Integer> {

    public RecordLengthDerivation() {
        super(TlsParameterType.RECORD_LENGTH, Integer.class);
    }

    public RecordLengthDerivation(Integer selectedValue) {
        this();
        setSelectedValue(selectedValue);
    }

    @Override
    public List<DerivationParameter<Config, Integer>> getParameterValues(
            DerivationScope derivationScope) {
        List<DerivationParameter<Config, Integer>> parameterValues = new LinkedList<>();

        if (context.getFeatureExtractionResult()
                        .getResult(TlsAnalyzedProperty.SUPPORTS_RECORD_FRAGMENTATION)
                == TestResults.TRUE) {
            parameterValues.add(new RecordLengthDerivation(50));
            parameterValues.add(new RecordLengthDerivation(111));
            parameterValues.add(new RecordLengthDerivation(1));
        }
        parameterValues.add(new RecordLengthDerivation(16384));
        return parameterValues;
    }

    @Override
    public void applyToConfig(Config config, DerivationScope derivationScope) {
        config.setDefaultMaxRecordData(getSelectedValue());
    }

    @Override
    protected TlsDerivationParameter<Integer> generateValue(Integer selectedValue) {
        return new RecordLengthDerivation(selectedValue);
    }
}
