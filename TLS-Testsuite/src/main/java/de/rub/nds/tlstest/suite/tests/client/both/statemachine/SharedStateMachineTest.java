/**
 * TLS-Testsuite - A testsuite for the TLS protocol
 *
 * <p>Copyright 2022 Ruhr University Bochum
 *
 * <p>Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.suite.tests.client.both.statemachine;

import de.rub.nds.anvilcore.teststate.AnvilTestCase;
import de.rub.nds.modifiablevariable.util.Modifiable;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.protocol.message.*;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlstest.framework.Validator;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;

/** Provides test and evaluation functionalities for both TLS 1.2 and 1.3 client state machines */
public class SharedStateMachineTest {

    public static void sharedSendServerHelloTwiceTest(
            Config config, WorkflowRunner runner, AnvilTestCase testCase) {
        runner.setPreparedConfig(config);
        WorkflowTrace workflowTrace = new WorkflowTrace();
        workflowTrace.addTlsAction(new ReceiveAction(new ClientHelloMessage()));
        ServerHelloMessage secondServerHello = new ServerHelloMessage(config);
        secondServerHello.setIncludeInDigest(Modifiable.explicit(false));
        secondServerHello.setAdjustContext(Modifiable.explicit(false));
        workflowTrace.addTlsAction(
                new SendAction(new ServerHelloMessage(config), secondServerHello));
        workflowTrace.addTlsAction(new ReceiveAction(new AlertMessage()));

        State state = runner.execute(workflowTrace, config);
        Validator.receivedFatalAlert(state, testCase);
    }

    public static void sharedBeginWithFinishedTest(
            Config config, WorkflowRunner runner, AnvilTestCase testCase) {
        runner.setPreparedConfig(config);
        WorkflowTrace workflowTrace =
                runner.generateWorkflowTraceUntilSendingMessage(
                        WorkflowTraceType.HELLO, HandshakeMessageType.SERVER_HELLO);
        FinishedMessage earlyFin = new FinishedMessage();
        workflowTrace.addTlsAction(new SendAction(earlyFin));
        workflowTrace.addTlsAction(new ReceiveAction(new AlertMessage()));
        State state = runner.execute(workflowTrace, config);
        Validator.receivedFatalAlert(state, testCase);
    }

    public static void sharedBeginWithApplicationDataTest(
            Config config, WorkflowRunner runner, AnvilTestCase testCase) {
        runner.setPreparedConfig(config);
        config.setDefaultApplicationMessageData("Test");
        WorkflowTrace workflowTrace =
                runner.generateWorkflowTraceUntilSendingMessage(
                        WorkflowTraceType.HELLO, HandshakeMessageType.SERVER_HELLO);
        ApplicationMessage applicationMessage = new ApplicationMessage();
        workflowTrace.addTlsAction(new SendAction(applicationMessage));
        workflowTrace.addTlsAction(new ReceiveAction(new AlertMessage()));
        State state = runner.execute(workflowTrace, config);
        Validator.receivedFatalAlert(state, testCase);
    }
}
