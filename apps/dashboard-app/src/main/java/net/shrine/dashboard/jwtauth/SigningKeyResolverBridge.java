package net.shrine.dashboard.jwtauth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;

import java.security.Key;

public class SigningKeyResolverBridge extends SigningKeyResolverAdapter {

    public Key resolveSigningKey(JwsHeader header, Claims claims) {
        String keyId = header.getKeyId();
        return KeySource.keyForString(keyId);
    }
}