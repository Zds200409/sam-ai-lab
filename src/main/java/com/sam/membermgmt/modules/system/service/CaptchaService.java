package com.sam.membermgmt.modules.system.service;

import com.sam.membermgmt.modules.system.dto.CaptchaVO;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形验证码服务
 *
 * <p>纯 Java AWT 绘制验证码图片，验证码以「key → (code, 过期时间)」结构存储于内存，
 * 有效期 5 分钟，验证成功后立即失效，可防止脚本暴力登录。</p>
 */
@Service
public class CaptchaService {

    /** 图片尺寸 */
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    /** 验证码位数 */
    private static final int LENGTH = 4;
    /** 有效期 5 分钟 */
    private static final long EXPIRE_MS = 5 * 60 * 1000L;
    /** 字符集：去除易混淆的 0/O/1/I/l */
    private static final char[] CHARS =
            "23456789ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz".toCharArray();

    private static class Item {
        final String code;
        final long expireAt;

        Item(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }

    private final ConcurrentHashMap<String, Item> cache = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /**
     * 生成验证码
     *
     * @return 验证码 key 与 Base64 图片
     */
    public CaptchaVO generate() {
        String code = randomCode();
        String key = UUID.randomUUID().toString().replace("-", "");
        cache.put(key, new Item(code, System.currentTimeMillis() + EXPIRE_MS));
        // 惰性清理已过期验证码，防止内存膨胀
        cache.entrySet().removeIf(e -> e.getValue().expireAt < System.currentTimeMillis());
        CaptchaVO vo = new CaptchaVO();
        vo.setCaptchaKey(key);
        vo.setCaptchaImage("data:image/png;base64," + Base64.getEncoder().encodeToString(toPngBytes(code)));
        return vo;
    }

    /**
     * 校验验证码（不区分大小写）
     *
     * @param key  验证码标识
     * @param code 用户输入的验证码
     * @return 是否通过
     */
    public boolean validate(String key, String code) {
        if (key == null || code == null) {
            return false;
        }
        Item item = cache.get(key);
        if (item == null || item.expireAt < System.currentTimeMillis()) {
            return false;
        }
        boolean matched = item.code.equalsIgnoreCase(code.trim());
        if (matched) {
            cache.remove(key); // 一次性使用
        }
        return matched;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHARS[random.nextInt(CHARS.length)]);
        }
        return sb.toString();
    }

    private byte[] toPngBytes(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        // 背景
        g.setColor(new Color(245, 247, 250));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        // 干扰线
        for (int i = 0; i < 5; i++) {
            g.setColor(randomGray(160, 220));
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            g.drawLine(x1, y1, x1 + random.nextInt(40) - 20, y1 + random.nextInt(30) - 15);
        }
        // 干扰点
        for (int i = 0; i < 60; i++) {
            g.setColor(randomGray(150, 230));
            g.fillRect(random.nextInt(WIDTH), random.nextInt(HEIGHT), 1, 1);
        }
        // 字符
        g.setFont(new Font("Arial", Font.BOLD, 26));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(randomGray(30, 140));
            double angle = (random.nextDouble() - 0.5) * 0.5;
            int cx = 16 + i * 26;
            g.rotate(angle, cx, 28);
            g.drawString(String.valueOf(code.charAt(i)), 10 + i * 26, 29);
            g.rotate(-angle, cx, 28);
        }
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new RuntimeException("生成验证码失败", e);
        }
        return out.toByteArray();
    }

    private Color randomGray(int min, int max) {
        int v = min + random.nextInt(max - min);
        return new Color(v, v, v);
    }
}
