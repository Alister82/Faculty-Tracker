package com.college.tracker.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AppUserRepository userRepository;

    @GetMapping
    public String adminDashboard(Model model) {
        List<AppUser> allUsers = userRepository.findAll();

        // Group users by department (exclude ROLE_ADMIN users)
        Map<String, List<AppUser>> byDepartment = allUsers.stream()
                .filter(u -> u.getRole() != UserRole.ROLE_ADMIN)
                .collect(Collectors.groupingBy(AppUser::getDepartment));

        // Find the current HOD for each department
        Map<String, AppUser> currentHods = new LinkedHashMap<>();
        for (Map.Entry<String, List<AppUser>> entry : byDepartment.entrySet()) {
            entry.getValue().stream()
                    .filter(u -> u.getRole() == UserRole.ROLE_HOD)
                    .findFirst()
                    .ifPresent(hod -> currentHods.put(entry.getKey(), hod));
        }

        model.addAttribute("byDepartment", byDepartment);
        model.addAttribute("currentHods", currentHods);
        return "admin";
    }

    /**
     * Promote a user to HOD for their department.
     * Demotes the existing HOD in that department back to ROLE_FACULTY.
     */
    @PostMapping("/setHod")
    public String setHod(@RequestParam("userId") Long userId) {
        AppUser newHod = userRepository.findById(userId).orElse(null);
        if (newHod == null) return "redirect:/admin?error=UserNotFound";

        String department = newHod.getDepartment();

        // Demote any existing HOD in this department
        List<AppUser> deptUsers = userRepository.findByDepartment(department);
        for (AppUser u : deptUsers) {
            if (u.getRole() == UserRole.ROLE_HOD && !u.getId().equals(userId)) {
                u.setRole(UserRole.ROLE_FACULTY);
                userRepository.save(u);
            }
        }

        // Promote the selected user
        newHod.setRole(UserRole.ROLE_HOD);
        userRepository.save(newHod);

        return "redirect:/admin?success=HodUpdated";
    }

    /**
     * Demote the current HOD of a department back to Faculty.
     */
    @PostMapping("/removeHod")
    public String removeHod(@RequestParam("userId") Long userId) {
        userRepository.findById(userId).ifPresent(u -> {
            if (u.getRole() == UserRole.ROLE_HOD) {
                u.setRole(UserRole.ROLE_FACULTY);
                userRepository.save(u);
            }
        });
        return "redirect:/admin?success=HodRemoved";
    }
}
