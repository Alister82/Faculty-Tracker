package com.college.tracker.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import java.util.Collection;

public class AppUserDetails extends User {
    
    private final String department;

    public AppUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities, String department) {
        super(username, password, authorities);
        this.department = department;
    }

    public String getDepartment() {
        return department;
    }
}
