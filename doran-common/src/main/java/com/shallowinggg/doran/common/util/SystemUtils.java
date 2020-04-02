package com.shallowinggg.doran.common.util;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * @author shallowinggg
 */
public final class SystemUtils {
    private static final int IPV4_LENGTH = 4;

    public static int getPid() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        // format: "pid@hostname"
        String name = runtime.getName();
        try {
            return Integer.parseInt(name.substring(0, name.indexOf('@')));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Obtain any ipv4 address that available and valid.
     * <p>
     * This method may first search public ip, if fail,
     * then it may return intranet ip if so.
     * <p>
     * If none, this method will return null.
     *
     * @return ip address for this machine
     */
    public static byte[] getIp() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip;
            byte[] internalIp = null;
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    ip = addresses.nextElement();
                    if (ip instanceof Inet4Address) {
                        byte[] ipBytes = ip.getAddress();
                        if (ipCheck(ipBytes)) {
                            if (!isInternalIp(ipBytes)) {
                                return ipBytes;
                            } else if (internalIp == null) {
                                internalIp = ipBytes;
                            }
                        }
                    }
                }
            }

            return internalIp;
        } catch (SocketException e) {
            throw new IllegalStateException("Can't find local ip", e);
        }
    }

    private static boolean ipCheck(byte[] ip) {
        if (ip.length != IPV4_LENGTH) {
            throw new IllegalArgumentException("illegal ipv4 bytes: " + Arrays.toString(ip));
        }

        // class A network
        if (ip[0] >= (byte) 1 && ip[0] <= (byte) 126) {
            if (ip[1] == (byte) 1 && ip[2] == (byte) 1 && ip[3] == (byte) 1) {
                return false;
            }
            return ip[1] != (byte) 0 || ip[2] != (byte) 0 || ip[3] != (byte) 0;
        } else if (ip[0] <= (byte) 191) {
            // class B network
            if (ip[2] == (byte) 1 && ip[3] == (byte) 1) {
                return false;
            }
            return ip[2] != (byte) 0 || ip[3] != (byte) 0;
        } else if (ip[0] <= (byte) 223) {
            // class C network
            if (ip[3] == (byte) 1) {
                return false;
            }
            return ip[3] != (byte) 0;
        }
        return false;
    }

    /**
     * Determine if the given ip is a intranet ip.
     *
     * @param ip the ip to be check
     * @return {@code true} if the given ip is intranet ip, otherwise return {@code false}
     */
    public static boolean isInternalIp(byte[] ip) {
        if (ip.length != IPV4_LENGTH) {
            throw new IllegalArgumentException("illegal ipv4 bytes: " + Arrays.toString(ip));
        }

        //10.0.0.0~10.255.255.255
        //172.16.0.0~172.31.255.255
        //192.168.0.0~192.168.255.255
        if (ip[0] == (byte) 10) {
            return true;
        } else if (ip[0] == (byte) 172) {
            return ip[1] >= (byte) 16 && ip[1] <= (byte) 31;
        } else if (ip[0] == (byte) 192) {
            return ip[1] == (byte) 168;
        }
        return false;
    }

    /**
     * Create a fake ip address with current time.
     *
     * @return fake ip
     */
    public static byte[] createFakeIp() {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(System.currentTimeMillis());
        bb.position(4);
        byte[] fakeIp = new byte[4];
        bb.get(fakeIp);
        return fakeIp;
    }

    private SystemUtils() {
    }
}
