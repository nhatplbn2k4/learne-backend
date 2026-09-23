package com.learn.learnE_Backend.admin;

import com.learn.learnE_Backend.admin.dto.AdminUserDto;
import com.learn.learnE_Backend.auth.Role;
import com.learn.learnE_Backend.auth.User;
import com.learn.learnE_Backend.auth.UserStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public List<AdminUserDto> list() {
        return adminUserService.list();
    }

    @PostMapping("/{userId}/approve")
    public AdminUserDto approve(@AuthenticationPrincipal User actor, @PathVariable Long userId) {
        return adminUserService.setStatus(actor, userId, UserStatus.ACTIVE);
    }

    /** Also used to suspend an account that was active before. */
    @PostMapping("/{userId}/reject")
    public AdminUserDto reject(@AuthenticationPrincipal User actor, @PathVariable Long userId) {
        return adminUserService.setStatus(actor, userId, UserStatus.REJECTED);
    }

    @PutMapping("/{userId}/role")
    public AdminUserDto setRole(
            @AuthenticationPrincipal User actor,
            @PathVariable Long userId,
            @RequestParam Role role
    ) {
        return adminUserService.setRole(actor, userId, role);
    }

    @DeleteMapping("/{userId}")
    public void delete(@AuthenticationPrincipal User actor, @PathVariable Long userId) {
        adminUserService.delete(actor, userId);
    }
}
