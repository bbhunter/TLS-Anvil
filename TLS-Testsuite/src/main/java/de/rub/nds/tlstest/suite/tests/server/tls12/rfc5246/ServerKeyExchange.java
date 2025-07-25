/**
 * TLS-Testsuite - A testsuite for the TLS protocol
 *
 * <p>Copyright 2022 Ruhr University Bochum
 *
 * <p>Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.suite.tests.server.tls12.rfc5246;

import static org.junit.jupiter.api.Assertions.*;

import de.rub.nds.anvilcore.annotation.AnvilTest;
import de.rub.nds.anvilcore.annotation.DynamicValueConstraints;
import de.rub.nds.anvilcore.annotation.ServerTest;
import de.rub.nds.anvilcore.teststate.AnvilTestCase;
import de.rub.nds.modifiablevariable.util.ArrayConverter;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.*;
import de.rub.nds.tlsattacker.core.protocol.message.*;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceResultUtil;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlstest.framework.Validator;
import de.rub.nds.tlstest.framework.annotations.KeyExchange;
import de.rub.nds.tlstest.framework.constants.KeyExchangeType;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.testClasses.Tls12Test;
import de.rub.nds.tlstest.suite.util.SignatureValidation;
import de.rub.nds.x509attacker.constants.X509PublicKeyType;
import java.util.Arrays;
import org.junit.jupiter.api.Tag;

/** */
@Tag("signature")
@ServerTest
public class ServerKeyExchange extends Tls12Test {

    @AnvilTest(id = "5246-nZ7mATYszU")
    @KeyExchange(supported = KeyExchangeType.ALL12, requiresServerKeyExchMsg = true)
    @DynamicValueConstraints(
            affectedIdentifiers = "CIPHER_SUITE",
            methods = "isSupportedCipherSuite")
    public void signatureIsValid(AnvilTestCase testCase, WorkflowRunner runner) {
        Config config = getPreparedConfig(runner);
        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HANDSHAKE);

        State state = runner.execute(workflowTrace, config);
        Validator.executedAsPlanned(state, testCase);
        assertTrue(
                signatureValid(state),
                "Server Key Exchange Message contained an invalid signature");
    }

    private Boolean signatureValid(State state) {
        WorkflowTrace executedTrace = state.getWorkflowTrace();
        ClientHelloMessage clientHello =
                (ClientHelloMessage)
                        WorkflowTraceResultUtil.getFirstSentMessage(
                                executedTrace, HandshakeMessageType.CLIENT_HELLO);
        ServerHelloMessage serverHello =
                (ServerHelloMessage)
                        WorkflowTraceResultUtil.getFirstReceivedMessage(
                                executedTrace, HandshakeMessageType.SERVER_HELLO);
        ServerKeyExchangeMessage serverKeyExchange =
                (ServerKeyExchangeMessage)
                        WorkflowTraceResultUtil.getFirstReceivedMessage(
                                executedTrace, HandshakeMessageType.SERVER_KEY_EXCHANGE);

        byte[] signedKeyExchangeParameters = getSignedDataFromKeyExchangeMessage(serverKeyExchange);
        byte[] completeSignedData =
                ArrayConverter.concatenate(
                        clientHello.getRandom().getValue(),
                        serverHello.getRandom().getValue(),
                        signedKeyExchangeParameters);

        byte[] givenSignature = serverKeyExchange.getSignature().getValue();
        SignatureAndHashAlgorithm selectedSignatureAndHashAlgo =
                SignatureAndHashAlgorithm.getSignatureAndHashAlgorithm(
                        serverKeyExchange.getSignatureAndHashAlgorithm().getValue());

        return SignatureValidation.validationSuccessful(
                selectedSignatureAndHashAlgo, state, completeSignedData, givenSignature);
    }

    private byte[] getSignedDataFromKeyExchangeMessage(ServerKeyExchangeMessage serverKeyExchange) {
        if (serverKeyExchange instanceof ECDHEServerKeyExchangeMessage) {
            ECDHEServerKeyExchangeMessage ecdheServerKeyExchange =
                    (ECDHEServerKeyExchangeMessage) serverKeyExchange;
            byte[] curveType = new byte[1];
            curveType[0] = ecdheServerKeyExchange.getGroupType().getValue();
            byte[] namedCurve = ecdheServerKeyExchange.getNamedGroup().getValue();
            byte[] publicKeyLength =
                    ecdheServerKeyExchange
                            .getPublicKeyLength()
                            .getByteArray(HandshakeByteLength.ECDHE_PARAM_LENGTH);
            byte[] publicKey = serverKeyExchange.getPublicKey().getValue();
            return ArrayConverter.concatenate(curveType, namedCurve, publicKeyLength, publicKey);
        } else if (serverKeyExchange instanceof DHEServerKeyExchangeMessage) {
            DHEServerKeyExchangeMessage dheServerKeyExchange =
                    (DHEServerKeyExchangeMessage) serverKeyExchange;
            return ArrayConverter.concatenate(
                    ArrayConverter.intToBytes(
                            dheServerKeyExchange.getModulusLength().getValue(),
                            HandshakeByteLength.DH_MODULUS_LENGTH),
                    dheServerKeyExchange.getModulus().getValue(),
                    ArrayConverter.intToBytes(
                            dheServerKeyExchange.getGeneratorLength().getValue(),
                            HandshakeByteLength.DH_GENERATOR_LENGTH),
                    dheServerKeyExchange.getGenerator().getValue(),
                    ArrayConverter.intToBytes(
                            dheServerKeyExchange.getPublicKeyLength().getValue(),
                            HandshakeByteLength.DH_PUBLICKEY_LENGTH),
                    dheServerKeyExchange.getPublicKey().getValue());

        } else {
            throw new AssertionError("Unsupported ServerKeyExchange type");
        }
    }

    public boolean isSupportedCipherSuite(CipherSuite cipherSuiteCandidate) {
        return cipherSuiteCandidate.isRealCipherSuite()
                && !cipherSuiteCandidate.isTls13()
                && cipherSuiteCandidate.isEphemeral()
                && (Arrays.stream(
                                AlgorithmResolver.getSuitableLeafCertificateKeyType(
                                        cipherSuiteCandidate))
                        .anyMatch(
                                kt ->
                                        kt == X509PublicKeyType.ECDH_ECDSA
                                                || kt == X509PublicKeyType.RSA
                                                || kt == X509PublicKeyType.DSA));
    }
}
