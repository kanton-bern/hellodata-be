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
package ch.bedag.dap.hellodata.docs.controller;

import ch.bedag.dap.hellodata.docs.AbstractKeycloakTestContainers;
import ch.bedag.dap.hellodata.docs.model.ProjectDoc;
import ch.bedag.dap.hellodata.docs.service.ProjectDocService;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Sql(scripts = "/db/changelog/changelogs/add_testdata.sql")
class ProjectsDocsControllerTest extends AbstractKeycloakTestContainers {

    @Autowired
    private ProjectDocService projectDocService;

    @Test
    void should_get_empty_list_from_backend() {
        Mockito.when(projectDocService.getAllProjectsDocs()).thenReturn(Collections.emptyList());
        given().header("Authorization", getAdminBearer()).when().get("/api/projects-docs").then().body("", equalTo(Collections.emptyList()));
    }

    @Test
    void should_get_non_empty_list_from_backend() {
        Mockito.when(projectDocService.getAllProjectsDocs())
                .thenReturn(List.of(new ProjectDoc("context_key_1", "test", "path", "img", "alt"), new ProjectDoc("context_key_1", "test1", "path1", "img1", "alt1")));
        given().header("Authorization", getAdminBearer()).when().get("/api/projects-docs").then().body("", not(equalTo(Collections.emptyList())));
    }

    @Test
    void momiUser_requestingProjectDocs_shouldGetMomiInListFromBackend() {
        //Given
        List<ProjectDoc> projectDocs = getListWith3ProjectDocs();
        Mockito.when(projectDocService.getAllProjectsDocs()).thenReturn(projectDocs);
        List<ProjectDoc> momi = projectDocs.stream().filter(pd -> pd.name().equals("momi")).toList();
        //When
        ValidatableResponse response = given().header("Authorization", getMomiUserBearer()).when().get("/api/projects-docs").then().statusCode(HttpStatus.OK.value());
        //Then
        List<ProjectDoc> returnedProjectDocs = response.extract().as(new TypeRef<List<ProjectDoc>>() {
        });
        assertFalse(returnedProjectDocs.isEmpty());
        assertEquals(momi, returnedProjectDocs);
    }

    @Test
    void adminUser_requestingProjectDocs_shouldGetAllProjectsInListFromBackend() {
        //Given
        List<ProjectDoc> projectDocs = getListWith3ProjectDocs();
        Mockito.when(projectDocService.getAllProjectsDocs()).thenReturn(projectDocs);
        //When
        ValidatableResponse response = given().header("Authorization", getAdminBearer()).when().get("/api/projects-docs").then().statusCode(HttpStatus.OK.value());
        //Then
        List<ProjectDoc> returnedProjectDocs = response.extract().as(new TypeRef<List<ProjectDoc>>() {
        });
        assertFalse(returnedProjectDocs.isEmpty());
        assertThat(projectDocs).hasSameElementsAs(returnedProjectDocs);
    }

    @Test
    void userWithoutRoles_requestingProjectDocs_shouldGetEmptyListFromBackend() {
        //Given
        List<ProjectDoc> projectDocs = getListWith3ProjectDocs();
        Mockito.when(projectDocService.getAllProjectsDocs()).thenReturn(projectDocs);
        //When
        ValidatableResponse response = given().header("Authorization", getNoRolesUserBearer()).when().get("/api/projects-docs").then().statusCode(HttpStatus.OK.value());
        //Then
        List<ProjectDoc> returnedProjectDocs = response.extract().as(new TypeRef<List<ProjectDoc>>() {
        });
        assertTrue(returnedProjectDocs.isEmpty());
    }

    @Test
    void should_get_one_doc_by_name() {
        String contextKey = "context_key_1";
        String name = "test";
        String path = "path";
        String imgPath = "img";
        String alt = "alt";
        Mockito.when(projectDocService.getByName(name)).thenReturn(Optional.of(new ProjectDoc(contextKey, name, path, imgPath, alt)));
        given().header("Authorization", getAdminBearer())
                .when()
                .get("/api/projects-docs/" + name + "/info")
                .then()
                .body("imgPath", equalTo(imgPath))
                .body("name", equalTo(name))
                .body("path", equalTo(path))
                .body("alt", equalTo(alt));
    }

    @Test
    void momiUser_requestingKibonProjectInfo_shouldGetForbidden() {
        //Given
        String requestedProject = "kibon";
        //When / Then
        Response response = given().header("Authorization", getMomiUserBearer()).when().get("/api/projects-docs/" + requestedProject + "/info");
        response.then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void userWithoutRoles_requestingKibonProjectInfo_shouldGetForbidden() {
        //Given
        String requestedProject = "kibon";
        //When / Then
        Response response = given().header("Authorization", getNoRolesUserBearer()).when().get("/api/projects-docs/" + requestedProject + "/info");
        response.then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void adminUser_requestingKibonProjectInfo_shouldGetKibonInfos() {
        //Given
        String requestedProject = "kibon";
        List<ProjectDoc> projectDocs = getListWith3ProjectDocs();
        Mockito.when(projectDocService.getAllProjectsDocs()).thenReturn(projectDocs);
        List<ProjectDoc> kibon = projectDocs.stream().filter(pd -> pd.name().equals(requestedProject)).toList();
        Mockito.when(projectDocService.getByName("kibon")).thenReturn(Optional.of(kibon.get(0)));

        //When / Then
        Response response = given().header("Authorization", getAdminBearer()).when().get("/api/projects-docs/" + requestedProject + "/info");
        response.then().statusCode(HttpStatus.OK.value()).body("name", equalTo(requestedProject));
    }

    @Test
    void adminUser_getMomiProjectByPath_shouldReturnData() {
        List<ProjectDoc> projectDocs = getListWith3ProjectDocs();
        Mockito.when(projectDocService.getAllProjectsDocs()).thenReturn(projectDocs);
        String contextKey = "context_key_1";
        String name = "test";
        String path = "path";
        String imgPath = "img";
        String alt = "alt";
        Mockito.when(projectDocService.getByName(name)).thenReturn(Optional.of(new ProjectDoc(contextKey, name, path, imgPath, alt)));
        Response response = given().header("Authorization", getAdminBearer()).queryParam("path", "/momi/index.html").when().get("/api/projects-docs/get-by-path");
        response.then().statusCode(HttpStatus.OK.value()).body(containsStringIgnoringCase("<html>momi</html>"));
    }

    @Test
    void momiUser_getMomiProjectByPath_shouldReturnData() {
        List<ProjectDoc> projectDocs = getListWith3ProjectDocs();
        Mockito.when(projectDocService.getAllProjectsDocs()).thenReturn(projectDocs);
        String contextKey = "context_key_1";
        String name = "test";
        String path = "path";
        String imgPath = "img";
        String alt = "alt";
        Mockito.when(projectDocService.getByName(name)).thenReturn(Optional.of(new ProjectDoc(contextKey, name, path, imgPath, alt)));
        Response response = given().header("Authorization", getMomiUserBearer()).queryParam("path", "/momi/index.html").when().get("/api/projects-docs/get-by-path");
        response.then().statusCode(HttpStatus.OK.value()).body(containsStringIgnoringCase("<html>momi</html>"));
    }

    @Test
    void momiUser_getKibonProjectByPath_shouldReturnForbidden() {
        List<ProjectDoc> projectDocs = getListWith3ProjectDocs();
        Mockito.when(projectDocService.getAllProjectsDocs()).thenReturn(projectDocs);
        Response response = given().header("Authorization", getMomiUserBearer()).queryParam("path", "/kibon/index.html").when().get("/api/projects-docs/get-by-path");
        response.then().statusCode(HttpStatus.FORBIDDEN.value()).body(emptyString());
    }

    @Test
    void userWithoutRoles_getKibonProjectByPath_shouldReturnForbidden() {
        List<ProjectDoc> projectDocs = getListWith3ProjectDocs();
        Mockito.when(projectDocService.getAllProjectsDocs()).thenReturn(projectDocs);
        Response response = given().header("Authorization", getNoRolesUserBearer()).queryParam("path", "/kibon/index.html").when().get("/api/projects-docs/get-by-path");
        response.then().statusCode(HttpStatus.FORBIDDEN.value()).body(emptyString());
    }

    private List<ProjectDoc> getListWith3ProjectDocs() {
        List<ProjectDoc> projectDocs = new ArrayList<>();
        projectDocs.add(new ProjectDoc("kibon", "kibon", "/kibon/index.html", "image.jpg", "image"));
        projectDocs.add(new ProjectDoc("momi", "momi", "/momi/index.html", "image.jpg", "image"));
        projectDocs.add(new ProjectDoc("efan", "efan", "/efan/index.html", "image.jpg", "image"));
        return projectDocs;
    }
}
