package net.shrine.dashboard.jwtauth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;

import java.security.Key;

/**
 * SigningKeyResolverBridge is in Java because JwsHeader takes a parameter, but SigningKeyResolverAdapter specifies it as a raw type.
 *
 * @author david
 * @since 1.21
 */
public class SigningKeyResolverBridge extends SigningKeyResolverAdapter {

    public Key resolveSigningKey(JwsHeader header, Claims claims) {
        String keyId = header.getKeyId();
        return KeySource.keyForString(keyId);
    }
}