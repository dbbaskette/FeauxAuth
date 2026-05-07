package com.baskettecase.feauxauth.controller;

import com.baskettecase.feauxauth.model.DeviceCode;
import com.baskettecase.feauxauth.model.OAuthClient;
import com.baskettecase.feauxauth.model.OAuthUser;
import com.baskettecase.feauxauth.service.ClientService;
import com.baskettecase.feauxauth.service.DeviceCodeService;
import com.baskettecase.feauxauth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class DeviceVerificationController {

    private final DeviceCodeService deviceCodeService;
    private final UserService userService;
    private final ClientService clientService;

    @GetMapping("/device")
    public String getDevicePage(
            @RequestParam(value = "user_code", required = false) String userCode,
            Model model) {
        model.addAttribute("userCode", userCode == null ? "" : userCode);
        return "oauth/device";
    }

    @PostMapping("/device")
    public String submitDevice(
            @RequestParam("user_code") String userCode,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "action", required = false) String action,
            Model model) {

        Optional<DeviceCode> dcOpt = deviceCodeService.findByUserCode(userCode);
        if (dcOpt.isEmpty()) {
            model.addAttribute("userCode", userCode);
            model.addAttribute("loginError", "Unknown code. Check the code on your device and try again.");
            return "oauth/device";
        }
        DeviceCode dc = dcOpt.get();

        if (dc.getExpiresAt().isBefore(LocalDateTime.now())) {
            model.addAttribute("userCode", userCode);
            model.addAttribute("loginError", "This code has expired. Return to your device and request a new code.");
            return "oauth/device";
        }
        if (!DeviceCode.STATUS_PENDING.equals(dc.getStatus())) {
            model.addAttribute("userCode", userCode);
            model.addAttribute("loginError", "This code has already been used.");
            return "oauth/device";
        }

        if ("deny".equals(action)) {
            deviceCodeService.deny(dc);
            model.addAttribute("denied", true);
            return "oauth/device-success";
        }

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            model.addAttribute("userCode", userCode);
            model.addAttribute("loginError", "Email and password are required.");
            return "oauth/device";
        }

        Optional<OAuthUser> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty() || !userOpt.get().isEnabled() || !userService.verifyPassword(userOpt.get(), password)) {
            model.addAttribute("userCode", userCode);
            model.addAttribute("loginError", "Invalid email or password.");
            return "oauth/device";
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
}
