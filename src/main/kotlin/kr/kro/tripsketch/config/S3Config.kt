package kr.kro.tripsketch.config

import java.net.URI
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class S3Config {

    @Value("\${aws.accessKeyId}")
    lateinit var accessKey: String

    @Value("\${aws.secretAccessKey}")
    lateinit var secretKey: String

    @Value("\${aws.region}")
    lateinit var region: String

    @Bean
    fun s3Client(): S3Client {
        return S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider {
                AwsBasicCredentials.create(accessKey, secretKey)
            }
            .endpointOverride(URI.create("https://ax6izwmsuv9c.compat.objectstorage.ap-osaka-1.oraclecloud.com"))
            .build()
    }
}