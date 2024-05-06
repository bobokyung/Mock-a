package com.mozart.mocka.service.generator;

import com.mozart.mocka.domain.ApiPath;
import com.mozart.mocka.domain.ApiProjects;
import com.mozart.mocka.domain.ApiRequest;
import com.mozart.mocka.domain.ApiResponse;
import com.mozart.mocka.dto.request.InitializerRequestDto;
import com.mozart.mocka.repository.ApiPathRepository;
import com.mozart.mocka.repository.ApiRequestRepository;
import com.mozart.mocka.repository.ApiResponseRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class GenController {
    private final ApiPathRepository apiPathRepository;
    private final ApiRequestRepository apiRequestRepository;
    private final ApiResponseRepository apiResponseRepository;

    public void createController(
        Path projectRoot, List<ApiProjects> apis, InitializerRequestDto request, int index) throws IOException {
        Path controllerDir = projectRoot.resolve("src/main/java/" + request.getSpringPackageName().replace(".", "/") + "/controller");
        Files.createDirectories(controllerDir); // 컨트롤러 디렉토리 생성

        for (ApiProjects api : apis) {
            String controllerName = api.getApiUriStr().split("/")[1];
            controllerName = controllerName.substring(0, 1).toUpperCase() + controllerName.substring(1);
            String className = controllerName + "Controller";
            Path controllerFile = controllerDir.resolve(className + ".java");

            List<String> lines;
            if (Files.exists(controllerFile)) {
                // 기존 컨트롤러 파일 수정
                lines = Files.readAllLines(controllerFile, StandardCharsets.UTF_8);
                int lastIndex = lines.size() - 1; // 마지막 중괄호의 인덱스 찾기
                // 새로운 메소드를 마지막 중괄호 바로 전에 추가
                lines.addAll(lastIndex, generateMethodLines(api, index++));
            } else {
                // 새 컨트롤러 파일 생성
                lines = new ArrayList<>();
                lines.add("package " + request.getSpringPackageName() + ".controller;\n");

                lines.add("import org.springframework.web.bind.annotation.*;");
                lines.add("import org.springframework.http.ResponseEntity;");
                lines.add("import " + request.getSpringPackageName() + ".dto.*;\n");

                lines.add("@RestController");
                lines.add("@RequestMapping(\"/api/" + controllerName + "\")");
                lines.add("public class " + className + " {");
                lines.addAll(generateMethodLines(api, index++));
                lines.add("}");
            }

            // 파일 쓰기
            Files.write(controllerFile, lines, StandardCharsets.UTF_8);
            log.info("Updated or created controller: " + controllerFile);
        }
    }

    private List<String> generateMethodLines(ApiProjects api, int index) {
        List<String> methodLines = new ArrayList<>();
        String requestUri = setUri(api.getApiUriStr());
        methodLines.add("\n    @" + api.getApiMethod() + "Mapping(\"" + requestUri + "\")");

        Long apiId = api.getApiId();
        List<ApiPath> apiPaths = apiPathRepository.findByApiProject_ApiId(apiId);
        List<ApiRequest> apiRequests = apiRequestRepository.findByApiProject_ApiId(apiId);
        List<ApiResponse> apiResponses = apiResponseRepository.findByApiProject_ApiId(apiId);

        String methodName = "autoCreatedApiNo" + index;
        String responseType = apiResponses.isEmpty() ? "?" : "ResponseDtoNo" + index; // DTO 클래스 이름 또는 Void
        String requestType = "RequestDtoNo" + index; // DTO 클래스 이름

        String methodSignature = "ResponseEntity<" + responseType + ">";
        if (apiPaths.isEmpty() && apiRequests.isEmpty()) {
            methodLines.add("    public " + methodSignature + " " + methodName + "() {");
        }
        else {
            methodLines.add("    public " + methodSignature + " " + methodName + "(");

            apiPaths.forEach(path -> {
                methodLines.add(
                    "        @PathVariable(\"" + path.getKey() + "\") " + path.getData() + " "
                        + path.getKey() + ",");
            });

            if (!apiRequests.isEmpty()) {
                methodLines.add("        @RequestBody " + requestType + " request) {");
            } else {
                methodLines.remove(methodLines.size() - 1); // 마지막 콤마 제거
                methodLines.add(
                    methodLines.remove(methodLines.size() - 1) + "\n        ) {"); // 메서드 매개변수 닫기
            }
        }

        if (responseType.equals("?")) {
            methodLines.add("        return ResponseEntity.ok().build();");
        } else {
            methodLines.add("        " + responseType + " response = new " + responseType + "();");
            methodLines.add("        return ResponseEntity.ok().body(response);");
        }

        methodLines.add("    }");
        return methodLines;
    }

    public String setUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return uri;
        }

        String[] segments = uri.split("/");
        StringBuilder result = new StringBuilder();
        for (int i = 2; i < segments.length; i++) {
            result.append("/").append(segments[i]);
        }

        return result.toString();
    }
}