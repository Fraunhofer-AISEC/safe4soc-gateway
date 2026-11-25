package de.fraunhofer.aisec.s4sgateway.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

@Service
public class PartnerIdentityService implements UserDetailsService {
    private final Map<String, UserDetails> identities;

    public PartnerIdentityService(GatewayAccessProperties properties) {
        Map<String, UserDetails> configured = new HashMap<>();
        for (var grant : properties.certificates()) {
            if (grant.sha256() == null || !grant.sha256().matches("[0-9a-fA-F]{64}")) {
                throw new IllegalArgumentException("A certificate grant must specify a 64-digit SHA-256 fingerprint");
            }
            if (grant.permissions() == null || grant.permissions().isEmpty()) {
                throw new IllegalArgumentException("A certificate grant must specify at least one permission");
            }
            String fingerprint = grant.sha256().toLowerCase(Locale.ROOT);
            var authorities = grant.permissions().stream()
                    .map(permission -> new SimpleGrantedAuthority(permission.name()))
                    .toList();
            if (configured.putIfAbsent(fingerprint, new User(fingerprint, "", authorities)) != null) {
                throw new IllegalArgumentException("Duplicate certificate grant for fingerprint " + fingerprint);
            }
        }
        identities = Map.copyOf(configured);
    }

    public static String fingerprint(X509Certificate certificate) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
            return HexFormat.of().formatHex(digest);
        } catch (CertificateEncodingException e) {
            throw new BadCredentialsException("Cannot read the client certificate", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String fingerprint) {
        UserDetails identity = identities.get(fingerprint);
        if (identity == null) {
            throw new UsernameNotFoundException("Client certificate has no gateway permissions");
        }
        return identity;
    }
}
