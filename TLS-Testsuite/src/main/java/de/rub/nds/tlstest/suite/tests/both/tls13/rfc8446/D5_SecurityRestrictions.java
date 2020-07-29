package de.rub.nds.tlstest.suite.tests.both.tls13.rfc8446;

import de.rub.nds.modifiablevariable.util.Modifiable;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.protocol.message.AlertMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ClientHelloMessage;
import de.rub.nds.tlsattacker.core.record.Record;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlstest.framework.Validator;
import de.rub.nds.tlstest.framework.annotations.RFC;
import de.rub.nds.tlstest.framework.annotations.TlsTest;
import de.rub.nds.tlstest.framework.constants.SeverityLevel;
import de.rub.nds.tlstest.framework.constants.TestEndpointType;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.testClasses.Tls13Test;


public class D5_SecurityRestrictions extends Tls13Test {

    @TlsTest(description = "Implementations MUST NOT send any records with a " +
            "version less than 0x0300. Implementations SHOULD NOT accept any " +
            "records with a version less than 0x0300 (but may inadvertently " +
            "do so if the record version number is ignored completely).", interoperabilitySeverity = SeverityLevel.MEDIUM)
    @RFC(number = 8446, section = "D.5. Security Restrictions Related to Backward Compatibility")
    public void invalidRecordVersion_ssl30(WorkflowRunner runner) {
        Config config = this.getConfig();
        runner.replaceSupportedCiphersuites = true;
        runner.replaceSelectedCiphersuite = true;

        Record record = new Record();
        record.setProtocolVersion(Modifiable.explicit(new byte[]{0x02, (byte)0x03}));

        WorkflowTrace trace;
        if (context.getConfig().getTestEndpointMode() == TestEndpointType.CLIENT) {
            trace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);
        } else {
            trace = new WorkflowTrace();
            trace.addTlsAction(new SendAction(new ClientHelloMessage(config)));
        }

        trace.addTlsActions(
                new ReceiveAction(new AlertMessage())
        );

        runner.setStateModifier(i -> {
            SendAction action;
            action = i.getWorkflowTrace().getFirstAction(SendAction.class);
            action.setRecords(record);
            return null;
        });

        runner.execute(trace, config).validateFinal(Validator::receivedFatalAlert);
    }
}
