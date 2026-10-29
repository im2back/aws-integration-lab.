
package com.github.im2back.aws_integration_lab.integration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import com.jayway.jsonpath.JsonPath;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.localstack.LocalStackContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class S3UploadIntegrationTest {

    @Autowired
    private MockMvc mvc;

    //Dependência utilizada na assertiva
    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;
    
    private static final String ENDPOINT = "/api/v1/s3/files";
    
	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
	        DockerImageName.parse("localstack/localstack:2026.10.3")
	)
	        .withEnv("LOCALSTACK_AUTH_TOKEN", "ls-BILU4624-WUmu-5933-giYI-RACEDILEef68")
	        .withEnv("SERVICES", "s3,iam,sts")
	        .withCopyFileToContainer(
	                MountableFile.forClasspathResource("localstack/init-aws.sh", 0755),
	                "/etc/localstack/init/ready.d/init-aws.sh"
	        )
	        .waitingFor(
	                Wait.forLogMessage(
	                        ".*Recursos AWS de teste criados com sucesso!.*\\n",
	                        1
	                )
	        );
	
	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
	
	    registry.add("aws.s3.endpoint",
	            () -> localstack.getEndpoint().toString());
	
	    registry.add("aws.sts.endpoint",
	            () -> localstack.getEndpoint().toString());
	}

    @Test
    @DisplayName("Deve armazenar o arquivo no S3 quando o upload for realizado pelo Controller")
    void shouldStoreFileInS3WhenUploadedThroughController() throws Exception {

        // ARRANGE
        byte[] content = "Teste de integração com S3".getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "integracao.md",
                "text/markdown",
                content
        );

        String objectKey = null;

        try {
            // ACT - Executa o endpoint real
            MvcResult result = mvc.perform(multipart(ENDPOINT).file(file))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.bucket").value(bucketName))
                    .andExpect(jsonPath("$.message").value("Arquivo enviado com sucesso."))
                    .andExpect(jsonPath("$.path").value("uploads/"))
                    .andExpect(jsonPath("$.key").exists())
                    .andReturn();

            objectKey = JsonPath.read(
                    result.getResponse().getContentAsString(),
                    "$.key"
            );

            // ASSERT

            // 1. Consulta o arquivo realmente armazenado no S3
            byte[] storedContent = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build()
            ).asByteArray();

            // 2. Valida o conteúdo armazenado no S3
            String uploadedKey = objectKey;

            assertAll("Validação da integração com S3",
                    () -> assertTrue(uploadedKey.startsWith("uploads/"), "A key deve começar com uploads/"),
                    () -> assertTrue(uploadedKey.endsWith(".md"), "A extensão original deve ser preservada"),
                    () -> assertArrayEquals(content, storedContent, "O conteúdo armazenado deve corresponder ao arquivo enviado")
            );

        } finally {
            // CLEANUP - Remove o objeto criado pelo teste
            if (objectKey != null) {
                s3Client.deleteObject(
                        DeleteObjectRequest.builder()
                                .bucket(bucketName)
                                .key(objectKey)
                                .build()
                );
            }
        }
    }
}
