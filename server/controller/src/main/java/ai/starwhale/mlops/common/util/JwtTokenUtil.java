/*
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.starwhale.mlops.common.util;

import static java.lang.String.format;

import ai.starwhale.mlops.configuration.security.JwtProperties;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import java.util.Date;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenUtil {

    private final JwtProperties jwtProperties;

    public String generateAccessToken(User user) {
        return generateAccessToken(user, jwtProperties.getExpireMinutes() * 60 * 1000, Map.of()); // 1 week
    }


    public String generateAccessToken(User user, Map<String, Object> claims) {
        return generateAccessToken(user, jwtProperties.getExpireMinutes() * 60 * 1000, claims);
    }

    public String generateAccessToken(User user, Long expireMilliSeconds, Map<String, Object> claims) {
        JwtBuilder jwtBuilder = Jwts.builder()
                .setSubject(format("%s,%s", user.getId(), user.getUsername()))
                .addClaims(claims)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(new Date())
                .signWith(SignatureAlgorithm.HS512, jwtProperties.getSecret());
        if (null != expireMilliSeconds) {
            jwtBuilder.setExpiration(new Date(System.currentTimeMillis()
                    + expireMilliSeconds));
        }
        return jwtBuilder.compact();
    }

    // Sample method to validate and read the JWT
    public Claims parseJwt(String token) {
        // This line will throw an exception if it is not a signed JWS (as expected)
        try {
            return Jwts.parser().setSigningKey(jwtProperties.getSecret()).parseClaimsJws(token).getBody();
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature - {}", ex.getMessage());
            throw new SwValidationException(ValidSubject.USER);
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token - {}", ex.getMessage());
            throw new SwValidationException(ValidSubject.USER);
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token - {}", ex.getMessage());
            throw new SwValidationException(ValidSubject.USER);
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token - {}", ex.getMessage());
            throw new SwValidationException(ValidSubject.USER);
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty - {}", ex.getMessage());
            throw new SwValidationException(ValidSubject.USER);
        }
    }

    public String getUserId(Claims claims) {
        return claims.getSubject().split(",")[0];
    }

    public String getUsername(Claims claims) {
        return claims.getSubject().split(",")[1];
    }

    public Date getExpirationDate(Claims claims) {
        return claims.getExpiration();
    }

}

