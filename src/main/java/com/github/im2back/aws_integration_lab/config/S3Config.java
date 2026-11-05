
package com.github.im2back.aws_integration_lab.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Configura o cliente Amazon S3.
 *
 * Utiliza as credenciais de um usuário IAM para autenticar
 * as requisições à AWS.
 *
 * O endpoint personalizado é opcional e permite utilizar
 * o LocalStack nos testes automatizados.
 *
 * Em uma implantação futura na EC2 ou EKS, poderemos
 * substituir o provedor estático de credenciais pela
 * cadeia padrão do AWS SDK, utilizando IAM Roles.
 */
@Configuration
public class S3Config {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.s3.endpoint:}")
    private String s3Endpoint;

    @Value("${aws.access-key}")
    private String accessKey;

    @Value("${aws.secret-key}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials));

        if (!s3Endpoint.isBlank()) {
            builder.endpointOverride(URI.create(s3Endpoint))
                    .forcePathStyle(true);
        }

        return builder.build();
    }
}
