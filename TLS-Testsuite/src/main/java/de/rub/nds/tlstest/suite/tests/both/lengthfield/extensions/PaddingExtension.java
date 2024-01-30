/**
 * TLS-Testsuite - A testsuite for the TLS protocol
 *
 * <p>Copyright 2022 Ruhr University Bochum
 *
 * <p>Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.suite.tests.both.lengthfield.extensions;

import de.rub.nds.anvilcore.annotation.AnvilTest;
import de.rub.nds.anvilcore.annotation.ExcludeParameter;
import de.rub.nds.anvilcore.annotation.ServerTest;
import de.rub.nds.anvilcore.coffee4j.model.ModelFromScope;
import de.rub.nds.anvilcore.teststate.AnvilTestCase;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.protocol.message.extension.PaddingExtensionMessage;
import de.rub.nds.tlstest.framework.annotations.KeyExchange;
import de.rub.nds.tlstest.framework.annotations.TlsVersion;
import de.rub.nds.tlstest.framework.constants.KeyExchangeType;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.testClasses.TlsGenericTest;
import org.junit.jupiter.api.Tag;

@ServerTest
public class PaddingExtension extends TlsGenericTest {

    @Tag("tls12")
    @TlsVersion(supported = ProtocolVersion.TLS12)
    @KeyExchange(supported = KeyExchangeType.ALL12)
    @AnvilTest(id = "XLF-thAfdtNTPh")
    @ExcludeParameter("INCLUDE_PADDING_EXTENSION")
    @ModelFromScope(modelType = "LENGTHFIELD")
    public void paddingExtensionLengthTLS12(AnvilTestCase testCase, WorkflowRunner runner) {
        Config config = context.getConfig().createConfig();
        config.setAddPaddingExtension(true);
        genericExtensionLengthTest(runner, testCase, config, PaddingExtensionMessage.class);
    }

    @Tag("tls13")
    @TlsVersion(supported = ProtocolVersion.TLS13)
    @KeyExchange(supported = KeyExchangeType.ALL13)
    @AnvilTest(id = "XLF-a56v24NnM5")
    @ExcludeParameter("INCLUDE_PADDING_EXTENSION")
    @ModelFromScope(modelType = "LENGTHFIELD")
    public void paddingExtensionLengthTLS13(AnvilTestCase testCase, WorkflowRunner runner) {
        Config config = context.getConfig().createTls13Config();
        config.setAddPaddingExtension(true);
        genericExtensionLengthTest(runner, testCase, config, PaddingExtensionMessage.class);
    }
}
