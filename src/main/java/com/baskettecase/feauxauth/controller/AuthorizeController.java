package com.baskettecase.feauxauth.controller;

import com.baskettecase.feauxauth.model.OAuthClient;
import com.baskettecase.feauxauth.model.OAuthUser;
import com.baskettecase.feauxauth.service.AuthCodeService;
import com.baskettecase.feauxauth.service.ClientService;
import com.baskettecase.feauxauth.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class AuthorizeController {

    private final ClientService clientService;
    private final UserService userService;
    private final AuthCodeService authCodeService;

    @GetMapping("/oauth/authorize")
    public String authorize(
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("response_type") String responseType,
            @RequestParam(value = "scope", defaultValue = "openid") String scope,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "code_challenge", required = false) String codeChallenge,
            @RequestParam(value = "code_challenge_method", required = false) String codeChallengeMethod,
            @RequestParam(value = "nonce", required = false) String nonce,
            HttpSession session,
            Model model) {

        if (!"code".equals(responseType)) {
            model.addAttribute("error", "unsupported_response_type");
            model.addAttribute("errorDescription", "Only response_type=code is supported");
            return "oauth/error";
        }

        Optional<OAuthClient> clientOpt = clientService.findByClientId(clientId);
        if (clientOpt.isEmpty() || !clientOpt.get().isEnabled()) {
            model.addAttribute("error", "invalid_client");
            model.addAttribute("errorDescription", "Unknown or disabled client");
            return "oauth/error";
        }

        OAuthClient client = clientOpt.get();

        boolean validRedirect = Arrays.stream(client.getRedirectUris().split("\\n"))
                .map(String::trim)
                .anyMatch(uri -> uri.equals(redirectUri));
        if (!validRedirect) {
            model.addAttribute("error", "invalid_redirect_uri");
            model.addAttribute("errorDescription", "Redirect URI not registered for this client");
            return "oauth/error";
        }

        if (client.isRequirePkce() && (codeChallenge == null || codeChallenge.isBlank())) {
            model.addAttribute("error", "invalid_request");
            model.addAttribute("errorDescription", "This client requires PKCE (code_challenge)");
            return "oauth/error";
        }

        session.setAttribute("auth_client_id", clientId);
        session.setAttribute("auth_redirect_uri", redirectUri);
        session.setAttribute("auth_scope", scope);
        session.setAttribute("auth_state", state);
        session.setAttribute("auth_code_challenge", codeChallenge);
        session.setAttribute("auth_nonce", nonce);

        model.addAttribute("clientName", client.getName());
        model.addAttribute("scope", scope);
        return "oauth/login";
    }

    @PostMapping("/oauth/authorize")
    public String login(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            HttpSession session,
            Model model) {

        String clientId = (String) session.getAttribute("auth_client_id");
        String redirectUri = (String) session.getAttribute("auth_redirect_uri");
        String scope = (String) session.getAttribute("auth_scope");
        String state = (String) session.getAttribute("auth_state");
        String codeChallenge = (String) session.getAttribute("auth_code_challenge");

        if (clientId == null || redirectUri == null) {
            model.addAttribute("error", "invalid_request");
            model.addAttribute("errorDescription", "Session expired. Please start the login flow again.");
            return "oauth/error";
        }

        Optional<OAuthUser> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty() || !userOpt.get().isEnabled() || !userService.verifyPassword(userOpt.get(), password)) {
            model.addAttribute("clientName", clientId);
            model.addAttribute("scope", scope);
            model.addAttribute("loginError", "Invalid email or password");
            return "oauth/login";
        }

        OAuthUser user = userOpt.get();
        userService.recordLogin(user);

        String code = authCodeService.generateCode(clientId, user.getId(), redirectUri, scope, codeChallenge);

        session.removeAttribute("auth_client_id");
        session.removeAttribute("auth_redirect_uri");
        session.removeAttribute("auth_scope");
        session.removeAttribute("auth_state");
        session.removeAttribute("auth_code_challenge");
        session.removeAttribute("auth_nonce");

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", code);
        if (state != null) {
            builder.queryParam("state", state);
        }

        return "redirect:" + builder.toUriString();
    }
}
