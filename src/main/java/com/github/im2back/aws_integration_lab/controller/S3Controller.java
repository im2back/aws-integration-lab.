package com.github.im2back.aws_integration_lab.controller;


import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.github.im2back.aws_integration_lab.service.S3Service;

/**
 * Controller responsável por disponibilizar os endpoints REST
 * para integração com o Amazon S3.
 *
 * Recebe as requisições HTTP e delega as operações ao S3Service,
 * mantendo a lógica de integração com a AWS na camada de serviço.
 *
 * Durante o desenvolvimento, os arquivos são armazenados no
 * Amazon S3 simulado pelo LocalStack.
 */
@RestController
@RequestMapping("/api/v1/s3/files")
public class S3Controller {

    private final S3Service s3Service;

    /**
     * Inicializa o controller com o serviço de integração S3.
     *
     * @param s3Service serviço responsável pelas operações no Amazon S3.
     */
    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    /**
     * Recebe um arquivo por requisição HTTP e realiza seu upload
     * para o bucket Amazon S3 configurado na aplicação.
     *
     * O arquivo deve ser enviado utilizando multipart/form-data.
     *
     * @param file arquivo recebido na requisição HTTP.
     * @return HTTP 201 Created com a chave do objeto armazenado.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {

        String objectKey = s3Service.upload(file);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Arquivo enviado com sucesso.",
                        "key", objectKey
                ));
    }
}
