/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.rub.nds.tlstest.suite.tests.server.tls13.rfc8446;

import de.rub.nds.modifiablevariable.util.Modifiable;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.ExtensionByteLength;
import de.rub.nds.tlsattacker.core.constants.ExtensionType;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.constants.ProtocolMessageType;
import de.rub.nds.tlsattacker.core.constants.PskKeyExchangeMode;
import de.rub.nds.tlsattacker.core.constants.RunningModeType;
import de.rub.nds.tlsattacker.core.protocol.ProtocolMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ClientHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.NewSessionTicketMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.PreSharedKeyExtensionMessage;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceUtil;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowConfigurationFactory;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlsscanner.serverscanner.rating.TestResult;
import de.rub.nds.tlsscanner.serverscanner.report.AnalyzedProperty;
import de.rub.nds.tlstest.framework.Validator;
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
import de.rub.nds.tlstest.framework.annotations.categories.SecurityCategory;
import de.rub.nds.tlstest.framework.constants.SeverityLevel;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.model.DerivationContainer;
import de.rub.nds.tlstest.framework.model.derivationParameter.BasicDerivationType;
import de.rub.nds.tlstest.framework.parameterExtensions.configurationOptionsExtension.configurationOptionDerivationParameter.ConfigurationOptionCompoundParameter;
import de.rub.nds.tlstest.framework.parameterExtensions.configurationOptionsExtension.configurationOptionDerivationParameter.DisablePskDerivation;
import de.rub.nds.tlstest.framework.testClasses.Tls13Test;
import java.util.Arrays;
import static org.junit.Assert.assertTrue;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;

@ServerTest
@RFC(number = 8446, section = "4.2.11 Pre-Shared Key Extension")
public class PreSharedKey extends Tls13Test {

    public ConditionEvaluationResult supportsPsk() {
        if (context.getSiteReport().getResult(AnalyzedProperty.SUPPORTS_TLS13_PSK) == TestResult.TRUE
                || context.getSiteReport().getResult(AnalyzedProperty.SUPPORTS_TLS13_PSK_DHE) == TestResult.TRUE) {
            return ConditionEvaluationResult.enabled("");
        } else {
            return ConditionEvaluationResult.disabled("Does not support PSK handshakes");
        }
    }

    private int getExpectedExtensionCount(Config c) {
        WorkflowConfigurationFactory dummyFactory = new WorkflowConfigurationFactory(c);
        WorkflowTrace dummyTrace = dummyFactory.createWorkflowTrace(WorkflowTraceType.SHORT_HELLO, RunningModeType.CLIENT);
        return dummyTrace.getFirstSendMessage(ClientHelloMessage.class).getExtensions().size();
    }

    private boolean cipherSuitesDisabledByConfigOptionsDerivation(DerivationContainer derivationContainer){
        ConfigurationOptionCompoundParameter coCompoundParam = derivationContainer.getDerivation(ConfigurationOptionCompoundParameter.class);
        if(coCompoundParam != null){
            DisablePskDerivation disablePskDerivation = coCompoundParam.getDerivation(DisablePskDerivation.class);
            if(disablePskDerivation != null && disablePskDerivation.getSelectedValue().isOptionSet()){
                // Psk ciphersuites are manually disabled using the disablePsk configuration option.
                return true;
            }
        }
        return false;
    }

    /*@TlsTest(description = "The \"pre_shared_key\" extension MUST be the last extension "
            + "in the ClientHello (this facilitates implementation as described below). "
            + "Servers MUST check that it is the last extension and otherwise fail "
            + "the handshake with an \"illegal_parameter\" alert.")
    @RFC(number = 8446, section = "4.2.11. Pre-Shared Key Extension")
    @MethodCondition(method = "supportsPsk")
    public void isNotLastExtension(WorkflowRunner runner) {
        Config c = this.getConfig();
        c.setAddPSKKeyExchangeModesExtension(true);
        c.setAddPreSharedKeyExtension(true);
        WorkflowTrace workflowTrace = runner.generateWorkflowTraceUntilLastReceivingMessage(WorkflowTraceType.FULL_TLS13_PSK, HandshakeMessageType.SERVER_HELLO);
        workflowTrace.addTlsAction(new ReceiveAction());

        AnnotatedStateContainer container = new AnnotatedStateContainer();
        int nonPskExtensions = getExpectedExtensionCount(c) - 1;
        for (int i = 0; i < nonPskExtensions; i++) {
            int myIndex = i;
            runner.setStateModifier(s -> {
                s.addAdditionalTestInfo("Position " + myIndex + " of " + (nonPskExtensions - 1));
                ClientHelloMessage cHello = s.getWorkflowTrace().getLastSendMessage(ClientHelloMessage.class);
                int extensionCount = cHello.getExtensions().size();
                PreSharedKeyExtensionMessage pskExt = (PreSharedKeyExtensionMessage) cHello.getExtensions().get(extensionCount - 1);
                cHello.getExtensions().remove(pskExt);
                cHello.getExtensions().add(myIndex, pskExt);
                return null;
            });

            container.addAll(runner.prepare(workflowTrace, c));
        }
        runner.execute(container).validateFinal(i -> {
            WorkflowTrace trace = i.getWorkflowTrace();
            System.out.println(trace.toString());
            Validator.receivedFatalAlert(i, false);
            AlertMessage alert = i.getWorkflowTrace().getFirstReceivedMessage(AlertMessage.class);
            if (alert == null) {
                return;
            }
            Validator.testAlertDescription(i, AlertDescription.ILLEGAL_PARAMETER, alert);
        });
    }

    @TlsTest(description = "The \"pre_shared_key\" extension MUST be the last extension "
            + "in the ClientHello (this facilitates implementation as described below). "
            + "Servers MUST check that it is the last extension and otherwise fail "
            + "the handshake with an \"illegal_parameter\" alert.")
    @RFC(number = 8446, section = "4.2.11. Pre-Shared Key Extension")
    @MethodCondition(method = "supportsPsk")
    public void duplicateExtension(WorkflowRunner runner) {
        Config c = this.getConfig();
        c.setAddPSKKeyExchangeModesExtension(true);
        c.setAddPreSharedKeyExtension(true);
        WorkflowTrace workflowTrace = runner.generateWorkflowTraceUntilLastReceivingMessage(WorkflowTraceType.FULL_TLS13_PSK, HandshakeMessageType.SERVER_HELLO);
        workflowTrace.addTlsAction(new ReceiveAction());

        AnnotatedStateContainer container = new AnnotatedStateContainer();
        int nonPskExtensions = getExpectedExtensionCount(c) - 1;
        for (int i = 0; i < nonPskExtensions; i++) {
            int myIndex = i;
            runner.setStateModifier(s -> {
                s.addAdditionalTestInfo("Position " + myIndex + " of " + (nonPskExtensions - 1));
                ClientHelloMessage cHello = s.getWorkflowTrace().getLastSendMessage(ClientHelloMessage.class);
                int extensionCount = cHello.getExtensions().size();
                PreSharedKeyExtensionMessage pskExt = (PreSharedKeyExtensionMessage) cHello.getExtensions().get(extensionCount - 1);
                cHello.getExtensions().add(myIndex, pskExt);
                return null;
            });

            container.addAll(runner.prepare(workflowTrace, c));
        }
        runner.execute(container).validateFinal(i -> {
            WorkflowTrace trace = i.getWorkflowTrace();
            Validator.receivedFatalAlert(i, false);
            AlertMessage alert = i.getWorkflowTrace().getFirstReceivedMessage(AlertMessage.class);
            if (alert == null) {
                return;
            }
            Validator.testAlertDescription(i, AlertDescription.ILLEGAL_PARAMETER, alert);
        });
    }*/
    @TlsTest(description = "Prior to accepting PSK key establishment, the server MUST validate"
            + "the corresponding binder value")
    @ScopeExtensions("BasicDerivationType.PRF_BITMASK")
    @MethodCondition(method = "supportsPsk")
    @HandshakeCategory(SeverityLevel.MEDIUM)
    @ComplianceCategory(SeverityLevel.HIGH)
    @CryptoCategory(SeverityLevel.CRITICAL)
    @SecurityCategory(SeverityLevel.CRITICAL)
    @AlertCategory(SeverityLevel.LOW)
    public void invalidBinder(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);
        if(cipherSuitesDisabledByConfigOptionsDerivation(derivationContainer)){
            return;
        }
        setupPskConfig(c);
        c.setLimitPsksToOne(true);
        WorkflowTrace workflowTrace = runner.generateWorkflowTraceUntilLastReceivingMessage(WorkflowTraceType.FULL_TLS13_PSK, HandshakeMessageType.SERVER_HELLO);
        workflowTrace.addTlsAction(new ReceiveAction());
        byte[] modificationBitmask = derivationContainer.buildBitmask();

        ClientHelloMessage cHello = workflowTrace.getLastSendMessage(ClientHelloMessage.class);
        PreSharedKeyExtensionMessage pskExt = cHello.getExtension(PreSharedKeyExtensionMessage.class);
        pskExt.setBinderListBytes(Modifiable.xor(modificationBitmask, ExtensionByteLength.PSK_BINDER_LENGTH));

        runner.execute(workflowTrace, c).validateFinal(i -> {
            WorkflowTrace trace = i.getWorkflowTrace();
            Validator.receivedFatalAlert(i, false);
        });
    }

    @TlsTest(description = "Prior to accepting PSK key establishment, the server MUST validate"
            + "the corresponding binder value")
    @MethodCondition(method = "supportsPsk")
    @HandshakeCategory(SeverityLevel.MEDIUM)
    @ComplianceCategory(SeverityLevel.HIGH)
    @CryptoCategory(SeverityLevel.CRITICAL)
    @SecurityCategory(SeverityLevel.CRITICAL)
    @AlertCategory(SeverityLevel.LOW)
    public void noBinder(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);
        if(cipherSuitesDisabledByConfigOptionsDerivation(derivationContainer)){
            return;
        }
        setupPskConfig(c);
        c.setLimitPsksToOne(true);

        WorkflowTrace workflowTrace = runner.generateWorkflowTraceUntilLastReceivingMessage(WorkflowTraceType.FULL_TLS13_PSK, HandshakeMessageType.SERVER_HELLO);
        workflowTrace.addTlsAction(new ReceiveAction());

        ClientHelloMessage cHello = workflowTrace.getLastSendMessage(ClientHelloMessage.class);
        PreSharedKeyExtensionMessage pskExt = cHello.getExtension(PreSharedKeyExtensionMessage.class);
        pskExt.setBinderListBytes(Modifiable.explicit(new byte[0]));
        pskExt.setBinderListLength(Modifiable.explicit(0));
        
        runner.execute(workflowTrace, c).validateFinal(i -> {
            WorkflowTrace trace = i.getWorkflowTrace();
            Validator.receivedFatalAlert(i, false);
        });

    }

    @TlsTest(description = "Clients MUST verify that the server’s selected_identity is within the "
            + "range supplied by the client")
    @MethodCondition(method = "supportsPsk")
    @HandshakeCategory(SeverityLevel.MEDIUM)
    @ComplianceCategory(SeverityLevel.HIGH)
    @SecurityCategory(SeverityLevel.HIGH)
    public void selectedPSKIndexIsWithinOfferedListSize(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config config = getPreparedConfig(argumentAccessor, runner);
        if(cipherSuitesDisabledByConfigOptionsDerivation(derivationContainer)){
            return;
        }
        setupPskConfig(config);

        WorkflowTrace workflowTrace;
        if (config.getTls13BackwardsCompatibilityMode()) {
            workflowTrace = runner.generateWorkflowTraceUntilLastSendingMessage(WorkflowTraceType.FULL_TLS13_PSK, ProtocolMessageType.CHANGE_CIPHER_SPEC);
        } else {
            workflowTrace = runner.generateWorkflowTraceUntilLastSendingMessage(WorkflowTraceType.FULL_TLS13_PSK, HandshakeMessageType.FINISHED);
        }

        runner.execute(workflowTrace, config).validateFinal(i -> {
            WorkflowTrace trace = i.getWorkflowTrace();
            Validator.executedAsPlanned(i);

            ClientHelloMessage pskClientHello = (ClientHelloMessage) WorkflowTraceUtil.getLastSendMessage(HandshakeMessageType.CLIENT_HELLO, trace);
            PreSharedKeyExtensionMessage pskExtension = pskClientHello.getExtension(PreSharedKeyExtensionMessage.class);
            int offeredPSKs = pskExtension.getIdentities().size();

            ServerHelloMessage pskServerHello = (ServerHelloMessage) WorkflowTraceUtil.getLastReceivedMessage(HandshakeMessageType.SERVER_HELLO, trace);
            assertTrue("PSK Handshake failed - Server did not select as PSK", pskServerHello.containsExtension(ExtensionType.PRE_SHARED_KEY));
            int selectedIdentityIndex = pskServerHello.getExtension(PreSharedKeyExtensionMessage.class).getSelectedIdentity().getValue();
            assertTrue("Server set an invalid selected PSK index (" + selectedIdentityIndex + " of " + offeredPSKs + " )", selectedIdentityIndex >= 0 && selectedIdentityIndex < offeredPSKs);
        });
    }
    
    private void setupPskConfig(Config config) {
        config.setAddPSKKeyExchangeModesExtension(true);
        config.setAddPreSharedKeyExtension(true);
        adjustPreSharedKeyModes(config);
    }
}
