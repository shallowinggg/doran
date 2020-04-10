package com.shallowinggg.doran.server.web.service;

import com.shallowinggg.doran.server.web.entity.ClientMetadata;
import io.netty.channel.Channel;

/**
 * @author shallowinggg
 */
public interface ClientService {

    /**
     * Determine if client has registered to this server.
     *
     * @param clientId the id of client
     * @return {@code true} if client has registered, otherwise return {@code false}
     */
    boolean hasClient(String clientId);

    /**
     * Get the {@link ClientMetadata} of client if has.
     *
     * @param clientId the id of client
     * @return ClientMetadata
     */
    ClientMetadata getClientMetaInfo(String clientId);

    /**
     * Register client with its base metadata and {@link Channel} that
     * communicates with server.
     *
     * @param clientMetadata base metadata for client
     * @param channel channel that client communicates with server
     */
    void registerClient(ClientMetadata clientMetadata, Channel channel);

    /**
     * Scan inactive clients and remove them.
     */
    void scanInactiveClient();
}
