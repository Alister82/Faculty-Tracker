package com.college.tracker;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.*;
import java.util.Base64;

@Service
public class AiExtractService {

    @Value("${groq.api.key:}")
    private String groqApiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    public CertificateExtractDto extract(MultipartFile file, String certType) throws Exception {
        if (groqApiKey == null || groqApiKey.isBlank() || groqApiKey.equals("YOUR_GROQ_API_KEY")) {
            throw new IllegalStateException("Groq API key not configured. Add 'groq.api.key=YOUR_KEY' to application-secret.properties.");
        }

        byte[] fileBytes = file.getBytes();
        String mimeType = determineMimeType(file);

        // Groq Vision models only support images. If the user uploads a PDF,
        // we must rasterize the first page to a JPEG image first.
        if ("application/pdf".equals(mimeType)) {
            try (PDDocument document = PDDocument.load(fileBytes)) {
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 150, ImageType.RGB);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bim, "jpeg", baos);
                fileBytes = baos.toByteArray();
                mimeType = "image/jpeg";
            }
        }

        String base64Data = Base64.getEncoder().encodeToString(fileBytes);

        String context = "";
        if ("fdp".equals(certType)) {
            context = "This is a Faculty Development Program (FDP) certificate. Extract the faculty/participant name, the FDP name/title, the start date, the end date, the duration in days, the hostInstitute (host college), and the location (city/venue). Also determine if the certificate explicitly mentions 'NITTT' or 'National Initiative for Technical Teachers Training' and set isNittt to true if it does.";
        } else if ("ws".equals(certType)) {
            context = "This is a Workshop certificate. Extract the faculty/participant name, the workshop title, the start date, the location/venue, and the hostInstitute.";
        } else if ("conf".equals(certType)) {
            context = "This is a Conference certificate. Extract the faculty/participant name, the conference title, the start date, the duration in days, the hostInstitute, and the location.";
        } else if ("pub".equals(certType)) {
            context = "This is a Publication document or certificate. Extract the author/faculty name, the paper title (eventName), the publication date (startDate), the journalName, the journalType (extract strictly as one of: 'SC', 'Book Chapter', 'UGC', or 'Other'), and the publicationLevel (extract strictly as: 'National' or 'International').";
        } else {
            context = "Extract the faculty name, the event/publication name, the start date/publication date, the end date, and the duration in days. Also extract the 'hostInstitute', and 'location' (city/venue).";
        }

        String systemPrompt = "Analyze this certificate image. " + context + 
                " CRITICAL: ALWAYS extract the exact Name of the Faculty or Person receiving this certificate, even if they aren't labeled 'Faculty'." +
                " Note: If duration is not explicitly mentioned but start/end dates are given, you must calculate the duration in days. If it's totally missing, default to '1 day'." +
                " Note 2: Format any dates you extract strictly in ISO YYYY-MM-DD format." +
                " Return the response exclusively in a structured JSON format with keys: " +
                "facultyName (string), eventName (string), startDate (string), endDate (string), duration (string), " +
                "hostInstitute (string), location (string), journalName (string), journalType (string), publicationLevel (string), isNittt (boolean). " +
                "Do not include markdown codeblocks or any conversational text. Return ONLY raw JSON.";

        String requestBody = """
            {
              "model": "meta-llama/llama-4-scout-17b-16e-instruct",
              "messages": [
                {
                  "role": "user",
                  "content": [
                    {
                      "type": "text",
                      "text": "%s"
                    },
                    {
                      "type": "image_url",
                      "image_url": {
                        "url": "data:%s;base64,%s"
                      }
                    }
                  ]
                }
              ],
              "temperature": 0.1
            }
            """.formatted(systemPrompt, mimeType, base64Data);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + groqApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Groq API error: " + response.statusCode() + " " + response.body());
        }

        return parseGroqResponse(response.body());
    }

    private String determineMimeType(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName != null) {
            String lower = originalName.toLowerCase();
            if (lower.endsWith(".pdf")) return "application/pdf";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".png")) return "image/png";
        }
        String contentType = file.getContentType();
        return contentType != null ? contentType : "image/jpeg";
    }

    private CertificateExtractDto parseGroqResponse(String responseBody) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(responseBody);
        
        com.fasterxml.jackson.databind.JsonNode choices = rootNode.path("choices");
        if (choices.isMissingNode() || !choices.isArray() || choices.size() == 0) {
            throw new RuntimeException("No choices found in Groq response: " + responseBody);
        }
        
        String contentText = choices.get(0).path("message").path("content").asText();

        // Strip markdown code fences if present (```json ... ```)
        if (contentText.contains("```")) {
            int jsonStart = contentText.indexOf("{");
            int jsonEnd = contentText.lastIndexOf("}");
            if (jsonStart != -1 && jsonEnd != -1) {
                contentText = contentText.substring(jsonStart, jsonEnd + 1);
            }
        }

        // Parse the inner JSON using Jackson too
        com.fasterxml.jackson.databind.JsonNode innerNode = mapper.readTree(contentText);
        
        CertificateExtractDto dto = new CertificateExtractDto();
        dto.setFacultyName(innerNode.path("facultyName").asText(null));
        dto.setEventName(innerNode.path("eventName").asText(null));
        dto.setStartDate(innerNode.path("startDate").asText(null));
        dto.setEndDate(innerNode.path("endDate").asText(null));
        dto.setDuration(innerNode.path("duration").asText(null));
        dto.setHostInstitute(innerNode.path("hostInstitute").asText(null));
        dto.setLocation(innerNode.path("location").asText(null));
        dto.setJournalName(innerNode.path("journalName").asText(null));
        dto.setJournalType(innerNode.path("journalType").asText(null));
        dto.setPublicationLevel(innerNode.path("publicationLevel").asText(null));
        
        if (!innerNode.path("isNittt").isMissingNode()) {
            dto.setIsNittt(innerNode.path("isNittt").asBoolean(false));
        }

        return dto;
    }
}
