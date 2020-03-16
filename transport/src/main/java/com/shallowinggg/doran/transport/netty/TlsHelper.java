/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shallowinggg.doran.transport.netty;

import com.shallowinggg.doran.transport.common.RemotingHelper;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.util.Properties;

import static com.shallowinggg.doran.transport.netty.TlsSystemConfig.*;

public class TlsHelper {

    public interface DecryptionStrategy {
        /**
         * Decrypt the target encrpted private key file.
         *
         * @param privateKeyEncryptPath A pathname string
         * @param forClient             tells whether it's a client-side key file
         * @return An input stream for a decrypted key file
         * @throws IOException if an I/O error has occurred
         */
        InputStream decryptPrivateKey(String privateKeyEncryptPath, boolean forClient) throws IOException;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(RemotingHelper.DORAN_REMOTING);

    private static DecryptionStrategy decryptionStrategy = (privateKeyEncryptPath, forClient) ->
            new FileInputStream(privateKeyEncryptPath);


    public static void registerDecryptionStrategy(final DecryptionStrategy decryptionStrategy) {
        TlsHelper.decryptionStrategy = decryptionStrategy;
    }

    public static SslContext buildSslContext(boolean forClient) throws IOException, CertificateException {
        //     /etc/rocketmq/tls.properties
        //  默认此文件不存在
        File configFile = new File(TlsSystemConfig.tlsConfigFile);
        extractTlsConfigFromFile(configFile);
        // 日志记录最终的配置信息
        logTheFinalUsedTlsConfig();

        SslProvider provider;
        if (OpenSsl.isAvailable()) {
            provider = SslProvider.OPENSSL;
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Using OpenSSL provider");
            }
        } else {
            provider = SslProvider.JDK;
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Using JDK SSL provider");
            }
        }

        // false
        if (forClient) {
            if (tlsTestModeEnable) {
                return SslContextBuilder
                        .forClient()
                        .sslProvider(SslProvider.JDK)
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
            } else {
                SslContextBuilder sslContextBuilder = SslContextBuilder.forClient().sslProvider(SslProvider.JDK);


                if (!tlsClientAuthServer) {
                    sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
                } else {
                    if (isNotEmpty(tlsClientTrustCertPath)) {
                        sslContextBuilder.trustManager(new File(tlsClientTrustCertPath));
                    }
                }

                return sslContextBuilder.keyManager(
                        isNotEmpty(tlsClientCertPath) ? new FileInputStream(tlsClientCertPath) : null,
                        isNotEmpty(tlsClientKeyPath) ? decryptionStrategy.decryptPrivateKey(tlsClientKeyPath, true) : null,
                        isNotEmpty(tlsClientKeyPassword) ? tlsClientKeyPassword : null)
                        .build();
            }
        } else {
            // tls.test.mode.enable 系统变量，默认为true
            if (tlsTestModeEnable) {
                // 使用netty内建的ssl context
                SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
                return SslContextBuilder
                        .forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey())
                        .sslProvider(SslProvider.JDK)
                        .clientAuth(ClientAuth.OPTIONAL)
                        .build();
            } else {
                SslContextBuilder sslContextBuilder = SslContextBuilder.forServer(
                        isNotEmpty(tlsServerCertPath) ? new FileInputStream(tlsServerCertPath) : null,
                        isNotEmpty(tlsServerKeyPath) ? decryptionStrategy.decryptPrivateKey(tlsServerKeyPath, false) : null,
                        isNotEmpty(tlsServerKeyPassword) ? tlsServerKeyPassword : null)
                        .sslProvider(provider);

                if (!tlsServerAuthClient) {
                    sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
                } else {
                    if (isNotEmpty(tlsServerTrustCertPath)) {
                        sslContextBuilder.trustManager(new File(tlsServerTrustCertPath));
                    }
                }

                sslContextBuilder.clientAuth(parseClientAuthMode(tlsServerNeedClientAuth));
                return sslContextBuilder.build();
            }
        }
    }

    private static void extractTlsConfigFromFile(final File configFile) {
        if (!(configFile.exists() && configFile.isFile() && configFile.canRead())) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Tls config file doesn't exist, skip it");
            }
            return;
        }

        Properties properties;
        properties = new Properties();
        try (InputStream inputStream = new FileInputStream(configFile)) {
            properties.load(inputStream);
        } catch (IOException ignore) {
        }

        tlsTestModeEnable = Boolean.parseBoolean(properties.getProperty(TLS_TEST_MODE_ENABLE, String.valueOf(tlsTestModeEnable)));
        tlsServerNeedClientAuth = properties.getProperty(TLS_SERVER_NEED_CLIENT_AUTH, tlsServerNeedClientAuth);
        tlsServerKeyPath = properties.getProperty(TLS_SERVER_KEYPATH, tlsServerKeyPath);
        tlsServerKeyPassword = properties.getProperty(TLS_SERVER_KEYPASSWORD, tlsServerKeyPassword);
        tlsServerCertPath = properties.getProperty(TLS_SERVER_CERTPATH, tlsServerCertPath);
        tlsServerAuthClient = Boolean.parseBoolean(properties.getProperty(TLS_SERVER_AUTHCLIENT, String.valueOf(tlsServerAuthClient)));
        tlsServerTrustCertPath = properties.getProperty(TLS_SERVER_TRUSTCERTPATH, tlsServerTrustCertPath);

        tlsClientKeyPath = properties.getProperty(TLS_CLIENT_KEYPATH, tlsClientKeyPath);
        tlsClientKeyPassword = properties.getProperty(TLS_CLIENT_KEYPASSWORD, tlsClientKeyPassword);
        tlsClientCertPath = properties.getProperty(TLS_CLIENT_CERTPATH, tlsClientCertPath);
        tlsClientAuthServer = Boolean.parseBoolean(properties.getProperty(TLS_CLIENT_AUTHSERVER, String.valueOf(tlsClientAuthServer)));
        tlsClientTrustCertPath = properties.getProperty(TLS_CLIENT_TRUSTCERTPATH, tlsClientTrustCertPath);
    }

    private static void logTheFinalUsedTlsConfig() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Log the final used tls related configuration");
            LOGGER.info("{} = {}", TLS_TEST_MODE_ENABLE, tlsTestModeEnable);
            LOGGER.info("{} = {}", TLS_SERVER_NEED_CLIENT_AUTH, tlsServerNeedClientAuth);
            LOGGER.info("{} = {}", TLS_SERVER_KEYPATH, tlsServerKeyPath);
            LOGGER.info("{} = {}", TLS_SERVER_KEYPASSWORD, tlsServerKeyPassword);
            LOGGER.info("{} = {}", TLS_SERVER_CERTPATH, tlsServerCertPath);
            LOGGER.info("{} = {}", TLS_SERVER_AUTHCLIENT, tlsServerAuthClient);
            LOGGER.info("{} = {}", TLS_SERVER_TRUSTCERTPATH, tlsServerTrustCertPath);

            LOGGER.info("{} = {}", TLS_CLIENT_KEYPATH, tlsClientKeyPath);
            LOGGER.info("{} = {}", TLS_CLIENT_KEYPASSWORD, tlsClientKeyPassword);
            LOGGER.info("{} = {}", TLS_CLIENT_CERTPATH, tlsClientCertPath);
            LOGGER.info("{} = {}", TLS_CLIENT_AUTHSERVER, tlsClientAuthServer);
            LOGGER.info("{} = {}", TLS_CLIENT_TRUSTCERTPATH, tlsClientTrustCertPath);
        }
    }

    private static ClientAuth parseClientAuthMode(String authMode) {
        if (null == authMode || authMode.trim().isEmpty()) {
            return ClientAuth.NONE;
        }

        for (ClientAuth clientAuth : ClientAuth.values()) {
            if (clientAuth.name().equals(authMode.toUpperCase())) {
                return clientAuth;
            }
        }

        return ClientAuth.NONE;
    }

    /**
     * Determine if a string is not {@code null} or {@link String#isEmpty()} returns {@code false}.
     */
    private static boolean isNotEmpty(String s) {
        return s != null && !s.isEmpty();
    }
}
