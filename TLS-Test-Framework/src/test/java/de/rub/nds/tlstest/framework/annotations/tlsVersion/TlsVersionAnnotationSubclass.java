/**
 * TLS-Test-Framework - A framework for modeling TLS tests
 *
 * <p>Copyright 2022 Ruhr University Bochum
 *
 * <p>Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.framework.annotations.tlsVersion;

import de.rub.nds.anvilcore.annotation.AnvilTest;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlstest.framework.annotations.TlsVersion;
import org.junit.jupiter.api.Disabled;

@TlsVersion(supported = ProtocolVersion.TLS12)
class Tls12SuperClass {}

public class TlsVersionAnnotationSubclass extends TlsVersionTest {

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
