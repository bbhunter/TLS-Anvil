/**
 * TLS-Testsuite - A testsuite for the TLS protocol
 *
 * <p>Copyright 2022 Ruhr University Bochum
 *
 * <p>Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.suite.tests.server.tls13.rfc8446;

import static org.junit.Assert.assertTrue;

import de.rub.nds.anvilcore.annotation.AnvilTest;
import de.rub.nds.anvilcore.annotation.MethodCondition;
import de.rub.nds.anvilcore.annotation.ServerTest;
import de.rub.nds.anvilcore.teststate.AnvilTestCase;
import de.rub.nds.modifiablevariable.util.Modifiable;
import de.rub.nds.scanner.core.probe.result.TestResults;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.ProtocolMessageType;
import de.rub.nds.tlsattacker.core.protocol.ProtocolMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ChangeCipherSpecMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ClientHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerHelloMessage;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceUtil;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlsscanner.core.constants.TlsAnalyzedProperty;
import de.rub.nds.tlstest.framework.Validator;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.testClasses.Tls13Test;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;

@ServerTest
public class MiddleboxCompatibility extends Tls13Test {

    public ConditionEvaluationResult sendsHelloRetryRequestForEmptyKeyShare() {
        if (context.getFeatureExtractionResult()
                        .getResult(TlsAnalyzedProperty.SENDS_HELLO_RETRY_REQUEST)
                == TestResults.TRUE) {
            return ConditionEvaluationResult.enabled("");
        }
        return ConditionEvaluationResult.disabled("Target does not send a Hello Retry Request");
    }

    @AnvilTest(id = "8446-bgegNHeUgg")
    @Tag("new")
    public void respectsClientCompatibilityWish(AnvilTestCase testCase, WorkflowRunner runner) {
        Config config = getPreparedConfig(runner);
        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);
        workflowTrace
                .getFirstSendMessage(ClientHelloMessage.class)
                .setSessionId(
                        Modifiable.explicit(config.getDefaultClientTicketResumptionSessionId()));
        State state = runner.execute(workflowTrace, config);

        Validator.executedAsPlanned(state, testCase);
        assertTrue(
                "Did not receive a compatibility CCS",
                WorkflowTraceUtil.didReceiveMessage(
                        ProtocolMessageType.CHANGE_CIPHER_SPEC, state.getWorkflowTrace()));
        List<ProtocolMessage> receivedMessages =
                WorkflowTraceUtil.getAllReceivedMessages(workflowTrace);
        for (ProtocolMessage receivedMessage : receivedMessages) {
            if (receivedMessage instanceof ServerHelloMessage) {
                assertTrue(
                        "Server did not send the compatibility CCS after the Server Hello",
                        receivedMessages.get(receivedMessages.indexOf(receivedMessage) + 1)
                                instanceof ChangeCipherSpecMessage);
            }
        }
    }

    @AnvilTest(id = "8446-vUL6yuqsbj")
    @MethodCondition(method = "sendsHelloRetryRequestForEmptyKeyShare")
    @Tag("new")
    public void respectsClientCompatibilityWishWithHrr(
            AnvilTestCase testCase, WorkflowRunner runner) {
        Config config = getPreparedConfig(runner);
        // enforce HRR
        config.setDefaultClientKeyShareNamedGroups(new LinkedList<>());
        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);
        workflowTrace
                .getFirstSendMessage(ClientHelloMessage.class)
                .setSessionId(
                        Modifiable.explicit(config.getDefaultClientTicketResumptionSessionId()));
        State state = runner.execute(workflowTrace, config);

        assertTrue(
                "Did not receive a compatibility CCS",
                WorkflowTraceUtil.didReceiveMessage(
                        ProtocolMessageType.CHANGE_CIPHER_SPEC, state.getWorkflowTrace()));
        List<ProtocolMessage> receivedMessages =
                WorkflowTraceUtil.getAllReceivedMessages(workflowTrace);
        for (ProtocolMessage receivedMessage : receivedMessages) {
            if (receivedMessage instanceof ServerHelloMessage) {
                assertTrue(
                        "Server did not send the compatibility CCS after the Hello Retry Request",
                        receivedMessages.get(receivedMessages.indexOf(receivedMessage) + 1)
                                instanceof ChangeCipherSpecMessage);
            }
        }
    }
}
