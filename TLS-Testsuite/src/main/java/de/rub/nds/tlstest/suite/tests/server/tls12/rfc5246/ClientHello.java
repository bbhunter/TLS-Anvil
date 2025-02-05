/**
 * TLS-Testsuite - A testsuite for the TLS protocol
 *
 * <p>Copyright 2022 Ruhr University Bochum
 *
 * <p>Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.suite.tests.server.tls12.rfc5246;

import static org.junit.Assert.assertFalse;

import de.rub.nds.anvilcore.annotation.AnvilTest;
import de.rub.nds.anvilcore.annotation.ExcludeParameter;
import de.rub.nds.anvilcore.annotation.ExcludeParameters;
import de.rub.nds.anvilcore.annotation.ServerTest;
import de.rub.nds.anvilcore.teststate.AnvilTestCase;
import de.rub.nds.modifiablevariable.util.Modifiable;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.ExtensionType;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.protocol.message.ClientHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerHelloDoneMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.ExtensionMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.GreaseExtensionMessage;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceResultUtil;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveTillAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlstest.framework.Validator;
import de.rub.nds.tlstest.framework.annotations.KeyExchange;
import de.rub.nds.tlstest.framework.constants.KeyExchangeType;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.model.derivationParameter.CipherSuiteDerivation;
import de.rub.nds.tlstest.framework.testClasses.Tls12Test;
import java.util.Arrays;
import org.junit.jupiter.api.Tag;

@ServerTest
public class ClientHello extends Tls12Test {

    @AnvilTest(id = "5246-ST5MN96BuF")
    public void unknownCipherSuite(AnvilTestCase testCase, WorkflowRunner runner) {
        Config c = getPreparedConfig(runner);

        ClientHelloMessage clientHelloMessage = new ClientHelloMessage(c);
        clientHelloMessage.setCipherSuites(Modifiable.insert(new byte[] {(byte) 0xfe, 0x00}, 0));

        WorkflowTrace workflowTrace = new WorkflowTrace();
        workflowTrace.addTlsActions(
                new SendAction(clientHelloMessage),
                new ReceiveTillAction(new ServerHelloDoneMessage()));

        State state = runner.execute(workflowTrace, c);
        Validator.executedAsPlanned(state, testCase);
    }

    @AnvilTest(id = "5246-P4AGQWTZsM")
    public void unknownCompressionMethod(AnvilTestCase testCase, WorkflowRunner runner) {
        Config c = getPreparedConfig(runner);

        ClientHelloMessage clientHelloMessage = new ClientHelloMessage(c);
        clientHelloMessage.setCompressions(Modifiable.explicit(new byte[] {0x00, 0x04}));

        WorkflowTrace workflowTrace = new WorkflowTrace();
        workflowTrace.addTlsActions(
                new SendAction(clientHelloMessage),
                new ReceiveTillAction(new ServerHelloDoneMessage()));

        State state = runner.execute(workflowTrace, c);
        Validator.executedAsPlanned(state, testCase);
    }

    @AnvilTest(id = "5246-YhZ7GJjrwk")
    public void includeUnknownExtension(AnvilTestCase testCase, WorkflowRunner runner) {
        Config config = getPreparedConfig(runner);
        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);

        // we use a Grease Extension for which we modify the type
        GreaseExtensionMessage greaseHelperExtension =
                new GreaseExtensionMessage(ExtensionType.GREASE_00, 32);
        greaseHelperExtension.setExtensionType(
                Modifiable.explicit(new byte[] {(byte) 0xBA, (byte) 0x9F}));

        ClientHelloMessage clientHello =
                (ClientHelloMessage)
                        WorkflowTraceResultUtil.getFirstSentMessage(
                                workflowTrace, HandshakeMessageType.CLIENT_HELLO);
        clientHello.addExtension(greaseHelperExtension);

        State state = runner.execute(workflowTrace, config);

        Validator.executedAsPlanned(state, testCase);
        ServerHelloMessage serverHello =
                (ServerHelloMessage)
                        WorkflowTraceResultUtil.getFirstReceivedMessage(
                                workflowTrace, HandshakeMessageType.SERVER_HELLO);
        if (serverHello.getExtensions() != null) {
            for (ExtensionMessage extension : serverHello.getExtensions()) {
                assertFalse(
                        "Server negotiated the undefined Extension",
                        Arrays.equals(
                                extension.getExtensionType().getValue(),
                                greaseHelperExtension.getType().getValue()));
            }
        }
    }

    @AnvilTest(id = "5246-RMehEjs346")
    @ExcludeParameter("INCLUDE_GREASE_CIPHER_SUITES")
    public void offerManyCipherSuites(AnvilTestCase testCase, WorkflowRunner runner) {
        Config c = getPreparedConfig(runner);

        // add pseudo cipher suites to reach 408, which is the sum of all
        // defined values and GREASE values
        CipherSuite selectedCipherSuite =
                parameterCombination.getParameter(CipherSuiteDerivation.class).getSelectedValue();
        byte[] explicitCipherSuites = new byte[408 * 2];
        byte firstByte = 0x0A;
        byte secondByte = 0;
        for (int i = 0; i < 407 * 2; i = i + 2) {
            explicitCipherSuites[i] = firstByte;
            explicitCipherSuites[i + 1] = secondByte;
            if (secondByte == (byte) 0xFF) {
                firstByte++;
            }
            secondByte++;
        }
        explicitCipherSuites[814] = selectedCipherSuite.getByteValue()[0];
        explicitCipherSuites[815] = selectedCipherSuite.getByteValue()[1];

        ClientHelloMessage clientHelloMessage = new ClientHelloMessage(c);
        clientHelloMessage.setCipherSuites(Modifiable.explicit(explicitCipherSuites));

        WorkflowTrace workflowTrace = new WorkflowTrace();
        workflowTrace.addTlsActions(
                new SendAction(clientHelloMessage),
                new ReceiveTillAction(new ServerHelloDoneMessage()));

        State state = runner.execute(workflowTrace, c);
        Validator.executedAsPlanned(state, testCase);
    }

    @AnvilTest(id = "5246-LtPf1AMt7Y")
    @ExcludeParameters({
        @ExcludeParameter("INCLUDE_ALPN_EXTENSION"),
        @ExcludeParameter("INCLUDE_ENCRYPT_THEN_MAC_EXTENSION"),
        @ExcludeParameter("INCLUDE_EXTENDED_MASTER_SECRET_EXTENSION"),
        @ExcludeParameter("INCLUDE_HEARTBEAT_EXTENSION"),
        @ExcludeParameter("INCLUDE_PADDING_EXTENSION"),
        @ExcludeParameter("INCLUDE_RENEGOTIATION_EXTENSION"),
        @ExcludeParameter("INCLUDE_SESSION_TICKET_EXTENSION"),
        @ExcludeParameter("MAX_FRAGMENT_LENGTH"),
        @ExcludeParameter("INCLUDE_GREASE_SIG_HASH_ALGORITHMS"),
        @ExcludeParameter("INCLUDE_GREASE_NAMED_GROUPS"),
        @ExcludeParameter("INCLUDE_PSK_EXCHANGE_MODES_EXTENSION")
    })
    @KeyExchange(supported = {KeyExchangeType.DH, KeyExchangeType.RSA})
    @Tag("new")
    public void leaveOutExtensions(AnvilTestCase testCase, WorkflowRunner runner) {
        Config config = getPreparedConfig(runner);
        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HANDSHAKE);
        ClientHelloMessage clientHello =
                workflowTrace.getFirstSendMessage(ClientHelloMessage.class);
        clientHello.setExtensionBytes(Modifiable.explicit(new byte[0]));
        State state = runner.execute(workflowTrace, config);
        Validator.executedAsPlanned(state, testCase);
    }
}
