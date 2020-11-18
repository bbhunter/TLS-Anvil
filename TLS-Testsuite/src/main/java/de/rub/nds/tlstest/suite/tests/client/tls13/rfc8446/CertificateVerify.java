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
import de.rub.nds.tlsattacker.core.constants.HashAlgorithm;
import de.rub.nds.tlsattacker.core.constants.SignatureAlgorithm;
import de.rub.nds.tlsattacker.core.constants.SignatureAndHashAlgorithm;
import de.rub.nds.tlsattacker.core.protocol.message.AlertMessage;
import de.rub.nds.tlsattacker.core.protocol.message.CertificateVerifyMessage;
import de.rub.nds.tlsattacker.core.protocol.message.FinishedMessage;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlstest.framework.Validator;
import de.rub.nds.tlstest.framework.annotations.ClientTest;
import de.rub.nds.tlstest.framework.annotations.ExplicitValues;
import de.rub.nds.tlstest.framework.annotations.ManualConfig;
import de.rub.nds.tlstest.framework.annotations.MethodCondition;
import de.rub.nds.tlstest.framework.annotations.RFC;
import de.rub.nds.tlstest.framework.annotations.ScopeExtensions;
import de.rub.nds.tlstest.framework.annotations.TlsTest;
import de.rub.nds.tlstest.framework.annotations.categories.Security;
import de.rub.nds.tlstest.framework.constants.SeverityLevel;
import de.rub.nds.tlstest.framework.execution.AnnotatedStateContainer;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.model.DerivationType;
import de.rub.nds.tlstest.framework.model.derivationParameter.DerivationParameter;
import de.rub.nds.tlstest.framework.model.derivationParameter.SigAndHashDerivation;
import de.rub.nds.tlstest.framework.testClasses.Tls13Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
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
    
    public List<DerivationParameter> getLegacyRSASAHAlgorithms() {
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
            "MUST NOT be used in any signatures of CertificateVerify messages.")
    @Security(SeverityLevel.MEDIUM)
    @ScopeExtensions(DerivationType.SIG_HASH_ALGORIHTM)
    @ExplicitValues(affectedTypes=DerivationType.SIG_HASH_ALGORIHTM,methods="getLegacyRSASAHAlgorithms")
    @ManualConfig(DerivationType.SIG_HASH_ALGORIHTM)
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

    @TlsTest(description = "RSA signatures MUST use an RSASSA-PSS algorithm, " +
            "regardless of whether RSASSA-PKCS1-v1_5 algorithms " +
            "appear in \"signature_algorithms\". The SHA-1 algorithm " +
            "MUST NOT be used in any signatures of CertificateVerify messages.", securitySeverity = SeverityLevel.MEDIUM)
    @MethodCondition(method = "supportsLegacyECDSASAHAlgorithms")
    public void selectLegacyECDSASignatureAlgorithm(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);
        SignatureAndHashAlgorithm selsectedLegacySigHash = SignatureAndHashAlgorithm.ECDSA_SHA1;

        c.setPreferedCertificateSignatureType(CertificateKeyType.ECDSA);
        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);
        workflowTrace.addTlsActions(new ReceiveAction(new AlertMessage()));

        workflowTrace.getFirstSendMessage(CertificateVerifyMessage.class)
                .setSignatureHashAlgorithm(Modifiable.explicit(SignatureAndHashAlgorithm.ECDSA_SHA1.getByteValue()));


        runner.execute(workflowTrace, c).validateFinal(Validator::receivedFatalAlert);
    }
    
    public List<DerivationParameter> getSigHashAlgorithms() {
        List<DerivationParameter> parameterValues = new LinkedList<>();
        List<CertificateKeyType> certificateKeyTypes = new ArrayList<CertificateKeyType>(){{
            add(CertificateKeyType.RSA);
            //TODO: add more CertificateKeyTypes?
        }};
        for (CertificateKeyType keyType : certificateKeyTypes) {
            List<SignatureAndHashAlgorithm> algorithms = context.getSiteReport().getSupportedSignatureAndHashAlgorithms().stream()
                    .filter(i -> i.getSignatureAlgorithm().toString().contains(keyType.toString()) &&
                            i.getHashAlgorithm() != HashAlgorithm.SHA1 &&
                            i.getSignatureAlgorithm() != SignatureAlgorithm.RSA &&
                            i.getSignatureAlgorithm() != SignatureAlgorithm.RSA_PSS_PSS)
                    .collect(Collectors.toList());
            for (SignatureAndHashAlgorithm sigHashAlg : algorithms) {
                parameterValues.add(new SigAndHashDerivation(sigHashAlg));
            }
        } 
        
        return parameterValues;
    }

    @TlsTest(description = "The receiver of a CertificateVerify message MUST verify " +
            "the signature field. If the verification fails, " +
            "the receiver MUST terminate the handshake with a \"decrypt_error\" alert.")
    @Security(SeverityLevel.MEDIUM)
    @ScopeExtensions(DerivationType.SIG_HASH_ALGORIHTM)
    @ExplicitValues(affectedTypes=DerivationType.SIG_HASH_ALGORIHTM,methods="getSigHashAlgorithms")
    public void invalidSignature(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);
        SignatureAndHashAlgorithm selsectedSigHash = derivationContainer.getDerivation(SigAndHashDerivation.class).getSelectedValue();

        //TODO: update this if more CertKey Types are used
        c.setPreferedCertificateSignatureType(CertificateKeyType.RSA);

        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);
        workflowTrace.addTlsActions(new ReceiveAction(new AlertMessage()));

        CertificateVerifyMessage msg = workflowTrace.getFirstSendMessage(CertificateVerifyMessage.class);
        msg.setSignatureHashAlgorithm(Modifiable.explicit(selsectedSigHash.getByteValue()));
        msg.setSignature(Modifiable.xor(new byte[]{0x01}, 0));


        runner.execute(workflowTrace, c).validateFinal(i -> {
            Validator.receivedFatalAlert(i);

            AlertMessage amsg = i.getWorkflowTrace().getFirstReceivedMessage(AlertMessage.class);
            if (amsg == null) return;
            Validator.testAlertDescription(i, AlertDescription.DECRYPT_ERROR, amsg);
        });
    }


    @TlsTest(description = "Servers MUST send this message when authenticating via a certificate.")
    @Security(SeverityLevel.CRITICAL)
    public void omitCertificateVerify(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);

        WorkflowTrace trace = runner.generateWorkflowTraceUntilSendingMessage(WorkflowTraceType.HELLO, HandshakeMessageType.CERTIFICATE_VERIFY);
        trace.addTlsActions(
                new SendAction(new FinishedMessage()),
                new ReceiveAction(new AlertMessage())
        );

        runner.execute(trace, c).validateFinal(Validator::receivedFatalAlert);
    }

    @TlsTest(description = "Servers MUST send this message when authenticating via a certificate.")
    @Security(SeverityLevel.CRITICAL)
    public void emptySignature(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);

        WorkflowTrace trace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);
        trace.addTlsActions(
                new ReceiveAction(new AlertMessage())
        );


        trace.getFirstSendMessage(CertificateVerifyMessage.class)
                .setSignature(Modifiable.explicit(new byte[]{}));


        runner.execute(trace, c).validateFinal(Validator::receivedFatalAlert);
    }

    @TlsTest(description = "Servers MUST send this message when authenticating via a certificate.")
    @Security(SeverityLevel.CRITICAL)
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

    @TlsTest(description = "Servers MUST send this message when authenticating via a certificate.")
    @Security(SeverityLevel.CRITICAL)
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
