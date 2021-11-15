/**
 * TLS-Testsuite - A testsuite for the TLS protocol
 *
 * Copyright 2020 Ruhr University Bochum and
 * TÜV Informationstechnik GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.suite.tests.client.tls13.rfc8446;

import de.rub.nds.modifiablevariable.util.Modifiable;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.AlertDescription;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.ExtensionType;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.constants.NamedGroup;
import de.rub.nds.tlsattacker.core.constants.ProtocolMessageType;
import de.rub.nds.tlsattacker.core.protocol.message.AlertMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ClientHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.ExtensionMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.KeyShareExtensionMessage;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceUtil;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlstest.framework.Validator;
import de.rub.nds.tlstest.framework.annotations.ClientTest;
import de.rub.nds.tlstest.framework.annotations.DynamicValueConstraints;
import de.rub.nds.tlstest.framework.annotations.ExplicitValues;
import de.rub.nds.tlstest.framework.annotations.MethodCondition;
import de.rub.nds.tlstest.framework.annotations.RFC;
import de.rub.nds.tlstest.framework.annotations.ScopeExtensions;
import de.rub.nds.tlstest.framework.annotations.ScopeLimitations;
import de.rub.nds.tlstest.framework.annotations.TlsTest;
import de.rub.nds.tlstest.framework.annotations.categories.AlertCategory;
import de.rub.nds.tlstest.framework.annotations.categories.ComplianceCategory;
import de.rub.nds.tlstest.framework.annotations.categories.CryptoCategory;
import de.rub.nds.tlstest.framework.annotations.categories.DeprecatedFeatureCategory;
import de.rub.nds.tlstest.framework.annotations.categories.HandshakeCategory;
import de.rub.nds.tlstest.framework.annotations.categories.InteroperabilityCategory;
import de.rub.nds.tlstest.framework.annotations.categories.MessageStructureCategory;
import de.rub.nds.tlstest.framework.annotations.categories.SecurityCategory;
import de.rub.nds.tlstest.framework.coffee4j.model.ModelFromScope;
import de.rub.nds.tlstest.framework.constants.SeverityLevel;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.model.DerivationScope;
import de.rub.nds.tlstest.framework.model.derivationParameter.BasicDerivationType;
import de.rub.nds.tlstest.framework.model.ModelType;
import de.rub.nds.tlstest.framework.model.derivationParameter.CipherSuiteDerivation;
import de.rub.nds.tlstest.framework.model.derivationParameter.DerivationParameter;
import de.rub.nds.tlstest.framework.model.derivationParameter.NamedGroupDerivation;
import de.rub.nds.tlstest.framework.model.derivationParameter.mirrored.MirroredCipherSuiteDerivation;
import de.rub.nds.tlstest.framework.testClasses.Tls13Test;
import de.rub.nds.tlstest.suite.tests.client.tls13.rfc8701.ServerInitiatedExtensionPoints;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;

@ClientTest
@RFC(number = 8446, section = "4.1.4 Hello Retry Request")
public class HelloRetryRequest extends Tls13Test {

    public List<DerivationParameter> getUnofferedGroups(DerivationScope scope) {
        List<DerivationParameter> parameterValues = new LinkedList<>();
        List<NamedGroup> offeredGroups = context.getSiteReport().getClientHelloNamedGroups();
        NamedGroup.getImplemented().stream().filter(group -> !offeredGroups.contains(group))
                .forEach(unofferedGroup -> parameterValues.add(new NamedGroupDerivation(unofferedGroup)));
        return parameterValues;
    }

    @TlsTest(description = "Upon receipt of this extension in a HelloRetryRequest, the client "
            + "MUST verify that (1) the selected_group field corresponds to a group "
            + "which was provided in the \"supported_groups\" extension in the "
            + "original ClientHello and [...] If either of these checks fails, then "
            + "the client MUST abort the handshake with an \"illegal_parameter\" "
            + "alert.")
    @RFC(number = 8446, section = "4.2.8 Key Share")
    @ExplicitValues(affectedTypes = BasicDerivationType.NAMED_GROUP, methods = "getUnofferedGroups")
    @HandshakeCategory(SeverityLevel.MEDIUM)
    @AlertCategory(SeverityLevel.MEDIUM)
    @ComplianceCategory(SeverityLevel.MEDIUM)
    public void helloRetryRequestsUnofferedGroup(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        performHelloRetryRequestTest(argumentAccessor, runner);
    }

    public List<DerivationParameter> getUnofferedTls13CipherSuites(DerivationScope scope) {
        List<DerivationParameter> parameterValues = new LinkedList<>();
        List<CipherSuite> offeredTls13 = CipherSuite.getCipherSuites(context.getReceivedClientHelloMessage().getCipherSuites().getValue());
        CipherSuite.getImplementedTls13CipherSuites().stream().filter(cipherSuite -> !offeredTls13.contains(cipherSuite))
                .forEach(cipherSuite -> parameterValues.add(new CipherSuiteDerivation(cipherSuite)));
        return parameterValues;
    }

    @TlsTest(description = "A client which receives a cipher suite that was not offered MUST "
            + "abort the handshake.")
    @ExplicitValues(affectedTypes = BasicDerivationType.CIPHERSUITE, methods = "getUnofferedTls13CipherSuites")
    @HandshakeCategory(SeverityLevel.MEDIUM)
    @AlertCategory(SeverityLevel.LOW)
    @ComplianceCategory(SeverityLevel.MEDIUM)
    public void helloRetryRequestsUnofferedTls13CipherSuite(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);
        runner.setAutoHelloRetryRequest(false);
        NamedGroup selectedGroup = derivationContainer.getDerivation(NamedGroupDerivation.class).getSelectedValue();

        WorkflowTrace workflowTrace = new WorkflowTrace();
        workflowTrace.addTlsActions(new ReceiveAction(new AlertMessage()));
        runner.insertHelloRetryRequest(workflowTrace, selectedGroup);

        runner.execute(workflowTrace, c).validateFinal(i -> {
            Validator.receivedFatalAlert(i);
        });
    }

    public boolean isKeyShareInInitialHello(NamedGroup group) {
        return context.getSiteReport().getClientHelloKeyShareGroups().contains(group);
    }

    @TlsTest(description = "Clients MUST abort the handshake with an "
            + "\"illegal_parameter\" alert if the HelloRetryRequest would not result "
            + "in any change in the ClientHello.")
    @DynamicValueConstraints(affectedTypes = BasicDerivationType.NAMED_GROUP, methods = "isKeyShareInInitialHello")
    @HandshakeCategory(SeverityLevel.MEDIUM)
    @AlertCategory(SeverityLevel.MEDIUM)
    @ComplianceCategory(SeverityLevel.MEDIUM)
    public void helloRetryRequestResultsInNoChanges(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        performHelloRetryRequestTest(argumentAccessor, runner);
    }

    private void performHelloRetryRequestTest(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);
        runner.setAutoHelloRetryRequest(false);
        NamedGroup selectedGroup = derivationContainer.getDerivation(NamedGroupDerivation.class).getSelectedValue();

        WorkflowTrace workflowTrace = new WorkflowTrace();
        workflowTrace.addTlsActions(new ReceiveAction(new AlertMessage()));
        runner.insertHelloRetryRequest(workflowTrace, selectedGroup);

        runner.execute(workflowTrace, c).validateFinal(i -> {
            Validator.receivedFatalAlert(i);
            AlertMessage alert = i.getWorkflowTrace().getFirstReceivedMessage(AlertMessage.class);
            Validator.testAlertDescription(i, AlertDescription.ILLEGAL_PARAMETER, alert);
        });
    }

    public boolean isNotKeyShareInInitialHello(NamedGroup group) {
        return !context.getSiteReport().getClientHelloKeyShareGroups().contains(group);
    }

    private NamedGroup getOtherSupportedNamedGroup(NamedGroup givenGroup) {
        for (NamedGroup group : context.getSiteReport().getSupportedTls13Groups()) {
            if (group != givenGroup) {
                return group;
            }
        }
        return null;
    }

    private ConditionEvaluationResult supportsMultipleNamedGroups() {
        if (context.getSiteReport().getSupportedTls13Groups().size() > 1) {
            return ConditionEvaluationResult.enabled("More than one NamedGroup supported by target in TLS 1.3");
        }
        return ConditionEvaluationResult.disabled("Target does not support more than one NamedGroup in TLS 1.3");
    }

    @TlsTest(description = "If a client receives a second "
            + "HelloRetryRequest in the same connection (i.e., where the ClientHello "
            + "was itself in response to a HelloRetryRequest), it MUST abort the "
            + "handshake with an \"unexpected_message\" alert.")
    @DynamicValueConstraints(affectedTypes = BasicDerivationType.NAMED_GROUP, methods = "isNotKeyShareInInitialHello")
    @MethodCondition(method = "supportsMultipleNamedGroups")
    @HandshakeCategory(SeverityLevel.MEDIUM)
    @AlertCategory(SeverityLevel.MEDIUM)
    @ComplianceCategory(SeverityLevel.HIGH)
    public void sendSecondHelloRetryRequest(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);
        WorkflowTrace workflowTrace = new WorkflowTrace();
        NamedGroup selectedGroup = derivationContainer.getDerivation(NamedGroupDerivation.class).getSelectedValue();
        //re-requesting the same group is covered by another testcase
        NamedGroup otherRequestableGroup = getOtherSupportedNamedGroup(selectedGroup);

        //first hello retry gets added by WorkflowRunner
        ServerHelloMessage secondHelloRetry = new ServerHelloMessage(c);
        secondHelloRetry.setRandom(Modifiable.explicit(ServerHelloMessage.getHelloRetryRequestRandom()));
        secondHelloRetry.getExtension(KeyShareExtensionMessage.class).setKeyShareListBytes(Modifiable.explicit(otherRequestableGroup.getValue()));

        workflowTrace.addTlsActions(
                new ReceiveAction(new ClientHelloMessage()),
                new SendAction(secondHelloRetry),
                new ReceiveAction(new AlertMessage())
        );

        runner.execute(workflowTrace, c).validateFinal(i -> {
            assertFalse("Client replied to second HelloRetryRequest with ClientHello", WorkflowTraceUtil.getLastReceivedMessage(i.getWorkflowTrace()) instanceof ClientHelloMessage 
                    && i.getWorkflowTrace().getLastReceivingAction().getReceivedMessages() != null
                    && i.getWorkflowTrace().getLastReceivingAction().getReceivedMessages().contains(WorkflowTraceUtil.getLastReceivedMessage(i.getWorkflowTrace())));
            Validator.receivedFatalAlert(i);
            AlertMessage alert = i.getWorkflowTrace().getFirstReceivedMessage(AlertMessage.class);
            Validator.testAlertDescription(i, AlertDescription.UNEXPECTED_MESSAGE, alert);
        });
    }

    private ConditionEvaluationResult supportsMultipleCipherSuites() {
        if (context.getSiteReport().getSupportedTls13CipherSuites().size() > 1) {
            return ConditionEvaluationResult.enabled("More than one CipherSuite supported by target in TLS 1.3");
        }
        return ConditionEvaluationResult.disabled("Target does not support more than one CipherSuite in TLS 1.3");
    }

    @TlsTest(description = "Upon receiving "
            + "the ServerHello, clients MUST check that the cipher suite supplied in "
            + "the ServerHello is the same as that in the HelloRetryRequest and "
            + "otherwise abort the handshake with an \"illegal_parameter\" alert.")
    @ScopeExtensions(BasicDerivationType.MIRRORED_CIPHERSUITE)
    @DynamicValueConstraints(affectedTypes = BasicDerivationType.NAMED_GROUP, methods = "isNotKeyShareInInitialHello")
    @MethodCondition(method = "supportsMultipleCipherSuites")
    @HandshakeCategory(SeverityLevel.MEDIUM)
    @AlertCategory(SeverityLevel.MEDIUM)
    @ComplianceCategory(SeverityLevel.HIGH)
    public void cipherSuiteDisparity(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);
        runner.setAutoHelloRetryRequest(false);
        NamedGroup selectedGroup = derivationContainer.getDerivation(NamedGroupDerivation.class).getSelectedValue();
        CipherSuite helloRetryCipherSuite = derivationContainer.getDerivation(MirroredCipherSuiteDerivation.class).getSelectedValue();

        WorkflowTrace workflowTrace = runner.generateWorkflowTraceUntilReceivingMessage(WorkflowTraceType.HANDSHAKE, ProtocolMessageType.CHANGE_CIPHER_SPEC);

        workflowTrace.addTlsActions(new ReceiveAction(new AlertMessage()));
        runner.insertHelloRetryRequest(workflowTrace, selectedGroup);
        ServerHelloMessage helloRetryRequest = (ServerHelloMessage) WorkflowTraceUtil.getFirstSendMessage(HandshakeMessageType.SERVER_HELLO, workflowTrace);
        helloRetryRequest.setSelectedCipherSuite(Modifiable.explicit(helloRetryCipherSuite.getByteValue()));

        runner.execute(workflowTrace, c).validateFinal(i -> {
            Validator.receivedFatalAlert(i);
            AlertMessage alert = i.getWorkflowTrace().getFirstReceivedMessage(AlertMessage.class);
            Validator.testAlertDescription(i, AlertDescription.ILLEGAL_PARAMETER, alert);
        });
    }

    @TlsTest(description = "A client which receives a legacy_session_id_echo "
            + "field that does not match what it sent in the ClientHello MUST "
            + "abort the handshake with an \"illegal_parameter\" alert.")
    @DynamicValueConstraints(affectedTypes = BasicDerivationType.NAMED_GROUP, methods = "isNotKeyShareInInitialHello")
    @HandshakeCategory(SeverityLevel.MEDIUM)
    @AlertCategory(SeverityLevel.MEDIUM)
    @ComplianceCategory(SeverityLevel.HIGH)
    public void helloRetryLegacySessionId(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        WorkflowTrace workflowTrace = getSharedTestWorkflowTrace(argumentAccessor, runner);
        ServerHello.sharedSessionIdTest(workflowTrace, runner);
    }

    @TlsTest(description = "legacy_compression_method: A single byte which "
            + "MUST have the value 0.")
    @DynamicValueConstraints(affectedTypes = BasicDerivationType.NAMED_GROUP, methods = "isNotKeyShareInInitialHello")
    @HandshakeCategory(SeverityLevel.MEDIUM)
    @AlertCategory(SeverityLevel.MEDIUM)
    @ComplianceCategory(SeverityLevel.MEDIUM)
    @DeprecatedFeatureCategory(SeverityLevel.HIGH)
    public void helloRetryCompressionValue(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        WorkflowTrace workflowTrace = getSharedTestWorkflowTrace(argumentAccessor, runner);
        ServerHello.sharedCompressionValueTest(workflowTrace, runner);
    }

    @TlsTest(description = "Clients MUST reject GREASE values when negotiated by the server. "
            + "In particular, the client MUST fail the connection "
            + "if a GREASE value appears in any of the following: "
            + "The \"cipher_suite\" value in a ServerHello")
    @RFC(number = 8701, section = "4. Server-Initiated Extension Points")
    @ScopeExtensions(BasicDerivationType.GREASE_CIPHERSUITE)
    @ScopeLimitations(BasicDerivationType.CIPHERSUITE)
    @DynamicValueConstraints(affectedTypes = BasicDerivationType.NAMED_GROUP, methods = "isNotKeyShareInInitialHello")
    @ComplianceCategory(SeverityLevel.MEDIUM)
    @HandshakeCategory(SeverityLevel.MEDIUM)
    public void helloRetryGreaseCipherSuite(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        WorkflowTrace workflowTrace = getSharedTestWorkflowTrace(argumentAccessor, runner);
        ServerInitiatedExtensionPoints.sharedGreaseCipherSuiteTest(workflowTrace, runner, derivationContainer);
    }

    @TlsTest(description = "Clients MUST reject GREASE values when negotiated by the server. "
            + "In particular, the client MUST fail the connection "
            + "if a GREASE value appears in any of the following: "
            + "Any ServerHello extension")
    @RFC(number = 8701, section = "4. Server-Initiated Extension Points")
    @ModelFromScope(baseModel = ModelType.CERTIFICATE)
    @ScopeExtensions(BasicDerivationType.GREASE_EXTENSION)
    @DynamicValueConstraints(affectedTypes = BasicDerivationType.NAMED_GROUP, methods = "isNotKeyShareInInitialHello")
    @ComplianceCategory(SeverityLevel.MEDIUM)
    @HandshakeCategory(SeverityLevel.MEDIUM)
    public void helloRetryGreaseExtension(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        WorkflowTrace workflowTrace = getSharedTestWorkflowTrace(argumentAccessor, runner);
        ServerInitiatedExtensionPoints.sharedServerHelloGreaseExtensionTest(workflowTrace, runner, derivationContainer);
    }

    @TlsTest(description = "Clients MUST reject GREASE values when negotiated by the server. "
            + "In particular, the client MUST fail the connection "
            + "if a GREASE value appears in any of the following: "
            + "The \"version\" value in a ServerHello or HelloRetryRequest")
    @RFC(number = 8701, section = "4. Server-Initiated Extension Points")
    @ModelFromScope(baseModel = ModelType.CERTIFICATE)
    @ScopeExtensions(BasicDerivationType.GREASE_PROTOCOL_VERSION)
    @DynamicValueConstraints(affectedTypes = BasicDerivationType.NAMED_GROUP, methods = "isNotKeyShareInInitialHello")
    @ComplianceCategory(SeverityLevel.MEDIUM)
    @HandshakeCategory(SeverityLevel.MEDIUM)
    public void helloRetryGreaseVersionSelected(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        WorkflowTrace workflowTrace = getSharedTestWorkflowTrace(argumentAccessor, runner);
        ServerInitiatedExtensionPoints.sharedGreaseVersionTest(workflowTrace, runner, derivationContainer);
    }

    private WorkflowTrace getSharedTestWorkflowTrace(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);
        runner.setAutoHelloRetryRequest(false);
        NamedGroup selectedGroup = derivationContainer.getDerivation(NamedGroupDerivation.class).getSelectedValue();
        WorkflowTrace workflowTrace = new WorkflowTrace();
        workflowTrace.addTlsAction(new ReceiveAction(new AlertMessage()));
        runner.insertHelloRetryRequest(workflowTrace, selectedGroup);

        return workflowTrace;
    }

    @RFC(number = 8446, section = "4.1.2 Client Hello")
    @TlsTest(description = "The client will also send a\n"
            + "ClientHello when the server has responded to its ClientHello with a "
            + "HelloRetryRequest. In that case, the client MUST send the same "
            + "ClientHello without modification, except as follows: [...]")
    @DynamicValueConstraints(affectedTypes = BasicDerivationType.NAMED_GROUP, methods = "isNotKeyShareInInitialHello")
    @InteroperabilityCategory(SeverityLevel.HIGH)
    @HandshakeCategory(SeverityLevel.MEDIUM)
    @AlertCategory(SeverityLevel.MEDIUM)
    @ComplianceCategory(SeverityLevel.HIGH)
    public void helloRetryIsUnmodifiedExceptAllowed(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);
        WorkflowTrace trace = runner.generateWorkflowTrace(WorkflowTraceType.SHORT_HELLO);

        runner.execute(trace, c).validateFinal(i -> {
            WorkflowTrace executedTrace = i.getWorkflowTrace();
            Validator.executedAsPlanned(i);

            ClientHelloMessage firstClientHello = (ClientHelloMessage) WorkflowTraceUtil.getFirstReceivedMessage(HandshakeMessageType.CLIENT_HELLO, trace);
            ClientHelloMessage retryClientHello = (ClientHelloMessage) WorkflowTraceUtil.getLastReceivedMessage(HandshakeMessageType.CLIENT_HELLO, trace);
            assertTrue("Did not receive two Client Hello messages", firstClientHello != null && retryClientHello != null && firstClientHello != retryClientHello);
            testIfExtensionsAreEqual(firstClientHello, retryClientHello);
            testIfClientHelloFieldsAreEqual(firstClientHello, retryClientHello);
        });
    }

    private void testIfExtensionsAreEqual(ClientHelloMessage firstClientHello, ClientHelloMessage retryClientHello) {
        // the client MUST send the same ClientHello without modification, except as follows:
        // -If a "key_share" extension was supplied in the HelloRetryRequest, replacing the list of shares with a list containing a single
        //  KeyShareEntry from the indicated group.
        // (-Including a "cookie" extension if one was provided in the HelloRetryRequest)
        // -Updating the "pre_shared_key" extension if present by recomputing the "obfuscated_ticket_age" and binder values
        //  and (optionally) removing any PSKs which are incompatible with the server’s indicated cipher suite.
        // -Optionally adding, removing, or changing the length of the "padding" extension
        List<ExtensionType> extensionsInSecondHello = new LinkedList<>();
        retryClientHello.getExtensions().forEach(extension -> extensionsInSecondHello.add(extension.getExtensionTypeConstant()));
        for (ExtensionMessage extension : firstClientHello.getExtensions()) {

            assertTrue("Extensions List not equal - second Client Hello did not contain " + extension.getExtensionTypeConstant(), retryClientHello.containsExtension(extension.getExtensionTypeConstant())
                    || extension.getExtensionTypeConstant() == ExtensionType.PADDING
                    || extension.getExtensionTypeConstant() == ExtensionType.EARLY_DATA
                    || extension.getExtensionTypeConstant() == ExtensionType.COOKIE
                    || extension.getExtensionTypeConstant() == ExtensionType.PRE_SHARED_KEY);

            if (extension.getExtensionTypeConstant() != ExtensionType.KEY_SHARE
                    && extension.getExtensionTypeConstant() != ExtensionType.PADDING
                    && extension.getExtensionTypeConstant() != ExtensionType.PRE_SHARED_KEY
                    && extension.getExtensionTypeConstant() != ExtensionType.EARLY_DATA
                    && extension.getExtensionTypeConstant() != ExtensionType.COOKIE) {
                assertTrue("Extension " + extension.getExtensionTypeConstant() + " is not identical to second Client Hello", Arrays.equals(extension.getExtensionBytes().getValue(), retryClientHello.getExtension(extension.getClass()).getExtensionBytes().getValue()));
            }
            extensionsInSecondHello.remove(extension.getExtensionTypeConstant());
        }

        //only these extensions may be added to retry Hello
        //we are not requesting a cookie value
        if (extensionsInSecondHello.size() > 0) {
            extensionsInSecondHello.remove(ExtensionType.PADDING);
            extensionsInSecondHello.remove(ExtensionType.KEY_SHARE);
        }
        assertTrue("Second Client Hello contained additional Extensions: " + extensionsInSecondHello.stream().map(ExtensionType::toString).collect(Collectors.joining(",")), extensionsInSecondHello.isEmpty());
    }

    private void testIfClientHelloFieldsAreEqual(ClientHelloMessage firstClientHello, ClientHelloMessage retryClientHello) {
        assertTrue("Offered CipherSuites are not identical", Arrays.equals(firstClientHello.getCipherSuites().getValue(), retryClientHello.getCipherSuites().getValue()));
        assertTrue("Offered CompressionList lengths are not identical", firstClientHello.getCompressionLength().getValue().equals(retryClientHello.getCompressionLength().getValue()));
        assertTrue("Selected ClientRandoms are not identical", Arrays.equals(firstClientHello.getRandom().getValue(), retryClientHello.getRandom().getValue()));
        assertTrue("Selected ProtocolVersions are not identical", Arrays.equals(firstClientHello.getProtocolVersion().getValue(), retryClientHello.getProtocolVersion().getValue()));
        assertTrue("TLS 1.3 compatibility SessionIDs are not identical", Arrays.equals(firstClientHello.getSessionId().getValue(), retryClientHello.getSessionId().getValue()));
    }
    
    public List<DerivationParameter> getTls12CipherSuites(DerivationScope scope) {
        List<DerivationParameter> parameterValues = new LinkedList<>();
        context.getSiteReport().getCipherSuites().forEach(cipherSuite -> parameterValues.add(new CipherSuiteDerivation(cipherSuite)));
        return parameterValues;
    }

    @TlsTest(description = "Enforce a TLS 1.3 HelloRetryRequest but select a TLS 1.2 Cipher Suite")
    @DynamicValueConstraints(affectedTypes = BasicDerivationType.NAMED_GROUP, methods = "isNotKeyShareInInitialHello")
    @ExplicitValues(affectedTypes = BasicDerivationType.CIPHERSUITE, methods = "getTls12CipherSuites")
    @HandshakeCategory(SeverityLevel.MEDIUM)
    @AlertCategory(SeverityLevel.MEDIUM)
    @ComplianceCategory(SeverityLevel.MEDIUM)
    @SecurityCategory(SeverityLevel.LOW)
    public void helloRetryRequestsTls12CipherSuite(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        performHelloRetryRequestTest(argumentAccessor, runner);
    }
}
