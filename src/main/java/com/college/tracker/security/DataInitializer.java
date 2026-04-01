package com.college.tracker.security;

import com.college.tracker.Activity;
import com.college.tracker.ActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ─── Name of the existing faculty who is the Computer dept HOD ───────────
    private static final String COMPUTER_HOD_NAME = "Gaurang Patkar";

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // 0. Migrate 'role' column from ENUM to VARCHAR if needed (so ROLE_ADMIN fits)
        try {
            jdbcTemplate.execute(
                "ALTER TABLE app_user MODIFY COLUMN role VARCHAR(20) NOT NULL"
            );
            System.out.println("✅ Migrated app_user.role column to VARCHAR(20).");
        } catch (Exception e) {
            // Column is already VARCHAR — safe to ignore
            System.out.println("ℹ️ app_user.role column already VARCHAR, no migration needed.");
        }

        // 1. Seed the Principal/Admin account
        if (!userRepository.existsByUsername("admin")) {
            AppUser admin = new AppUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(UserRole.ROLE_ADMIN);
            admin.setDepartment("Administration");
            admin.setMustChangePassword(false);
            userRepository.save(admin);
            System.out.println("✅ Seeded Admin/Principal account: admin / admin123");
        }

        // 2. Read all activities and tag existing ones with Computer department
        List<Activity> allActivities = activityRepository.findAll();

        // 3. Create AppUser accounts for every unique faculty name in the DB
        List<String> uniqueFacultyNames = allActivities.stream()
                .map(Activity::getFacultyName)
                .filter(name -> name != null && !name.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());

        for (String facultyName : uniqueFacultyNames) {
            String username = facultyName.trim();
            if (!userRepository.existsByUsername(username)) {
                UserRole role = COMPUTER_HOD_NAME.equalsIgnoreCase(username)
                        ? UserRole.ROLE_HOD : UserRole.ROLE_FACULTY;

                AppUser facultyUser = new AppUser();
                facultyUser.setUsername(username);
                facultyUser.setPassword(passwordEncoder.encode("Welcome@123"));
                facultyUser.setRole(role);
                facultyUser.setDepartment("Computer");
                facultyUser.setMustChangePassword(true);
                userRepository.save(facultyUser);

                System.out.println("✅ Auto-created " + (role == UserRole.ROLE_HOD ? "HOD" : "faculty") + " account for: " + username);
            } else {
                // If account already exists for Gaurang Patkar but is still FACULTY, promote them
                if (COMPUTER_HOD_NAME.equalsIgnoreCase(username)) {
                    userRepository.findByUsername(username).ifPresent(u -> {
                        if (u.getRole() != UserRole.ROLE_HOD) {
                            u.setRole(UserRole.ROLE_HOD);
                            userRepository.save(u);
                            System.out.println("✅ Promoted '" + COMPUTER_HOD_NAME + "' to HOD.");
                        }
                    });
                }
            }
        }

        // 4. Tag existing activity rows with department and link username
        int updatedCount = 0;
        for (Activity activity : allActivities) {
            boolean changed = false;
            if (activity.getDepartment() == null || activity.getDepartment().trim().isEmpty()) {
                activity.setDepartment("Computer");
                changed = true;
            }
            if (activity.getCreatedByUsername() == null || activity.getCreatedByUsername().trim().isEmpty()) {
                if (activity.getFacultyName() != null) {
                    activity.setCreatedByUsername(activity.getFacultyName().trim());
                    changed = true;
                }
            }
            if (changed) {
                activityRepository.save(activity);
                updatedCount++;
            }
        }

        if (updatedCount > 0) {
            System.out.println("✅ Tagged " + updatedCount + " existing records with 'Computer' department.");
        }
    }
}
