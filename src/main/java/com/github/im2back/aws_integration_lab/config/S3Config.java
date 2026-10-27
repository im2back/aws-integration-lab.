
package com.github.im2back.aws_integration_lab.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

/**
 * Configura os clientes AWS necessários para a integração com o Amazon S3.
 *
 * No ambiente local, utiliza o LocalStack para simular S3, IAM e STS.
 *
 * Neste laboratório, utilizamos credenciais iniciais fictícias para
 * autenticar a chamada STS AssumeRole e obter credenciais temporárias.
 * Essas credenciais temporárias são utilizadas pelo S3Client.
 *
 * Em ambientes corporativos, normalmente não é necessário manter
 * Access Key e Secret Key permanentes na aplicação.
 *
 * A identidade pode ser fornecida pela própria infraestrutura AWS:
 *
 * - EC2: uma IAM Role associada à instância por um Instance Profile.
 * - EKS: uma IAM Role associada ao workload, utilizando mecanismos
 *   como EKS Pod Identity ou IRSA.
 *
 * Nesses cenários, o AWS SDK pode obter e renovar automaticamente
 * as credenciais temporárias pela cadeia padrão de credenciais,
 * sem precisar de chaves fixas no código ou no application.properties.
 *
 * Portanto, a utilização explícita de STS AssumeRole nesta classe
 * faz parte do laboratório e poderá ser simplificada no futuro
 * quando a aplicação for implantada no Amazon EKS.
 */
@Configuration
public class S3Config {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.s3.endpoint:}")
    private String s3Endpoint;

    @Value("${aws.sts.endpoint:}")
    private String stsEndpoint;

    @Value("${aws.sts.role-arn}")
    private String roleArn;

    @Value("${aws.sts.role-session-name}")
    private String roleSessionName;

    @Value("${aws.access-key:}")
    private String accessKey;

    @Value("${aws.secret-key:}")
    private String secretKey;

    /**
     * Cria o cliente responsável pela comunicação com o AWS STS.
     *
     * No LocalStack, utiliza credenciais fictícias iniciais.
     *
     * Na AWS real, quando não há endpoint local configurado,
     * utiliza a cadeia padrão de credenciais do AWS SDK.
     *
     * @return cliente STS configurado.
     */
    @Bean
    public StsClient stsClient() {
        var builder = StsClient.builder()
                .region(Region.of(region));

        if (!stsEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(stsEndpoint))
                    .credentialsProvider(
                            StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(accessKey, secretKey)));
        }

        return builder.build();
    }

    /**
     * Cria o provedor responsável pela obtenção de credenciais temporárias.
     *
     * Utiliza o AWS STS para assumir a IAM Role configurada.
     *
     * O SDK mantém as credenciais em cache e realiza sua renovação
     * quando necessário.
     *
     * Em uma aplicação no EKS com Pod Identity ou IRSA, normalmente
     * seria possível utilizar diretamente a cadeia padrão do SDK,
     * sem realizar este AssumeRole explícito.
     *
     * @param stsClient cliente responsável pela comunicação com o STS.
     * @return provedor de credenciais temporárias da IAM Role.
     */
    @Bean(destroyMethod = "close")
    public StsAssumeRoleCredentialsProvider s3CredentialsProvider(StsClient stsClient) {
        AssumeRoleRequest request = AssumeRoleRequest.builder()
                .roleArn(roleArn)
                .roleSessionName(roleSessionName)
                .build();

        return StsAssumeRoleCredentialsProvider.builder()
                .stsClient(stsClient)
                .refreshRequest(request)
                .build();
    }

    /**
     * Cria o cliente responsável pelas operações com Amazon S3.
     *
     * Utiliza as credenciais temporárias obtidas pelo STS.
     *
     * Quando um endpoint local está configurado, direciona
     * as requisições para o LocalStack.
     *
     * @param credentialsProvider provedor de credenciais temporárias.
     * @return cliente Amazon S3 configurado.
     */
    @Bean
    public S3Client s3Client(StsAssumeRoleCredentialsProvider credentialsProvider) {
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider);

        if (!s3Endpoint.isBlank()) {
            builder.endpointOverride(URI.create(s3Endpoint))
                    .forcePathStyle(true);
        }

        return builder.build();
    }
}
