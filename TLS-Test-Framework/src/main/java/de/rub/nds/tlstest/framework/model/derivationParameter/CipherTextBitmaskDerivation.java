/**
 * TLS-Test-Framework - A framework for modeling TLS tests
 *
 * <p>Copyright 2022 Ruhr University Bochum
 *
 * <p>Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.framework.model.derivationParameter;

import de.rub.nds.anvilcore.model.DerivationScope;
import de.rub.nds.anvilcore.model.constraint.ConditionalConstraint;
import de.rub.nds.anvilcore.model.parameter.DerivationParameter;
import de.rub.nds.anvilcore.model.parameter.ParameterIdentifier;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.AlgorithmResolver;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.CipherType;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlstest.framework.anvil.TlsDerivationParameter;
import de.rub.nds.tlstest.framework.anvil.TlsParameterIdentifierProvider;
import de.rub.nds.tlstest.framework.model.TlsParameterType;
import de.rwth.swc.coffee4j.model.constraints.ConstraintBuilder;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class CipherTextBitmaskDerivation extends TlsDerivationParameter<Integer> {

    public CipherTextBitmaskDerivation() {
        super(TlsParameterType.CIPHERTEXT_BITMASK, Integer.class);
    }

    public CipherTextBitmaskDerivation(Integer selectedValue) {
        this();
        setSelectedValue(selectedValue);
    }

    @Override
    public List<ConditionalConstraint> getDefaultConditionalConstraints(DerivationScope scope) {
        List<ConditionalConstraint> condConstraints = new LinkedList<>();
        condConstraints.add(getMustBeWithinBlocksizeConstraint());
        condConstraints.add(getMustBeWithinCiphertextSizeConstraint(scope));
        return condConstraints;
    }

    private ConditionalConstraint getMustBeWithinBlocksizeConstraint() {
        Set<ParameterIdentifier> requiredDerivations = new HashSet<>();
        requiredDerivations.add(new ParameterIdentifier(TlsParameterType.CIPHER_SUITE));

        return new ConditionalConstraint(
                requiredDerivations,
                ConstraintBuilder.constrain(
                                TlsParameterType.CIPHERTEXT_BITMASK.name(),
                                TlsParameterType.CIPHER_SUITE.name())
                        .by(
                                (DerivationParameter bytePos, DerivationParameter cipherSuite) -> {
                                    int chosenBytePos = (Integer) bytePos.getSelectedValue();
                                    CipherSuiteDerivation cipherDev =
                                            (CipherSuiteDerivation) cipherSuite;
                                    return AlgorithmResolver.getCipher(cipherDev.getSelectedValue())
                                                    .getBlocksize()
                                            > chosenBytePos;
                                }));
    }

    private ConditionalConstraint getMustBeWithinCiphertextSizeConstraint(DerivationScope scope) {
        Set<ParameterIdentifier> requiredDerivationsCiphertext = new HashSet<>();
        requiredDerivationsCiphertext.add(new ParameterIdentifier(TlsParameterType.CIPHER_SUITE));
        requiredDerivationsCiphertext.add(new ParameterIdentifier(TlsParameterType.APP_MSG_LENGHT));

        // ensure that the selected byte is within ciphertext size (for non-padded)
        return new ConditionalConstraint(
                requiredDerivationsCiphertext,
                ConstraintBuilder.constrain(
                                TlsParameterType.CIPHERTEXT_BITMASK.name(),
                                TlsParameterType.CIPHER_SUITE.name(),
                                TlsParameterType.APP_MSG_LENGHT.name())
                        .by(
                                (CipherTextBitmaskDerivation cipherTextBitmaskDerivation,
                                        CipherSuiteDerivation cipherSuiteDerivation,
                                        AppMsgLengthDerivation appMsgLenParam) -> {
                                    int selectedBitmaskBytePosition =
                                            cipherTextBitmaskDerivation.getSelectedValue();
                                    CipherSuite selectedCipherSuite =
                                            cipherSuiteDerivation.getSelectedValue();
                                    int selectedAppMsgLength = appMsgLenParam.getSelectedValue();

                                    if ((!TlsParameterIdentifierProvider.isTls13Test(scope)
                                                    && !selectedCipherSuite.isUsingPadding(
                                                            ProtocolVersion.TLS12))
                                            || AlgorithmResolver.getCipherType(selectedCipherSuite)
                                                    == CipherType.AEAD) {
                                        return selectedAppMsgLength > selectedBitmaskBytePosition;
                                    }
                                    return true;
                                }));
    }

    @Override
    protected TlsDerivationParameter<Integer> generateValue(Integer selectedValue) {
        return new CipherTextBitmaskDerivation(selectedValue);
    }

    @Override
    public List<DerivationParameter<Config, Integer>> getParameterValues(
            DerivationScope derivationScope) {
        int maxCipherTextByteLen = 0;
        Set<CipherSuite> cipherSuiteList = context.getFeatureExtractionResult().getCipherSuites();
        if (TlsParameterIdentifierProvider.isTls13Test(derivationScope)) {
            cipherSuiteList = context.getFeatureExtractionResult().getSupportedTls13CipherSuites();
        }
        for (CipherSuite cipherSuite : cipherSuiteList) {
            if (AlgorithmResolver.getCipher(cipherSuite).getBlocksize() > maxCipherTextByteLen) {
                maxCipherTextByteLen = AlgorithmResolver.getCipher(cipherSuite).getBlocksize();
            }
        }

        List<DerivationParameter<Config, Integer>> parameterValues = new LinkedList<>();
        for (int i = 0; i < maxCipherTextByteLen; i++) {
            parameterValues.add(new CipherTextBitmaskDerivation(i));
        }
        return parameterValues;
    }
}
