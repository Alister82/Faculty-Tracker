package com.college.tracker.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RegistrationController {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("department") String department,
            @RequestParam(value = "roleType", defaultValue = "FACULTY") String roleType,
            Model model) {

        if (userRepository.existsByUsername(username)) {
            model.addAttribute("error", "Username is already taken.");
            return "register";
        }

        AppUser newUser = new AppUser();
        newUser.setUsername(username);
        newUser.setEmail(email != null && !email.trim().isEmpty() ? email.trim() : null);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setDepartment(department);
        
        if ("HOD".equalsIgnoreCase(roleType)) {
            newUser.setRole(UserRole.ROLE_HOD);
        } else {
            newUser.setRole(UserRole.ROLE_FACULTY);
        }
        
        newUser.setMustChangePassword(false); // They just set it

        try {
            userRepository.save(newUser);
            return "redirect:/login?registered";
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred during registration. Email must be unique.");
            return "register";
        }
    }
}
