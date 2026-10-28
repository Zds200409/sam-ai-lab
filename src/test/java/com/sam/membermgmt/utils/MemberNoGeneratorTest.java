package com.sam.membermgmt.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberNoGeneratorTest {

    @Test
    void testGenerateFormat() {
        MemberNoGenerator generator = new MemberNoGenerator();
        String no = generator.generate();

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertTrue(no.startsWith("M" + today),
                "会员编号应以 M+日期 开头，实际: " + no);
        assertEquals(13, no.length(),
                "会员编号应为 M + 8 位日期 + 4 位序号 = 13 字符，实际: " + no);
        assertTrue(no.substring(9).matches("\\d{4}"),
                "末 4 位应为数字序号，实际: " + no.substring(9));
    }

    @Test
    void testSequenceIncrement() {
        MemberNoGenerator generator = new MemberNoGenerator();
        String first = generator.generate();
        String second = generator.generate();

        assertNotEquals(first, second, "连续调用应返回不同编号");
        int seq1 = Integer.parseInt(first.substring(9));
        int seq2 = Integer.parseInt(second.substring(9));
        assertEquals(seq1 + 1, seq2, "序号应递增 1");
    }

    @Test
    void testPaddingWidth() {
        MemberNoGenerator generator = new MemberNoGenerator();
        String no = generator.generate();

        // 当日序号至少为 4 位，不足补 0
        String seqStr = no.substring(9);
        assertEquals(4, seqStr.length(), "序号位数应为 4");
        assertTrue(seqStr.startsWith("0") || Integer.parseInt(seqStr) > 0,
                "序号首位是 0 或首位非 0 的真实数字（不允许负号或字母）");
    }
}