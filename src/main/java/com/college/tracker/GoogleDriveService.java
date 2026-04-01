package com.college.tracker;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;

@Service
@SuppressWarnings("deprecation")
public class GoogleDriveService {

        @Value("${google.drive.client.id}")
        private String clientId;

        @Value("${google.drive.client.secret}")
        private String clientSecret;

        @Value("${google.drive.refresh.token}")
        private String refreshToken;

        @Value("${google.drive.folder.id}")
        private String folderId;

        private Drive getDriveService() throws Exception {
                // This logs in securely as YOUR personal account using the Refresh Token!
                Credential credential = new GoogleCredential.Builder()
                                .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                                .setJsonFactory(GsonFactory.getDefaultInstance())
                                .setClientSecrets(clientId, clientSecret)
                                .build()
                                .setRefreshToken(refreshToken);

                return new Drive.Builder(
                                GoogleNetHttpTransport.newTrustedTransport(),
                                GsonFactory.getDefaultInstance(),
                                credential)
                                .setApplicationName("Faculty Tracker")
                                .build();
        }

        public String uploadFile(MultipartFile multipartFile) throws Exception {
                Drive driveService = getDriveService();

                File fileMetadata = new File();
                fileMetadata.setName(multipartFile.getOriginalFilename());
                fileMetadata.setParents(Collections.singletonList(folderId));

                InputStreamContent mediaContent = new InputStreamContent(
                                multipartFile.getContentType(), multipartFile.getInputStream());

                File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                                .setFields("id, webViewLink")
                                .execute();

                Permission permission = new Permission()
                                .setType("anyone")
                                .setRole("reader");
                driveService.permissions().create(uploadedFile.getId(), permission).execute();

                return uploadedFile.getWebViewLink();
        }
}