/**
 * TLS-Test-Framework - A framework for modeling TLS tests
 *
 * <p>Copyright 2020 Ruhr University Bochum and TÜV Informationstechnik GmbH
 *
 * <p>Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.framework.model.derivationParameter;

import de.rub.nds.tlstest.framework.model.DerivationType;

/** Represents the properties affected by the test derivation models. */
public enum BasicDerivationType implements DerivationType {
    CIPHERSUITE,
    NAMED_GROUP,
    MAC_BITMASK,
    ALERT,
    RECORD_LENGTH,
    TCP_FRAGMENTATION,
    CIPHERTEXT_BITMASK,
    AUTH_TAG_BITMASK,
    APP_MSG_LENGHT,
    PADDING_BITMASK,
    INVALID_CCS_CONTENT,
    PRF_BITMASK,
    GREASE_CIPHERSUITE,
    GREASE_PROTOCOL_VERSION,
    GREASE_EXTENSION,
    GREASE_NAMED_GROUP,
    GREASE_SIG_HASH,
    PROTOCOL_VERSION,
    SIG_HASH_ALGORIHTM,
    EXTENSION,
    CHOSEN_HANDSHAKE_MSG,
    MIRRORED_CIPHERSUITE,
    CERTIFICATE,
    SIGNATURE_BITMASK,
    BIT_POSITION,
    INCLUDE_RENEGOTIATION_EXTENSION,
    INCLUDE_EXTENDED_MASTER_SECRET_EXTENSION,
    INCLUDE_PADDING_EXTENSION,
    INCLUDE_ENCRYPT_THEN_MAC_EXTENSION,
    INCLUDE_ALPN_EXTENSION,
    INCLUDE_HEARTBEAT_EXTENSION,
    INCLUDE_CHANGE_CIPHER_SPEC,
    INCLUDE_PSK_EXCHANGE_MODES_EXTENSION,
    INCLUDE_SESSION_TICKET_EXTENSION,
    INCLUDE_GREASE_CIPHER_SUITES,
    INCLUDE_GREASE_SIG_HASH_ALGORITHMS,
    INCLUDE_GREASE_NAMED_GROUPS,
    ADDITIONAL_PADDING_LENGTH,
    COMPRESSION_METHOD,
    PROTOCOL_MESSAGE_TYPE,
    FFDHE_SHARE_OUT_OF_BOUNDS,
    MAX_FRAGMENT_LENGTH;

    public boolean isBitmaskDerivation() {
        return this.name().contains("BITMASK");
    }
}
