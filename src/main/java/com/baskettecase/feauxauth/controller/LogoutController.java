package com.baskettecase.feauxauth.controller;

import com.baskettecase.feauxauth.model.OAuthUser;
import com.baskettecase.feauxauth.service.TokenService;
import com.baskettecase.feauxauth.service.UserService;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class LogoutController {

    private final TokenService tokenService;
    private final UserService userService;

    @GetMapping("/oauth/logout")
    public String logout(
            @RequestParam(value = "id_token_hint", required = false) String idTokenHint,
            @RequestParam(value = "post_logout_redirect_uri", required = false) String postLogoutRedirectUri,
            @RequestParam(value = "state", required = false) String state,
            HttpSession session,
            Model model) {

        // Revoke tokens for the user identified by the id_token_hint
        if (idTokenHint != null) {
            try {
                SignedJWT jwt = SignedJWT.parse(idTokenHint);
                JWTClaimsSet claims = jwt.getJWTClaimsSet();
                String sub = claims.getSubject();
                Optional<OAuthUser> userOpt = userService.findByEmail(sub);
                userOpt.ifPresent(user -> tokenService.revokeAllForUser(user.getId()));
            } catch (Exception e) {
                log.debug("Failed to parse id_token_hint during logout", e);
            }
        }

        session.invalidate();

        if (postLogoutRedirectUri != null && !postLogoutRedirectUri.isBlank()) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(postLogoutRedirectUri);
            if (state != null) {
                builder.queryParam("state", state);
            }
            return "redirect:" + builder.toUriString();
        }

        return "oauth/logout";
    }
}
