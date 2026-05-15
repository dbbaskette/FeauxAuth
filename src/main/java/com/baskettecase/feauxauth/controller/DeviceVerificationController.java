package com.baskettecase.feauxauth.controller;

import com.baskettecase.feauxauth.model.DeviceCode;
import com.baskettecase.feauxauth.model.OAuthClient;
import com.baskettecase.feauxauth.model.OAuthUser;
import com.baskettecase.feauxauth.oauth.ScopeRisk;
import com.baskettecase.feauxauth.service.ClientService;
import com.baskettecase.feauxauth.service.DeviceCodeService;
import com.baskettecase.feauxauth.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class DeviceVerificationController {

    private static final String SESSION_USER_CODE = "device_user_code";
    private static final String SESSION_USER_ID = "device_user_id";

    private final DeviceCodeService deviceCodeService;
    private final UserService userService;
    private final ClientService clientService;

    // ---------- Step 1: enter device code ----------

    @GetMapping("/device")
    public String getDevicePage(
            @RequestParam(value = "user_code", required = false) String userCode,
            HttpSession session,
            Model model) {
        session.removeAttribute(SESSION_USER_CODE);
        session.removeAttribute(SESSION_USER_ID);
        model.addAttribute("userCode", userCode == null ? "" : userCode);
        return "oauth/device-code";
    }

    /**
     * The /device POST handler accepts three shapes:
     *  1. user_code only (or action=deny) — the new step-1 flow
     *  2. user_code + email + password [+ action] — the legacy single-page flow,
     *     kept for backwards compatibility with existing CLI samples and tests
     *  3. user_code + action=deny — denial at any step
     */
    @PostMapping("/device")
    public String submitDevice(
            @RequestParam("user_code") String userCode,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "action", required = false) String action,
            HttpSession session,
            Model model) {

        Optional<DeviceCode> dcOpt = lookupValidCode(userCode);
        if (dcOpt.isEmpty()) {
            return renderCodeError(userCode, model, codeErrorReason(userCode));
        }
        DeviceCode dc = dcOpt.get();

        if ("deny".equals(action)) {
            deviceCodeService.deny(dc);
            model.addAttribute("denied", true);
            return "oauth/device-success";
        }

        boolean hasCredentials = email != null && !email.isBlank() && password != null && !password.isBlank();
        if (!hasCredentials) {
            // Step-1 verification only — stash the code and move to step 2.
            session.setAttribute(SESSION_USER_CODE, dc.getUserCode());
            return "redirect:/device/sign-in";
        }

        // Legacy single-page flow — handle credentials inline.
        Optional<OAuthUser> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty() || !userOpt.get().isEnabled() || !userService.verifyPassword(userOpt.get(), password)) {
            model.addAttribute("userCode", userCode);
            model.addAttribute("loginError", "Invalid email or password.");
            return "oauth/device-code";
        }

        OAuthUser user = userOpt.get();
        userService.recordLogin(user);
        deviceCodeService.approve(dc, user.getId());

        Optional<OAuthClient> clientOpt = clientService.findByClientId(dc.getClientId());
        model.addAttribute("clientName", clientOpt.map(OAuthClient::getName).orElse(dc.getClientId()));
        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("denied", false);
        return "oauth/device-success";
    }

    // ---------- Step 2: sign in ----------

    @GetMapping("/device/sign-in")
    public String getSignIn(HttpSession session, Model model) {
        String userCode = (String) session.getAttribute(SESSION_USER_CODE);
        if (userCode == null) return "redirect:/device";

        Optional<DeviceCode> dcOpt = lookupValidCode(userCode);
        if (dcOpt.isEmpty()) {
            session.removeAttribute(SESSION_USER_CODE);
            return "redirect:/device?error=expired";
        }
        DeviceCode dc = dcOpt.get();
        Optional<OAuthClient> clientOpt = clientService.findByClientId(dc.getClientId());
        model.addAttribute("clientName", clientOpt.map(OAuthClient::getName).orElse(dc.getClientId()));
        model.addAttribute("formattedUserCode", DeviceCodeService.formatUserCode(dc.getUserCode()));
        return "oauth/device-signin";
    }

    @PostMapping("/device/sign-in")
    public String postSignIn(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam(value = "action", required = false) String action,
            HttpSession session,
            Model model) {

        String userCode = (String) session.getAttribute(SESSION_USER_CODE);
        if (userCode == null) return "redirect:/device";

        Optional<DeviceCode> dcOpt = lookupValidCode(userCode);
        if (dcOpt.isEmpty()) {
            session.removeAttribute(SESSION_USER_CODE);
            return "redirect:/device?error=expired";
        }
        DeviceCode dc = dcOpt.get();

        if ("deny".equals(action)) {
            deviceCodeService.deny(dc);
            session.removeAttribute(SESSION_USER_CODE);
            model.addAttribute("denied", true);
            return "oauth/device-success";
        }

        Optional<OAuthUser> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty() || !userOpt.get().isEnabled() || !userService.verifyPassword(userOpt.get(), password)) {
            Optional<OAuthClient> clientOpt = clientService.findByClientId(dc.getClientId());
            model.addAttribute("clientName", clientOpt.map(OAuthClient::getName).orElse(dc.getClientId()));
            model.addAttribute("formattedUserCode", DeviceCodeService.formatUserCode(dc.getUserCode()));
            model.addAttribute("loginError", "Invalid email or password.");
            return "oauth/device-signin";
        }

        OAuthUser user = userOpt.get();
        userService.recordLogin(user);
        session.setAttribute(SESSION_USER_ID, user.getId().toString());
        return "redirect:/device/authorize";
    }

    // ---------- Step 3: approve / deny ----------

    @GetMapping("/device/authorize")
    public String getAuthorize(HttpSession session, Model model) {
        String userCode = (String) session.getAttribute(SESSION_USER_CODE);
        String userIdStr = (String) session.getAttribute(SESSION_USER_ID);
        if (userCode == null) return "redirect:/device";
        if (userIdStr == null) return "redirect:/device/sign-in";

        Optional<DeviceCode> dcOpt = lookupValidCode(userCode);
        if (dcOpt.isEmpty()) {
            session.removeAttribute(SESSION_USER_CODE);
            session.removeAttribute(SESSION_USER_ID);
            return "redirect:/device?error=expired";
        }
        DeviceCode dc = dcOpt.get();
        Optional<OAuthClient> clientOpt = clientService.findByClientId(dc.getClientId());
        Optional<OAuthUser> userOpt = userService.findById(java.util.UUID.fromString(userIdStr));

        model.addAttribute("clientName", clientOpt.map(OAuthClient::getName).orElse(dc.getClientId()));
        model.addAttribute("clientInitial", clientInitial(clientOpt.map(OAuthClient::getName).orElse(dc.getClientId())));
        model.addAttribute("userEmail", userOpt.map(OAuthUser::getEmail).orElse(null));
        model.addAttribute("scopeItems", buildScopeItems(dc.getScope()));
        return "oauth/device-consent";
    }

    @PostMapping("/device/authorize")
    public String postAuthorize(
            @RequestParam("approve") String approve,
            HttpSession session,
            Model model) {

        String userCode = (String) session.getAttribute(SESSION_USER_CODE);
        String userIdStr = (String) session.getAttribute(SESSION_USER_ID);
        if (userCode == null || userIdStr == null) return "redirect:/device";

        Optional<DeviceCode> dcOpt = lookupValidCode(userCode);
        if (dcOpt.isEmpty()) {
            session.removeAttribute(SESSION_USER_CODE);
            session.removeAttribute(SESSION_USER_ID);
            return "redirect:/device?error=expired";
        }
        DeviceCode dc = dcOpt.get();
        java.util.UUID userId = java.util.UUID.fromString(userIdStr);

        if (!"true".equals(approve)) {
            deviceCodeService.deny(dc);
            session.removeAttribute(SESSION_USER_CODE);
            session.removeAttribute(SESSION_USER_ID);
            model.addAttribute("denied", true);
            return "oauth/device-success";
        }

        deviceCodeService.approve(dc, userId);
        Optional<OAuthClient> clientOpt = clientService.findByClientId(dc.getClientId());
        Optional<OAuthUser> userOpt = userService.findById(userId);
        model.addAttribute("clientName", clientOpt.map(OAuthClient::getName).orElse(dc.getClientId()));
        model.addAttribute("userEmail", userOpt.map(OAuthUser::getEmail).orElse(null));
        model.addAttribute("denied", false);
        session.removeAttribute(SESSION_USER_CODE);
        session.removeAttribute(SESSION_USER_ID);
        return "oauth/device-success";
    }

    // ---------- helpers ----------

    private Optional<DeviceCode> lookupValidCode(String userCode) {
        if (userCode == null || userCode.isBlank()) return Optional.empty();
        return deviceCodeService.findByUserCode(userCode)
            .filter(dc -> DeviceCode.STATUS_PENDING.equals(dc.getStatus()))
            .filter(dc -> dc.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    private String codeErrorReason(String userCode) {
        Optional<DeviceCode> raw = deviceCodeService.findByUserCode(userCode);
        if (raw.isEmpty()) return "Unknown code. Check the code on your device and try again.";
        DeviceCode dc = raw.get();
        if (dc.getExpiresAt().isBefore(LocalDateTime.now())) {
            return "This code has expired. Return to your device and request a new code.";
        }
        return "This code has already been used.";
    }

    private String renderCodeError(String userCode, Model model, String reason) {
        model.addAttribute("userCode", userCode);
        model.addAttribute("loginError", reason);
        return "oauth/device-code";
    }

    private List<AuthorizeController.ScopeItem> buildScopeItems(String scope) {
        if (scope == null) return List.of();
        return Arrays.stream(scope.trim().split("\\s+"))
            .filter(s -> !s.isBlank())
            .map(s -> new AuthorizeController.ScopeItem(
                s,
                ScopeRisk.classify(s).cssToken(),
                describeScope(s)))
            .toList();
    }

    private static final java.util.Map<String, String> SCOPE_DESCRIPTIONS = java.util.Map.of(
        "openid", "Verify your identity",
        "profile", "Access your name and display name",
        "email", "Access your email address",
        "offline_access", "Stay signed in with a refresh token"
    );

    private String describeScope(String scope) {
        return SCOPE_DESCRIPTIONS.getOrDefault(scope, "Access: " + scope);
    }

    private String clientInitial(String clientName) {
        if (clientName == null || clientName.isBlank()) return "?";
        return clientName.substring(0, 1).toUpperCase(Locale.ROOT);
    }
}
