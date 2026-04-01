package com.college.tracker.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProfileController {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    public String showProfile(Authentication authentication, Model model) {
        String username = authentication.getName();
        AppUser user = userRepository.findByUsername(username).orElse(null);
        
        if (user != null) {
            model.addAttribute("user", user);
        }
        
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            Authentication authentication,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "password", required = false) String password,
            Model model) {

        String username = authentication.getName();
        AppUser user = userRepository.findByUsername(username).orElse(null);

        if (user != null) {
            if (email != null && !email.trim().isEmpty()) {
                user.setEmail(email.trim());
            }

            if (password != null && !password.trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(password));
                user.setMustChangePassword(false); // Reset the flag!
            }
            
            try {
                userRepository.save(user);
                return "redirect:/profile?success";
            } catch (Exception e) {
                model.addAttribute("user", user);
                model.addAttribute("error", "Email update failed. It might be taken by another user.");
                return "profile";
            }
        }
        return "redirect:/login";
    }
}
