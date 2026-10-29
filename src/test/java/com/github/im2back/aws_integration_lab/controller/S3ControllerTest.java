
package com.github.im2back.aws_integration_lab.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import com.github.im2back.aws_integration_lab.controller.dto.out.S3UploadResponse;
import com.github.im2back.aws_integration_lab.service.S3Service;

import jakarta.servlet.ServletException;

@WebMvcTest(S3Controller.class)
@ExtendWith(MockitoExtension.class)
class S3ControllerTest {

    @MockitoBean
    private S3Service s3Service;

    @Autowired
    private MockMvc mvc;

    @Captor
    private ArgumentCaptor<MultipartFile> fileCaptor;

    private static final String ENDPOINT = "/api/v1/s3/files";

    @Test
    @DisplayName("Deve retornar HTTP 201 e os dados do upload quando o arquivo for válido")
    void shouldReturnCreatedWhenFileIsValid() throws Exception {

        // ARRANGE
        byte[] content = "Conteúdo do arquivo".getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "documento.md",
                "text/markdown",
                content
        );

        S3UploadResponse expectedResponse = new S3UploadResponse(
                "aws-integration-lab",
                "Arquivo enviado com sucesso.",
                "uploads/",
                "uploads/arquivo-123.md"
        );

        when(s3Service.upload(any(MultipartFile.class)))
                .thenReturn(expectedResponse);

        // ACT - Valida o contrato HTTP
        mvc.perform(multipart(ENDPOINT)
                .file(file))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bucket").value(expectedResponse.bucket()))
                .andExpect(jsonPath("$.message").value(expectedResponse.message()))
                .andExpect(jsonPath("$.path").value(expectedResponse.path()))
                .andExpect(jsonPath("$.key").value(expectedResponse.key()));

        // ASSERT

        // 1. Verifica a chamada à Service e captura o argumento
        verify(s3Service, times(1)).upload(fileCaptor.capture());

        MultipartFile capturedFile = fileCaptor.getValue();

        // 2. Valida o MultipartFile encaminhado à Service
        assertAll("Validação do MultipartFile",
                () -> assertEquals(file.getName(), capturedFile.getName(), "O nome do campo deve ser preservado"),
                () -> assertEquals(file.getOriginalFilename(), capturedFile.getOriginalFilename(), "O nome original deve ser preservado"),
                () -> assertEquals(file.getContentType(), capturedFile.getContentType(), "O Content-Type deve ser preservado"),
                () -> assertEquals(file.getSize(), capturedFile.getSize(), "O tamanho do arquivo deve ser preservado"),
                () -> assertArrayEquals(file.getBytes(), capturedFile.getBytes(), "Os bytes do arquivo devem ser preservados")
        );

        // 3. Verifica se não houve outras chamadas à Service
        verifyNoMoreInteractions(s3Service);
    }

    @Test
    @DisplayName("Deve retornar HTTP 400 quando o parâmetro file não for informado")
    void shouldReturnBadRequestWhenFileIsMissing() throws Exception {

        // ARRANGE
        // Requisição multipart sem o parâmetro obrigatório "file".

        // ACT - Valida o contrato HTTP
        mvc.perform(multipart(ENDPOINT))
                .andExpect(status().isBadRequest());

        // ASSERT

        // 1. Verifica se a Service não foi acionada
        verifyNoInteractions(s3Service);
    }

    @Test
    @DisplayName("Deve retornar HTTP 415 quando o Content-Type da requisição for incompatível")
    void shouldReturnUnsupportedMediaTypeWhenContentTypeIsInvalid() throws Exception {

        // ARRANGE
        String jsonRequest = """
                {
                    "file": "documento.md"
                }
                """;

        // ACT - Valida o contrato HTTP
        mvc.perform(post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isUnsupportedMediaType());

        // ASSERT

        // 1. Verifica se a Service não foi acionada
        verifyNoInteractions(s3Service);
    }

    @Test
    @DisplayName("Deve retornar HTTP 405 quando uma requisição GET for enviada ao endpoint de upload")
    void shouldReturnMethodNotAllowedWhenRequestUsesGet() throws Exception {

        // ARRANGE
        // O endpoint de upload aceita somente requisições POST.

        // ACT - Valida o contrato HTTP
        mvc.perform(get(ENDPOINT))
                .andExpect(status().isMethodNotAllowed());

        // ASSERT

        // 1. Verifica se a Service não foi acionada
        verifyNoInteractions(s3Service);
    }

    @Test
    @DisplayName("Deve propagar IllegalArgumentException quando a Service rejeitar o arquivo")
    void shouldPropagateExceptionWhenServiceRejectsFile() {

        // ARRANGE
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "documento.md",
                "text/markdown",
                new byte[0]
        );

        IllegalArgumentException originalException =
                new IllegalArgumentException("O arquivo não pode estar vazio.");

        when(s3Service.upload(any(MultipartFile.class)))
                .thenThrow(originalException);

        // ACT
        ServletException exception = assertThrows(
                ServletException.class,
                () -> mvc.perform(multipart(ENDPOINT).file(file))
        );

        // ASSERT

        // 1. Verifica a chamada à Service e captura o argumento
        verify(s3Service, times(1)).upload(fileCaptor.capture());

        MultipartFile capturedFile = fileCaptor.getValue();

        // 2. Valida o MultipartFile encaminhado à Service
        assertAll("Validação do MultipartFile",
                () -> assertEquals(file.getOriginalFilename(), capturedFile.getOriginalFilename(), "O nome original deve ser preservado"),
                () -> assertTrue(capturedFile.isEmpty(), "O arquivo encaminhado deve estar vazio")
        );

        // 3. Valida a exceção propagada
        assertAll("Validação da exceção",
                () -> assertSame(originalException, exception.getCause(), "Deve preservar a exceção original da Service"),
                () -> assertEquals("O arquivo não pode estar vazio.", exception.getCause().getMessage(), "A mensagem de erro deve ser preservada")
        );

        // 4. Verifica se não houve outras chamadas à Service
        verifyNoMoreInteractions(s3Service);
    }
}
