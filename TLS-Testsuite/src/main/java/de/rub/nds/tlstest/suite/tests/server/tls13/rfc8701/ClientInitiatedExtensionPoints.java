/**
 * TLS-Testsuite - A testsuite for the TLS protocol
 *
 * <p>Copyright 2022 Ruhr University Bochum
 *
 * <p>Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.suite.tests.server.tls13.rfc8701;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import de.rub.nds.anvilcore.annotation.*;
import de.rub.nds.anvilcore.teststate.AnvilTestCase;
import de.rub.nds.modifiablevariable.util.ArrayConverter;
import de.rub.nds.modifiablevariable.util.Modifiable;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.*;
import de.rub.nds.tlsattacker.core.protocol.message.CertificateVerifyMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ClientHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.EncryptedExtensionsMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.AlpnExtensionMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.EllipticCurvesExtensionMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.GreaseExtensionMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.KeyShareExtensionMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.alpn.AlpnEntry;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceConfigurationUtil;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlstest.framework.Validator;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.model.derivationParameter.GreaseCipherSuiteDerivation;
import de.rub.nds.tlstest.framework.model.derivationParameter.GreaseExtensionDerivation;
import de.rub.nds.tlstest.framework.model.derivationParameter.GreaseNamedGroupDerivation;
import de.rub.nds.tlstest.framework.model.derivationParameter.GreaseSigHashDerivation;
import de.rub.nds.tlstest.framework.testClasses.Tls13Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ServerTest
public class ClientInitiatedExtensionPoints extends Tls13Test {

    @AnvilTest(id = "8701-iaW1cm19MU")
    @IncludeParameter("GREASE_CIPHERSUITE")
    @ExcludeParameters({
        @ExcludeParameter("INCLUDE_GREASE_CIPHER_SUITES"),
        @ExcludeParameter("INCLUDE_GREASE_NAMED_GROUPS"),
        @ExcludeParameter("INCLUDE_GREASE_SIG_HASH_ALGORITHMS")
    })
    public void advertiseGreaseCiphersuites(AnvilTestCase testCase, WorkflowRunner runner) {
        Config c = getPreparedConfig(runner);
        CipherSuite selectedGrease =
                parameterCombination
                        .getParameter(GreaseCipherSuiteDerivation.class)
                        .getSelectedValue();

        c.getDefaultClientSupportedCipherSuites().add(0, selectedGrease);

        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);
        State state = runner.execute(workflowTrace, c);

        Validator.executedAsPlanned(state, testCase);

        assertEquals(
                "Server selected wrong ciphersuite",
                c.getDefaultSelectedCipherSuite(),
                state.getTlsContext().getSelectedCipherSuite());
    }

    @AnvilTest(id = "8701-PErDdQZt7u")
    @IncludeParameter("GREASE_EXTENSION")
    @ExcludeParameters({
        @ExcludeParameter("INCLUDE_GREASE_CIPHER_SUITES"),
        @ExcludeParameter("INCLUDE_GREASE_NAMED_GROUPS"),
        @ExcludeParameter("INCLUDE_GREASE_SIG_HASH_ALGORITHMS")
    })
    public void advertiseGreaseExtensions(AnvilTestCase testCase, WorkflowRunner runner) {
        Config c = getPreparedConfig(runner);
        ExtensionType selectedGrease =
                parameterCombination
                        .getParameter(GreaseExtensionDerivation.class)
                        .getSelectedValue();

        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);

        ClientHelloMessage ch =
                ((ClientHelloMessage)
                        WorkflowTraceConfigurationUtil.getFirstStaticConfiguredSendMessage(
                                workflowTrace, HandshakeMessageType.CLIENT_HELLO));
        ch.addExtension(new GreaseExtensionMessage(selectedGrease, 25));

        State state = runner.execute(workflowTrace, c);

        Validator.executedAsPlanned(state, testCase);

        ServerHelloMessage msg =
                state.getWorkflowTrace().getFirstReceivedMessage(ServerHelloMessage.class);
        state.getTlsContext()
                .getNegotiatedExtensionSet()
                .forEach(
                        j -> {
                            assertFalse(
                                    "Server negotiated GREASE extension",
                                    j.name().startsWith("GREASE"));
                        });
    }

    @AnvilTest(id = "8701-2XMSQq7p9T")
    @IncludeParameter("GREASE_NAMED_GROUP")
    @ExcludeParameters({
        @ExcludeParameter("INCLUDE_GREASE_CIPHER_SUITES"),
        @ExcludeParameter("INCLUDE_GREASE_NAMED_GROUPS"),
        @ExcludeParameter("INCLUDE_GREASE_SIG_HASH_ALGORITHMS")
    })
    public void advertiseGreaseNamedGroup(AnvilTestCase testCase, WorkflowRunner runner) {
        Config c = getPreparedConfig(runner);
        NamedGroup selectedGrease =
                parameterCombination
                        .getParameter(GreaseNamedGroupDerivation.class)
                        .getSelectedValue();
        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);

        byte[] greaseLength = new byte[] {0, 32};
        byte[] greaseKeyShare = new byte[32];
        byte[] completeGreaseKeyShareEntry =
                ArrayConverter.concatenate(selectedGrease.getValue(), greaseLength, greaseKeyShare);

        ((ClientHelloMessage)
                        WorkflowTraceConfigurationUtil.getFirstStaticConfiguredSendMessage(
                                workflowTrace, HandshakeMessageType.CLIENT_HELLO))
                .getExtension(EllipticCurvesExtensionMessage.class)
                .setSupportedGroups(Modifiable.insert(selectedGrease.getValue(), 0));
        ((ClientHelloMessage)
                        WorkflowTraceConfigurationUtil.getFirstStaticConfiguredSendMessage(
                                workflowTrace, HandshakeMessageType.CLIENT_HELLO))
                .getExtension(KeyShareExtensionMessage.class)
                .setKeyShareListBytes(Modifiable.insert(completeGreaseKeyShareEntry, 0));

        State state = runner.execute(workflowTrace, c);

        Validator.executedAsPlanned(state, testCase);

        ServerHelloMessage msg =
                state.getWorkflowTrace().getFirstReceivedMessage(ServerHelloMessage.class);
        KeyShareExtensionMessage keyshares = msg.getExtension(KeyShareExtensionMessage.class);
        keyshares
                .getKeyShareList()
                .forEach(
                        j -> {
                            assertFalse(
                                    "Server negotiated GREASE named group",
                                    NamedGroup.getNamedGroup(j.getGroup().getValue()).isGrease());
                        });
    }

    @AnvilTest(id = "8701-ek86W17BUz")
    @IncludeParameter("GREASE_SIG_HASH")
    @ExcludeParameters({
        @ExcludeParameter("INCLUDE_GREASE_CIPHER_SUITES"),
        @ExcludeParameter("INCLUDE_GREASE_NAMED_GROUPS"),
        @ExcludeParameter("INCLUDE_GREASE_SIG_HASH_ALGORITHMS")
    })
    public void advertiseGreaseSignatureAlgorithms(AnvilTestCase testCase, WorkflowRunner runner) {
        Config c = getPreparedConfig(runner);
        SignatureAndHashAlgorithm selectedGrease =
                parameterCombination.getParameter(GreaseSigHashDerivation.class).getSelectedValue();
        c.getDefaultClientSupportedSignatureAndHashAlgorithms().add(0, selectedGrease);

        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);

        State state = runner.execute(workflowTrace, c);

        Validator.executedAsPlanned(state, testCase);

        CertificateVerifyMessage msg =
                state.getWorkflowTrace().getFirstReceivedMessage(CertificateVerifyMessage.class);
        SignatureAndHashAlgorithm selected =
                SignatureAndHashAlgorithm.getSignatureAndHashAlgorithm(
                        msg.getSignatureHashAlgorithm().getValue());
        assertFalse("Server selected GREASE signature and hash algorithm", selected.isGrease());
    }

    @AnvilTest(id = "8701-fe7Ev3bbiq")
    @ExcludeParameters({
        @ExcludeParameter("INCLUDE_GREASE_CIPHER_SUITES"),
        @ExcludeParameter("INCLUDE_GREASE_NAMED_GROUPS"),
        @ExcludeParameter("INCLUDE_GREASE_SIG_HASH_ALGORITHMS")
    })
    public void advertiseGreaseALPNIdentifiers(AnvilTestCase testCase, WorkflowRunner runner) {
        Config c = getPreparedConfig(runner);
        c.setAddAlpnExtension(true);
        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HELLO);

        List<AlpnEntry> alpnEntries = new ArrayList<>();
        for (CipherSuite i :
                Arrays.stream(CipherSuite.values())
                        .filter(CipherSuite::isGrease)
                        .collect(Collectors.toList())) {
            alpnEntries.add(new AlpnEntry(i.name()));
        }

        ClientHelloMessage msg =
                ((ClientHelloMessage)
                        WorkflowTraceConfigurationUtil.getFirstStaticConfiguredSendMessage(
                                workflowTrace, HandshakeMessageType.CLIENT_HELLO));
        AlpnExtensionMessage ext = msg.getExtension(AlpnExtensionMessage.class);
        ext.setAlpnEntryList(alpnEntries);

        State state = runner.execute(workflowTrace, c);

        Validator.executedAsPlanned(state, testCase);

        EncryptedExtensionsMessage emsg =
                state.getWorkflowTrace().getFirstReceivedMessage(EncryptedExtensionsMessage.class);
        AlpnExtensionMessage aext = emsg.getExtension(AlpnExtensionMessage.class);
        if (aext == null) {
            return;
        }

        assertEquals(
                "AlpnEntryExtension contains more or less than one protocol",
                1,
                aext.getAlpnEntryList().size());
        assertFalse(
                "Server negotiated GREASE ALPN Identifier",
                ext.getAlpnEntryList().get(0).getAlpnEntryConfig().contains("GREASE"));
    }
}
