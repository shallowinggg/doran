package com.shallowinggg.doran.client.resolver;

import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

/**
 * Default implementation for interface {@link InetAddressChecker}.
 * <p>
 * This class will split address into hostname and port by
 * {@link #SEPARATOR}. Simultaneously，it will invoke
 * {@link InetAddress#getByName(String)} and
 * {@link Integer#parseInt(String)} method to check if the
 * given hostname and port is valid.
 *
 * @author shallowinggg
 * @see InetAddress#getByName(String)
 * @see Integer#parseInt(String)
 */
public class DefaultInetAddressChecker implements InetAddressChecker {
    private static final String SEPARATOR = ":";

    @Override
    public void check(String address) throws InvalidInetAddressException {
        String[] s = address.split(SEPARATOR);
        if (s.length == 2) {
            String hostname = s[0];
            String port = s[1];
            try {
                if (System.getSecurityManager() != null) {
                    AccessController.doPrivileged((PrivilegedExceptionAction<InetAddress>)
                            () -> InetAddress.getByName(hostname));
                } else {
                    InetAddress.getByName(address);
                }

                Integer.parseInt(port);
            } catch (Throwable t) {
                throw new InvalidInetAddressException(t);
            }
        }
        throw new InvalidInetAddressException("Expected 'hostname:port', but " + address);
    }
}
