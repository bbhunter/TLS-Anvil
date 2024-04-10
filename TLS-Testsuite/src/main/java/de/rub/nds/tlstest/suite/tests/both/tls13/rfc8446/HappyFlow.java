/**
 * TLS-Testsuite - A testsuite for the TLS protocol
 *
 * <p>Copyright 2022 Ruhr University Bochum
 *
 * <p>Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.suite.tests.both.tls13.rfc8446;

import de.rub.nds.anvilcore.annotation.AnvilTest;
import de.rub.nds.anvilcore.coffee4j.model.ModelFromScope;
import de.rub.nds.anvilcore.teststate.AnvilTestCase;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.AlertDescription;
import de.rub.nds.tlsattacker.core.constants.ProtocolMessageType;
import de.rub.nds.tlsattacker.core.protocol.message.AlertMessage;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceUtil;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlstest.framework.Validator;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.testClasses.Tls13Test;
import org.junit.jupiter.api.Tag;

/** Runs a benign handshake with all default derivations to identify parameter-related bugs. */
@Tag("happyflow13")
public class HappyFlow extends Tls13Test {

    @AnvilTest(id = "8446-jVohiUKi4u")
    @ModelFromScope(modelType = "CERTIFICATE")
    public void happyFlow(AnvilTestCase testCase, WorkflowRunner runner) {
        Config c = getPreparedConfig(runner);

        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HANDSHAKE);
        State state = runner.execute(workflowTrace, c);

        boolean receivedAlert =
                WorkflowTraceUtil.didReceiveMessage(
                        ProtocolMessageType.ALERT, state.getWorkflowTrace());
        if (receivedAlert) {
            AlertMessage alert =
                    (AlertMessage)
                            WorkflowTraceUtil.getFirstReceivedMessage(
                                    ProtocolMessageType.ALERT, state.getWorkflowTrace());
            LOGGER.error(
                    "Received Alert "
                            + AlertDescription.getAlertDescription(
                                    alert.getDescription().getValue())
                            + " for Happy Flow using derivations:\n"
                            + parameterCombination.toString()
                            + "\nWorkflowTrace:\n"
                            + state.getWorkflowTrace().toString());
        }
        Validator.executedAsPlanned(state, testCase);
    }
}
