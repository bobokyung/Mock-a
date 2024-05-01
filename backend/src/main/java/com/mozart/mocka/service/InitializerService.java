package com.mozart.mocka.service;

import com.mozart.mocka.domain.ApiProjects;
import com.mozart.mocka.dto.request.InitializerRequestDto;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InitializerService {

    // 파일 생성
    public Path createInitializerFiles(InitializerRequestDto request, List<ApiProjects> apis) throws IOException {

        Path projectRoot = Paths.get(request.getSpringArtifactId());
        createDirectories(projectRoot, request);
        createApplicationProperties(projectRoot);
        createApplicationClass(projectRoot, request);
        createController(projectRoot, apis, request);

        if ("maven".equalsIgnoreCase(request.getSpringType())) {
            createPomFile(projectRoot, request);
        } else if ("gradle".equalsIgnoreCase(request.getSpringType())) {
            createGradleBuildFile(projectRoot, request);
        }

        return projectRoot;
    }

    private void createController(Path projectRoot, List<ApiProjects> apis, InitializerRequestDto request) throws IOException {
        Path controllerDir = projectRoot.resolve("src/main/java/" + request.getSpringPackageName().replace(".", "/") + "/controller");
        Files.createDirectories(controllerDir); // 컨트롤러 디렉토리 생성

        for (ApiProjects api : apis) {
            String className = api.getApiMethod() + "Controller";
            Path controllerFile = controllerDir.resolve(className + ".java");

            List<String> lines;
            if (Files.exists(controllerFile)) {
                // 기존 컨트롤러 파일 수정
                lines = Files.readAllLines(controllerFile, StandardCharsets.UTF_8);
                int lastIndex = lines.size() - 1; // 마지막 중괄호의 인덱스 찾기
                // 새로운 메소드를 마지막 중괄호 바로 전에 추가
                lines.addAll(lastIndex, generateMethodLines(api));
            } else {
                // 새 컨트롤러 파일 생성
                lines = new ArrayList<>();
                lines.add("package " + request.getSpringPackageName() + ".controller;\n");
                lines.add("import org.springframework.web.bind.annotation.*;");
                lines.add("import org.springframework.http.ResponseEntity;\n");
                lines.add("@RestController");
                lines.add("public class " + className + " {");
                lines.addAll(generateMethodLines(api));
                lines.add("}");
            }

            // 파일 쓰기
            Files.write(controllerFile, lines, StandardCharsets.UTF_8);
            log.info("Updated or created controller: " + controllerFile);
        }
    }

    private List<String> generateMethodLines(ApiProjects api) {
        List<String> methodLines = new ArrayList<>();
        String methodName = "handle" + api.getApiMethod().toUpperCase();
        methodLines.add("\n    @" + api.getApiMethod().toUpperCase() + "Mapping(\"" + api.getApiUriStr() + "\")");
        methodLines.add("    public ResponseEntity<?> " + methodName + "() {");
        methodLines.add("        return ResponseEntity.ok().body(\"This is a response from " + api.getApiMethod().toUpperCase() + " endpoint.\");");
        methodLines.add("    }");
        return methodLines;
    }

    private void createDirectories(Path projectRoot, InitializerRequestDto request) throws IOException {
        Files.createDirectories(projectRoot.resolve("src/main/java/" + request.getSpringPackageName().replace('.', '/')));
        Files.createDirectories(projectRoot.resolve("src/main/resources"));
        Files.createDirectories(projectRoot.resolve("src/test/java/" + request.getSpringPackageName().replace('.', '/')));
        Files.createDirectories(projectRoot.resolve("src/test/resources"));
    }

    private void createPomFile(Path projectRoot, InitializerRequestDto request) throws IOException {
        Path pomPath = projectRoot.resolve("pom.xml");
        try (BufferedWriter writer = Files.newBufferedWriter(pomPath)) {
            writer.write(generatePomContent(request));
        }
    }

    private void createApplicationProperties(Path projectRoot) throws IOException {
        Path propertiesPath = projectRoot.resolve("src/main/resources/application.properties");
        Files.createFile(propertiesPath);
    }

    private void createApplicationClass(Path projectRoot, InitializerRequestDto request) throws IOException {
        Path mainClassPath = projectRoot.resolve("src/main/java/" + request.getSpringPackageName().replace('.', '/') + "/Application.java");
        try (BufferedWriter writer = Files.newBufferedWriter(mainClassPath)) {
            writer.write(generateMainClassContent(request));
        }
    }

    private String generateMainClassContent(InitializerRequestDto request) {
        return "package " + request.getSpringPackageName() + ";\n\n" +
            "import org.springframework.boot.SpringApplication;\n" +
            "import org.springframework.boot.autoconfigure.SpringBootApplication;\n\n" +
            "@SpringBootApplication\n" +
            "public class Application {\n\n" +
            "    public static void main(String[] args) {\n" +
            "        SpringApplication.run(ApplicationApplication.class, args);\n" +
            "    }\n" +
            "}\n";
    }

    private String generatePomContent(InitializerRequestDto request) {
        return "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>" + request.getSpringGroupId() + "</groupId>\n" +
            "    <artifactId>" + request.getSpringArtifactId() + "</artifactId>\n" +
            "    <version>0.0.1-SNAPSHOT</version>\n" +
            "    <name>" + request.getSpringName() + "</name>\n" +
            "    <description>" + request.getSpringDescription() + "</description>\n" +
            "    <parent>\n" +
            "        <groupId>org.springframework.boot</groupId>\n" +
            "        <artifactId>spring-boot-starter-parent</artifactId>\n" +
            "        <version>" + request.getSpringPlatformVersion() + "</version>\n" +
            "        <relativePath/> <!-- lookup parent from repository -->\n" +
            "    </parent>\n" +
            "    <dependencies>\n" +
            "        <dependency>\n" +
            "            <groupId>org.springframework.boot</groupId>\n" +
            "            <artifactId>spring-boot-starter</artifactId>\n" +
            "        </dependency>\n" +
            "        <!-- Additional dependencies can be added here -->\n" +
            "    </dependencies>\n" +
            "    <properties>\n" +
            "        <java.version>" + request.getSpringJvmVersion() + "</java.version>\n" +
            "    </properties>\n" +
            "</project>";
    }

    private void createGradleBuildFile(Path projectRoot, InitializerRequestDto request) throws IOException {
        Path buildFilePath = projectRoot.resolve("build.gradle");
        try (BufferedWriter writer = Files.newBufferedWriter(buildFilePath)) {
            writer.write(generateGradleBuildContent(request));
        }
    }

    private String generateGradleBuildContent(InitializerRequestDto request) {
        return "plugins {\n" +
            "    id 'org.springframework.boot' version '" + request.getSpringPlatformVersion() + "'\n" +
            "    id 'io.spring.dependency-management' version '1.0.11.RELEASE'\n" +
            "    id 'java'\n" +
            "}\n\n" +
            "group = '" + request.getSpringGroupId() + "'\n" +
            "version = '0.0.1-SNAPSHOT'\n" +
            "sourceCompatibility = '" + request.getSpringJvmVersion() + "'\n\n" +
            "repositories {\n" +
            "    mavenCentral()\n" +
            "}\n\n" +
            "dependencies {\n" +
            "    implementation 'org.springframework.boot:spring-boot-starter-web'\n" +
            "    testImplementation('org.springframework.boot:spring-boot-starter-test')\n" +
            "    // Add additional dependencies here\n" +
            "}\n\n" +
            "test {\n" +
            "    useJUnitPlatform()\n" +
            "}";
    }

    // 파일 압축
    public Path packageProject(Path projectRoot) throws IOException {
        Path zipPath = projectRoot.resolveSibling(projectRoot.getFileName().toString() + ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            Files.walk(projectRoot)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    ZipEntry zipEntry = new ZipEntry(projectRoot.relativize(path).toString());
                    try {
                        zos.putNextEntry(zipEntry);
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        System.err.println("Error while zipping: " + e.getMessage());
                    }
                });
        }
        return zipPath;
    }

    // 파일 삭제
    public void cleanUpFiles(Path zipPath, Path projectRoot) {
        try {
            // zip 파일 삭제
            Files.deleteIfExists(zipPath);
            // 원본 파일 삭제
            Files.walk(projectRoot)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        } catch (IOException e) {
            System.err.println("Error cleaning up project files: " + e.getMessage());
        }
    }
}
