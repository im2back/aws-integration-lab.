package com.github.im2back.aws_integration_lab.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

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
import software.amazon.awssdk.services.s3.model.S3Exception;

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
	@DisplayName("Deve enviar o arquivo ao S3 e retornar os dados corretamente quando o arquivo for válido")
	void shouldUploadFileWhenFileIsValid()  throws IOException {
		
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

	@Test
	@DisplayName("Deve lançar IllegalArgumentException quando o arquivo for nulo")
	void shouldThrowExceptionWhenFileIsNull() {
	
	    // ARRANGE
	    MultipartFile file = null;
	
	    // ACT
	    IllegalArgumentException exception = assertThrows(
	            IllegalArgumentException.class,
	            () -> service.upload(file)
	    );
	
	    // ASSERT
	
	    // 1. Valida a exceção lançada pela Service
	    assertAll("Validação da exceção",
	            () -> assertEquals("O arquivo não pode estar vazio.", exception.getMessage(), "Deve retornar a mensagem esperada")
	    );
	
	    // 2. Verifica se nenhuma chamada foi realizada ao S3
	    verifyNoInteractions(s3Client);
	}
	
	@Test
	@DisplayName("Deve lançar IllegalArgumentException quando o arquivo estiver vazio")
	void shouldThrowExceptionWhenFileIsEmpty() {
	
	    // ARRANGE
	    MultipartFile file = new MockMultipartFile(
	            "file",
	            "documento.md",
	            "text/markdown",
	            new byte[0]
	    );
	
	    // ACT
	    IllegalArgumentException exception = assertThrows(
	            IllegalArgumentException.class,
	            () -> service.upload(file)
	    );
	
	    // ASSERT
	
	    // 1. Valida a exceção lançada pela Service
	    assertAll("Validação da exceção",
	            () -> assertEquals("O arquivo não pode estar vazio.", exception.getMessage(), "Deve retornar a mensagem esperada")
	    );
	
	    // 2. Verifica se nenhuma chamada foi realizada ao S3
	    verifyNoInteractions(s3Client);
	}
	
	@Test
	@DisplayName("Deve realizar o upload sem extensão quando o nome original do arquivo for nulo")
	void shouldUploadFileWhenOriginalFilenameIsNull() throws IOException {
	
	    // ARRANGE
	    MultipartFile file = new MockMultipartFile(
	            "file",
	            null,
	            "text/plain",
	            "Conteúdo do arquivo".getBytes(StandardCharsets.UTF_8)
	    );
	
	    PutObjectResponse response = PutObjectResponse.builder().build();
	
	    when(s3Client.putObject(
	            any(PutObjectRequest.class),
	            any(RequestBody.class)
	    )).thenReturn(response);
	
	    // ACT
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
	            () -> assertDoesNotThrow(() -> UUID.fromString(capturedRequest.key().substring("uploads/".length())), "A key deve conter somente um UUID válido após o prefixo")
	    );
	
	    // RequestBody
	    assertAll("Validação do RequestBody",
	            () -> assertEquals(file.getSize(), capturedBody.contentLength(), "O tamanho deve corresponder ao arquivo original"),
	            () -> assertArrayEquals(file.getBytes(),
	                    capturedBody.contentStreamProvider().newStream().readAllBytes(),
	                    "O conteúdo enviado deve corresponder ao arquivo original")
	    );
	
	    // 3. Valida o DTO retornado pela Service
	    assertAll("Validação do S3UploadResponse",
	            () -> assertEquals(bucketName, result.bucket(), "O DTO deve retornar o bucket correto"),
	            () -> assertEquals("Arquivo enviado com sucesso.", result.message(), "A mensagem de sucesso deve estar correta"),
	            () -> assertEquals("uploads/", result.path(), "O path deve corresponder ao prefixo"),
	            () -> assertEquals(capturedRequest.key(), result.key(), "A key retornada deve ser a mesma enviada ao S3")
	    );
	}
	
	@Test
	@DisplayName("Deve realizar o upload sem extensão quando o nome original não possuir extensão")
	void shouldUploadFileWhenOriginalFilenameHasNoExtension() throws IOException {
	
	    // ARRANGE
	    MultipartFile file = new MockMultipartFile(
	            "file",
	            "documento",
	            "text/plain",
	            "Conteúdo do arquivo".getBytes(StandardCharsets.UTF_8)
	    );
	
	    PutObjectResponse response = PutObjectResponse.builder().build();
	
	    when(s3Client.putObject(
	            any(PutObjectRequest.class),
	            any(RequestBody.class)
	    )).thenReturn(response);
	
	    // ACT
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
	            () -> assertDoesNotThrow(() -> UUID.fromString(capturedRequest.key().substring("uploads/".length())), "A key não deve conter extensão")
	    );
	
	    // RequestBody
	    assertAll("Validação do RequestBody",
	            () -> assertEquals(file.getSize(), capturedBody.contentLength(), "O tamanho deve corresponder ao arquivo original"),
	            () -> assertArrayEquals(file.getBytes(),
	                    capturedBody.contentStreamProvider().newStream().readAllBytes(),
	                    "O conteúdo enviado deve corresponder ao arquivo original")
	    );
	
	    // 3. Valida o DTO retornado pela Service
	    assertAll("Validação do S3UploadResponse",
	            () -> assertEquals(bucketName, result.bucket(), "O DTO deve retornar o bucket correto"),
	            () -> assertEquals("Arquivo enviado com sucesso.", result.message(), "A mensagem de sucesso deve estar correta"),
	            () -> assertEquals("uploads/", result.path(), "O path deve corresponder ao prefixo"),
	            () -> assertEquals(capturedRequest.key(), result.key(), "A key retornada deve ser a mesma enviada ao S3")
	    );
	}
	
	@Test
	@DisplayName("Deve utilizar application/octet-stream quando o Content-Type do arquivo for nulo")
	void shouldUseDefaultContentTypeWhenContentTypeIsNull() throws IOException {
	
	    // ARRANGE
	    MultipartFile file = new MockMultipartFile(
	            "file",
	            "documento.md",
	            null,
	            "Conteúdo do arquivo".getBytes(StandardCharsets.UTF_8)
	    );
	
	    PutObjectResponse response = PutObjectResponse.builder().build();
	
	    when(s3Client.putObject(
	            any(PutObjectRequest.class),
	            any(RequestBody.class)
	    )).thenReturn(response);
	
	    // ACT
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
	            () -> assertEquals("application/octet-stream", capturedRequest.contentType(), "Deve utilizar o Content-Type padrão"),
	            () -> assertTrue(capturedRequest.key().startsWith("uploads/"), "A key deve começar com uploads/"),
	            () -> assertTrue(capturedRequest.key().endsWith(".md"), "A key deve preservar a extensão do arquivo")
	    );
	
	    // RequestBody
	    assertAll("Validação do RequestBody",
	            () -> assertEquals(file.getSize(), capturedBody.contentLength(), "O tamanho deve corresponder ao arquivo original"),
	            () -> assertArrayEquals(file.getBytes(),
	                    capturedBody.contentStreamProvider().newStream().readAllBytes(),
	                    "O conteúdo enviado deve corresponder ao arquivo original")
	    );
	
	    // 3. Valida o DTO retornado pela Service
	    assertAll("Validação do S3UploadResponse",
	            () -> assertEquals(bucketName, result.bucket(), "O DTO deve retornar o bucket correto"),
	            () -> assertEquals("Arquivo enviado com sucesso.", result.message(), "A mensagem de sucesso deve estar correta"),
	            () -> assertEquals("uploads/", result.path(), "O path deve corresponder ao prefixo"),
	            () -> assertEquals(capturedRequest.key(), result.key(), "A key retornada deve ser a mesma enviada ao S3")
	    );
	}
	
	@Test
	@DisplayName("Deve lançar IllegalStateException quando ocorrer uma falha na leitura do arquivo")
	void shouldThrowIllegalStateExceptionWhenFileReadFails() throws IOException {
	
	    // ARRANGE
	    MultipartFile file = mock(MultipartFile.class);
	
	    IOException originalException = new IOException("Falha simulada na leitura.");
	
	    when(file.isEmpty()).thenReturn(false);
	    when(file.getOriginalFilename()).thenReturn("documento.md");
	    when(file.getInputStream()).thenThrow(originalException);
	
	    // ACT
	    IllegalStateException exception = assertThrows(
	            IllegalStateException.class,
	            () -> service.upload(file)
	    );
	
	    // ASSERT
	
	    // 1. Valida a exceção lançada pela Service
	    assertAll("Validação da exceção",
	            () -> assertEquals("Erro ao ler o arquivo para upload.", exception.getMessage(), "Deve retornar a mensagem de erro esperada"),
	            () -> assertSame(originalException, exception.getCause(), "Deve preservar a IOException original como causa")
	    );
	
	    // 2. Verifica se nenhuma chamada foi realizada ao S3
	    verifyNoInteractions(s3Client);
	}
	
	@Test
	@DisplayName("Deve propagar a exceção quando ocorrer uma falha na operação de upload do S3")
	void shouldPropagateExceptionWhenS3UploadFails() throws IOException {
	
	    // ARRANGE
	    MultipartFile file = new MockMultipartFile(
	            "file",
	            "documento.md",
	            "text/markdown",
	            "Conteúdo do arquivo".getBytes(StandardCharsets.UTF_8)
	    );
	
	    S3Exception originalException = (S3Exception) S3Exception.builder()
	            .message("Falha simulada no S3.")
	            .statusCode(500)
	            .build();
	
	    when(s3Client.putObject(
	            any(PutObjectRequest.class),
	            any(RequestBody.class)
	    )).thenThrow(originalException);
	
	    // ACT
	    S3Exception exception = assertThrows(
	            S3Exception.class,
	            () -> service.upload(file)
	    );
	
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
	                    "O conteúdo enviado deve corresponder ao arquivo original")
	    );
	
	    // 3. Valida a exceção propagada pela Service
	    assertAll("Validação da exceção do S3",
	            () -> assertSame(originalException, exception, "Deve propagar a mesma exceção lançada pelo S3Client"),
	            () -> assertEquals(500, exception.statusCode(), "Deve preservar o código HTTP da falha")
	    );
	}

}
