package com.github.im2back.aws_integration_lab.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.github.im2back.aws_integration_lab.controller.dto.out.S3UploadResponse;
import com.github.im2back.aws_integration_lab.service.S3Service;

/**
 * Controller responsável por disponibilizar os endpoints REST
 * para integração com o Amazon S3.
 *
 * Recebe as requisições HTTP e delega as operações ao S3Service.
 */
@RestController
@RequestMapping("/api/v1/s3/files")
public class S3Controller {

    private final S3Service s3Service;

    /**
     * Inicializa o Controller com o serviço de integração S3.
     *
     * @param s3Service serviço responsável pelas operações no S3.
     */
    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    /**
     * Recebe um arquivo via multipart/form-data e realiza
     * seu upload para o Amazon S3.
     *
     * @param file arquivo recebido na requisição HTTP.
     * @return HTTP 201 Created com os dados do upload.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<S3UploadResponse> upload(@RequestParam("file") MultipartFile file) {

        S3UploadResponse response = s3Service.upload(file);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}







