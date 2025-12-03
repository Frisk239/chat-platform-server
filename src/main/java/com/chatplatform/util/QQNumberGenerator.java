package com.chatplatform.util;

import com.chatplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * QQ号生成器
 * 生成8位随机数字作为QQ号，确保唯一性
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QQNumberGenerator {

    private final UserRepository userRepository;

    private final SecureRandom random = new SecureRandom();

    // 缓存已生成的QQ号，避免重复查询数据库
    private final Set<String> generatedCache = new HashSet<>();

    // 本地缓存，避免重复生成
    private final ConcurrentHashMap<String, Boolean> localCache = new ConcurrentHashMap<>();

    // 重试锁，防止并发情况下生成相同的QQ号
    private final ReentrantLock generateLock = new ReentrantLock();

    // 最大重试次数
    private static final int MAX_RETRY_COUNT = 100;

    // QQ号位数
    private static final int QQ_NUMBER_LENGTH = 8;

    // 起始QQ号（避免生成太小的数字）
    private static final int MIN_QQ_NUMBER = 10000000;

    // 最大QQ号
    private static final int MAX_QQ_NUMBER = 99999999;

    /**
     * 生成唯一的QQ号
     *
     * @return 生成的QQ号
     * @throws RuntimeException 当超过最大重试次数时抛出异常
     */
    @Transactional
    public String generateQQNumber() {
        generateLock.lock();
        try {
            return doGenerateQQNumber();
        } finally {
            generateLock.unlock();
        }
    }

    /**
     * 执行QQ号生成的核心逻辑
     */
    private String doGenerateQQNumber() {
        int retryCount = 0;

        while (retryCount < MAX_RETRY_COUNT) {
            String qqNumber = generateRandomQQNumber();

            // 检查本地缓存
            if (localCache.containsKey(qqNumber)) {
                retryCount++;
                continue;
            }

            // 检查已生成缓存
            if (generatedCache.contains(qqNumber)) {
                retryCount++;
                continue;
            }

            // 检查数据库
            if (userRepository.existsByQqNumber(qqNumber)) {
                generatedCache.add(qqNumber);
                retryCount++;
                continue;
            }

            // 检查通过，添加到缓存并返回
            localCache.put(qqNumber, true);
            if (localCache.size() > 10000) { // 防止缓存过大
                localCache.clear();
            }

            log.info("成功生成QQ号: {}, 重试次数: {}", qqNumber, retryCount);
            return qqNumber;
        }

        throw new RuntimeException("生成QQ号失败，已达到最大重试次数: " + MAX_RETRY_COUNT);
    }

    /**
     * 生成随机QQ号
     */
    private String generateRandomQQNumber() {
        // 生成8位随机数字，确保大于10000000
        int randomNumber = random.nextInt(MAX_QQ_NUMBER - MIN_QQ_NUMBER + 1) + MIN_QQ_NUMBER;
        return String.format("%08d", randomNumber);
    }

    /**
     * 批量生成QQ号（用于初始化数据）
     *
     * @param count 生成数量
     * @return QQ号列表
     */
    @Transactional
    public java.util.List<String> generateQQNumbers(int count) {
        java.util.List<String> qqNumbers = new java.util.ArrayList<>();

        for (int i = 0; i < count; i++) {
            try {
                String qqNumber = generateQQNumber();
                qqNumbers.add(qqNumber);
            } catch (RuntimeException e) {
                log.error("批量生成QQ号失败，当前已生成: {}/{}", i + 1, count, e);
                break;
            }
        }

        return qqNumbers;
    }

    /**
     * 验证QQ号格式
     *
     * @param qqNumber QQ号
     * @return 是否为有效格式
     */
    public boolean isValidQQNumber(String qqNumber) {
        if (qqNumber == null || qqNumber.length() != QQ_NUMBER_LENGTH) {
            return false;
        }

        try {
            int number = Integer.parseInt(qqNumber);
            return number >= MIN_QQ_NUMBER && number <= MAX_QQ_NUMBER;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 检查QQ号是否可用
     *
     * @param qqNumber QQ号
     * @return 是否可用
     */
    public boolean isQQNumberAvailable(String qqNumber) {
        if (!isValidQQNumber(qqNumber)) {
            return false;
        }

        return !userRepository.existsByQqNumber(qqNumber);
    }

    /**
     * 获取QQ号信息
     *
     * @param qqNumber QQ号
     * @return QQ号信息字符串
     */
    public String getQQNumberInfo(String qqNumber) {
        if (!isValidQQNumber(qqNumber)) {
            return "无效的QQ号格式";
        }

        if (userRepository.existsByQqNumber(qqNumber)) {
            return "QQ号已被使用";
        }

        return "QQ号可用";
    }

    /**
     * 清理缓存
     */
    public void clearCache() {
        generatedCache.clear();
        localCache.clear();
        log.info("QQ号生成器缓存已清理");
    }

    /**
     * 获取缓存统计信息
     */
    public String getCacheStatistics() {
        return String.format("本地缓存大小: %d, 生成缓存大小: %d",
                           localCache.size(), generatedCache.size());
    }

    /**
     * 预热缓存（生成一些QQ号备用）
     *
     * @param count 预热数量
     */
    public void warmUpCache(int count) {
        generateLock.lock();
        try {
            int successCount = 0;
            for (int i = 0; i < count; i++) {
                try {
                    String qqNumber = generateRandomQQNumber();
                    if (!userRepository.existsByQqNumber(qqNumber) &&
                        !localCache.containsKey(qqNumber) &&
                        !generatedCache.contains(qqNumber)) {
                        localCache.put(qqNumber, true);
                        successCount++;
                    }
                } catch (Exception e) {
                    log.warn("预热缓存时生成QQ号失败: {}", e.getMessage());
                }
            }
            log.info("预热缓存完成，成功生成 {} 个备用QQ号", successCount);
        } finally {
            generateLock.unlock();
        }
    }

    /**
     * 生成指定前缀的QQ号（特殊用途）
     *
     * @param prefix 前缀
     * @return 带前缀的QQ号
     */
    public String generateCustomQQNumber(String prefix) {
        generateLock.lock();
        try {
            int retryCount = 0;
            while (retryCount < MAX_RETRY_COUNT) {
                String randomPart = generateRandomQQNumber().substring(prefix.length());
                String qqNumber = prefix + randomPart;

                if (isQQNumberAvailable(qqNumber)) {
                    localCache.put(qqNumber, true);
                    return qqNumber;
                }
                retryCount++;
            }
            throw new RuntimeException("生成自定义QQ号失败");
        } finally {
            generateLock.unlock();
        }
    }

    /**
     * 获取当前时间戳作为QQ号的一部分（测试用）
     */
    public String generateTimestampQQNumber() {
        generateLock.lock();
        try {
            long timestamp = System.currentTimeMillis();
            String timestampStr = String.valueOf(timestamp);

            // 取时间戳的后几位作为QQ号的基础
            String baseNumber = timestampStr.substring(Math.max(0, timestampStr.length() - QQ_NUMBER_LENGTH));

            // 如果不足8位，前面补0
            while (baseNumber.length() < QQ_NUMBER_LENGTH) {
                baseNumber = "0" + baseNumber;
            }

            // 如果超过8位，截取后8位
            if (baseNumber.length() > QQ_NUMBER_LENGTH) {
                baseNumber = baseNumber.substring(baseNumber.length() - QQ_NUMBER_LENGTH);
            }

            return baseNumber;
        } finally {
            generateLock.unlock();
        }
    }
}