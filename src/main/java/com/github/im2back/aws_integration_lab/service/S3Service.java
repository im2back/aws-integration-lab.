
package com.github.im2back.aws_integration_lab.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.github.im2back.aws_integration_lab.controller.dto.out.S3UploadResponse;
import com.github.im2back.aws_integration_lab.controller.mapper.out.S3RestMapper;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Serviço responsável pelas operações de integração com o Amazon S3.
 *
 * Utiliza o AWS SDK for Java 2.x para realizar operações de
 * armazenamento de objetos no bucket configurado.
 *
 * Durante o desenvolvimento, as operações são direcionadas ao
 * LocalStack. Em produção, poderão ser executadas no Amazon S3 real.
 *
 * A autenticação é gerenciada pelo S3Client configurado na S3Config,
 * que utiliza credenciais temporárias obtidas por meio do AWS STS.
 */
@Service
public class S3Service {

    private final S3Client s3Client;
    private final String bucketName;

    /**
     * Inicializa o serviço com as dependências necessárias.
     *
     * @param s3Client cliente responsável pela comunicação com o S3.
     * @param bucketName nome do bucket utilizado para armazenar os arquivos.
     */
    public S3Service(S3Client s3Client, @Value("${aws.s3.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    /**
     * Realiza o upload de um arquivo para o Amazon S3.
     *
     * Gera uma chave única para evitar que arquivos com o mesmo nome
     * sobrescrevam objetos anteriormente armazenados.
     *
     * O conteúdo é enviado por meio da operação PutObject do AWS SDK.
     *
     * @param file arquivo recebido pela API REST.
     * @return chave do objeto armazenado no bucket S3.
     * @throws IllegalArgumentException se o arquivo estiver vazio.
     * @throws IllegalStateException se ocorrer uma falha na leitura do arquivo.
     */
    public S3UploadResponse upload(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo não pode estar vazio.");
        }

        String originalFilename = file.getOriginalFilename();

        String extension = "";
        if (originalFilename != null && originalFilename.lastIndexOf('.') > 0) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }

        String objectKey = "uploads/" + UUID.randomUUID() + extension;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(file.getContentType() != null
                        ? file.getContentType()
                        : "application/octet-stream")
                .build();

        try {
        	s3Client.putObject(
        	        request	,
        	        RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        	);

        	return S3RestMapper.toUploadResponse(request);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao ler o arquivo para upload.", e);
        }
    }
}
