/**
 * TLS-Testsuite - A testsuite for the TLS protocol
 *
 * <p>Copyright 2022 Ruhr University Bochum
 *
 * <p>Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.suite.tests.client.tls13.statemachine;

import de.rub.nds.anvilcore.annotation.AnvilTest;
import de.rub.nds.anvilcore.annotation.ClientTest;
import de.rub.nds.anvilcore.annotation.ExcludeParameter;
import de.rub.nds.anvilcore.annotation.NonCombinatorialAnvilTest;
import de.rub.nds.anvilcore.coffee4j.model.ModelFromScope;
import de.rub.nds.anvilcore.teststate.AnvilTestCase;
import de.rub.nds.modifiablevariable.util.Modifiable;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.AlertDescription;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.constants.ProtocolMessageType;
import de.rub.nds.tlsattacker.core.protocol.message.*;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.action.DeactivateEncryptionAction;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.action.SetEncryptChangeCipherSpecConfigAction;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlstest.framework.Validator;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.testClasses.Tls13Test;
import de.rub.nds.tlstest.suite.tests.client.both.statemachine.SharedStateMachineTest;
import org.junit.jupiter.api.Tag;

/**
 * Contains tests to evaluate the target's state machine. Some test flows are based on results found
 * for TLS 1.2 servers in "Protocol State Fuzzing of TLS Implementations" (de Ruiter et al.)
 */
@Tag("statemachine")
@ClientTest
public class StateMachine extends Tls13Test {

    @AnvilTest(id = "XSM-LdxAqeL2Te")
    @ModelFromScope(modelType = "CERTIFICATE")
    public void sendFinishedWithoutCert(AnvilTestCase testCase, WorkflowRunner runner) {
        Config c = getPreparedConfig(runner);
        WorkflowTrace workflowTrace =
                runner.generateWorkflowTraceUntilSendingMessage(
                        WorkflowTraceType.HELLO, HandshakeMessageType.CERTIFICATE);
        workflowTrace.addTlsActions(
                new SendAction(new FinishedMessage()), new ReceiveAction(new AlertMessage()));

        State state = runner.execute(workflowTrace, c);
        Validator.receivedFatalAlert(state, testCase);
    }

    @AnvilTest(id = "XSM-2iKDTUhXF5")
    @ExcludeParameter("INCLUDE_CHANGE_CIPHER_SPEC")
    @ModelFromScope(modelType = "CERTIFICATE")
    public void sendHandshakeTrafficSecretEncryptedChangeCipherSpec(
            AnvilTestCase testCase, WorkflowRunner runner) {
        Config config = getPreparedConfig(runner);
        config.setTls13BackwardsCompatibilityMode(true);
        WorkflowTrace workflowTrace =
                runner.generateWorkflowTraceUntilLastSendingMessage(
                        WorkflowTraceType.HELLO, ProtocolMessageType.CHANGE_CIPHER_SPEC);

        workflowTrace.addTlsAction(new SetEncryptChangeCipherSpecConfigAction(true));
        SendAction sendActionEncryptedCCS = new SendAction(new ChangeCipherSpecMessage());

        workflowTrace.addTlsAction(sendActionEncryptedCCS);
        workflowTrace.addTlsAction(new ReceiveAction(new AlertMessage()));

        State state = runner.execute(workflowTrace, config);
        Validator.receivedFatalAlert(state, testCase);
    }

    @AnvilTest(id = "XSM-Xb6pAYY3fT")
    @ModelFromScope(modelType = "CERTIFICATE")
    public void sendAppTrafficSecretEncryptedChangeCipherSpec(
            AnvilTestCase testCase, WorkflowRunner runner) {
        Config config = getPreparedConfig(runner);
        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HANDSHAKE);

        workflowTrace.addTlsAction(new SetEncryptChangeCipherSpecConfigAction(true));
        SendAction sendActionEncryptedCCS = new SendAction(new ChangeCipherSpecMessage());

        workflowTrace.addTlsAction(sendActionEncryptedCCS);
        workflowTrace.addTlsAction(new ReceiveAction(new AlertMessage()));

        State state = runner.execute(workflowTrace, config);
        Validator.receivedFatalAlert(state, testCase);
    }

    @AnvilTest(id = "XSM-sHFfpjZxQh")
    @ModelFromScope(modelType = "CERTIFICATE")
    public void sendLegacyChangeCipherSpecAfterFinished(
            AnvilTestCase testCase, WorkflowRunner runner) {
        Config config = getPreparedConfig(runner);
        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HANDSHAKE);
        workflowTrace.addTlsAction(new SendAction(new ChangeCipherSpecMessage()));
        workflowTrace.addTlsAction(new ReceiveAction(new AlertMessage()));

        State state = runner.execute(workflowTrace, config);
        Validator.receivedFatalAlert(state, testCase);
    }

    @AnvilTest(id = "XSM-gN2Mz9wD2D")
    @ModelFromScope(modelType = "CERTIFICATE")
    public void sendLegacyFlowCertificate(AnvilTestCase testCase, WorkflowRunner runner) {
        Config config = getPreparedConfig(runner);
        WorkflowTrace workflowTrace =
                runner.generateWorkflowTraceUntilSendingMessage(
                        WorkflowTraceType.HELLO, HandshakeMessageType.SERVER_HELLO);
        workflowTrace.addTlsAction(new SendAction(new ServerHelloMessage(config)));
        workflowTrace.addTlsAction(new DeactivateEncryptionAction());
        workflowTrace.addTlsAction(new SendAction(new CertificateMessage()));
        workflowTrace.addTlsAction(new ReceiveAction(new AlertMessage()));

        State state = runner.execute(workflowTrace, config);
        Validator.receivedFatalAlert(state, testCase);
    }

    @AnvilTest(id = "XSM-aWBzNYEKwz")
    @ModelFromScope(modelType = "CERTIFICATE")
    public void sendLegacyFlowECDHEKeyExchange(AnvilTestCase testCase, WorkflowRunner runner) {
        Config config = getPreparedConfig(runner);
        WorkflowTrace workflowTrace =
                runner.generateWorkflowTraceUntilSendingMessage(
                        WorkflowTraceType.HELLO, HandshakeMessageType.SERVER_HELLO);
        workflowTrace.addTlsAction(new SendAction(new ServerHelloMessage(config)));
        workflowTrace.addTlsAction(new DeactivateEncryptionAction());
        workflowTrace.addTlsAction(
                new SendAction(new CertificateMessage(), new ECDHEServerKeyExchangeMessage()));
        workflowTrace.addTlsAction(new ReceiveAction(new AlertMessage()));

        State state = runner.execute(workflowTrace, config);
        Validator.receivedFatalAlert(state, testCase);
    }

    @AnvilTest(id = "XSM-F8VTZ3optN")
    @ModelFromScope(modelType = "CERTIFICATE")
    public void sendLegacyFlowDHEKeyExchange(AnvilTestCase testCase, WorkflowRunner runner) {
        Config config = getPreparedConfig(runner);
        WorkflowTrace workflowTrace =
                runner.generateWorkflowTraceUntilSendingMessage(
                        WorkflowTraceType.HELLO, HandshakeMessageType.SERVER_HELLO);
        workflowTrace.addTlsAction(new SendAction(new ServerHelloMessage(config)));
        workflowTrace.addTlsAction(new DeactivateEncryptionAction());
        workflowTrace.addTlsAction(
                new SendAction(new CertificateMessage(), new DHEServerKeyExchangeMessage()));
        workflowTrace.addTlsAction(new ReceiveAction(new AlertMessage()));

        State state = runner.execute(workflowTrace, config);
        Validator.receivedFatalAlert(state, testCase);
    }

    @NonCombinatorialAnvilTest(id = "XSM-iTKLQBFN9A")
    public void beginWithApplicationData(WorkflowRunner runner, AnvilTestCase testCase) {
        Config config = getConfig();
        SharedStateMachineTest.sharedBeginWithApplicationDataTest(config, runner, testCase);
    }

    @NonCombinatorialAnvilTest(id = "XSM-TQQj27kntr")
    public void beginWithFinished(WorkflowRunner runner, AnvilTestCase testCase) {
        Config config = getConfig();
        SharedStateMachineTest.sharedBeginWithFinishedTest(config, runner, testCase);
    }

    @AnvilTest(id = "XSM-FLPgMqSvg9")
    public void sendServerHelloTwice(AnvilTestCase testCase, WorkflowRunner runner) {
        Config config = getPreparedConfig(runner);
        SharedStateMachineTest.sharedSendServerHelloTwiceTest(config, runner, testCase);
    }

    @AnvilTest(id = "XSM-LrxDfiLZM5")
    @ModelFromScope(modelType = "CERTIFICATE")
    public void sendEndOfEarlyDataAsServer(AnvilTestCase testCase, WorkflowRunner runner) {
        Config config = getPreparedConfig(runner);
        WorkflowTrace workflowTrace =
                runner.generateWorkflowTraceUntilSendingMessage(
                        WorkflowTraceType.HELLO, HandshakeMessageType.FINISHED);
        EndOfEarlyDataMessage endOfEarlyData = new EndOfEarlyDataMessage();
        endOfEarlyData.setAdjustContext(Modifiable.explicit(Boolean.FALSE));
        workflowTrace.addTlsAction(new SendAction(endOfEarlyData));
        workflowTrace.addTlsAction(new ReceiveAction(new AlertMessage()));

        State state = runner.execute(workflowTrace, config);

        Validator.receivedFatalAlert(state, testCase);
        AlertMessage msg = state.getWorkflowTrace().getFirstReceivedMessage(AlertMessage.class);
        Validator.testAlertDescription(state, testCase, AlertDescription.UNEXPECTED_MESSAGE, msg);
    }

    @AnvilTest(id = "XSM-gN61eQrmNv")
    public void omitCertificateVerify(AnvilTestCase testCase, WorkflowRunner runner) {
        Config c = getPreparedConfig(runner);

        WorkflowTrace trace =
                runner.generateWorkflowTraceUntilSendingMessage(
                        WorkflowTraceType.HELLO, HandshakeMessageType.CERTIFICATE_VERIFY);
        trace.addTlsActions(
                new SendAction(new FinishedMessage()), new ReceiveAction(new AlertMessage()));

        State state = runner.execute(trace, c);
        Validator.receivedFatalAlert(state, testCase);
    }
}
