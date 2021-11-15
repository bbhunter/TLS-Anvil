/**
 * TLS-Testsuite - A testsuite for the TLS protocol
 *
 * Copyright 2020 Ruhr University Bochum and
 * TÜV Informationstechnik GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.suite.tests.server.tls13.rfc8446;

import de.rub.nds.modifiablevariable.util.Modifiable;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.AlertDescription;
import de.rub.nds.tlsattacker.core.constants.AlertLevel;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.constants.ProtocolMessageType;
import de.rub.nds.tlsattacker.core.protocol.message.AlertMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ClientHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.FinishedMessage;
import de.rub.nds.tlsattacker.core.record.Record;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceUtil;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlsscanner.serverscanner.rating.TestResult;
import de.rub.nds.tlsscanner.serverscanner.report.AnalyzedProperty;
import de.rub.nds.tlstest.framework.Validator;
import de.rub.nds.tlstest.framework.annotations.EnforcedSenderRestriction;
import de.rub.nds.tlstest.framework.annotations.MethodCondition;
import de.rub.nds.tlstest.framework.annotations.RFC;
import de.rub.nds.tlstest.framework.annotations.ScopeExtensions;
import de.rub.nds.tlstest.framework.annotations.ScopeLimitations;
import de.rub.nds.tlstest.framework.annotations.ServerTest;
import de.rub.nds.tlstest.framework.annotations.TlsTest;
import de.rub.nds.tlstest.framework.annotations.categories.AlertCategory;
import de.rub.nds.tlstest.framework.annotations.categories.ComplianceCategory;
import de.rub.nds.tlstest.framework.annotations.categories.CryptoCategory;
import de.rub.nds.tlstest.framework.annotations.categories.DeprecatedFeatureCategory;
import de.rub.nds.tlstest.framework.annotations.categories.HandshakeCategory;
import de.rub.nds.tlstest.framework.annotations.categories.InteroperabilityCategory;
import de.rub.nds.tlstest.framework.annotations.categories.MessageStructureCategory;
import de.rub.nds.tlstest.framework.annotations.categories.RecordLayerCategory;
import de.rub.nds.tlstest.framework.annotations.categories.SecurityCategory;
import de.rub.nds.tlstest.framework.constants.SeverityLevel;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.model.derivationParameter.BasicDerivationType;
import de.rub.nds.tlstest.framework.model.derivationParameter.AlertDerivation;
import de.rub.nds.tlstest.framework.testClasses.Tls13Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;

@ServerTest
@RFC(number = 8446, section = "5.1. Record Layer")
public class RecordLayer extends Tls13Test {

    @TlsTest(description = "Implementations MUST NOT send "
            + "zero-length fragments of Handshake types, even "
            + "if those fragments contain padding.")
    @RecordLayerCategory(SeverityLevel.LOW)
    @ComplianceCategory(SeverityLevel.HIGH)
    @AlertCategory(SeverityLevel.LOW)
    @EnforcedSenderRestriction
    public void zeroLengthRecord_CH(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);
        c.setUseAllProvidedRecords(true);

        SendAction clientHello = new SendAction(new ClientHelloMessage(c));
        Record record = new Record();
        record.setMaxRecordLengthConfig(0);
        clientHello.setRecords(record);

        WorkflowTrace trace = new WorkflowTrace();
        trace.addTlsActions(
                clientHello,
                new ReceiveAction(new AlertMessage())
        );

        runner.execute(trace, c).validateFinal(Validator::receivedFatalAlert);
    }

    @TlsTest(description = "Implementations " +
        "MUST NOT send Handshake and Alert records that have a zero-length " +
        "TLSInnerPlaintext.content; if such a message is received, the " +
        "receiving implementation MUST terminate the connection with an " +
        "\"unexpected_message\" alert.")
    @RFC(number = 8446, section = "5.4. Record Padding")
    @RecordLayerCategory(SeverityLevel.MEDIUM)
    @ComplianceCategory(SeverityLevel.HIGH)
    @AlertCategory(SeverityLevel.LOW)
    public void zeroLengthRecord_Finished(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);
        c.setUseAllProvidedRecords(true);

        Record record = new Record();
        record.setMaxRecordLengthConfig(0);
        WorkflowTrace trace = runner.generateWorkflowTraceUntilSendingMessage(WorkflowTraceType.HANDSHAKE, HandshakeMessageType.FINISHED);
        trace.addTlsActions(
                new SendAction(new FinishedMessage()),
                new ReceiveAction(new AlertMessage())
        );

        SendAction finished = (SendAction) WorkflowTraceUtil.getFirstSendingActionForMessage(HandshakeMessageType.FINISHED, trace);
        finished.setRecords(record);

        runner.execute(trace, c).validateFinal(i -> {
            WorkflowTrace workflowtrace = i.getWorkflowTrace();
            Validator.receivedFatalAlert(i);

            AlertMessage msg = workflowtrace.getFirstReceivedMessage(AlertMessage.class);
            Validator.testAlertDescription(i, AlertDescription.UNEXPECTED_MESSAGE, msg);
        });
    }

    public ConditionEvaluationResult supportsRecordFragmentation() {
        if (context.getSiteReport().getResult(AnalyzedProperty.SUPPORTS_RECORD_FRAGMENTATION) == TestResult.TRUE) {
            return ConditionEvaluationResult.enabled("");
        }
        return ConditionEvaluationResult.disabled("Target does not support Record fragmentation");
    }
    
    @TlsTest(description = "Handshake messages MUST NOT be interleaved "
            + "with other record types. That is, if a handshake message is split over two or more\n"
            + "records, there MUST NOT be any other records between them.")
    @ScopeLimitations(BasicDerivationType.RECORD_LENGTH)
    @ScopeExtensions(BasicDerivationType.ALERT)
    @RecordLayerCategory(SeverityLevel.LOW)
    @ComplianceCategory(SeverityLevel.HIGH)
    @AlertCategory(SeverityLevel.LOW)
    @MethodCondition(method = "supportsRecordFragmentation")
    @EnforcedSenderRestriction
    public void interleaveRecords(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);
        WorkflowTrace trace = runner.generateWorkflowTrace(WorkflowTraceType.HANDSHAKE);
        SendAction sendFinished = (SendAction) WorkflowTraceUtil.getFirstSendingActionForMessage(HandshakeMessageType.FINISHED, trace);
        AlertDescription selectedAlert = derivationContainer.getDerivation(AlertDerivation.class).getSelectedValue();
        
        Record finishedFragmentRecord = new Record();
        finishedFragmentRecord.setMaxRecordLengthConfig(10);
        Record alertRecord = new Record();
        
        //we add a record that will remain untouched by record layer but has
        //an alert set as explicit content
        alertRecord.setMaxRecordLengthConfig(0);
        alertRecord.setContentType(Modifiable.explicit(ProtocolMessageType.ALERT.getValue()));
        byte[] alertContent = new byte [] {AlertLevel.WARNING.getValue(), selectedAlert.getValue()};
        alertRecord.setProtocolMessageBytes(Modifiable.explicit(alertContent));
        
        sendFinished.setRecords(finishedFragmentRecord, alertRecord);

        trace.addTlsAction(new ReceiveAction(new AlertMessage()));

        runner.execute(trace, c).validateFinal(Validator::receivedFatalAlert);
    }
}
