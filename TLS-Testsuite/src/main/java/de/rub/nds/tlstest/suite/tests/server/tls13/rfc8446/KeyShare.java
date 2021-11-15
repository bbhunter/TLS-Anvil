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

import de.rub.nds.modifiablevariable.util.ArrayConverter;
import de.rub.nds.modifiablevariable.util.Modifiable;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.AlertDescription;
import de.rub.nds.tlsattacker.core.constants.AlertLevel;
import de.rub.nds.tlsattacker.core.constants.ExtensionType;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.constants.NamedGroup;
import de.rub.nds.tlsattacker.core.protocol.message.AlertMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ClientHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.EllipticCurvesExtensionMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.KeyShareExtensionMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.keyshare.KeyShareEntry;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceUtil;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlstest.framework.Validator;
import de.rub.nds.tlstest.framework.annotations.EnforcedSenderRestriction;
import de.rub.nds.tlstest.framework.annotations.ExplicitValues;
import de.rub.nds.tlstest.framework.annotations.RFC;
import de.rub.nds.tlstest.framework.annotations.ScopeLimitations;
import de.rub.nds.tlstest.framework.annotations.ServerTest;
import de.rub.nds.tlstest.framework.annotations.TlsTest;
import de.rub.nds.tlstest.framework.annotations.categories.ComplianceCategory;
import de.rub.nds.tlstest.framework.annotations.categories.CryptoCategory;
import de.rub.nds.tlstest.framework.annotations.categories.DeprecatedFeatureCategory;
import de.rub.nds.tlstest.framework.annotations.categories.HandshakeCategory;
import de.rub.nds.tlstest.framework.annotations.categories.InteroperabilityCategory;
import de.rub.nds.tlstest.framework.annotations.categories.SecurityCategory;
import de.rub.nds.tlstest.framework.constants.AssertMsgs;
import de.rub.nds.tlstest.framework.constants.SeverityLevel;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.model.DerivationScope;
import de.rub.nds.tlstest.framework.model.derivationParameter.BasicDerivationType;
import de.rub.nds.tlstest.framework.model.derivationParameter.DerivationParameter;
import de.rub.nds.tlstest.framework.model.derivationParameter.NamedGroupDerivation;
import de.rub.nds.tlstest.framework.testClasses.Tls13Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;

@ServerTest
@RFC(number = 8446, section = "4.2.8. Key Share")
public class KeyShare extends Tls13Test {

    @TlsTest(description = "Each KeyShareEntry value MUST correspond "
            + "to a group offered in the \"supported_groups\" extension "
            + "and MUST appear in the same order.")
    @ScopeLimitations(BasicDerivationType.NAMED_GROUP)
    @HandshakeCategory(SeverityLevel.MEDIUM)
    @ComplianceCategory(SeverityLevel.MEDIUM)
    /*
        Servers MAY check for violations of these rules and abort the
        handshake with an "illegal_parameter" alert if one is violated.
    */
    @EnforcedSenderRestriction
    public void testOrderOfKeyshareEntries(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);

        List<NamedGroup> groups = new ArrayList<NamedGroup>() {
            {
                add(NamedGroup.SECP256R1);
                add(NamedGroup.SECP384R1);
                add(NamedGroup.SECP521R1);
                add(NamedGroup.ECDH_X25519);
                add(NamedGroup.ECDH_X448);
            }
        };

        c.setDefaultClientKeyShareNamedGroups(new ArrayList<>(groups));
        Collections.reverse(groups);
        c.setDefaultClientNamedGroups(groups);

        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HANDSHAKE);
        runner.execute(workflowTrace, c).validateFinal(i -> {
            WorkflowTrace trace = i.getWorkflowTrace();
            AlertMessage alert = trace.getFirstReceivedMessage(AlertMessage.class);
            ServerHelloMessage shm = trace.getFirstReceivedMessage(ServerHelloMessage.class);
            if (alert != null && shm == null) {
                assertEquals("No fatal alert received", AlertLevel.FATAL.getValue(), alert.getLevel().getValue().byteValue());
                Validator.testAlertDescription(i, AlertDescription.ILLEGAL_PARAMETER, alert);
                i.addAdditionalResultInfo("Received alert");
                return;
            }

            assertTrue(AssertMsgs.WorkflowNotExecuted + ", server likely selected the wrong key share",
                    i.getWorkflowTrace().executedAsPlanned());
        });
    }

    @TlsTest(description = "If using (EC)DHE key establishment, servers offer exactly one KeyShareEntry in the ServerHello. "
            + "This value MUST be in the same group as the KeyShareEntry value offered by the client "
            + "that the server has selected for the negotiated key exchange.")
    @InteroperabilityCategory(SeverityLevel.MEDIUM)
    @HandshakeCategory(SeverityLevel.MEDIUM)
    @ComplianceCategory(SeverityLevel.HIGH)
    public void serverOnlyOffersOneKeyshare(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);
        List<NamedGroup> supportedTls13 = context.getSiteReport().getSupportedTls13Groups();
        
        //place selected group at the top to avoid (optional) HRR
        NamedGroup selectedGroup = derivationContainer.getDerivation(NamedGroupDerivation.class).getSelectedValue();
        supportedTls13.remove(selectedGroup);
        supportedTls13.add(0, selectedGroup);
        
        c.setDefaultClientNamedGroups(supportedTls13);
        performOneKeyshareTest(c, runner);
    }

    @TlsTest(description = "If using (EC)DHE key establishment, servers offer exactly one KeyShareEntry in the ServerHello. "
            + "This value MUST be in the same group as the KeyShareEntry value offered by the client "
            + "that the server has selected for the negotiated key exchange.")
    @ScopeLimitations(BasicDerivationType.NAMED_GROUP)
    @InteroperabilityCategory(SeverityLevel.MEDIUM)
    @HandshakeCategory(SeverityLevel.MEDIUM)
    @ComplianceCategory(SeverityLevel.HIGH)
    public void serverOnlyOffersOneKeyshareAllGroupsAtOnce(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);
        List<NamedGroup> supportedTls13 = context.getSiteReport().getSupportedTls13Groups();
        c.setDefaultClientKeyShareNamedGroups(supportedTls13);
        c.setDefaultClientNamedGroups(supportedTls13);
        performOneKeyshareTest(c, runner);
    }

    public void performOneKeyshareTest(Config c, WorkflowRunner runner) {
        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);

        runner.execute(workflowTrace, c).validateFinal(i -> {
            ServerHelloMessage serverHello = i.getWorkflowTrace().getFirstReceivedMessage(ServerHelloMessage.class);
            assertTrue("No ServerHello has been received", serverHello != null);
            KeyShareExtensionMessage keyshare = serverHello.getExtension(KeyShareExtensionMessage.class);
            if(serverHello.isTls13HelloRetryRequest()) {
                i.addAdditionalResultInfo("Server enforced own preferred group");
                assertTrue("Server requested an unproposed group in HelloRetryRequest",c.getDefaultClientNamedGroups().contains(keyshare.getKeyShareList().stream().map(KeyShareEntry::getGroupConfig).collect(Collectors.toList()).get(0)));
            } else {
                Validator.executedAsPlanned(i);
                assertTrue("Server selected group for which no Key Share was sent outside of HelloRetryRequest", c.getDefaultClientKeyShareNamedGroups().contains(keyshare.getKeyShareList().stream().map(KeyShareEntry::getGroupConfig).collect(Collectors.toList()).get(0)));
            }
            assertEquals("Server offered more than one keyshare entry", 1, keyshare.getKeyShareList().size());    
        });
    }
    
    public List<DerivationParameter> getLegacyGroups(DerivationScope scope) {
        List<DerivationParameter> parameterValues = new LinkedList<>();
        List<NamedGroup> groups = NamedGroup.getImplemented();
        groups.removeIf(i -> i.isTls13());
        groups.forEach(i -> parameterValues.add(new NamedGroupDerivation(i)));
        return parameterValues;
    }

    @TlsTest(description = "RFC 8446 (TLS 1.3) and RFC 8422 deprecated curves may not be used")
    @ExplicitValues(affectedTypes = BasicDerivationType.NAMED_GROUP, methods = "getLegacyGroups")
    @HandshakeCategory(SeverityLevel.MEDIUM)
    @ComplianceCategory(SeverityLevel.HIGH)
    @CryptoCategory(SeverityLevel.HIGH)
    @DeprecatedFeatureCategory(SeverityLevel.HIGH)
    @SecurityCategory(SeverityLevel.LOW)
    public void serverAcceptsDeprecatedGroups(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);
        List<NamedGroup> groups = NamedGroup.getImplemented();
        groups.removeIf(i -> i.isTls13());
        performDeprecatedGroupsTest(c, runner);
    }

    @TlsTest(description = "RFC 8446 (TLS 1.3) and RFC 8422 deprecated curves may not be used")
    @ScopeLimitations(BasicDerivationType.NAMED_GROUP)
    @HandshakeCategory(SeverityLevel.MEDIUM)
    @ComplianceCategory(SeverityLevel.HIGH)
    @CryptoCategory(SeverityLevel.HIGH)
    @DeprecatedFeatureCategory(SeverityLevel.HIGH)
    @SecurityCategory(SeverityLevel.LOW)
    public void serverAcceptsDeprecatedGroupsAllAtOnce(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config c = getPreparedConfig(argumentAccessor, runner);
        List<NamedGroup> groups = NamedGroup.getImplemented();
        groups.removeIf(i -> i.isTls13());
        c.setDefaultClientNamedGroups(groups);
        c.setDefaultClientKeyShareNamedGroups(groups);

        performDeprecatedGroupsTest(c, runner);
    }

    public void performDeprecatedGroupsTest(Config c, WorkflowRunner runner) {
        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HANDSHAKE);
        List<NamedGroup> groups = c.getDefaultClientKeyShareNamedGroups();

        runner.execute(workflowTrace, c).validateFinal(i -> {
            WorkflowTrace trace = i.getWorkflowTrace();
            if (WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO, trace) && trace.getFirstReceivedMessage(ServerHelloMessage.class).containsExtension(ExtensionType.KEY_SHARE)) {
                KeyShareExtensionMessage ksExt = trace.getFirstReceivedMessage(ServerHelloMessage.class).getExtension(KeyShareExtensionMessage.class);
                assertFalse("Server accepted a deprecated group", groups.contains(ksExt.getKeyShareList().stream().map(KeyShareEntry::getGroupConfig).collect(Collectors.toList()).get(0)));
                //other groups may not be used - even in HelloRetryRequest
                assertTrue("Server selected an unproposed group", groups.contains(ksExt.getKeyShareList().stream().map(KeyShareEntry::getGroupConfig).collect(Collectors.toList()).get(0)));
            }
        });
    }

    @TlsTest(description = "Send a Client Hello with an undefined Named Group")
    @InteroperabilityCategory(SeverityLevel.CRITICAL)
    @HandshakeCategory(SeverityLevel.MEDIUM)
    @ComplianceCategory(SeverityLevel.CRITICAL)
    public void includeUnknownGroup(ArgumentsAccessor argumentAccessor, WorkflowRunner runner) {
        Config config = getPreparedConfig(argumentAccessor, runner);
        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);

        byte[] undefinedGroup = new byte[]{(byte) 123, 124};
        byte[] dummyLength = new byte[]{(byte) 0, 56};
        byte[] dummyPublicKey = new byte[56];

        byte[] completeEntry = ArrayConverter.concatenate(undefinedGroup, dummyLength, dummyPublicKey);

        ClientHelloMessage clientHello = (ClientHelloMessage) WorkflowTraceUtil.getFirstSendMessage(HandshakeMessageType.CLIENT_HELLO, workflowTrace);
        EllipticCurvesExtensionMessage ellipticCurvesExtension = clientHello.getExtension(EllipticCurvesExtensionMessage.class);
        ellipticCurvesExtension.setSupportedGroups(Modifiable.insert(undefinedGroup, 0));

        KeyShareExtensionMessage keyShareExtension = clientHello.getExtension(KeyShareExtensionMessage.class);
        keyShareExtension.setKeyShareListBytes(Modifiable.insert(completeEntry, 0));

        runner.execute(workflowTrace, config).validateFinal(Validator::executedAsPlanned);
    }

}
