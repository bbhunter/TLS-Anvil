/**
 * TLS-Testsuite - A testsuite for the TLS protocol
 *
 * <p>Copyright 2022 Ruhr University Bochum
 *
 * <p>Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.suite.tests.both.tls12.rfc5246;

import de.rub.nds.anvilcore.annotation.*;
import de.rub.nds.anvilcore.coffee4j.model.ModelFromScope;
import de.rub.nds.anvilcore.teststate.AnvilTestCase;
import de.rub.nds.modifiablevariable.util.Modifiable;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.AlertDescription;
import de.rub.nds.tlsattacker.core.protocol.message.AlertMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ApplicationMessage;
import de.rub.nds.tlsattacker.core.record.Record;
import de.rub.nds.tlsattacker.core.record.RecordCryptoComputations;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlstest.framework.Validator;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.testClasses.Tls12Test;
import java.util.List;

public class AEADCiphers extends Tls12Test {

    @AnvilTest(id = "5246-7JhgKXeTXv")
    @ModelFromScope(modelType = "CERTIFICATE")
    @IncludeParameter("AUTH_TAG_BITMASK")
    @ValueConstraints({
        @ValueConstraint(identifier = "CIPHER_SUITE", method = "isAEAD"),
    })
    public void invalidAuthTag(AnvilTestCase testCase, WorkflowRunner runner) {
        Config c = getPreparedConfig(runner);
        byte[] modificationBitmask = parameterCombination.buildBitmask();

        Record record = new Record();
        record.setComputations(new RecordCryptoComputations());
        record.getComputations().setAuthenticationTag(Modifiable.xor(modificationBitmask, 0));

        SendAction sendAction = new SendAction(new ApplicationMessage());
        sendAction.setConfiguredRecords(List.of(record));

        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HANDSHAKE);
        workflowTrace.addTlsActions(sendAction, new ReceiveAction(new AlertMessage()));

        State state = runner.execute(workflowTrace, c);

        WorkflowTrace trace = state.getWorkflowTrace();
        Validator.receivedFatalAlert(state, testCase);

        AlertMessage msg = trace.getFirstReceivedMessage(AlertMessage.class);
        Validator.testAlertDescription(state, testCase, AlertDescription.BAD_RECORD_MAC, msg);
    }

    public boolean recordLengthAllowsModification(Integer lengthCandidate) {
        return lengthCandidate >= 50;
    }

    @AnvilTest(id = "5246-sYXZ8a3B4C")
    @ModelFromScope(modelType = "CERTIFICATE")
    @IncludeParameters({
        @IncludeParameter("CIPHERTEXT_BITMASK"),
        @IncludeParameter("APP_MSG_LENGHT")
    })
    @ValueConstraints({
        @ValueConstraint(identifier = "CIPHER_SUITE", method = "isAEAD"),
    })
    @DynamicValueConstraints(
            affectedIdentifiers = "RECORD_LENGTH",
            methods = "recordLengthAllowsModification")
    public void invalidCiphertext(AnvilTestCase testCase, WorkflowRunner runner) {
        Config c = getPreparedConfig(runner);
        byte[] modificationBitmask = parameterCombination.buildBitmask();

        Record record = new Record();
        record.setComputations(new RecordCryptoComputations());
        record.getComputations().setCiphertext(Modifiable.xor(modificationBitmask, 0));

        ApplicationMessage appData = new ApplicationMessage();
        appData.setData(Modifiable.explicit(c.getDefaultApplicationMessageData().getBytes()));

        SendAction sendAction = new SendAction(appData);
        sendAction.setConfiguredRecords(List.of(record));

        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HANDSHAKE);
        workflowTrace.addTlsActions(sendAction, new ReceiveAction(new AlertMessage()));

        State state = runner.execute(workflowTrace, c);

        WorkflowTrace trace = state.getWorkflowTrace();
        Validator.receivedFatalAlert(state, testCase);

        AlertMessage msg = trace.getFirstReceivedMessage(AlertMessage.class);
        Validator.testAlertDescription(state, testCase, AlertDescription.BAD_RECORD_MAC, msg);
    }
}
