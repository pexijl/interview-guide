package interview.guide.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * S3客户端配置（用于RustFS）
 * 该类负责配置并初始化连接到S3兼容存储服务（如RustFS、MinIO等）的客户端实例
 */
@Configuration // 声明该类是一个配置类，会被Spring容器扫描并管理
@RequiredArgsConstructor // Lombok注解：生成包含final字段的构造函数，用于依赖注入
public class S3Config {

    // 使用@RequiredArgsConstructor 构造器注入
    // 定义不可变的存储配置属性对象，通过构造函数注入，保证了配置的安全性
    private final StorageConfigProperties storageConfig;

    /**
     * 创建并配置S3Client Bean
     *
     * @return 配置好的S3Client实例
     */
    @Bean // 声明该方法返回一个由Spring管理的Bean
    public S3Client s3Client() {
        // 1. 创建AWS基础认证凭证
        // 从配置对象中获取AccessKey和SecretKey创建认证对象
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                storageConfig.getAccessKey(), // 获取访问密钥
                storageConfig.getSecretKey() // 获取私钥
        );

        // 2. 构建并返回S3Client实例
        return S3Client.builder()
                // 设置服务端点地址（覆盖默认的AWS地址），支持自定义的RustFS服务地址
                .endpointOverride(URI.create(storageConfig.getEndpoint()))
                // 设置区域信息，S3协议需要区域配置，即使是私有部署也需要指定一个区域标识
                .region(Region.of(storageConfig.getRegion()))
                // 设置凭证提供者，使用刚才创建的静态凭证（AccessKey/SecretKey）
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                // 关键配置：开启路径风格访问
                // 设置为true：URL格式为 http://endpoint/bucket/object (路径风格)
                // 如果不设置（默认false）：URL格式为 http://bucket.endpoint/object (虚拟主机风格)
                // 对于私有化部署（如RustFS/MinIO），通常DNS不支持泛域名解析，必须开启此项，否则会导致DNS解析失败
                .forcePathStyle(true)
                // 构建最终的S3Client对象
                .build();
    }
}
