package com.mozart.mocka.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mozart.mocka.dto.request.ApiCreateRequestDto;
import com.mozart.mocka.service.ApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/method")
public class MethodController {
   private final ApiService apiService;

    @PostMapping("{projectId}")
    public ResponseEntity<?> createApi(@PathVariable("projectId") Long projectId, @RequestBody ApiCreateRequestDto requestDto) throws JsonProcessingException {
        //edit 인증 체크

        //method 중복 체크

        apiService.createApi(projectId, requestDto);
        return null;
    }
}
