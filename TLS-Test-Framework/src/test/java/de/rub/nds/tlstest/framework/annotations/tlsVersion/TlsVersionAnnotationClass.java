/**
 * TLS-Test-Framework - A framework for modeling TLS tests
 *
 * <p>Copyright 2022 Ruhr University Bochum
 *
 * <p>Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.framework.annotations.tlsVersion;

import de.rub.nds.anvilcore.annotation.AnvilTest;
import de.rub.nds.anvilcore.context.AnvilContext;
import de.rub.nds.anvilcore.context.AnvilTestConfig;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlstest.framework.TestContext;
import de.rub.nds.tlstest.framework.annotations.TlsVersion;
import de.rub.nds.tlstest.framework.anvil.TlsParameterIdentifierProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;

@TlsVersion(supported = ProtocolVersion.TLS12)
public class TlsVersionAnnotationClass extends TlsVersionTest {

    @BeforeAll
    static void setup() {
        AnvilContext.createInstance(
                new AnvilTestConfig(), "", new TlsParameterIdentifierProvider());
        TestContext testContext = TestContext.getInstance();
    }

    @AnvilTest
    @TlsVersion(supported = ProtocolVersion.TLS12)
    @Disabled
    public void execute_supported() {}

    @AnvilTest
    @Disabled
    public void execute_inheritedClassAnnotation() {}

    @AnvilTest
    @TlsVersion(supported = ProtocolVersion.SSL3)
    @Disabled
    public void execute_supported_overwrittenClassAnnotation() {}

    @AnvilTest
    @TlsVersion(supported = ProtocolVersion.TLS13)
    @Disabled
    public void not_execute_unsupported() {}
}
