package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Controller
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ComplaintService complaintService;

    @GetMapping("/student/register")
    public String showRegistrationForm() {
        return "register";
    }

    @PostMapping("/student/register")
    public String registerUser(@RequestParam("enrollmentNumber") String enrollmentNumber,
            @RequestParam("studentName") String studentName,
            @RequestParam("email") String email,
            @RequestParam("department") String department,
            @RequestParam("course") String course,
            @RequestParam("password") String password,
            @RequestParam("photo") MultipartFile photo,
            Model model) {

        // Handle the photo upload
        String photoPath = null;
        try {
            if (!photo.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + photo.getOriginalFilename(); // Generate a unique
                                                                                                  // filename
                photoPath = fileName; // store the file name
                Path path = Paths.get("src/main/resources/static/user_profile/" + fileName);
                Files.createDirectories(path.getParent());
                Files.copy(photo.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            model.addAttribute("error", "Failed to upload photo: " + e.getMessage());
            return "register";
        }

        Student student = new Student();
        student.setEnrollmentNumber(enrollmentNumber);
        student.setStudentName(studentName);
        student.setEmail(email);
        student.setDepartment(department);
        student.setCourse(course);
        student.setPassword(passwordEncoder.encode(password));
        student.setPhotoPath(photoPath); // Only store the filename here

        studentService.registerUser(student);
        return "redirect:/student/login";
    }

    @GetMapping("/student/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid email or password. Please try again.");
        }
        return "login";
    }

    @GetMapping("/student/dashboard")
    public String showUserProfile(Model model, Authentication authentication) {
        String email = authentication.getName(); // Get the logged-in user's email
        Student student = studentService.findStudentByEmail(email); // Fetch student details from DB

        String profileImageUrl = student.getPhotoPath() != null ? "/user_profile/" + student.getPhotoPath()
                : "/images/user-profile.jpg";

        model.addAttribute("student", student);
        model.addAttribute("profileImageUrl", profileImageUrl);

        return "dashboard";
    }

    @GetMapping("/complaint")
    public String showComplaintForm(Model model, Authentication authentication) {
        String email = authentication.getName(); // Get logged-in user's email
        Student student = studentService.findStudentByEmail(email); // Fetch student details from DB

        String profileImageUrl = student.getPhotoPath() != null ? "/user_profile/" + student.getPhotoPath()
                : "/images/user-profile.jpg";

        model.addAttribute("student", student);
        model.addAttribute("profileImageUrl", profileImageUrl);
        model.addAttribute("student", student);
        return "complaint";
    }

    @PostMapping("/registerComplaint")
    public String registerComplaint(@RequestParam("enrollmentNumber") String enrollmentNumber,
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("course") String course,
            @RequestParam("department") String department,
            @RequestParam("complaintType") String complaintType,
            @RequestParam("description") String description,
            Model model) {
        try {
            // Create and save the Complaint
            Complaint complaint = new Complaint();
            complaint.setEnrollmentNumber(enrollmentNumber);
            complaint.setName(name);
            complaint.setEmail(email);
            complaint.setCourse(course);
            complaint.setDepartment(department);
            complaint.setComplaintType(complaintType);
            complaint.setDescription(description);

            complaintService.saveComplaint(complaint);

            model.addAttribute("message", "Complaint registered successfully.");
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred while registering the complaint: " + e.getMessage());
            return "student";
        }

        return "redirect:/student/dashboard";
    }

    @GetMapping("/student/allComplaints")
    public String viewAllComplaints(Model model, Authentication authentication) {
        List<Complaint> allComplaints = complaintService.getAllComplaints();
        model.addAttribute("complaints", allComplaints);

        return "all_complaints";
    }

    @GetMapping("/student/myComplaints")
    public String viewMyComplaints(Model model, Authentication authentication) {
        String email = authentication.getName();
        Student student = studentService.findStudentByEmail(email); // Fetch student details from DB
        List<Complaint> myComplaints = complaintService.getComplaintsByEmail(email);

        String profileImageUrl = student.getPhotoPath() != null ? "/user_profile/" + student.getPhotoPath()
                : "/images/user-profile.jpg";

        model.addAttribute("student", student);
        model.addAttribute("profileImageUrl", profileImageUrl);
        model.addAttribute("complaints", myComplaints);

        return "my_complaints";
    }

    @GetMapping("/student/student_profile")
    public String showStudentProfile(Model model, Authentication authentication) {
        if (authentication == null) {
            return "redirect:/student/login";
        }

        String email = authentication.getName(); // Get logged-in user's email
        Student student = studentService.findStudentByEmail(email); // Fetch student details from DB

        String profileImageUrl = student.getPhotoPath() != null ? "/user_profile/" + student.getPhotoPath()
                : "/images/user-profile.jpg";

        model.addAttribute("student", student);
        model.addAttribute("profileImageUrl", profileImageUrl);

        return "student_profile";
    }

    @PostMapping("/student/updatePassword")
    public String updatePassword(@RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model,
            Authentication authentication) {

        String email = authentication.getName();
        Student student = studentService.findStudentByEmail(email);

        if (!passwordEncoder.matches(currentPassword, student.getPassword())) {
            model.addAttribute("error", "Current password is incorrect");
            return "student_profile";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "New password and confirmation do not match");
            return "student_profile";
        }

        student.setPassword(passwordEncoder.encode(newPassword));
        studentService.updateStudent(student);

        model.addAttribute("message", "Password updated successfully");
        return "redirect:/student/login";
    }
}
