package com.shallowinggg.doran.client.resolver;

/**
 * Check an arbitrary string that represents the name
 * of an endpoint into an address and port.
 *
 * @author shallowinggg
 */
public interface InetAddressChecker {

    /**
     * Resolves the specified name into an address.
     *
     * @param address the net address to check
     * @throws InvalidInetAddressException if check net address fail
     */
    void check(String address) throws InvalidInetAddressException;
}
