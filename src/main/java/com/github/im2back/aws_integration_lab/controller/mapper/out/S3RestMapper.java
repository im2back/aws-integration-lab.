package com.github.im2back.aws_integration_lab.controller.mapper.out;


import com.github.im2back.aws_integration_lab.controller.dto.out.S3UploadResponse;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Mapper responsável por converter os dados das operações
 * do Amazon S3 em DTOs de resposta da API REST.
 *
 * Centraliza o mapeamento dos dados utilizados na integração,
 * evitando a construção de DTOs diretamente na camada de serviço.
 */
public final class S3RestMapper {

    private S3RestMapper() {
    }

    /**
     * Converte os dados de uma requisição de upload ao S3
     * em um DTO de resposta da aplicação.
     *
     * @param request requisição utilizada no upload do objeto.
     * @return DTO contendo bucket, mensagem, path e key.
     */
    public static S3UploadResponse toUploadResponse(PutObjectRequest request) {

        String key = request.key();

        String path = key.contains("/")
                ? key.substring(0, key.lastIndexOf('/') + 1)
                : "";

        return new S3UploadResponse(
                request.bucket(),
                "Arquivo enviado com sucesso.",
                path,
                key
        );
    }
}
