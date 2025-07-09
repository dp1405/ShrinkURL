package org.stir.shrinkurl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.stir.shrinkurl.dto.UpdateProfileRequest;
import org.stir.shrinkurl.dto.UserDTO;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.service.UserService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/user")
@Slf4j
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public String profile(Model model, @AuthenticationPrincipal User user, Authentication authentication) {
        UserDTO userDTO = userService.getUserWithSubscription(user.getId());
        model.addAttribute("user", userDTO);
        model.addAttribute("principal", authentication.getPrincipal());
        model.addAttribute("updateForm", new UpdateProfileRequest());
        return "user/profile";
    }
    
    @PostMapping("/profile/update")
    @PreAuthorize("isAuthenticated()")
    public String updateProfile(@Valid @ModelAttribute("updateForm") UpdateProfileRequest request,
                              BindingResult result,
                              @AuthenticationPrincipal User user,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            UserDTO userDTO = userService.getUserWithSubscription(user.getId());
            model.addAttribute("user", userDTO);
            return "user/profile";
        }
        
        try {
            userService.updateUserProfile(user.getId(), request);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
            return "redirect:/user/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update profile");
            return "redirect:/user/profile";
        }
    }
}
