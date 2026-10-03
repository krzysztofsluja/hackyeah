package com.sluja.hackyeah.ui.web;

import com.sluja.hackyeah.ui.DemoBackend;
import com.sluja.hackyeah.ui.view.HospitalView;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/** Model attributes shared by every page, e.g. the hospital list for the role switcher. */
@ControllerAdvice(basePackageClasses = DemoModelAdvice.class)
public class DemoModelAdvice {

    private final DemoBackend backend;

    public DemoModelAdvice(DemoBackend backend) {
        this.backend = backend;
    }

    @ModelAttribute("allHospitals")
    public List<HospitalView> allHospitals() {
        return backend.hospitals();
    }
}
