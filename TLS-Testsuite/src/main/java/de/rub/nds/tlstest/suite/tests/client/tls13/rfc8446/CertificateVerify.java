/**
 * TLS-Testsuite - A testsuite for the TLS protocol
 *
 * Copyright 2020 Ruhr University Bochum and
 * TÜV Informationstechnik GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.suite.tests.client.tls13.rfc8446;

import de.rub.nds.modifiablevariable.util.Modifiable;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.AlertDescription;
import de.rub.nds.tlsattacker.core.constants.CertificateKeyType;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.constants.ProtocolMessageType;
import de.rub.nds.tlsattacker.core.constants.SignatureAlgorithm;
import de.rub.nds.tlsattacker.core.constants.SignatureAndHashAlgorithm;
import de.rub.nds.tlsattacker.core.protocol.message.AlertMessage;
import de.rub.nds.tlsattacker.core.protocol.message.CertificateVerifyMessage;
import de.rub.nds.tlsattacker.core.protocol.message.FinishedMessage;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlstest.framework.TestContext;
import de.rub.nds.tlstest.framework.Validator;
import de.rub.nds.tlstest.framework.annotations.ClientTest;
import de.rub.nds.tlstest.framework.annotations.ExplicitValues;
import de.rub.nds.tlstest.framework.annotations.ManualConfig;
import de.rub.nds.tlstest.framework.annotations.MethodCondition;
import de.rub.nds.tlstest.framework.annotations.RFC;
import de.rub.nds.tlstest.framework.annotations.ScopeExtensions;
import de.rub.nds.tlstest.framework.annotations.TlsTest;
import de.rub.nds.tlstest.framework.annotations.categories.AlertCategory;
import de.rub.nds.tlstest.framework.annotations.categories.CertificateCategory;
import de.rub.nds.tlstest.framework.annotations.categories.ComplianceCategory;
import de.rub.nds.tlstest.framework.annotations.categories.CryptoCategory;
import de.rub.nds.tlstest.framework.annotations.categories.DeprecatedFeatureCategory;
import de.rub.nds.tlstest.framework.annotations.categories.InteroperabilityCategory;
import de.rub.nds.tlstest.framework.annotations.categories.SecurityCategory;
import de.rub.nds.tlstest.framework.coffee4j.model.ModelFromScope;
import de.rub.nds.tlstest.framework.constants.SeverityLevel;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.model.DerivationScope;
import de.rub.nds.tlstest.framework.model.derivationParameter.BasicDerivationType;
import de.rub.nds.tlstest.framework.model.ModelType;
import de.rub.nds.tlstest.framework.model.derivationParameter.DerivationParameter;
import de.rub.nds.tlstest.framework.model.derivationParameter.SigAndHashDerivation;
import de.rub.nds.tlstest.framework.testClasses.Tls13Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;

@ClientTest
@RFC(number = 8446, section = "4.4.3. Certificate Verify")
public class CertificateVerify extends Tls13Test {

    public ConditionEvaluationResult supportsLegacyRSASAHAlgorithms() {
        List<SignatureAndHashAlgorithm> algos = context.getSiteReport().getSupportedSignatureAndHashAlgorithms();
        algos = algos.stream().filter(i -> i.getSignatureAlgorithm() == SignatureAlgorithm.RSA).collect(Collectors.toList());

        if (algos.size() > 0) {
            return ConditionEvaluationResult.enabled("");
        }
        return ConditionEvaluationResult.disabled("Client does not support legacy rsa signature and hash algorithms");
    }
    
    public List<DerivationParameter> getLegacyRSASAHAlgorithms(DerivationScope scope) {
        List<DerivationParameter> parameterValues = new LinkedList<>();
        for (SignatureAndHashAlgorithm algo : context.getSiteReport().getSupportedSignatureAndHashAlgorithms()) {
            if (algo.getSignatureAlgorithm() == SignatureAlgorithm.RSA) {
                parameterValues.add(new SigAndHashDerivation(algo));
            }
        }
        return parameterValues;
    }

    @TlsTest(description = "RSA signatures MUST use an RSASSA-PSS algorithm, " +
            "regardless of whether RSASSA-PKCS1-v1_5 algorithms " +
            "appear in \"signature_algorithms\". The SHA-1 algorithm " +
            "MUST NOT be used in any signatures of CertificateVerify messages. " + 
            "All SHA-1 signature algorithms in this specification are defined " +
            "solely for use in legacy certificates and are not valid for " +
            "CertificateVerify signatures.")
    @SecurityCategory(SeverityLevel.MEDIUM)
    @DeprecatedFeatureCategory(SeverityLevel.MEDIUM)
    @CryptoCategory(SeverityLevel.MEDIUM)
    @CertificateCategory(SeverityLevel.MEDIUM)
    @ComplianceCategory(SeverityLevel.HIGH)
    @AlertCategory(SeverityLevel.LOW)
    @ScopeExtensions(BasicDerivationType.SIG_HASH_ALGORIHTM)
    @ExplicitValues(affectedTypes=BasicDerivationType.SIG_HASH_ALGORIHTM,methods="getLegacyRSASAHAlgorithms")
    @ManualConfig(BasicDerivationType.SIG_HASH_ALGORIHTM)
    @MethodCondition(method = "supportsLegacyRSASAHAlgorithms")
    public void selectLegacyRSASignatureAlgorithm(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);
        SignatureAndHashAlgorithm selsectedLegacySigHash = derivationContainer.getDerivation(SigAndHashDerivation.class).getSelectedValue();
        
        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);
        workflowTrace.addTlsActions(new ReceiveAction(new AlertMessage()));
        workflowTrace.getFirstSendMessage(CertificateVerifyMessage.class).setSignatureHashAlgorithm(Modifiable.explicit(selsectedLegacySigHash.getByteValue()));


        runner.execute(workflowTrace, c).validateFinal(Validator::receivedFatalAlert);
    }

    public ConditionEvaluationResult supportsLegacyECDSASAHAlgorithms() {
        if (context.getSiteReport().getSupportedSignatureAndHashAlgorithms().contains(SignatureAndHashAlgorithm.ECDSA_SHA1)) {
            return ConditionEvaluationResult.enabled("");
        }
        return ConditionEvaluationResult.disabled("Client does not support legacy rsa signature and hash algorithms");
    }

    @TlsTest(description = "The SHA-1 algorithm " +
            "MUST NOT be used in any signatures of CertificateVerify messages." + 
            "All SHA-1 signature algorithms in this specification are defined " +
            "solely for use in legacy certificates and are not valid for " +
            "CertificateVerify signatures.")
    @SecurityCategory(SeverityLevel.MEDIUM)
    @DeprecatedFeatureCategory(SeverityLevel.MEDIUM)
    @CryptoCategory(SeverityLevel.MEDIUM)
    @CertificateCategory(SeverityLevel.MEDIUM)
    @ComplianceCategory(SeverityLevel.HIGH)
    @AlertCategory(SeverityLevel.LOW)
    @MethodCondition(method = "supportsLegacyECDSASAHAlgorithms")
    public void selectLegacyECDSASignatureAlgorithm(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);
        c.setAutoAdjustSignatureAndHashAlgorithm(false);
        c.setDefaultSelectedSignatureAndHashAlgorithm(SignatureAndHashAlgorithm.ECDSA_SHA1);
        c.setPreferredCertificateSignatureType(CertificateKeyType.ECDSA);
        
        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);
        workflowTrace.addTlsActions(new ReceiveAction(new AlertMessage()));

        runner.execute(workflowTrace, c).validateFinal(Validator::receivedFatalAlert);
    }

    @TlsTest(description = "The receiver of a CertificateVerify message MUST verify " +
            "the signature field. If the verification fails, " +
            "the receiver MUST terminate the handshake with a \"decrypt_error\" alert.")
    @ModelFromScope(baseModel = ModelType.CERTIFICATE)
    @SecurityCategory(SeverityLevel.CRITICAL)
    @CryptoCategory(SeverityLevel.CRITICAL)
    @AlertCategory(SeverityLevel.MEDIUM)
    @CertificateCategory(SeverityLevel.CRITICAL)
    @ScopeExtensions(BasicDerivationType.SIGNATURE_BITMASK)
    public void invalidSignature(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);
        byte[] bitmask = derivationContainer.buildBitmask();

        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);
        workflowTrace.addTlsActions(new ReceiveAction(new AlertMessage()));

        CertificateVerifyMessage msg = workflowTrace.getFirstSendMessage(CertificateVerifyMessage.class);
        msg.setSignature(Modifiable.xor(bitmask, 0));


        runner.execute(workflowTrace, c).validateFinal(i -> {
            if(msg.getSignatureLength().getValue() < bitmask.length) {
                //we can't determine the ECDSA signature length beforehand
                //as trailing zeros may be stripped - the manipulation won't be
                //applied in these cases which results in false positives
                i.addAdditionalResultInfo("Bitmask exceeded signature length");
                return;
            }
            Validator.receivedFatalAlert(i);

            AlertMessage amsg = i.getWorkflowTrace().getFirstReceivedMessage(AlertMessage.class);
            Validator.testAlertDescription(i, AlertDescription.DECRYPT_ERROR, amsg);
        });
    }
    
    public List<DerivationParameter> getUnproposedSignatureAndHashAlgorithms(DerivationScope scope) {
        List<DerivationParameter> unsupportedAlgorithms = new LinkedList<>();
        SignatureAndHashAlgorithm.getImplemented().stream()
                .filter(algorithm -> !TestContext.getInstance().getSiteReport().getSupportedSignatureAndHashAlgorithms().contains(algorithm))
                .filter(algorithm -> algorithm.getSignatureAlgorithm() != SignatureAlgorithm.ANONYMOUS)
                .forEach(algorithm -> unsupportedAlgorithms.add(new SigAndHashDerivation(algorithm)));
        return unsupportedAlgorithms;
    }
    
    @TlsTest(description = "If the CertificateVerify message is sent by a server, the signature " +
        "algorithm MUST be one offered in the client's \"signature_algorithms\" " +
        "extension unless no valid certificate chain can be produced without " +
        "unsupported algorithms")
    @ModelFromScope(baseModel = ModelType.CERTIFICATE)
    @SecurityCategory(SeverityLevel.HIGH)
    @CryptoCategory(SeverityLevel.HIGH)
    @CertificateCategory(SeverityLevel.HIGH)
    @ComplianceCategory(SeverityLevel.HIGH)
    @AlertCategory(SeverityLevel.LOW)
    @ExplicitValues(affectedTypes = BasicDerivationType.SIG_HASH_ALGORIHTM, methods = "getUnproposedSignatureAndHashAlgorithms")
    public void acceptsUnproposedSignatureAndHash(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);

        WorkflowTrace workflowTrace = runner.generateWorkflowTraceUntilReceivingMessage(WorkflowTraceType.HANDSHAKE, ProtocolMessageType.CHANGE_CIPHER_SPEC);
        workflowTrace.addTlsAction(new ReceiveAction(new AlertMessage()));
        
        runner.execute(workflowTrace, c).validateFinal(i -> {
            Validator.receivedFatalAlert(i);
        });
    }


    @TlsTest(description = "The receiver of a CertificateVerify message MUST verify the signature " +
            "field.  [...] If the verification fails, the receiver MUST terminate the handshake " +
            "with a \"decrypt_error\" alert.")
    @SecurityCategory(SeverityLevel.CRITICAL)
    @CryptoCategory(SeverityLevel.CRITICAL)
    @CertificateCategory(SeverityLevel.CRITICAL)
    @AlertCategory(SeverityLevel.MEDIUM)
    public void emptySignature(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);

        WorkflowTrace trace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);
        trace.addTlsActions(
                new ReceiveAction(new AlertMessage())
        );


        trace.getFirstSendMessage(CertificateVerifyMessage.class)
                .setSignature(Modifiable.explicit(new byte[]{}));


        runner.execute(trace, c).validateFinal(i -> {
            Validator.receivedFatalAlert(i);

            AlertMessage alert = i.getWorkflowTrace().getFirstReceivedMessage(AlertMessage.class);
            Validator.testAlertDescription(i, AlertDescription.DECRYPT_ERROR, alert);
        });
    }

    @TlsTest(description = "Send a Certificate Verify message with an empty Signature Algorithm field")
    @SecurityCategory(SeverityLevel.HIGH)
    @CryptoCategory(SeverityLevel.HIGH)
    @AlertCategory(SeverityLevel.LOW)
    @CertificateCategory(SeverityLevel.HIGH)
    @ComplianceCategory(SeverityLevel.CRITICAL)
    public void emptySigAlgorithm(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);

        WorkflowTrace trace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);
        trace.addTlsActions(
                new ReceiveAction(new AlertMessage())
        );


        trace.getFirstSendMessage(CertificateVerifyMessage.class)
                .setSignatureHashAlgorithm(Modifiable.explicit(new byte[]{}));

        runner.execute(trace, c).validateFinal(Validator::receivedFatalAlert);
    }

    @TlsTest(description = "Send a Certificate Verify message with an empty Signature Algorithm and empty Signature field")
    @SecurityCategory(SeverityLevel.CRITICAL)
    @CryptoCategory(SeverityLevel.CRITICAL)
    @AlertCategory(SeverityLevel.LOW)
    @CertificateCategory(SeverityLevel.CRITICAL)
    public void emptyBoth(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);

        WorkflowTrace trace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);
        trace.addTlsActions(
                new ReceiveAction(new AlertMessage())
        );


        trace.getFirstSendMessage(CertificateVerifyMessage.class)
                .setSignatureHashAlgorithm(Modifiable.explicit(new byte[]{}));
        trace.getFirstSendMessage(CertificateVerifyMessage.class)
                .setSignature(Modifiable.explicit(new byte[]{}));

        runner.execute(trace, c).validateFinal(Validator::receivedFatalAlert);
    }


}
