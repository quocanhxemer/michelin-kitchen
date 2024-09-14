package backend.server.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    // secret key used for hashing to create jwt tokens
    private static final String SECRET = null;

    public String extractUsername(String token) {
        // extract the username of the token (subject)
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts
                .builder()
                .claims()
                // add custom claims such as roles
                .add(extraClaims)
                // set subject for the token (can be used as identification)
                .subject(userDetails.getUsername())
                // created at what time
                .issuedAt(new Date(System.currentTimeMillis()))
                // expire in 1 day
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 24))
                .and()
                // define hashing strategy
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();
    }

    // if the token is valid then the user is authenticated (but not yet authorized)
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // extract claims from the token
    public Claims extractAllClaims(String token) {
        return Jwts
                // parse into 3 parts
                .parser()
                // verify whether the token has been changed
                .verifyWith(getSignInKey())
                .build()
                // extract the claims
                .parseSignedClaims(token)
                // get payload, needed for authentication and authorization
                .getPayload();
    }

    // create key from SECRET
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}