/**
 * TLS-Testsuite - A testsuite for the TLS protocol
 *
 * <p>Copyright 2022 Ruhr University Bochum
 *
 * <p>Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.suite.tests.server.tls13.rfc8446;

import static org.junit.jupiter.api.Assertions.*;

import de.rub.nds.anvilcore.annotation.*;
import de.rub.nds.anvilcore.model.DerivationScope;
import de.rub.nds.anvilcore.model.parameter.DerivationParameter;
import de.rub.nds.anvilcore.teststate.AnvilTestCase;
import de.rub.nds.modifiablevariable.util.Modifiable;
import de.rub.nds.scanner.core.probe.result.TestResults;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.*;
import de.rub.nds.tlsattacker.core.protocol.message.AlertMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ClientHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.FinishedMessage;
import de.rub.nds.tlsattacker.core.record.Record;
import de.rub.nds.tlsattacker.core.record.RecordCryptoComputations;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceConfigurationUtil;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.ReceivingAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlsscanner.core.constants.TlsAnalyzedProperty;
import de.rub.nds.tlstest.framework.Validator;
import de.rub.nds.tlstest.framework.annotations.EnforcedSenderRestriction;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.model.derivationParameter.AlertDerivation;
import de.rub.nds.tlstest.framework.model.derivationParameter.ProtocolVersionDerivation;
import de.rub.nds.tlstest.framework.testClasses.Tls13Test;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;

@ServerTest
public class RecordLayer extends Tls13Test {

    @AnvilTest(id = "8446-HWUJWNwjoA")
    @EnforcedSenderRestriction
    public void zeroLengthRecord_CH(AnvilTestCase testCase, WorkflowRunner runner) {
        Config c = getPreparedConfig(runner);
        c.setUseAllProvidedRecords(true);

        SendAction clientHello = new SendAction(new ClientHelloMessage(c));
        Record record = new Record();
        record.setMaxRecordLengthConfig(0);
        clientHello.setConfiguredRecords(List.of(record));

        WorkflowTrace trace = new WorkflowTrace();
        trace.addTlsActions(clientHello, new ReceiveAction(new AlertMessage()));

        State state = runner.execute(trace, c);
        Validator.receivedFatalAlert(state, testCase);
    }

    @AnvilTest(id = "8446-orNs8sPcM8")
    public void zeroLengthRecord_Finished(AnvilTestCase testCase, WorkflowRunner runner) {
        Config c = getPreparedConfig(runner);
        c.setUseAllProvidedRecords(true);

        Record record = new Record();
        record.setMaxRecordLengthConfig(0);
        WorkflowTrace trace =
                runner.generateWorkflowTraceUntilSendingMessage(
                        WorkflowTraceType.HANDSHAKE, HandshakeMessageType.FINISHED);
        trace.addTlsActions(
                new SendAction(new FinishedMessage()), new ReceiveAction(new AlertMessage()));

        SendAction finished =
                (SendAction)
                        WorkflowTraceConfigurationUtil.getFirstStaticConfiguredSendAction(
                                trace, HandshakeMessageType.FINISHED);
        finished.setConfiguredRecords(List.of(record));

        State state = runner.execute(trace, c);

        WorkflowTrace workflowtrace = state.getWorkflowTrace();
        Validator.receivedFatalAlert(state, testCase);

        AlertMessage msg = workflowtrace.getFirstReceivedMessage(AlertMessage.class);
        Validator.testAlertDescription(state, testCase, AlertDescription.UNEXPECTED_MESSAGE, msg);
    }

    public ConditionEvaluationResult supportsRecordFragmentation() {
        if (context.getFeatureExtractionResult()
                        .getResult(TlsAnalyzedProperty.SUPPORTS_RECORD_FRAGMENTATION)
                == TestResults.TRUE) {
            return ConditionEvaluationResult.enabled("");
        }
        return ConditionEvaluationResult.disabled("Target does not support Record fragmentation");
    }

    @AnvilTest(id = "8446-EHgkL2huNs")
    @ExcludeParameter("RECORD_LENGTH")
    @IncludeParameter("ALERT")
    @MethodCondition(method = "supportsRecordFragmentation")
    @EnforcedSenderRestriction
    public void interleaveRecords(AnvilTestCase testCase, WorkflowRunner runner) {
        Config c = getPreparedConfig(runner);
        WorkflowTrace trace = runner.generateWorkflowTrace(WorkflowTraceType.HANDSHAKE);
        SendAction sendFinished =
                (SendAction)
                        WorkflowTraceConfigurationUtil.getFirstStaticConfiguredSendAction(
                                trace, HandshakeMessageType.FINISHED);
        AlertDescription selectedAlert =
                parameterCombination.getParameter(AlertDerivation.class).getSelectedValue();

        Record finishedFragmentRecord = new Record();
        finishedFragmentRecord.setMaxRecordLengthConfig(10);
        Record alertRecord = new Record();

        // we add a record that will remain untouched by record layer but has
        // an alert set as explicit content
        alertRecord.setMaxRecordLengthConfig(0);
        alertRecord.setContentType(Modifiable.explicit(ProtocolMessageType.ALERT.getValue()));
        byte[] alertContent = new byte[] {AlertLevel.WARNING.getValue(), selectedAlert.getValue()};
        alertRecord.setProtocolMessageBytes(Modifiable.explicit(alertContent));

        sendFinished.setConfiguredRecords(List.of(finishedFragmentRecord, alertRecord));

        trace.addTlsAction(new ReceiveAction(new AlertMessage()));

        State state = runner.execute(trace, c);
        Validator.receivedFatalAlert(state, testCase);
    }

    @AnvilTest(id = "8446-UCLQ6PhSyy")
    @IncludeParameter("PROTOCOL_VERSION")
    @ExplicitValues(affectedIdentifiers = "PROTOCOL_VERSION", methods = "getRecordProtocolVersions")
    @Tag("new")
    public void ignoresInitialRecordVersion(AnvilTestCase testCase, WorkflowRunner runner) {
        Config config = getPreparedConfig(runner);
        byte[] selectedRecordVersion =
                parameterCombination
                        .getParameter(ProtocolVersionDerivation.class)
                        .getSelectedValue();

        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);
        // manually insert HRR to manipulate very first hello
        if (runner.shouldInsertHelloRetryRequest()) {
            runner.insertHelloRetryRequest(workflowTrace, config.getDefaultSelectedNamedGroup());
        }
        runner.setAutoHelloRetryRequest(false);

        Record initialRecord = new Record();
        initialRecord.setComputations(new RecordCryptoComputations());
        initialRecord.setProtocolVersion(Modifiable.explicit(selectedRecordVersion));
        ((SendAction) workflowTrace.getFirstSendingAction())
                .setConfiguredRecords(List.of(initialRecord));

        State state = runner.execute(workflowTrace, config);
        Validator.executedAsPlanned(state, testCase);
    }

    public List<DerivationParameter<Config, byte[]>> getRecordProtocolVersions(
            DerivationScope scope) {
        List<DerivationParameter<Config, byte[]>> parameterValues = new LinkedList<>();
        parameterValues.add(new ProtocolVersionDerivation(new byte[] {0x03, 0x00}));
        parameterValues.add(new ProtocolVersionDerivation(new byte[] {0x03, 0x01}));
        parameterValues.add(new ProtocolVersionDerivation(new byte[] {0x03, 0x02}));
        parameterValues.add(new ProtocolVersionDerivation(new byte[] {0x03, 0x03}));
        parameterValues.add(new ProtocolVersionDerivation(new byte[] {0x03, 0x04}));
        parameterValues.add(new ProtocolVersionDerivation(new byte[] {0x03, 0x05}));
        return parameterValues;
    }

    @AnvilTest(id = "8446-qrenZekKeD")
    @Tag("new")
    public void checkRecordProtocolVersion(AnvilTestCase testCase, WorkflowRunner runner) {
        Config config = getPreparedConfig(runner);
        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HANDSHAKE);

        State state = runner.execute(workflowTrace, config);

        Validator.executedAsPlanned(state, testCase);
        for (ReceivingAction receiving : state.getWorkflowTrace().getReceivingActions()) {
            ReceiveAction receiveAction = (ReceiveAction) receiving;
            if (receiveAction.getReceivedRecords() != null
                    && !receiveAction.getReceivedRecords().isEmpty()) {
                for (Record record : receiveAction.getReceivedRecords()) {
                    if (record.getContentMessageType() != ProtocolMessageType.CHANGE_CIPHER_SPEC) {
                        assertArrayEquals(
                                record.getProtocolVersion().getValue(),
                                ProtocolVersion.TLS12.getValue(),
                                "Record used wrong protocol version");
                    }
                }
            }
        }
    }
}
