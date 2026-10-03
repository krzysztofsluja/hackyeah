package com.sluja.hackyeah.ui.web;

import com.sluja.hackyeah.ui.DemoBackend;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

@Controller
public class RoleController {

    private final DemoBackend backend;

    public RoleController(DemoBackend backend) {
        this.backend = backend;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/doctor";
    }

    @PostMapping("/demo/reset")
    public String reset(@RequestHeader(value = "Referer", required = false) String referer,
                        RedirectAttributes redirectAttributes) {
        backend.reset();
        redirectAttributes.addFlashAttribute("flash", "Demo zresetowane do stanu startowego.");
        return "redirect:" + localPath(referer);
    }

    /** Only the path of the Referer is reused, so the redirect never leaves the app. */
    private static String localPath(String referer) {
        if (referer == null) {
            return "/";
        }
        try {
            String path = URI.create(referer).getPath();
            return path == null || path.isBlank() ? "/" : path;
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }
}
