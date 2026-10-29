package com.github.im2back.aws_integration_lab.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.github.im2back.aws_integration_lab.controller.dto.out.S3UploadResponse;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    private String bucketName;

    private S3Service service;
    
    @Captor
    private ArgumentCaptor<PutObjectRequest> requestCaptor;

    @Captor
    private ArgumentCaptor<RequestBody> bodyCaptor;
    
    @BeforeEach
    void setUp() {
        bucketName = "aws-integration-lab";
        service = new S3Service(s3Client, bucketName);
    }
	@Test
	@DisplayName("")
	void uploadCenario01() throws IOException {
		//ARRANGE
	    MultipartFile file = new MockMultipartFile(
	            "file",
	            "documento.md",
	            "text/markdown",
	            "Conteúdo do arquivo".getBytes(StandardCharsets.UTF_8)
	            
	    );
	    
	    PutObjectResponse response = PutObjectResponse.builder()
	            .eTag("\"abc123456\"")
	            .versionId("version-001")
	            .build();

	    BDDMockito.when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(response);
	    
		//ACT
	    S3UploadResponse result = service.upload(file);
	    

	    // ASSERT

	    // 1. Verifica a chamada ao S3 e captura os argumentos
	    verify(s3Client, times(1)).putObject(
	            requestCaptor.capture(),
	            bodyCaptor.capture()
	    );

	    PutObjectRequest capturedRequest = requestCaptor.getValue();
	    RequestBody capturedBody = bodyCaptor.getValue();

	    // 2. Valida os argumentos capturados do putObject()

	    // PutObjectRequest
	    assertAll("Validação do PutObjectRequest",
	            () -> assertEquals(bucketName, capturedRequest.bucket(), "O bucket deve ser o configurado na Service"),
	            () -> assertEquals(file.getContentType(), capturedRequest.contentType(), "O Content-Type deve vir do MultipartFile"),
	            () -> assertTrue(capturedRequest.key().startsWith("uploads/"), "A key deve começar com uploads/"),
	            () -> assertTrue(capturedRequest.key().endsWith(".md"), "A key deve preservar a extensão do arquivo")
	    );

	    // RequestBody
	    assertAll("Validação do RequestBody",
	            () -> assertEquals(file.getSize(), capturedBody.contentLength(), "O tamanho deve corresponder ao arquivo original"),
	            () -> assertArrayEquals(file.getBytes(),
	                    capturedBody.contentStreamProvider().newStream().readAllBytes(),
	                    "O conteúdo enviado deve corresponder aos bytes do arquivo original")
	    );

	    // 3. Valida o DTO retornado pela Service
	    assertAll("Validação do S3UploadResponse",
	            () -> assertEquals(bucketName, result.bucket(), "O DTO deve retornar o bucket correto"),
	            () -> assertEquals("Arquivo enviado com sucesso.", result.message(), "A mensagem de sucesso deve estar correta"),
	            () -> assertEquals("uploads/", result.path(), "O path deve corresponder ao prefixo do objeto"),
	            () -> assertEquals(capturedRequest.key(), result.key(), "A key retornada deve ser a mesma enviada ao S3")
	    );

	}

}
