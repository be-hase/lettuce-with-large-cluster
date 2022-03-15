package com.example.demo

import io.lettuce.core.RedisURI
import io.lettuce.core.cluster.ClusterClientOptions
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions
import io.lettuce.core.cluster.RedisClusterClient
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands
import io.lettuce.core.resource.ClientResources
import io.lettuce.core.resource.DefaultClientResources
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class DemoApplication

private val enableRefresh = System.getenv()["ENABLE_REFRESH"] == "true"

fun main(args: Array<String>) {
    println("ENABLE_REFRESH: $enableRefresh")

    runApplication<DemoApplication>(*args)
}

@Configuration(proxyBeanMethods = false)
class DemoConfiguration {
    @Bean(destroyMethod = "shutdown")
    fun clientResources(): ClientResources {
        return DefaultClientResources.builder()
            .build()
    }

    @Bean(destroyMethod = "shutdown")
    fun redisClusterClient(
        clientResources: ClientResources
    ): RedisClusterClient {
        val topologyRefreshOptionsBuilder = ClusterTopologyRefreshOptions.builder()
        if (enableRefresh) {
            // If you run it at short intervals, the problem will be noticeable. This time we will use the default settings.
            topologyRefreshOptionsBuilder.enablePeriodicRefresh()
        }

        val clusterClientOptions = ClusterClientOptions.builder()
            .topologyRefreshOptions(topologyRefreshOptionsBuilder.build())
            .build()

        val redisURI = RedisURI.builder()
            .withHost("localhost")
            .withPort(17000)
            .build()

        return RedisClusterClient.create(clientResources, redisURI)
            .apply {
                setOptions(clusterClientOptions)
            }
    }

    @Bean(destroyMethod = "close")
    fun redisClusterConnection(
        redisClusterClient: RedisClusterClient
    ): StatefulRedisClusterConnection<String, String> {
        return redisClusterClient.connect()
    }

    @Bean
    fun redisAdvancedClusterCommands(
        redisClusterConnection: StatefulRedisClusterConnection<String, String>
    ): RedisAdvancedClusterCommands<String, String> {
        return redisClusterConnection.sync()
    }
}

@RestController
class DemoController(
    private val redis: RedisAdvancedClusterCommands<String, String>
) {
    // A simple endpoint that just exec SET
    @GetMapping("/test")
    fun test(): String {
        return redis.set("hoge", "hoge")
    }
}
