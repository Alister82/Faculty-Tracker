package com.college.tracker;

import com.college.tracker.security.*;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ActivityController {

    @Autowired
    private GoogleDriveService googleDriveService;

    @Autowired
    private ActivityRepository repository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private AiExtractService aiExtractService;

    // ─── Login page ───────────────────────────────────────────────────────────
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // ─── Home / Search ────────────────────────────────────────────────────────
    @GetMapping("/")
    public String viewHomePage(Model model,
            Authentication authentication,
            @RequestParam(value = "searchType", required = false) String searchType,
            @RequestParam(value = "typeFilter", required = false) String typeFilter,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "advFaculty", required = false) String advFaculty,
            @RequestParam(value = "advYear", required = false) String advYear,
            @RequestParam(value = "advNittt", required = false) Boolean advNittt,
            @RequestParam(value = "advTypes", required = false) List<String> advTypes) {

        String username = authentication.getName();
        boolean isHod = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        AppUser currentUser = userRepository.findByUsername(username).orElse(null);
        String department = (currentUser != null) ? currentUser.getDepartment() : null;

        // Pass user info to view
        model.addAttribute("currentUsername", username);
        model.addAttribute("isHod", isHod || isAdmin); // Admin can also approve
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("currentDepartment", isAdmin ? "All Departments" : department);

        // All activities, filtered to scope
        List<Activity> scopedActivities = getScopedActivities(username, isHod, isAdmin, department);

        boolean isSearch = false;
        List<Activity> searchResults = null;

        if ("method1".equals(searchType)) {
            String k = (keyword != null) ? keyword : "";
            searchResults = repository.searchMethodOne(typeFilter, k).stream()
                    .filter(a -> hasAccess(a, username, isHod || isAdmin, department, isAdmin))
                    .collect(Collectors.toList());
            isSearch = true;
        } else if ("method2".equals(searchType)) {
            String f = (advFaculty != null) ? advFaculty : "";
            String y = (advYear != null) ? advYear : "";
            Boolean n = (advNittt != null && advNittt) ? true : null;
            List<Activity> raw;
            if (advTypes != null && !advTypes.isEmpty()) {
                raw = repository.searchMethodTwoFiltered(advTypes, f, y, n);
            } else {
                raw = repository.searchMethodTwo(f, y, n);
            }
            searchResults = raw.stream()
                    .filter(a -> hasAccess(a, username, isHod || isAdmin, department, isAdmin))
                    .collect(Collectors.toList());
            isSearch = true;
        }

        model.addAttribute("fdpList", scopedActivities.stream().filter(a -> "FDP".equals(a.getActivityType())).collect(Collectors.toList()));
        model.addAttribute("workshopList", scopedActivities.stream().filter(a -> "Workshop".equals(a.getActivityType())).collect(Collectors.toList()));
        model.addAttribute("conferenceList", scopedActivities.stream().filter(a -> "Conference".equals(a.getActivityType())).collect(Collectors.toList()));
        model.addAttribute("publicationList", scopedActivities.stream().filter(a -> "Publication".equals(a.getActivityType())).collect(Collectors.toList()));
        model.addAttribute("newActivity", new Activity());
        model.addAttribute("isSearch", isSearch);
        model.addAttribute("searchResults", searchResults);
        model.addAttribute("searchType", searchType);
        model.addAttribute("typeFilter", typeFilter);
        model.addAttribute("keyword", keyword);
        model.addAttribute("advFaculty", advFaculty);
        model.addAttribute("advYear", advYear);
        model.addAttribute("advNittt", advNittt);
        model.addAttribute("advTypes", advTypes);
        model.addAttribute("approvalStatuses", ApprovalStatus.values());

        return "index";
    }

    private List<Activity> getScopedActivities(String username, boolean isHod, boolean isAdmin, String department) {
        List<Activity> all = repository.findAll();
        if (isAdmin) {
            // Admin sees everything
            return all;
        } else if (isHod) {
            // HOD sees all records in their department
            return all.stream()
                    .filter(a -> department != null && department.equals(a.getDepartment()))
                    .collect(Collectors.toList());
        } else {
            // Faculty sees only their own records
            return all.stream()
                    .filter(a -> username.equals(a.getCreatedByUsername()))
                    .collect(Collectors.toList());
        }
    }

    private boolean hasAccess(Activity a, String username, boolean isHodOrAdmin, String department, boolean isAdmin) {
        if (isAdmin) return true;
        if (isHodOrAdmin) {
            return department != null && department.equals(a.getDepartment());
        }
        return username.equals(a.getCreatedByUsername());
    }

    // ─── Save (Create + Update) ────────────────────────────────────────────────
    @PostMapping("/save")
    public String saveActivity(Authentication authentication,
            @ModelAttribute("newActivity") Activity activity,
            @RequestParam(value = "certificateFile", required = false) MultipartFile certificateFile) {

        String username = authentication.getName();
        AppUser currentUser = userRepository.findByUsername(username).orElse(null);

        if (activity.getId() != null) {
            // EDIT: preserve existing status, department, certificate, and facultyName
            repository.findById(activity.getId()).ifPresent(existing -> {
                activity.setStatus(existing.getStatus());
                activity.setCreatedByUsername(existing.getCreatedByUsername());
                activity.setDepartment(existing.getDepartment());
                activity.setFacultyName(existing.getFacultyName()); // preserve
                if (certificateFile == null || certificateFile.isEmpty()) {
                    activity.setCertificateUrl(existing.getCertificateUrl());
                }
            });
        } else {
            // NEW RECORD: set pending status and link to current user profile
            activity.setStatus(ApprovalStatus.PENDING);
            activity.setCreatedByUsername(username);
            
            // Only auto-fill faculty name if they are a regular faculty (i.e. if the field was hidden and submitted empty)
            if (activity.getFacultyName() == null || activity.getFacultyName().trim().isEmpty()) {
                activity.setFacultyName(username); 
            }
            
            if (currentUser != null) {
                activity.setDepartment(currentUser.getDepartment());
            }
        }

        if (certificateFile != null && !certificateFile.isEmpty()) {
            try {
                String driveUrl = googleDriveService.uploadFile(certificateFile);
                activity.setCertificateUrl(driveUrl);
                // Upload sets status back to pending for review
                if (activity.getId() != null) {
                    activity.setStatus(ApprovalStatus.PENDING);
                }
            } catch (Exception e) {
                System.out.println("Failed to upload to Google Drive: " + e.getMessage());
            }
        }

        repository.save(activity);
        return "redirect:/";
    }

    // ─── Update Approval Status (HOD only) ─────────────────────────────────────
    @PostMapping("/updateStatus/{id}")
    public String updateStatus(@PathVariable Long id,
            @RequestParam("status") String status,
            Authentication authentication) {

        String username = authentication.getName();
        AppUser user = userRepository.findByUsername(username).orElse(null);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        repository.findById(id).ifPresent(activity -> {
            // Ensure HOD can only update records from their own department
            // Admin can broadly update any record.
            if (isAdmin || (user != null && user.getDepartment() != null && user.getDepartment().equals(activity.getDepartment()))) {
                try {
                    activity.setStatus(ApprovalStatus.valueOf(status));
                    repository.save(activity);
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid status: " + status);
                }
            }
        });

        return "redirect:/";
    }

    @GetMapping("/delete/{id}")
    public String deleteActivity(@PathVariable("id") Long id) {
        repository.deleteById(id);
        return "redirect:/";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model, Authentication authentication) {
        Activity activity = repository.findById(id).orElse(null);
        model.addAttribute("newActivity", activity);

        boolean isHod = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        model.addAttribute("isHod", isHod);
        model.addAttribute("approvalStatuses", ApprovalStatus.values());

        return "edit_activity";
    }

    // ─── AI Extract Endpoint ──────────────────────────────────────────────────
    @PostMapping("/ai/extract")
    public ResponseEntity<?> extractFromCertificate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", required = false) String type) {
        try {
            CertificateExtractDto dto = aiExtractService.extract(file, type);
            if (dto == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(java.util.Map.of("error", "Unknown internal AI extraction error."));
            }
            return ResponseEntity.ok(dto);
        } catch (Throwable e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

    // ─── Word Download ───────────────────────────────────────────────────────
    @GetMapping("/download/word")
    public ResponseEntity<byte[]> downloadWord(@RequestParam("type") String type,
            Authentication authentication) throws IOException {
        String username = authentication.getName();
        boolean isHod = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        AppUser currentUser = userRepository.findByUsername(username).orElse(null);
        String department = (currentUser != null) ? currentUser.getDepartment() : null;

        List<Activity> list = getScopedActivities(username, isHod, isAdmin, department).stream()
                .filter(a -> type.equals(a.getActivityType()))
                .collect(Collectors.toList());

        byte[] docBytes = buildWordDocument(type + " Records", list, type);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + type + "_records.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(docBytes);
    }

    @GetMapping("/download/search/word")
    public ResponseEntity<byte[]> downloadSearchWord(
            Authentication authentication,
            @RequestParam(value = "searchType", required = false) String searchType,
            @RequestParam(value = "typeFilter", required = false) String typeFilter,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "advFaculty", required = false) String advFaculty,
            @RequestParam(value = "advYear", required = false) String advYear,
            @RequestParam(value = "advNittt", required = false) Boolean advNittt,
            @RequestParam(value = "advTypes", required = false) List<String> advTypes) throws IOException {

        String username = authentication.getName();
        boolean isHod = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        AppUser currentUser = userRepository.findByUsername(username).orElse(null);
        String department = (currentUser != null) ? currentUser.getDepartment() : null;

        List<Activity> results;
        if ("method1".equals(searchType)) {
            results = repository.searchMethodOne(typeFilter, keyword != null ? keyword : "");
        } else if ("method2".equals(searchType)) {
            String f = advFaculty != null ? advFaculty : "";
            String y = advYear != null ? advYear : "";
            Boolean n = (advNittt != null && advNittt) ? true : null;
            if (advTypes != null && !advTypes.isEmpty()) {
                results = repository.searchMethodTwoFiltered(advTypes, f, y, n);
            } else {
                results = repository.searchMethodTwo(f, y, n);
            }
        } else {
            results = repository.findAll();
        }

        results = results.stream()
                .filter(a -> hasAccess(a, username, isHod || isAdmin, department, isAdmin))
                .collect(Collectors.toList());

        byte[] docBytes = buildWordDocument("Search Results", results, null);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"search_results.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(docBytes);
    }

    // ─── The Smart Word Document Builder ──────────────────────────────────────
    private byte[] buildWordDocument(String heading, List<Activity> activities, String type) throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {

            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText(heading);
            titleRun.setBold(true);
            titleRun.setFontSize(16);
            titleRun.addBreak();

            boolean isFdpTab = "FDP".equals(type);
            boolean isWorkshopTab = "Workshop".equals(type);
            boolean isConferenceTab = "Conference".equals(type);
            boolean isPublicationTab = "Publication".equals(type);

            boolean hasFdp = isFdpTab || (type == null && activities.stream()
                    .anyMatch(a -> a.getActivityType() != null && a.getActivityType().trim().equalsIgnoreCase("FDP")));
            boolean hasWorkshop = isWorkshopTab || (type == null && activities.stream()
                    .anyMatch(a -> a.getActivityType() != null && a.getActivityType().trim().equalsIgnoreCase("Workshop")));
            boolean hasConf = isConferenceTab || (type == null && activities.stream()
                    .anyMatch(a -> a.getActivityType() != null && a.getActivityType().trim().equalsIgnoreCase("Conference")));
            boolean hasPub = isPublicationTab || (type == null && activities.stream()
                    .anyMatch(a -> a.getActivityType() != null && a.getActivityType().trim().equalsIgnoreCase("Publication")));

            String[] headers;
            if (isFdpTab) {
                headers = new String[]{"Faculty", "FDP Title", "Host Institute", "Date", "Place", "Duration", "NITTT", "Status"};
            } else if (isWorkshopTab) {
                headers = new String[]{"Faculty Name", "Workshop Title", "Date", "Venue", "Host Institute", "Status"};
            } else if (isConferenceTab) {
                headers = new String[]{"Conference Title", "Faculty", "Date", "Duration", "Host Institute", "Location", "Status"};
            } else if (isPublicationTab) {
                headers = new String[]{"Paper Title", "Faculty", "Journal", "Date", "Type", "Level"};
            } else {
                List<String> headerList = new ArrayList<>();
                headerList.add("Type");
                headerList.add("Faculty");
                headerList.add("Title");
                headerList.add("Date");
                if (hasFdp || hasWorkshop || hasConf) {
                    headerList.add("Host Institute");
                    headerList.add("Location");
                }
                if (hasFdp || hasConf) headerList.add("Duration");
                if (hasFdp) headerList.add("NITTT");
                if (hasPub) {
                    headerList.add("Journal / Type");
                    headerList.add("Level");
                }
                headers = headerList.toArray(new String[0]);
            }

            XWPFTable table = doc.createTable(1, headers.length);
            table.setWidth("100%");

            XWPFTableRow headerRow = table.getRow(0);
            for (int i = 0; i < headers.length; i++) {
                XWPFTableCell cell = headerRow.getCell(i);
                cell.setText(headers[i]);
                cell.getParagraphs().get(0).getRuns().get(0).setBold(true);
                cell.setColor("4F46E5");
            }

            for (Activity a : activities) {
                XWPFTableRow row = table.createRow();
                String statusStr = a.getStatus() != null ? a.getStatus().name() : "—";
                String[] values;
                if (isFdpTab) {
                    values = new String[]{safe(a.getFacultyName()), safe(a.getTitle()), safe(a.getHostInstitute()),
                            safe(a.getDate()), safe(a.getLocation()), safe(a.getDuration()),
                            a.isNitttCertified() ? "Yes" : "No", statusStr};
                } else if (isWorkshopTab) {
                    values = new String[]{safe(a.getFacultyName()), safe(a.getTitle()), safe(a.getDate()),
                            safe(a.getLocation()), safe(a.getHostInstitute()), statusStr};
                } else if (isConferenceTab) {
                    values = new String[]{safe(a.getTitle()), safe(a.getFacultyName()), safe(a.getDate()),
                            safe(a.getDuration()), safe(a.getHostInstitute()), safe(a.getLocation()), statusStr};
                } else if (isPublicationTab) {
                    values = new String[]{safe(a.getTitle()), safe(a.getFacultyName()), safe(a.getJournalName()),
                            safe(a.getDate()), safe(a.getJournalType()), safe(a.getPublicationLevel())};
                } else {
                    List<String> valList = new ArrayList<>();
                    valList.add(safe(a.getActivityType()));
                    valList.add(safe(a.getFacultyName()));
                    valList.add(safe(a.getTitle()));
                    valList.add(safe(a.getDate()));
                    if (hasFdp || hasWorkshop || hasConf) {
                        valList.add(safe(a.getHostInstitute()));
                        valList.add(safe(a.getLocation()));
                    }
                    if (hasFdp || hasConf) valList.add(safe(a.getDuration()));
                    if (hasFdp) valList.add(a.isNitttCertified() ? "Yes" : "-");
                    if (hasPub) {
                        String pubInfo = safe(a.getJournalName());
                        if (a.getJournalType() != null && !a.getJournalType().isEmpty()) {
                            pubInfo += " (" + a.getJournalType() + ")";
                        }
                        valList.add(pubInfo);
                        valList.add(safe(a.getPublicationLevel()));
                    }
                    values = valList.toArray(new String[0]);
                }

                for (int i = 0; i < values.length && i < row.getTableCells().size(); i++) {
                    row.getCell(i).setText(values[i]);
                }
            }

            XWPFParagraph footer = doc.createParagraph();
            XWPFRun footerRun = footer.createRun();
            footerRun.addBreak();
            footerRun.setText("Total Records: " + activities.size());
            footerRun.setItalic(true);
            footerRun.setFontSize(10);
            footerRun.setColor("888888");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

    private String safe(String s) {
        return s != null ? s : "";
    }
}