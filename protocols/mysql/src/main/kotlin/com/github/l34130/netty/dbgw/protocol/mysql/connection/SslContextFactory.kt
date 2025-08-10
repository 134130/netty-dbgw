package com.github.l34130.netty.dbgw.protocol.mysql.connection

import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import java.io.InputStream

object SslContextFactory {
    val serverSslContext: SslContext by lazy {
        SslContextBuilder
            .forServer(
                getCertificate(),
                getPrivateKey(),
            ).build()
    }

    val clientSslContext: SslContext by lazy {
        SslContextBuilder
            .forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build()
    }

    private fun getCertificate(): InputStream = this.javaClass.classLoader.getResourceAsStream("certificate.pem")

    private fun getPrivateKey(): InputStream = this.javaClass.classLoader.getResourceAsStream("private.key")
}
