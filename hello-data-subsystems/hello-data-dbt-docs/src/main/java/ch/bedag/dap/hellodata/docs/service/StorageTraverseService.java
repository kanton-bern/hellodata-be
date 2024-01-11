/*
 * Copyright © 2024, Kanton Bern
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ch.bedag.dap.hellodata.docs.service;

import ch.bedag.dap.hellodata.docs.model.ProjectDoc;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class StorageTraverseService {

    private static final List<String> INDEX_FILE_NAMES = List.of("index.html", "index.htm", "index");
    private final ProjectDocService projectDocService;
    @Value("${hello-data.files.static-location}")
    private String storageLocation;
    @Value("${hello-data.files.static-location-to-omit}")
    private String storageLocationToOmit;

    public static String getLastElementOfPath(String path) {
        return Paths.get(path).getFileName().toString();
    }

    private static String getIconPath(String projectName) {
        //TODO how to get an icon location?
        String imgPath = "../assets/projects/blank.png";
        if (projectName.equalsIgnoreCase("momi")) {
            imgPath = "../assets/projects/momi.png";
        } else if (projectName.equalsIgnoreCase("kibon")) {
            imgPath = "../assets/projects/kibon.png";
        }
        return imgPath;
    }

    @PostConstruct
    public void init() {
        log.info("Storage location: {}", storageLocation);
        log.info("Storage location to omit: {}", storageLocationToOmit);
    }

    /**
     * Walk file tree in storage to search for documentation files.
     * <p>
     * The storage location is the root-location where each data-domain contains one folder. Each folder itself can hold n subfolders / project-docs
     */
    @Scheduled(fixedDelayString = "${hello-data.files.scan-interval-seconds}", timeUnit = TimeUnit.SECONDS)
    public void walkFilesInStorage() {
        Path storagePath = Path.of(storageLocation);
        log.info("--- Searching for changes in documentation storage: {}", storagePath);
        List<Path> storageLocationsToOmit = new ArrayList<>();
        if (storageLocationToOmit != null) {
            storageLocationsToOmit.addAll(Arrays.stream(storageLocationToOmit.split(",")).map(location -> Paths.get(location)).toList());
        }

        List<ProjectDoc> result = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(storagePath, 1)) {
            walk.filter(Files::isDirectory)
                .filter(path -> !path.equals(storagePath))
                .filter(path -> storageLocationsToOmit.stream().noneMatch(locationToOmit -> path.startsWith(locationToOmit)))
                .forEach(path -> {
                    List<ProjectDoc> subDocs = collectProjectDocs(path);
                    printDebugOutput(path, subDocs);
                    result.addAll(subDocs);
                });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        projectDocService.clearCache();
        result.forEach(projectDocService::addProject);
        log.info("--- Finished search for changes in documentation");
    }

    private void printDebugOutput(Path path, List<ProjectDoc> subDocs) {
        log.info("{}:", path);
        for (ProjectDoc subDoc : subDocs) {
            log.info(subDoc);
        }
    }

    private List<ProjectDoc> collectProjectDocs(Path storagePath) {
        List<ProjectDoc> result = new LinkedList<>();
        String folderName = getLastElementOfPath(storagePath.toString());
        try (Stream<Path> pathStream = Files.walk(storagePath, 5)) {
            pathStream.filter(path -> path.toFile().isFile()).filter(path -> INDEX_FILE_NAMES.contains(path.toFile().getName())).forEach(filePath -> {
                log.info("------ Found file {}", filePath);
                String projectName = filePath.getParent().getFileName().toString();
                String projectPath = FilenameUtils.separatorsToUnix(filePath.toString()).replace(storageLocation, "");
                String imgPath = getIconPath(projectName);
                ProjectDoc projectDoc =
                        new ProjectDoc(folderName, projectName, FilenameUtils.separatorsToUnix(projectPath), imgPath, projectName, getLastModifiedTime(filePath.getParent()));
                result.add(projectDoc);
            });
        } catch (IOException e) {
            log.error("", e);
        }
        return result;
    }

    private LocalDateTime getLastModifiedTime(Path path) {
        try {
            FileTime lastModifiedTime = Files.getLastModifiedTime(path);
            return LocalDateTime.ofInstant(lastModifiedTime.toInstant(), ZoneId.systemDefault());
        } catch (IOException e) {
            log.error("Could not get lastModifiedDate of folder {}", path, e);
        }
        return LocalDateTime.now();
    }

    public List<String> listDataDomainDirectories() {
        try {
            Path storagePath = Path.of(storageLocation);
            log.info("--- Searching for data domain directories in: {}", storagePath);
            List<String> dataDomainDirs = new ArrayList<>();
            try (Stream<Path> list = Files.list(storagePath)) {
                return list.toList().stream().map(t -> t.getFileName().toString()).toList();
            }
        } catch (Exception e) {
            return null;
        }
    }
}
