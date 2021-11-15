package de.rub.nds.tlstest.framework.model.derivationParameter;

import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlstest.framework.TestContext;
import de.rub.nds.tlstest.framework.model.DerivationScope;
import de.rub.nds.tlstest.framework.model.DerivationType;
import java.util.LinkedList;
import java.util.List;

public class IncludeEncryptThenMacExtensionDerivation extends DerivationParameter<Boolean> {

    public IncludeEncryptThenMacExtensionDerivation() {
        super(BasicDerivationType.INCLUDE_ENCRYPT_THEN_MAC_EXTENSION, Boolean.class);
    }
    
    public IncludeEncryptThenMacExtensionDerivation(Boolean selectedValue) {
        this();
        setSelectedValue(selectedValue);
    }
    
    @Override
    public List<DerivationParameter> getParameterValues(TestContext context, DerivationScope scope) {
        List<DerivationParameter> parameterValues = new LinkedList<>();
        parameterValues.add(new IncludeEncryptThenMacExtensionDerivation(true));
        parameterValues.add(new IncludeEncryptThenMacExtensionDerivation(false));
        return parameterValues;
    }

    @Override
    public void applyToConfig(Config config, TestContext context) {
        config.setAddEncryptThenMacExtension(getSelectedValue());
    }

}
