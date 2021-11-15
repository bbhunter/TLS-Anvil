package de.rub.nds.tlstest.framework.model.derivationParameter;

import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlstest.framework.TestContext;
import de.rub.nds.tlstest.framework.model.DerivationScope;
import de.rub.nds.tlstest.framework.model.DerivationType;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class IncludeExtendedMasterSecretExtensionDerivation extends DerivationParameter<Boolean> {

    public IncludeExtendedMasterSecretExtensionDerivation() {
        super(BasicDerivationType.INCLUDE_EXTENDED_MASTER_SECRET_EXTENSION, Boolean.class);
    }
    public IncludeExtendedMasterSecretExtensionDerivation(Boolean selectedValue) {
        this();
        setSelectedValue(selectedValue);
    }

    @Override
    public List<DerivationParameter> getParameterValues(TestContext context, DerivationScope scope) {
        List<DerivationParameter> parameterValues = new LinkedList<>();
        parameterValues.add(new IncludeExtendedMasterSecretExtensionDerivation(true));
        parameterValues.add(new IncludeExtendedMasterSecretExtensionDerivation(false));
        return parameterValues;
    }

    @Override
    public void applyToConfig(Config config, TestContext context) {
        config.setAddExtendedMasterSecretExtension(getSelectedValue());
    }

}
