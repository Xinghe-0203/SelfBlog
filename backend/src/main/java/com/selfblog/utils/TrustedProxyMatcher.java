package com.selfblog.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 可信代理（trusted proxy）判定（S2）。
 *
 * <p>此前的实现把整个私有网段（{@code 10.*}、{@code 192.168.*}、{@code 172.16-31.*}、
 * {@code 127.0.0.1}、{@code ::1}）一律视为可信代理。问题在于：用 {@code docker run -p 8825:8825}
 * 发布端口时，容器看到的直连对端是网桥网关 {@code 172.17.0.1} —— 落在私有网段里 ——
 * 于是<b>任何</b>客户端都能伪造 {@code X-Forwarded-For}，进而控制限流桶、审计日志里的
 * 客户端 IP，以及（task-3 之后）访客哈希的输入。</p>
 *
 * <p>因此改为显式可配置的白名单，默认只信任环回地址。前置于应用的宿主机反向代理需要
 * 运维在 {@code app.trusted-proxies} / {@code TRUSTED_PROXY_CIDRS} 里显式登记。
 * 若不做登记，客户端 IP 会退化为直连对端地址 —— 这是安全的失败方向。</p>
 */
public final class TrustedProxyMatcher {

    private static final Logger log = LoggerFactory.getLogger(TrustedProxyMatcher.class);

    /** 默认只信任环回地址（IPv4 + IPv6 等价写法）。 */
    public static final String DEFAULT_TRUSTED_PROXIES = "127.0.0.1/32,::1/128";

    /**
     * 只接受 IP 字面量字符，避免把配置值交给 {@link InetAddress#getByName} 触发 DNS 解析。
     */
    private static final Pattern IP_LITERAL_PATTERN = Pattern.compile("^[0-9a-fA-F:.]+$");

    private final List<CidrBlock> blocks;

    private TrustedProxyMatcher(List<CidrBlock> blocks) {
        this.blocks = blocks;
    }

    /**
     * 解析逗号分隔的 IP/CIDR 列表。空值或全部无效时回退到 {@link #DEFAULT_TRUSTED_PROXIES}，
     * 绝不退化成"全部信任"。
     */
    public static TrustedProxyMatcher parse(String csv) {
        List<CidrBlock> parsed = new ArrayList<>();
        String source = (csv == null || csv.isBlank()) ? DEFAULT_TRUSTED_PROXIES : csv;
        for (String token : source.split(",")) {
            String value = token.trim();
            if (value.isEmpty()) {
                continue;
            }
            CidrBlock block = CidrBlock.parse(value);
            if (block == null) {
                log.warn("[TrustedProxy] 忽略无法解析的条目: {}", value);
                continue;
            }
            parsed.add(block);
        }
        if (parsed.isEmpty()) {
            log.warn("[TrustedProxy] 配置里没有有效条目，回退到默认环回白名单: {}", DEFAULT_TRUSTED_PROXIES);
            for (String token : DEFAULT_TRUSTED_PROXIES.split(",")) {
                CidrBlock block = CidrBlock.parse(token.trim());
                if (block != null) {
                    parsed.add(block);
                }
            }
        }
        return new TrustedProxyMatcher(parsed);
    }

    /** {@code addr} 是否落在任一白名单网段内。 */
    public boolean matches(String addr) {
        if (addr == null || addr.isEmpty()) {
            return false;
        }
        String trimmed = addr.trim();
        if (!IP_LITERAL_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        byte[] candidate;
        try {
            candidate = InetAddress.getByName(trimmed).getAddress();
        } catch (UnknownHostException e) {
            return false;
        }
        for (CidrBlock block : blocks) {
            if (block.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    /** 单个 CIDR 网段。 */
    private static final class CidrBlock {

        private final byte[] network;
        private final int prefixLength;

        private CidrBlock(byte[] network, int prefixLength) {
            this.network = network;
            this.prefixLength = prefixLength;
        }

        static CidrBlock parse(String value) {
            String addressPart = value;
            int prefixLength = -1;
            int slash = value.indexOf('/');
            if (slash >= 0) {
                addressPart = value.substring(0, slash);
                String prefixPart = value.substring(slash + 1).trim();
                try {
                    prefixLength = Integer.parseInt(prefixPart);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            addressPart = addressPart.trim();
            if (addressPart.isEmpty() || !IP_LITERAL_PATTERN.matcher(addressPart).matches()) {
                return null;
            }
            byte[] address;
            try {
                address = InetAddress.getByName(addressPart).getAddress();
            } catch (UnknownHostException e) {
                return null;
            }
            int maxPrefix = address.length * 8;
            if (prefixLength < 0) {
                prefixLength = maxPrefix;
            }
            if (prefixLength < 0 || prefixLength > maxPrefix) {
                return null;
            }
            return new CidrBlock(address, prefixLength);
        }

        boolean contains(byte[] candidate) {
            if (candidate.length != network.length) {
                return false;
            }
            int fullBytes = prefixLength / 8;
            for (int i = 0; i < fullBytes; i++) {
                if (candidate[i] != network[i]) {
                    return false;
                }
            }
            int remainingBits = prefixLength % 8;
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (8 - remainingBits) & 0xFF;
            return (candidate[fullBytes] & mask) == (network[fullBytes] & mask);
        }
    }

    /** 供日志/诊断使用的规范化文本。 */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (CidrBlock block : blocks) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            try {
                builder.append(InetAddress.getByAddress(block.network).getHostAddress())
                        .append('/').append(block.prefixLength);
            } catch (UnknownHostException e) {
                builder.append("invalid");
            }
        }
        return builder.toString().toLowerCase(Locale.ROOT);
    }
}
