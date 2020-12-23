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

import de.rub.nds.modifiablevariable.VariableModification;
import de.rub.nds.modifiablevariable.bytearray.ByteArrayModificationFactory;
import de.rub.nds.modifiablevariable.bytearray.ModifiableByteArray;
import de.rub.nds.modifiablevariable.util.Modifiable;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.AlertDescription;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.protocol.message.AlertMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerHelloDoneMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.SupportedVersionsExtensionMessage;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.action.executor.ActionOption;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlstest.framework.TestContext;
import de.rub.nds.tlstest.framework.Validator;
import de.rub.nds.tlstest.framework.annotations.ClientTest;
import de.rub.nds.tlstest.framework.annotations.ExplicitValues;
import de.rub.nds.tlstest.framework.annotations.KeyExchange;
import de.rub.nds.tlstest.framework.annotations.RFC;
import de.rub.nds.tlstest.framework.annotations.ScopeExtensions;
import de.rub.nds.tlstest.framework.annotations.ScopeLimitations;
import de.rub.nds.tlstest.framework.annotations.TlsTest;
import de.rub.nds.tlstest.framework.annotations.categories.Interoperability;
import de.rub.nds.tlstest.framework.annotations.categories.Security;
import de.rub.nds.tlstest.framework.coffee4j.model.ModelFromScope;
import de.rub.nds.tlstest.framework.constants.AssertMsgs;
import de.rub.nds.tlstest.framework.constants.KeyExchangeType;
import de.rub.nds.tlstest.framework.constants.SeverityLevel;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.model.DerivationType;
import de.rub.nds.tlstest.framework.model.ModelType;
import de.rub.nds.tlstest.framework.model.derivationParameter.DerivationParameter;
import de.rub.nds.tlstest.framework.model.derivationParameter.ProtocolVersionDerivation;
import de.rub.nds.tlstest.framework.testClasses.Tls13Test;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;

import java.util.Random;

@RFC(number = 8446, section = "4.1.3 Server Hello")
@ClientTest
public class ServerHello extends Tls13Test {

    @TlsTest(description = "A client which receives a legacy_session_id_echo " +
            "field that does not match what it sent in the ClientHello MUST " +
            "abort the handshake with an \"illegal_parameter\" alert.")
    @ModelFromScope(baseModel = ModelType.CERTIFICATE)
    @Interoperability(SeverityLevel.HIGH)
    public void testSessionId(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);

        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);
        workflowTrace.addTlsActions(
                new ReceiveAction(new AlertMessage())
        );
        sharedSessionIdTest(workflowTrace, runner);
    }
    
    public static void sharedSessionIdTest(WorkflowTrace workflowTrace, WorkflowRunner runner) {
        ServerHelloMessage sh = workflowTrace.getFirstSendMessage(ServerHelloMessage.class);
        sh.setSessionId(Modifiable.explicit(new byte[]{0x01, 0x02, 0x03, 0x04}));


        runner.execute(workflowTrace, runner.getPreparedConfig()).validateFinal(i -> {
            WorkflowTrace trace = i.getWorkflowTrace();
            Validator.receivedFatalAlert(i);

            AlertMessage msg = trace.getFirstReceivedMessage(AlertMessage.class);
            if (msg == null) {
                return;
            }
            Validator.testAlertDescription(i, AlertDescription.ILLEGAL_PARAMETER, msg);
        });
    }

    @TlsTest(description = "A client which receives a cipher suite that was " +
            "not offered MUST abort the handshake with " +
            "an \"illegal_parameter\" alert.")
    @ModelFromScope(baseModel = ModelType.CERTIFICATE)
    @Interoperability(SeverityLevel.CRITICAL)
    @Security(SeverityLevel.HIGH)
    @ScopeLimitations(DerivationType.CIPHERSUITE)
    public void testCipherSuite(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);

        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);
        workflowTrace.addTlsActions(
                new ReceiveAction(new AlertMessage())
        );

        ServerHelloMessage sh = workflowTrace.getFirstSendMessage(ServerHelloMessage.class);
        sh.setSelectedCipherSuite(Modifiable.explicit(CipherSuite.GREASE_00.getByteValue()));


        runner.execute(workflowTrace, c).validateFinal(i -> {
            WorkflowTrace trace = i.getWorkflowTrace();
            Validator.receivedFatalAlert(i);

            AlertMessage msg = trace.getFirstReceivedMessage(AlertMessage.class);
            if (msg == null) {
                return;
            }
            Validator.testAlertDescription(i, AlertDescription.ILLEGAL_PARAMETER, msg);
        });
    }


    @TlsTest(description = "legacy_compression_method: A single byte which " +
            "MUST have the value 0.")
    @ModelFromScope(baseModel = ModelType.CERTIFICATE)
    @Interoperability(SeverityLevel.MEDIUM)
    @Security(SeverityLevel.MEDIUM)
    public void testCompressionValue(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);

        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);
        workflowTrace.addTlsActions(
                new ReceiveAction(new AlertMessage())
        );
        sharedCompressionValueTest(workflowTrace, runner);
    }
    
    public static void sharedCompressionValueTest(WorkflowTrace workflowTrace, WorkflowRunner runner) {
        ServerHelloMessage sh = workflowTrace.getFirstSendMessage(ServerHelloMessage.class);
        sh.setSelectedCompressionMethod(Modifiable.explicit((byte) 0x01));

        runner.execute(workflowTrace, runner.getPreparedConfig()).validateFinal(i -> {
            WorkflowTrace trace = i.getWorkflowTrace();
            Validator.receivedFatalAlert(i);

            AlertMessage msg = trace.getFirstReceivedMessage(AlertMessage.class);
            if (msg == null) {
                return;
            }
            assertNotNull(AssertMsgs.AlertNotReceived, msg);
        });
    }


    @TlsTest(description = "TLS 1.3 clients receiving a ServerHello indicating TLS 1.2 or below MUST " +
            "check that the last 8 bytes are not equal to either of these values. " +
            "If a match is found, the client MUST abort the handshake " +
            "with an \"illegal_parameter\" alert.")
    @ModelFromScope(baseModel = ModelType.CERTIFICATE)
    @Interoperability(SeverityLevel.MEDIUM)
    @Security(SeverityLevel.HIGH)
    @KeyExchange(supported = KeyExchangeType.ALL12)
    public void testRandomDowngradeValue(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = prepareConfig(context.getConfig().createConfig(), argumentAccessor, runner);

        Random random = new Random();
        byte[] serverRandom = new byte[32];
        random.nextBytes(serverRandom);

        byte[] downgradeValue = new byte[]{0x44, 0x4F, 0x57, 0x4E, 0x47, 0x52, 0x44, 0x01};
        System.arraycopy(downgradeValue, 0, serverRandom, 24, downgradeValue.length);

        WorkflowTrace workflowTrace = runner.generateWorkflowTraceUntilSendingMessage(WorkflowTraceType.HANDSHAKE, HandshakeMessageType.SERVER_HELLO_DONE);
        workflowTrace.addTlsActions(
                new SendAction(ActionOption.MAY_FAIL, new ServerHelloDoneMessage()),
                new ReceiveAction(new AlertMessage())
        );

        workflowTrace.getFirstSendMessage(ServerHelloMessage.class).setRandom(Modifiable.explicit(serverRandom));

        runner.execute(workflowTrace, c).validateFinal(i -> {
            WorkflowTrace trace = i.getWorkflowTrace();
            Validator.receivedFatalAlert(i);

            AlertMessage msg = trace.getFirstReceivedMessage(AlertMessage.class);
            if (msg == null) {
                return;
            }
            Validator.testAlertDescription(i, AlertDescription.ILLEGAL_PARAMETER, msg);
        });
    }
    
    public List<DerivationParameter> getUnsupportedProtocolVersions() {
        SupportedVersionsExtensionMessage clientSupportedVersions = TestContext.getInstance().getReceivedClientHelloMessage().getExtension(SupportedVersionsExtensionMessage.class);
        List<DerivationParameter> parameterValues = new LinkedList<>();
        getUnsupportedTlsVersions(clientSupportedVersions).forEach(version -> parameterValues.add(new ProtocolVersionDerivation(version.getValue())));
        return parameterValues;
    }
    
    private List<ProtocolVersion> getUnsupportedTlsVersions(SupportedVersionsExtensionMessage clientSupportedVersions) {
        //negotiating SSL3 is a separate test
        List<ProtocolVersion> versions = new LinkedList<>();
        versions.add(ProtocolVersion.TLS10);
        versions.add(ProtocolVersion.TLS11);
        versions.add(ProtocolVersion.TLS12);
        
        byte[] supportedVersions = clientSupportedVersions.getSupportedVersions().getValue();
        int versionLength = clientSupportedVersions.getSupportedVersionsLength().getValue();
        
        for(int i = 0; i < versionLength; i+=2) {
            ProtocolVersion version = ProtocolVersion.getProtocolVersion(Arrays.copyOfRange(supportedVersions, i, i + 2));
            versions.remove(version);
        }
        
        return versions;
    }
    
    @TlsTest(description = "TLS 1.3 clients receiving a ServerHello indicating TLS 1.2 or below MUST " +
            "check that the last 8 bytes are not equal to either of these values. " +
            "If a match is found, the client MUST abort the handshake " +
            "with an \"illegal_parameter\" alert.")
    @Security(SeverityLevel.HIGH)
    @ScopeExtensions(DerivationType.PROTOCOL_VERSION)
    @ExplicitValues(affectedTypes = DerivationType.PROTOCOL_VERSION, methods = "getUnsupportedProtocolVersions")
    @KeyExchange(supported = KeyExchangeType.ALL12)
    public void selectUnsupportedProtocolVersion(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config config = prepareConfig(context.getConfig().createConfig(), argumentAccessor, runner);
        byte[] oldProtocolVersion = derivationContainer.getDerivation(ProtocolVersionDerivation.class).getSelectedValue();
        
        WorkflowTrace workflowTrace = runner.generateWorkflowTraceUntilSendingMessage(WorkflowTraceType.HELLO, HandshakeMessageType.SERVER_HELLO);
        ServerHelloMessage serverHello = new ServerHelloMessage(config);
        serverHello.setProtocolVersion(Modifiable.explicit(oldProtocolVersion));
        workflowTrace.addTlsAction(new SendAction(serverHello));
        workflowTrace.addTlsAction(new ReceiveAction(new AlertMessage()));
        
        runner.execute(workflowTrace, config).validateFinal(Validator::receivedFatalAlert);
    }

}
